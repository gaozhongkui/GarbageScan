package com.ihs.device.clean.junk.cache.nonapp.pathrule;

import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;

interface IPathFileCacheCleanListener {
   void onStarted();
   void onProgressUpdated(in int processedCount, in int total, in HSPathFileCache pathFileCache);
   void onSucceeded(in List<HSPathFileCache> pathFileCacheList, in long dataSize);
   void onFailed(int code, in String failMsg);
}
