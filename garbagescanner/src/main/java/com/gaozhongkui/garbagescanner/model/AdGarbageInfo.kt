package com.gaozhongkui.garbagescanner.model

import android.graphics.drawable.Drawable
import com.gaozhongkui.garbagescanner.base.BaseScanInfo

data class AdGarbageInfo(val packageName: String, val filePaths: List<String>, var appIcon: Drawable? = null) : BaseScanInfo(itemType = ScanItemType.AD_GARBAGE)