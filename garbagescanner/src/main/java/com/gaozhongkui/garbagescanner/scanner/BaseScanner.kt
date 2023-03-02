package com.gaozhongkui.garbagescanner.scanner

import android.content.Context
import com.gaozhongkui.garbagescanner.callback.IScannerCallback

interface BaseScanner {
    fun startScan(cxt: Context, callback: IScannerCallback)
    fun stopScan()
}