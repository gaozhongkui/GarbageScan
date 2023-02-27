package com.gaozhongkui.garbagescanner.data.model

data class BaseScanInfo(val fileSize: Long, val itemType: ScanItemType, var isChecked: Boolean = false)
