package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import com.gaozhongkui.garbagescanner.base.BaseScanner
import com.gaozhongkui.garbagescanner.callback.IScannerCallback

/**
 * 常规的垃圾清理（系统垃圾、非广告类型、非apk类型）
 */
class NormalGarbageScanner : BaseScanner {
    override fun startScan(cxt: Context, callback: IScannerCallback) {
    }

    override fun stopScan() {
    }
}