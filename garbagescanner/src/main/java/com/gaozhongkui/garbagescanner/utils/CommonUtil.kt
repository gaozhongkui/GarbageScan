package com.gaozhongkui.garbagescanner.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Environment
import com.gaozhongkui.garbagescanner.model.GarbagePathInfo
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object CommonUtil {
    fun isAppInstalled(context: Context, pkgName: String?): Boolean {
        val pm = context.packageManager
        if (pm != null) {
            try {
                pm.getPackageInfo(pkgName!!, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return false
    }

    fun isSystemApp(info: ApplicationInfo): Boolean {
        return info.flags and 1 > 0
    }


    fun getPathList(pathInfoList: List<GarbagePathInfo>): Array<String> {
        val resultArray = mutableListOf<String>()
        for (info in pathInfoList) {
            //判断如果已经有了，则不需要重复添加
            if (resultArray.contains(info.filePath)) {
                continue
            }
            resultArray.add(info.filePath)
        }
        return resultArray.toTypedArray()
    }
}