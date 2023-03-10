package com.gaozhongkui.garbagescanner.model

import android.text.TextUtils
import com.gaozhongkui.garbagescanner.base.BaseScanInfo

data class GarbagePathInfo(val id: Long, val filePath: String, val packageName: String) : BaseScanInfo() {

    override fun equals(other: Any?): Boolean {
        if (other is GarbagePathInfo) {
            if (TextUtils.equals(other.filePath, filePath) || TextUtils.equals(other.packageName, packageName)) {
                return true
            }
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return filePath.hashCode()
    }
}
