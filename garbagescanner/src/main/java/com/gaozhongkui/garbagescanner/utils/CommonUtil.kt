package com.gaozhongkui.garbagescanner.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.gaozhongkui.garbagescanner.base.BaseScanInfo
import com.gaozhongkui.garbagescanner.model.*
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

    fun getTotalFileSize(list: List<BaseScanInfo>): Long {
        var totalFileSize = 0L
        list.forEach {
            totalFileSize += it.fileSize
        }
        return totalFileSize
    }

    fun getAppLabel(context: Context, packageName: String): CharSequence? {
        return try {
            val manager = context.packageManager
            val applicationInfo = manager.getApplicationInfo(
                packageName, PackageManager.GET_META_DATA
            )
            applicationInfo.loadLabel(manager)
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            val manager = context.packageManager
            val applicationInfo = manager.getApplicationInfo(
                packageName, PackageManager.GET_META_DATA
            )
            applicationInfo.loadIcon(manager)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getApkIcon(context: Context, apkPath: String): Drawable? {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES)
        if (info != null) {
            val appInfo = info.applicationInfo
            appInfo.sourceDir = apkPath
            appInfo.publicSourceDir = apkPath
            try {
                return appInfo.loadIcon(pm)
            } catch (_: OutOfMemoryError) {
            }
        }
        return null
    }


    /**
     * 获取文件路径
     */
    fun getFilePathByBaseScanInfo(info: BaseScanInfo): String? {
        return when (info) {
            is AdGarbageInfo -> {
                if (info.filePaths.isEmpty()) ""
                else info.filePaths.first()
            }
            is ApkFileInfo -> {
                info.filePath
            }
            is GarbagePathInfo -> {
                info.filePath
            }
            is NormalGarbageInfo -> {
                info.filePath
            }
            is UnloadResidueInfo -> {
                info.filePath
            }
            else -> {
                null
            }
        }
    }

    /**
     * 是否为不能扫描的文件
     */
    fun isNoScannerFile(filePath: String): Boolean {
        if (filePath.uppercase().contains("DCIM/CAMERA")) {
            return true
        }
        return false
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