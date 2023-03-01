package com.gaozhongkui.garbagescanner.data.model

abstract class BaseScanInfo(val fileSize: Long = 0, val itemType: ScanItemType = ScanItemType.OTHER_GARBAGE, var isChecked: Boolean = false)
