package com.ihs.device.clean.junk.cache.app.nonsys.junk.task;

import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;

public class AppJunkCacheCleanTaskProgress {
    public int processedCount, total;
    public HSAppJunkCache appJunk;

    public AppJunkCacheCleanTaskProgress(int processedCount, int total, HSAppJunkCache appJunk) {
        this.processedCount = processedCount;
        this.total = total;
        this.appJunk = appJunk;
    }
}
