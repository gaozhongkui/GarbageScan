package com.gaozhongkui.garbagescanner.data.model

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

    //内存垃圾
    MEM_GARBAGE,

    //其他垃圾
    OTHER_GARBAGE
}