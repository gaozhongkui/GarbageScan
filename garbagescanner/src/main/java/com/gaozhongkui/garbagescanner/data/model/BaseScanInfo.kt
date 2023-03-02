package com.gaozhongkui.garbagescanner.data.model

abstract class BaseScanInfo(
    val name: String = "",
    val fileSize: Long = 0,
    val packageName: String = "",
    val itemType: ScanItemType = ScanItemType.OTHER_GARBAGE,
    var isChecked: Boolean = false
)
