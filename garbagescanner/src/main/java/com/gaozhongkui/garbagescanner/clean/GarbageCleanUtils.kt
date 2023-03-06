package com.gaozhongkui.garbagescanner.clean

import android.content.Context
import android.content.pm.IPackageDataObserver
import android.os.Environment
import android.os.StatFs

/**
 * 垃圾清理
 */
class GarbageCleanUtils {

    /**
     * 清空app的缓冲数据
     */
    fun clearAppCache(cxt: Context, packageName: String) {

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
}