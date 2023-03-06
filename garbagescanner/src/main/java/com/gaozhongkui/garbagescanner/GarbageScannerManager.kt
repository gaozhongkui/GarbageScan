package com.gaozhongkui.garbagescanner

import android.content.Context
import com.gaozhongkui.garbagescanner.base.BaseScanInfo
import com.gaozhongkui.garbagescanner.callback.IGarbageScannerCallback
import com.gaozhongkui.garbagescanner.callback.IScannerCallback
import com.gaozhongkui.garbagescanner.model.ScanItemType
import com.gaozhongkui.garbagescanner.scanner.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 垃圾扫描管理类
 */
class GarbageScannerManager {
    private val adGarbageScanner by lazy { AdGarbageScanner() }
    private val apkFileScanner by lazy { ApkFileScanner() }
    private val appCacheScanner by lazy { AppCacheScanner() }
    private val normalGarbageScanner by lazy { NormalGarbageScanner() }
    private val unloadResidueScanner by lazy { UnloadResidueScanner() }

    //外部的扫描回调
    private var scannerCallback: IGarbageScannerCallback? = null

    //结束的次数
    private val finishItemCount = AtomicInteger(SCANNER_TYPE_COUNT)

    //开始的次数
    private val startItemCount = AtomicInteger(SCANNER_TYPE_COUNT)

    /**
     * 开始所有的扫描
     */
    fun startAllScan(cxt: Context) {
        //必须设置回调，不然扫描就结果没有扫描用了
        if (scannerCallback == null) {
            throw NullPointerException("IGarbageScannerCallback is null")
        }

        finishItemCount.set(SCANNER_TYPE_COUNT)
        startItemCount.set(SCANNER_TYPE_COUNT)
        adGarbageScanner.startScan(cxt, innerScannerCallback)
        apkFileScanner.startScan(cxt, innerScannerCallback)
        appCacheScanner.startScan(cxt, innerScannerCallback)
        normalGarbageScanner.startScan(cxt, innerScannerCallback)
        unloadResidueScanner.startScan(cxt, innerScannerCallback)
    }

    fun startAllScan(cxt: Context, callback: IGarbageScannerCallback) {
        this.scannerCallback = callback
        startAllScan(cxt)
    }

    /**
     * 停止所有的扫描
     */
    fun stopAllScan() {
        adGarbageScanner.stopScan()
        apkFileScanner.stopScan()
        appCacheScanner.stopScan()
        normalGarbageScanner.stopScan()
        unloadResidueScanner.stopScan()
    }

    /**
     * 设置扫描的回调
     */
    fun setScannerCallback(callback: IGarbageScannerCallback) {
        this.scannerCallback = callback
    }

    /**
     * 内部类用于处理所有子类的扫描回调
     */
    private val innerScannerCallback = object : IScannerCallback {
        private val mapTypes = HashMap<ScanItemType, List<BaseScanInfo>>()
        private var startScannerTime = 0L
        override fun onStart() {
            val value = startItemCount.decrementAndGet()
            if (value > 0) {
                return
            }
            startScannerTime = System.currentTimeMillis()
            scannerCallback?.onStart()
        }

        override fun onFind(info: BaseScanInfo) {
            //小于指定的时间间隔，则直接返回
            if ((System.currentTimeMillis() - startScannerTime) <= CALL_BACK_INTERVAL_TIME) {
                return
            }
            startScannerTime = System.currentTimeMillis()
            scannerCallback?.onFind(info)
        }

        override fun onFinish(totalList: List<BaseScanInfo>) {
            //判断如果不为空时，则添加到map中
            if (totalList.isNotEmpty()) {
                val scanInfo = totalList.first()
                mapTypes[scanInfo.itemType] = totalList
            }

            val value = finishItemCount.decrementAndGet()
            if (value > 0) {
                return
            }
            scannerCallback?.onFinish(mapTypes)
        }

    }

    companion object {
        private const val SCANNER_TYPE_COUNT = 5
        private const val CALL_BACK_INTERVAL_TIME = 10
    }

}