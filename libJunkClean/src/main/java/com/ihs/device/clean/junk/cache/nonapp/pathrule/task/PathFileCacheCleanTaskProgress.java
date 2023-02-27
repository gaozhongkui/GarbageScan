package com.ihs.device.clean.junk.cache.nonapp.pathrule.task;

import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;

public class PathFileCacheCleanTaskProgress {
    public int processedCount, total;
    public HSPathFileCache pathFileCache;

    public PathFileCacheCleanTaskProgress(int processedCount, int total, HSPathFileCache pathFileCache) {
        this.processedCount = processedCount;
        this.total = total;
        this.pathFileCache = pathFileCache;
    }
}
