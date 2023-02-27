package com.ihs.device.clean.junk.cache.nonapp.pathrule.task;

import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class PathFileCacheCleanProcessor extends AsyncProcessor<List<HSPathFileCache>, PathFileCacheCleanTaskProgress, List<HSPathFileCache>> {

    public PathFileCacheCleanProcessor(OnProcessListener<PathFileCacheCleanTaskProgress, List<HSPathFileCache>> processListener) {
        super(processListener);
    }

    @SafeVarargs
    @Override
    protected final List<HSPathFileCache> doInBackground(List<HSPathFileCache>... params) {
        List<HSPathFileCache> toCleanList = new ArrayList<>();
        if (params != null && params.length > 0) {
            toCleanList.addAll(params[0]);
        }

        List<HSPathFileCache> pathFileCacheList = new ArrayList<>();
        int processed = 0;
        int total = toCleanList.size();
        for (HSPathFileCache pathFileCache : toCleanList) {
            if (!isRunning()) {
                return pathFileCacheList;
            }

            Utils.deleteFilePath(pathFileCache.getPath());
            pathFileCacheList.add(pathFileCache);
            postOnProgressUpdated(new PathFileCacheCleanTaskProgress(++processed, total, pathFileCache));
        }
        return pathFileCacheList;
    }
}
