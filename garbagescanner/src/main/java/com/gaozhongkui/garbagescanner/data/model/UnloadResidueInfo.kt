package com.gaozhongkui.garbagescanner.data.model

/**
 * 卸载的垃圾对象
 */
data class UnloadResidueInfo(val packageName: String,val filePath: String) : BaseScanInfo(itemType = ScanItemType.UNLOAD_RESIDUE)