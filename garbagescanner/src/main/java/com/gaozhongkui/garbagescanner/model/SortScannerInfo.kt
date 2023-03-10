package com.gaozhongkui.garbagescanner.model

import com.gaozhongkui.garbagescanner.base.BaseScanInfo

/**
 * 分类的信息
 */
data class SortScannerInfo(override val itemType: ScanItemType, var childList: List<BaseScanInfo>, var selectFileTotalSize: Long = 0) :
    BaseScanInfo(itemType = itemType)