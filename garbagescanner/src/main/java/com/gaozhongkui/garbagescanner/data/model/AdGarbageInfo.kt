package com.gaozhongkui.garbagescanner.data.model

data class AdGarbageInfo(val packageName: String, val filePath: String) : BaseScanInfo(itemType = ScanItemType.AD_GARBAGE)