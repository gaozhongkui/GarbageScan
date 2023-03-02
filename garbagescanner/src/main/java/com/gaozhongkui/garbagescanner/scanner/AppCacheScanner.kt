package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import android.content.pm.PackageManager
import com.gaozhongkui.garbagescanner.callback.IScannerCallback
import com.gaozhongkui.garbagescanner.utils.AppPackageUtils
import com.gaozhongkui.garbagescanner.utils.CommonUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

/**
 * 应用缓冲扫描
 */
class AppCacheScanner : BaseScanner {
    private var isStopScanner = false

    @OptIn(DelicateCoroutinesApi::class)
    override fun startScan(cxt: Context, callback: IScannerCallback) {
        isStopScanner = false
        GlobalScope.launch(Dispatchers.IO) {
            val packageManager = cxt.packageManager
            val installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            if (installedApplications.isEmpty()) {
                return@launch
            }

            for (appInfo in installedApplications) {
                //判断如果为系统应用时，则直接跳过
                if (CommonUtil.isSystemApp(appInfo)) {
                    continue
                }
                //退出循环
                if (isStopScanner) {
                    return@launch
                }
                val countDownLatch = CountDownLatch(1)
                AppPackageUtils.getAppCacheSize(cxt, appInfo.packageName) {
                    countDownLatch.countDown()
                }
                countDownLatch.await()
            }
        }
    }


    override fun stopScan() {
        isStopScanner = true
    }


}