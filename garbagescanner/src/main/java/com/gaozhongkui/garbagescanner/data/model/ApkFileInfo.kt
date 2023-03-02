package com.gaozhongkui.garbagescanner.data.model

data class ApkFileInfo(val filePath: String) : BaseScanInfo(itemType = ScanItemType.INSTALL_PACKAGE){

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