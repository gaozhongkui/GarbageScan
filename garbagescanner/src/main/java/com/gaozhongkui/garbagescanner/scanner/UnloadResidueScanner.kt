package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import com.gaozhongkui.garbagescanner.base.BaseScanner
import com.gaozhongkui.garbagescanner.callback.IScannerCallback
import com.gaozhongkui.garbagescanner.database.GarbageManagerDB
import com.gaozhongkui.garbagescanner.model.GarbagePathInfo
import com.gaozhongkui.garbagescanner.model.ScanItemType
import com.gaozhongkui.garbagescanner.model.SortScannerInfo
import com.gaozhongkui.garbagescanner.model.UnloadResidueInfo
import com.gaozhongkui.garbagescanner.utils.CommonUtil
import kotlinx.coroutines.*
import java.io.File
import java.util.Collections

/**
 * 卸载的垃圾扫描
 */
class UnloadResidueScanner : BaseScanner {
    private var isStopScanner = false
    private var fileScanner: FileScanner? = null
    private val unloadResidueList = Collections.synchronizedList(mutableListOf<UnloadResidueInfo>())

    @OptIn(DelicateCoroutinesApi::class)
    override fun startScan(cxt: Context, callback: IScannerCallback) {
        isStopScanner = false
        callback.onStart()
        GlobalScope.launch(Dispatchers.IO) {
            val pathInfoList = getAppUnInstalled(cxt)
            if (isStopScanner || !isActive) {
                return@launch
            }
            scanUnloadResidue(pathInfoList, callback)
        }
    }

    override fun stopScan() {
        isStopScanner = true
        fileScanner?.stopScan()
    }

    /**
     * 开始扫描卸载垃圾文件
     */
    private fun scanUnloadResidue(pathInfoList: List<GarbagePathInfo>, callback: IScannerCallback) {
        //判断如果集合为空时，则直接返回
        if (pathInfoList.isEmpty()) {
            val info = SortScannerInfo(ScanItemType.UNLOAD_RESIDUE, emptyList())
            callback.onFinish(info)
            return
        }

        fileScanner = FileScanner()
        fileScanner?.apply {
            setScanPath(CommonUtil.getPathList(pathInfoList))
            setScanParams(null, null, 4, -1, true)
            startScan(object : FileScanner.ScanCallback {
                override fun onStart() {
                    unloadResidueList.clear()
                }

                override fun onFind(threadId: Long, path: String?, size: Long, modify: Long) {
                    path?.let {
                        if (CommonUtil.isNoScannerFile(path)) {
                            return
                        }
                        val file = File(it)
                        val info = UnloadResidueInfo("", it)
                        info.fileSize = size
                        info.name = file.name
                        unloadResidueList.add(info)
                    }
                }

                override fun onFinish(isCancel: Boolean) {
                    //回调解锁
                    val info = SortScannerInfo(ScanItemType.UNLOAD_RESIDUE, unloadResidueList)
                    info.fileSize = CommonUtil.getTotalFileSize(unloadResidueList)
                    info.selectFileTotalSize = info.fileSize
                    callback.onFinish(info)
                }

            })
        }
    }

    /**
     * 获取未安装的应用
     */
    private fun getAppUnInstalled(cxt: Context): List<GarbagePathInfo> {
        val resultList = mutableListOf<GarbagePathInfo>()
        val garbageDB = GarbageManagerDB.getInstance(cxt)
        val infoList = garbageDB.getDBGarbagePathInfoList()
        val sdRootPath = Environment.getExternalStorageDirectory().absolutePath
        for (info in infoList) {
            //判断如果已经安装了，则直接跳过
            if (CommonUtil.isAppInstalled(cxt, info.packageName)) {
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

}