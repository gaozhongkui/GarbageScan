package com.gaozhongkui.garbagescanner.data.model

data class GarbagePathInfo(val id: Long, val filePath: String, val packageName: String, val garbageName: String) : BaseScanInfo(){

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return filePath.hashCode()
    }
}
