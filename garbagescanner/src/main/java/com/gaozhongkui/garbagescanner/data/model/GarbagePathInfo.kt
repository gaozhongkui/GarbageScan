package com.gaozhongkui.garbagescanner.data.model

data class GarbagePathInfo(val id: Long, val filePath: String, val packageName: String, val garbageName: String) : BaseScanInfo()
