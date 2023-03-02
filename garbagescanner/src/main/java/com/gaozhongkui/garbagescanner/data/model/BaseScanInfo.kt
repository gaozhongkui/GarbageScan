package com.gaozhongkui.garbagescanner.data.model

abstract class BaseScanInfo(
    var name: String = "", var fileSize: Long = 0, val itemType: ScanItemType = ScanItemType.OTHER_GARBAGE, var isChecked: Boolean = false
)
