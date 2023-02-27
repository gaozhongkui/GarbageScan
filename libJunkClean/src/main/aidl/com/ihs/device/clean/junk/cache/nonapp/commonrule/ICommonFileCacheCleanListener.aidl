package com.ihs.device.clean.junk.cache.nonapp.commonrule;

import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;

interface ICommonFileCacheCleanListener {
   void onStarted();
   void onProgressUpdated(in int processedCount, in int total, in HSCommonFileCache commonFileCache);
   void onSucceeded(in long dataSize);
   void onFailed(int code, in String failMsg);
}
