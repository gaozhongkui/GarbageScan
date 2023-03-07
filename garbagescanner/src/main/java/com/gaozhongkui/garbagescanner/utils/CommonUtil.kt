package com.gaozhongkui.garbagescanner.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.gaozhongkui.garbagescanner.model.GarbagePathInfo
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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

    fun getCpuCoreCount(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    fun copyToFile(inputStream: InputStream, destFile: File): Boolean {
        return try {
            if (destFile.exists()) {
                destFile.delete()
            }
            val out = FileOutputStream(destFile)
            try {
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } >= 0) {
                    out.write(buffer, 0, bytesRead)
                }
            } finally {
                out.flush()
                try {
                    out.fd.sync()
                } catch (ignored: Exception) {
                }
                out.close()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}