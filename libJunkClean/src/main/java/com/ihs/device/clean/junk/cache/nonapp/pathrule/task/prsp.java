package com.ihs.device.clean.junk.cache.nonapp.pathrule.task;

import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;

public class prsp { //proguard PathFileCacheScanTaskProgress
    public int processedCount;
    public HSPathFileCache pathFileCache;

    public prsp(int processedCount, HSPathFileCache pathFileCache) {
        this.processedCount = processedCount;
        this.pathFileCache = pathFileCache;
    }
}
