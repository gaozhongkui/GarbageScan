package com.gaozhongkui.garbagescanner.model

import com.gaozhongkui.garbagescanner.base.BaseScanInfo

data class AdGarbageInfo(val packageName: String, val filePath: String) : BaseScanInfo(itemType = ScanItemType.AD_GARBAGE)