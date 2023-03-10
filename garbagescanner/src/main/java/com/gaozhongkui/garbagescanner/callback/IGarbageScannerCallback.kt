package com.gaozhongkui.garbagescanner.callback

import com.gaozhongkui.garbagescanner.base.BaseScanInfo
import com.gaozhongkui.garbagescanner.model.SortScannerInfo

/**
 * 扫描回调类
 */
interface IGarbageScannerCallback {

    fun onStart()

    fun onFind(info: BaseScanInfo)

    fun onFinish(sortList: List<SortScannerInfo>)
}