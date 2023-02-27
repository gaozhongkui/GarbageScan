package com.ihs.device.clean.junk.cache.app.nonsys.junk;

import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;

interface IAppJunkCacheCleanListener {
   void onStarted();
   void onProgressUpdated(in int processedCount, in int total, in HSAppJunkCache appJunkCache);
   void onSucceeded(in List<HSAppJunkCache> appJunkCacheList, in long dataSize);
   void onFailed(int code, in String failMsg);
}
