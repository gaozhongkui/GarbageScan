package com.ihs.device.clean.junk.cache.app.sys.task;

import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;

public class SysExternalCacheTaskProgress {
    public int processedCount, total;
    public HSAppSysCache appSysCache;

    public SysExternalCacheTaskProgress(int processedCount, int total, HSAppSysCache appSysCache) {
        this.processedCount = processedCount;
        this.total = total;
        this.appSysCache = appSysCache;
    }
}
