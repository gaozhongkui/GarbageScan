package com.gaozhongkui.garbagescanner.data.model

data class GarbagePathInfo(val id: Long, var appName: String, val filePath: String, val garbageName: String, val packageName: String) : BaseScanInfo()
