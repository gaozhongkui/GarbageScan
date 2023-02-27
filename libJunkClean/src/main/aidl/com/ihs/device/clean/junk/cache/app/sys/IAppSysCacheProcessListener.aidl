package com.ihs.device.clean.junk.cache.app.sys;

import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;

interface IAppSysCacheProcessListener {
   void onStarted();
   void onProgressUpdated(in int processedCount, in int total, in HSAppSysCache appSysCache);
   void onSucceeded(in List<HSAppSysCache> appSysCacheList, in long dataSize);
   void onFailed(int code, in String failMsg);
}
