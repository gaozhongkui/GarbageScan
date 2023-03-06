package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import com.gaozhongkui.garbagescanner.base.BaseScanner
import com.gaozhongkui.garbagescanner.callback.IScannerCallback
import com.gaozhongkui.garbagescanner.database.GarbageManagerDB
import com.gaozhongkui.garbagescanner.model.GarbagePathInfo
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
                        val file = File(it)
                        val info = UnloadResidueInfo("", it)
                        info.fileSize = size
                        info.name = file.name
                        unloadResidueList.add(info)
                    }
                }

                override fun onFinish(isCancel: Boolean) {
                    callback.onFinish(unloadResidueList)
                }

            })
        }
    }

    /**
     * 获取未安装的应用
     */
    private fun getAppUnInstalled(cxt: Context): List<GarbagePathInfo> {
        val resultList = mutableListOf<GarbagePathInfo>()
        val garbageDB = GarbageManagerDB(cxt)
        val infoList = garbageDB.getDBGarbagePathInfoList()
        for (info in infoList) {
            //判断如果已经安装了，则直接跳过
            if (CommonUtil.isAppInstalled(cxt, info.packageName)) {
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