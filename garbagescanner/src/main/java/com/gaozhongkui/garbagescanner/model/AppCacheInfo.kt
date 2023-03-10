package com.gaozhongkui.garbagescanner.model

import android.graphics.drawable.Drawable
import com.gaozhongkui.garbagescanner.base.BaseScanInfo

data class AppCacheInfo(val packageName: String, var appIcon: Drawable? = null) : BaseScanInfo(itemType = ScanItemType.CACHE_GARBAGE)