package com.ihs.device.clean.junk.cache.nonapp.commonrule.task;

import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class CommonFileCacheCleanProcessor extends AsyncProcessor<List<HSCommonFileCache>, CommonFileCacheTaskProgress, List<HSCommonFileCache>> {

    public CommonFileCacheCleanProcessor(OnProcessListener<CommonFileCacheTaskProgress, List<HSCommonFileCache>> processListener) {
        super(processListener);
    }

    @Override
    protected List<HSCommonFileCache> doInBackground(List<HSCommonFileCache>... params) {
        List<HSCommonFileCache> toCleanList = new ArrayList<>();
        if (params != null && params.length > 0) {
            toCleanList.addAll(params[0]);
        }
        List<HSCommonFileCache> result_apps = new ArrayList<>();

        int total = toCleanList.size();
        int count = 0;

        for (HSCommonFileCache commonFileCache : toCleanList) {
            try {
                Utils.deleteFilePath(commonFileCache.getFilePath());
            } catch (Exception ignored) {
                if (HSLog.isDebugging()) {
                    throw ignored;
                }
                ignored.printStackTrace();
            }
            result_apps.add(commonFileCache);
            CommonFileCacheTaskProgress boostProgress = new CommonFileCacheTaskProgress(++count, total, commonFileCache);
            postOnProgressUpdated(boostProgress);
        }

        return result_apps;
    }
}