package com.ihs.device.clean.junk.cache.nonapp.commonrule.task;

import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;

public class CommonFileCacheTaskProgress {
    public int processedCount, total;
    public HSCommonFileCache commonFileCache;

    public CommonFileCacheTaskProgress(int processedCount, int total, HSCommonFileCache commonFileCache) {
        this.processedCount = processedCount;
        this.total = total;
        this.commonFileCache = commonFileCache;
    }
}
