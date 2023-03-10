package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import com.gaozhongkui.garbagescanner.base.BaseScanner
import com.gaozhongkui.garbagescanner.callback.IScannerCallback
import com.gaozhongkui.garbagescanner.model.AppCacheInfo
import com.gaozhongkui.garbagescanner.model.ScanItemType
import com.gaozhongkui.garbagescanner.model.SortScannerInfo
import com.gaozhongkui.garbagescanner.utils.AppPackageUtils
import com.gaozhongkui.garbagescanner.utils.CommonUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

/**
 * 应用缓存扫描
 */
class AppCacheScanner : BaseScanner {
    private var isStopScanner = false

    @OptIn(DelicateCoroutinesApi::class)
    override fun startScan(cxt: Context, callback: IScannerCallback) {
        isStopScanner = false
        callback.onStart()
        GlobalScope.launch(Dispatchers.IO) {
            val packageManager = cxt.packageManager
            val installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            if (installedApplications.isEmpty()) {
                return@launch
            }

            val resultDataList = mutableListOf<AppCacheInfo>()

            for (appInfo in installedApplications) {
                //判断如果为系统应用时，则直接跳过
                if (CommonUtil.isSystemApp(appInfo)) {
                    continue
                }
                //判断如果为自己包时，则直接跳过
                if (TextUtils.equals(appInfo.packageName, cxt.packageName)) {
                    continue
                }
                //退出循环
                if (isStopScanner) {
                    return@launch
                }
                val countDownLatch = CountDownLatch(1)
                AppPackageUtils.getAppCacheSize(cxt, appInfo.packageName) {
                    val appCacheInfo = AppCacheInfo(appInfo.packageName, packageManager.getApplicationIcon(appInfo))
                    appCacheInfo.fileSize = it
                    appCacheInfo.name = packageManager.getApplicationLabel(appInfo).toString()
                    resultDataList.add(appCacheInfo)
                    countDownLatch.countDown()
                }
                countDownLatch.await()
            }
            //回调解锁
            val info = SortScannerInfo(ScanItemType.CACHE_GARBAGE, resultDataList)
            info.fileSize = CommonUtil.getTotalFileSize(resultDataList)
            info.selectFileTotalSize = info.fileSize
            callback.onFinish(info)
        }
    }


    override fun stopScan() {
        isStopScanner = true
    }


}