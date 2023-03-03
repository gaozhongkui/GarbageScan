package com.gaozhongkui.garbagescanner.model

import com.gaozhongkui.garbagescanner.base.BaseScanInfo

/**
 * 卸载的垃圾对象
 */
data class UnloadResidueInfo(val packageName: String,val filePath: String) : BaseScanInfo(itemType = ScanItemType.UNLOAD_RESIDUE)