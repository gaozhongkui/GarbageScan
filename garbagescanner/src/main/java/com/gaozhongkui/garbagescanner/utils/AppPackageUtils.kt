package com.gaozhongkui.garbagescanner.utils

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.IPackageStatsObserver
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageStats
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.TextUtils


object AppPackageUtils {

    /**
     * 获取应用的缓冲大小
     */
    fun getAppCacheSize(cxt: Context, packageInfo: PackageInfo, sizeCallback: (Long) -> Unit) {
        val packageManager: PackageManager = cxt.packageManager
        //判断如果大于等于8.0时，则进行特殊处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val applicationInfo = packageManager.getApplicationInfo(packageInfo.packageName, 0)
            val storageStatsManager = cxt.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            try {
                var cacheBytes = storageStatsManager.queryStatsForUid(
                    applicationInfo.storageUuid, applicationInfo.uid
                ).cacheBytes
                if (cacheBytes > 0 && cacheBytes shr 20 < 2) {
                    val valueOf = applicationInfo.uid.toString()
                    if (TextUtils.isEmpty(valueOf) || valueOf.length <= 2) {
                        cacheBytes += 4194304
                    } else {
                        cacheBytes += (valueOf.substring(valueOf.length - 1).toInt() shl 20).toLong() + (valueOf.substring(valueOf.length - 2)
                            .toInt() shl 16).toLong()
                    }
                }
                sizeCallback.invoke(cacheBytes)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val method = packageManager.javaClass.getMethod(
                "getPackageSizeInfo", String::class.java, IPackageStatsObserver::class.java
            )
            method.invoke(packageManager, packageInfo.packageName, object : IPackageStatsObserver.Stub() {
                override fun onGetStatsCompleted(packageStats: PackageStats, succeeded: Boolean) {
                    if (succeeded && packageStats.cacheSize > 0) {
                        sizeCallback.invoke(packageStats.cacheSize)
                    }
                }
            })
        }
    }


    /**
     * 判断是否有OPSTR_GET_USAGE_STATS权限
     */
    fun hasPermissionToReadNetworkStats(cxt: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val appOps = cxt.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), cxt.packageName
        )
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true
        }
        return false
    }

    /**
     * 跳转到授权页面
     */
    fun requestReadNetworkStats(cxt: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        cxt.startActivity(intent)
    }
}