package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import com.gaozhongkui.garbagescanner.base.BaseScanner
import com.gaozhongkui.garbagescanner.callback.IScannerCallback
import com.gaozhongkui.garbagescanner.database.GarbageManagerDB
import com.gaozhongkui.garbagescanner.model.AdGarbageInfo
import com.gaozhongkui.garbagescanner.model.GarbagePathInfo
import com.gaozhongkui.garbagescanner.model.ScanItemType
import com.gaozhongkui.garbagescanner.model.SortScannerInfo
import com.gaozhongkui.garbagescanner.utils.CommonUtil
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

/**
 * 卸载的垃圾扫描
 */
class AdGarbageScanner : BaseScanner {
    private var isStopScanner = false
    private var fileScanner: FileScanner? = null
    private val adGarbageList = Collections.synchronizedList(ArrayList<AdGarbageInfo>())

    @OptIn(DelicateCoroutinesApi::class)
    override fun startScan(cxt: Context, callback: IScannerCallback) {
        isStopScanner = false
        callback.onStart()
        GlobalScope.launch(Dispatchers.IO) {
            val pathInfoList = getAdAppInstalled(cxt)
            if (isStopScanner || !isActive) {
                return@launch
            }
            scanUnloadResidue(cxt, pathInfoList, callback)
        }
    }

    override fun stopScan() {
        isStopScanner = true
        fileScanner?.stopScan()
    }

    /**
     * 开始扫描卸载垃圾文件
     */
    private fun scanUnloadResidue(cxt: Context, pathInfoList: List<GarbagePathInfo>, callback: IScannerCallback) {
        //判断如果集合为空时，则直接返回
        if (pathInfoList.isEmpty()) {
            val info = SortScannerInfo(ScanItemType.AD_GARBAGE, emptyList())
            callback.onFinish(info)
            return
        }
        adGarbageList.clear()
        fileScanner = FileScanner()
        val patList = mutableListOf<String>()
        val totalFileSize = AtomicLong()
        //遍历数据
        for (info in pathInfoList) {
            //判断是否退出
            if (isStopScanner) {
                return
            }
            val countDownLatch = CountDownLatch(1)
            fileScanner?.apply {
                setScanPath(CommonUtil.getPathList(pathInfoList))
                setScanParams(null, null, 2, -1, true)
                startScan(object : FileScanner.ScanCallback {
                    override fun onStart() {
                        patList.clear()
                        totalFileSize.set(0)
                    }

                    override fun onFind(threadId: Long, path: String?, size: Long, modify: Long) {
                        path?.let {
                            patList.add(it)
                            totalFileSize.addAndGet(size)
                        }
                    }

                    override fun onFinish(isCancel: Boolean) {
                        val itemInfo = AdGarbageInfo(info.packageName, patList, CommonUtil.getAppIcon(cxt, info.packageName))
                        itemInfo.fileSize = totalFileSize.get()
                        itemInfo.name = CommonUtil.getAppLabel(cxt, info.packageName).toString()
                        adGarbageList.add(itemInfo)
                        callback.onFind(info)
                        countDownLatch.countDown()
                    }

                })
            }
            countDownLatch.await()
        }

        //回调解锁
        val info = SortScannerInfo(ScanItemType.AD_GARBAGE, adGarbageList)
        info.fileSize = CommonUtil.getTotalFileSize(adGarbageList)
        info.selectFileTotalSize = info.fileSize
        callback.onFinish(info)
    }

    /**
     * 获取已安装的广告应用
     */
    private fun getAdAppInstalled(cxt: Context): List<GarbagePathInfo> {
        val resultList = mutableListOf<GarbagePathInfo>()
        val garbageDB = GarbageManagerDB.getInstance(cxt)
        val sdRootPath = Environment.getExternalStorageDirectory().absolutePath
        val infoList = garbageDB.getAdGarbagePathInfoList()
        for (info in infoList) {
            //判断如果未安装了，则直接跳过
            if (!CommonUtil.isAppInstalled(cxt, info.packageName)) {
                continue
            }

            if (resultList.contains(info)) {
                continue
            }

            //判断如果是根目录，则直接返回
            if (TextUtils.equals(sdRootPath, info.filePath)) {
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