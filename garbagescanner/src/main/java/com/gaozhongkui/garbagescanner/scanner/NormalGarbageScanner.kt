package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import com.gaozhongkui.garbagescanner.base.BaseScanner
import com.gaozhongkui.garbagescanner.callback.IScannerCallback
import com.gaozhongkui.garbagescanner.database.GarbageManagerDB
import com.gaozhongkui.garbagescanner.model.GarbagePathInfo
import com.gaozhongkui.garbagescanner.model.NormalGarbageInfo
import com.gaozhongkui.garbagescanner.model.ScanItemType
import com.gaozhongkui.garbagescanner.model.SortScannerInfo
import com.gaozhongkui.garbagescanner.utils.CommonUtil
import kotlinx.coroutines.*
import java.io.File
import java.util.Collections

/**
 * 常规的垃圾清理（系统垃圾、非广告类型、非apk类型）
 */
class NormalGarbageScanner : BaseScanner {
    private var isStopScanner = false
    private var fileScanner: FileScanner? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun startScan(cxt: Context, callback: IScannerCallback) {
        isStopScanner = false
        callback.onStart()
        GlobalScope.launch(Dispatchers.IO) {
            val list = queryContentResolverExternal(cxt, callback)
            if (isStopScanner || !isActive) {
                return@launch
            }
            val pathInfoList = getAppInstalled(cxt)
            scanGarbageFile(pathInfoList, list, callback)
        }
    }

    override fun stopScan() {
        isStopScanner = true
        fileScanner?.stopScan()
    }


    private fun scanGarbageFile(pathInfoList: List<GarbagePathInfo>, existGarbageFileList: MutableList<NormalGarbageInfo>, callback: IScannerCallback) {
        //判断如果集合为空时，则直接返回
        if (pathInfoList.isEmpty()) {
            val info = SortScannerInfo(ScanItemType.OTHER_GARBAGE, existGarbageFileList)
            info.fileSize = CommonUtil.getTotalFileSize(existGarbageFileList)
            info.selectFileTotalSize = info.fileSize
            callback.onFinish(info)
            return
        }


        fileScanner = FileScanner()
        fileScanner?.apply {
            setScanPath(CommonUtil.getPathList(pathInfoList))
            val suffixes = arrayOf("log", "txt", "db")
            setScanParams(suffixes, null, 4, -1, true)
            startScan(object : FileScanner.ScanCallback {
                val scanList = Collections.synchronizedList(mutableListOf<NormalGarbageInfo>())
                override fun onStart() {
                }

                override fun onFind(threadId: Long, path: String?, size: Long, modify: Long) {
                    path?.let {
                        if (CommonUtil.isNoScannerFile(path)) {
                            return
                        }
                        val file = File(it)
                        val info = NormalGarbageInfo(it)
                        info.fileSize = size
                        info.name = file.name
                        scanList.add(info)
                        //回调找到的APK
                        callback.onFind(info)
                    }
                }

                override fun onFinish(isCancel: Boolean) {
                    existGarbageFileList.addAll(scanList)
                    //回调解锁
                    val info = SortScannerInfo(ScanItemType.OTHER_GARBAGE, existGarbageFileList)
                    info.fileSize = CommonUtil.getTotalFileSize(existGarbageFileList)
                    info.selectFileTotalSize = info.fileSize
                    callback.onFinish(info)
                }

            })
        }
    }

    /**
     * 获取已安装的app列表
     */
    private fun getAppInstalled(cxt: Context): List<GarbagePathInfo> {
        val resultList = mutableListOf<GarbagePathInfo>()
        val garbageDB = GarbageManagerDB.getInstance(cxt)
        val infoList = garbageDB.getDBGarbagePathInfoList()
        val sdRootPath = Environment.getExternalStorageDirectory().absolutePath
        for (info in infoList) {
            //判断如果未安装了，则直接跳过
            if (!CommonUtil.isAppInstalled(cxt, info.packageName)) {
                continue
            }
            //判断如果是根目录，则直接返回
            if (TextUtils.equals(sdRootPath, info.filePath)) {
                continue
            }

            if (!File(info.filePath).exists()) {
                continue
            }

            if (isStopScanner) {
                break
            }
            resultList.add(info)
        }
        return resultList
    }

    /**
     * 查找系统的垃圾
     */
    private fun queryContentResolverExternal(cxt: Context, callback: IScannerCallback): MutableList<NormalGarbageInfo> {
        val result = mutableListOf<NormalGarbageInfo>()
        val resolver = cxt.contentResolver
        resolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf("_data", "_size"),
            "_data like '%cache%' or _data like '%.thumbnails%' or _data == 0 ",
            null,
            null
        )?.use {
            val moveToFirst = it.moveToFirst()
            if (!moveToFirst) {
                return@use
            }
            do {
                val filePath = it.getString(0)
                val fileSize = it.getLong(1)
                val file = File(filePath)
                //判断大小是否0 或者是否不存在
                if (fileSize <= 0 || !file.exists()) {
                    continue
                }
                val fileInfo = NormalGarbageInfo(filePath)
                fileInfo.fileSize = fileSize
                fileInfo.name = file.name
                result.add(fileInfo)
                //回调找到的垃圾文件
                callback.onFind(fileInfo)
            } while (it.moveToNext() && !isStopScanner)
        }
        return result
    }
}