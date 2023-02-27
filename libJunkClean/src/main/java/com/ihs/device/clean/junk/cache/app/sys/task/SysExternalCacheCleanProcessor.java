package com.ihs.device.clean.junk.cache.app.sys.task;

import android.os.Environment;

import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;
import com.ihs.device.common.HSAppFilter;
import com.ihs.device.common.HSAppInfoUtils;
import com.ihs.device.common.async.AsyncProcessor;

import java.util.ArrayList;
import java.util.List;

import static com.ihs.device.common.utils.Utils.buildPath;
import static com.ihs.device.common.utils.Utils.deleteFile;

public class SysExternalCacheCleanProcessor extends AsyncProcessor<List<HSAppSysCache>, SysCacheProgress, List<HSAppSysCache>> {

    public SysExternalCacheCleanProcessor(OnProcessListener<SysCacheProgress, List<HSAppSysCache>> processListener) {
        super(processListener);
    }

    @Override
    protected List<HSAppSysCache> doInBackground(List<HSAppSysCache>... params) {
        List<HSAppSysCache> toCleanList = new ArrayList<>();
        if (params != null && params.length > 0) {
            toCleanList.addAll(params[0]);
        }
        List<HSAppSysCache> resultApps = new ArrayList<>();
        int processedCount = 0;
        if (toCleanList.isEmpty()) {
            toCleanList = HSAppInfoUtils.getInstalledAppInfoList(HSAppSysCache.class, new HSAppFilter());
        }
        for (HSAppSysCache appSysCache : toCleanList) {
            if (!isRunning()) {
                break;
            }
            deleteFile(buildPath(Environment.getExternalStorageDirectory(), "Android", "data", appSysCache.getPackageName(), "cache"));
            postOnProgressUpdated(new SysCacheProgress(++processedCount, toCleanList.size(), appSysCache));
        }
        return resultApps;
    }

}
