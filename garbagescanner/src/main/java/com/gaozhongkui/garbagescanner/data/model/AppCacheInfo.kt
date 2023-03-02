package com.gaozhongkui.garbagescanner.data.model

data class AppCacheInfo(val packageName: String) : BaseScanInfo(itemType = ScanItemType.CACHE_GARBAGE)