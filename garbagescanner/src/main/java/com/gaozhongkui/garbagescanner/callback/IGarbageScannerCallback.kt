package com.gaozhongkui.garbagescanner.callback

import com.gaozhongkui.garbagescanner.base.BaseScanInfo
import com.gaozhongkui.garbagescanner.model.ScanItemType

/**
 * 扫描回调类
 */
interface IGarbageScannerCallback {

    fun onStart()

    fun onFind(info: BaseScanInfo)

    fun onFinish(mapTypes: Map<ScanItemType, List<BaseScanInfo>>)
}