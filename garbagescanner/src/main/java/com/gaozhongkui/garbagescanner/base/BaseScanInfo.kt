package com.gaozhongkui.garbagescanner.base

import com.gaozhongkui.garbagescanner.model.ScanItemType

abstract class BaseScanInfo(
    var name: String = "", var fileSize: Long = 0, open val itemType: ScanItemType = ScanItemType.OTHER_GARBAGE, var isChecked: Boolean = true
)
