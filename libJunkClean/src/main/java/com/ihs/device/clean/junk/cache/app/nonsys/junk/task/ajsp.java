package com.ihs.device.clean.junk.cache.app.nonsys.junk.task;

import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;

public class ajsp { //proguard AppJunkCacheScanTaskProgress
    public int processedCount;
    public HSAppJunkCache appJunkCache;

    public ajsp(int processedCount, HSAppJunkCache appJunkCache) {
        this.processedCount = processedCount;
        this.appJunkCache = appJunkCache;
    }
}
