package com.gaozhongkui.garbagescanner.base

import android.content.Context
import com.gaozhongkui.garbagescanner.callback.IScannerCallback

interface BaseScanner {
    fun startScan(cxt: Context, callback: IScannerCallback)
    fun stopScan()
}