package com.gaozhongkui.garbagescanner.clean

import android.content.Context
import android.content.pm.IPackageDataObserver
import android.os.Environment
import android.os.StatFs
import androidx.documentfile.provider.DocumentFile
import com.gaozhongkui.garbagescanner.base.BaseScanInfo
import com.gaozhongkui.garbagescanner.model.*
import java.io.File
import java.io.IOException

/**
 * 垃圾清理
 */
object GarbageCleanUtils {

    /**
     * 删除扫描的对象
     */
    fun deleteScannerInfo(cxt: Context, list: List<BaseScanInfo>) {
        for (info in list) {
            //判断如果未选中，则不做删除
            if (!info.isChecked) {
                continue
            }
            deleteScannerInfo(cxt, info)
        }
    }

    fun deleteScannerInfo(cxt: Context, softInfo: SortScannerInfo) {
        for (info in softInfo.childList) {
            //判断如果未选中，则不做删除
            if (!info.isChecked) {
                continue
            }
            deleteScannerInfo(cxt, info)
        }
    }

    /**
     * 删除扫描的对象
     */
    fun deleteScannerInfo(cxt: Context, info: BaseScanInfo) {
        when (info) {
            is AdGarbageInfo -> {
                info.filePaths.forEach {
                    deleteFile(cxt, it)
                }
            }
            is ApkFileInfo -> {
                deleteFile(cxt, info.filePath)
            }
            is AppCacheInfo -> {
                clearAppCache(cxt, info.packageName)
            }
            is GarbagePathInfo -> {
                deleteFile(cxt, info.filePath)
            }
            is NormalGarbageInfo -> {
                deleteFile(cxt, info.filePath)
            }
            is UnloadResidueInfo -> {
                deleteFile(cxt, info.filePath)
            }
        }
    }

    /**
     * 清空app的缓冲数据
     */
    private fun clearAppCache(cxt: Context, packageName: String) {

    }

    /**
     * 删除文件
     */
    private fun deleteFile(context: Context, file: String) {
        deleteFile(context, File(file))
    }

    private fun deleteFile(context: Context, file: File) {
        forceDelete(context, file)
    }

    /**
     * 删除目录
     */
    private fun deleteDirectory(context: Context, dir: String) {
        deleteDirectory(context, File(dir))
    }

    fun deleteDirectory(context: Context, dir: File) {
        delDirectory(context, dir, true)
    }

    private fun clearCache(context: Context) {
        try {
            val packManager = context.packageManager
            val method = packManager.javaClass.getMethod("freeStorageAndNotify", java.lang.Long.TYPE, IPackageDataObserver::class.java)
            val valueOf = getDataDirectorySize() - 1
            method.invoke(packManager, valueOf, object : IPackageDataObserver.Stub() {
                override fun onRemoveCompleted(str: String, z: Boolean) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getDataDirectorySize(): Long {
        val dataDirectory = Environment.getDataDirectory()
        val statFs = StatFs(dataDirectory.path)
        return statFs.blockCount.toLong() * statFs.blockSize.toLong()
    }


    private fun delDirectory(context: Context, file: File, isSelf: Boolean) {
        if (file.exists() && file.isDirectory) {
            val listFiles = file.listFiles() ?: throw IOException("Failed to list contents of $file")
            for (file2 in listFiles) {
                if (file2.isDirectory) {
                    cleanDirectory(context, file2)
                } else {
                    file2.delete()
                }
            }
            if (isSelf) {
                file.delete()
            }
        }
    }

    private fun cleanDirectory(context: Context, file: File) {
        if (file.exists() && file.isDirectory) {
            val listFiles = file.listFiles()
            listFiles?.let {
                for (forceDelete in it) {
                    forceDelete(context, forceDelete)
                }
            }
            file.delete()
        }
    }

    private fun forceDelete(context: Context, file: File) {
        if (file.isDirectory) {
            cleanDirectory(context, file)
            return
        }
        if (file.exists()) {
            file.delete()
        }

        //如果没有删除，则通过另一种方式删除
        if (file.exists()) {
            DocumentFile.fromFile(file).delete()
        }

    }
}