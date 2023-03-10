package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import com.gaozhongkui.garbagescanner.base.BaseScanner
import com.gaozhongkui.garbagescanner.callback.IScannerCallback
import com.gaozhongkui.garbagescanner.model.ApkFileInfo
import com.gaozhongkui.garbagescanner.model.ScanItemType
import com.gaozhongkui.garbagescanner.model.SortScannerInfo
import com.gaozhongkui.garbagescanner.utils.CommonUtil
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class ApkFileScanner : BaseScanner {
    private var isStopScanner = false
    private var fileScanner: FileScanner? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun startScan(cxt: Context, callback: IScannerCallback) {
        isStopScanner = false
        callback.onStart()
        GlobalScope.launch(Dispatchers.IO) {
            val infoList = queryContentResolverExternal(cxt, callback)
            if (isStopScanner || !isActive) {
                return@launch
            }
            scanApkFile(cxt, infoList, callback)
        }
    }

    override fun stopScan() {
        isStopScanner = true
        fileScanner?.stopScan()
    }

    /**
     * 开始扫描apk文件
     */
    private fun scanApkFile(cxt: Context, existApkFileList: MutableList<ApkFileInfo>, callback: IScannerCallback) {
        fileScanner = FileScanner()
        fileScanner?.apply {
            val sdPath = Environment.getExternalStorageDirectory().absolutePath
            val scanPath = arrayOf(sdPath)
            setScanPath(scanPath)
            setScanParams(arrayOf("apk", "aab", "apks", "xapk"), null, 4, -1, true)
            startScan(object : FileScanner.ScanCallback {
                override fun onStart() {

                }

                override fun onFind(threadId: Long, path: String?, size: Long, modify: Long) {
                    if (size <= 0 || TextUtils.isEmpty(path)) {
                        return
                    }

                    path?.let {
                        val apkFile = File(path)
                        val apkFileInfo = ApkFileInfo(it)
                        apkFileInfo.appIcon = CommonUtil.getApkIcon(cxt, path)
                        apkFileInfo.fileSize = size
                        apkFileInfo.name = apkFile.name
                        //判断如果已经存在了，则直接返回
                        if (existApkFileList.contains(apkFileInfo)) {
                            return
                        }
                        existApkFileList.add(apkFileInfo)
                        //回调找到的APK
                        callback.onFind(apkFileInfo)
                    }
                }

                override fun onFinish(isCancel: Boolean) {
                    //回调解锁
                    val info = SortScannerInfo(ScanItemType.INSTALL_PACKAGE, existApkFileList)
                    info.fileSize = CommonUtil.getTotalFileSize(existApkFileList)
                    info.selectFileTotalSize = info.fileSize
                    callback.onFinish(info)
                }

            })
        }
    }

    /**
     * 先从内容提供者中获取文件
     */
    private fun queryContentResolverExternal(cxt: Context, callback: IScannerCallback): MutableList<ApkFileInfo> {
        val result = Collections.synchronizedList(mutableListOf<ApkFileInfo>())
        val resolver = cxt.contentResolver
        resolver.query(
            MediaStore.Files.getContentUri("external"), arrayOf("_data", "_size"), "_data like ?", arrayOf("%.apk%"), null
        )?.use {
            val moveToFirst = it.moveToFirst()
            if (!moveToFirst) {
                return@use
            }
            do {
                val apkPath = it.getString(0)
                val apkSize = it.getLong(1)
                val apkFile = File(apkPath)
                //判断大小是否0 或者是否不存在
                if (apkSize <= 0 || !apkFile.exists()) {
                    continue
                }
                val apkFileInfo = ApkFileInfo(apkPath)
                apkFileInfo.appIcon = CommonUtil.getApkIcon(cxt, apkPath)
                apkFileInfo.fileSize = apkSize
                apkFileInfo.name = apkFile.name
                result.add(apkFileInfo)
                //回调找到的APK
                callback.onFind(apkFileInfo)
            } while (it.moveToNext() && !isStopScanner)
        }
        return result
    }
}