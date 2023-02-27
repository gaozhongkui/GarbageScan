package com.ihs.device.clean.junk.cache.app.nonsys.junk.task;

import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class AppJunkCacheCleanProcessor extends AsyncProcessor<List<HSAppJunkCache>, AppJunkCacheCleanTaskProgress, List<HSAppJunkCache>> {

    public AppJunkCacheCleanProcessor(OnProcessListener<AppJunkCacheCleanTaskProgress, List<HSAppJunkCache>> processListener) {
        super(processListener);
    }

    @Override
    protected List<HSAppJunkCache> doInBackground(List<HSAppJunkCache>... params) {
        List<HSAppJunkCache> toCleanList = new ArrayList<>();
        if (params != null && params.length > 0) {
            toCleanList.addAll(params[0]);
        }

        List<HSAppJunkCache> result_apps = new ArrayList<>();
        int processed = 0;
        int total = toCleanList.size();
        for (HSAppJunkCache appJunkCache : toCleanList) {
            if (!isRunning()) {
                return result_apps;
            }
            //            if (AppUtil.isPackageInstalled(appJunkCache.getPackageName())) {
            //                continue;
            //            }
            try {
                Utils.deleteFilePath(appJunkCache.getPath());
                result_apps.add(appJunkCache);
            } catch (Exception e) {
                if (HSLog.isDebugging()) {
                    throw e;
                }
                e.printStackTrace();
            }
            postOnProgressUpdated(new AppJunkCacheCleanTaskProgress(++processed, total, appJunkCache));
        }
        return result_apps;
    }
}
