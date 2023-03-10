package com.gaozhongkui.garbagescanner.model

import android.graphics.drawable.Drawable
import com.gaozhongkui.garbagescanner.base.BaseScanInfo

data class ApkFileInfo(val filePath: String, var appIcon: Drawable? = null) : BaseScanInfo(itemType = ScanItemType.INSTALL_PACKAGE){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApkFileInfo

        if (filePath != other.filePath) return false

        return true
    }

    override fun hashCode(): Int {
        return filePath.hashCode()
    }
}