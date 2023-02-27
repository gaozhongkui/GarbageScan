package com.ihs.device.clean.junk.cache.app.sys.task;

import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;

public class SysCacheProgress {
    public int processedCount, total;
    public HSAppSysCache appSysCache;

    public SysCacheProgress(int processedCount, int total, HSAppSysCache appSysCache) {
        this.processedCount = processedCount;
        this.total = total;
        this.appSysCache = appSysCache;
    }
}
