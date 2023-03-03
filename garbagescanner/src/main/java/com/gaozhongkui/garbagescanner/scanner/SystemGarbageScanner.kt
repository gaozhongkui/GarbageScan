package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import com.gaozhongkui.garbagescanner.callback.IScannerCallback

/**
 * 系统垃圾扫描
 */
class SystemGarbageScanner : BaseScanner {
    private var isStopScanner = false
    override fun startScan(cxt: Context, callback: IScannerCallback) {
    }

    override fun stopScan() {
    }
}