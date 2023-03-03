package com.gaozhongkui.garbagescanner.model

import com.gaozhongkui.garbagescanner.base.BaseScanInfo

data class GarbagePathInfo(val id: Long, val filePath: String, val packageName: String) : BaseScanInfo(){

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return filePath.hashCode()
    }
}
