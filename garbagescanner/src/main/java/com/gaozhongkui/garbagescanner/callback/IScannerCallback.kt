package com.gaozhongkui.garbagescanner.callback

import com.gaozhongkui.garbagescanner.base.BaseScanInfo

/**
 * 扫描回调类
 */
interface IScannerCallback {

    fun onStart()

    fun onFind(info: BaseScanInfo)

    fun onFinish(totalList: List<BaseScanInfo>)
}