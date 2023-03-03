package com.gaozhongkui.garbagescanner.model

/**
 * 扫描的类型
 */
enum class ScanItemType {
    //缓存垃圾
    CACHE_GARBAGE,

    //广告垃圾
    AD_GARBAGE,

    //卸载残留
    UNLOAD_RESIDUE,

    //安装包
    INSTALL_PACKAGE,

    //其他垃圾
    OTHER_GARBAGE
}