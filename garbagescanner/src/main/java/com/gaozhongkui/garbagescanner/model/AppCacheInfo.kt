package com.gaozhongkui.garbagescanner.model

import com.gaozhongkui.garbagescanner.base.BaseScanInfo

data class AppCacheInfo(val packageName: String) : BaseScanInfo(itemType = ScanItemType.CACHE_GARBAGE)