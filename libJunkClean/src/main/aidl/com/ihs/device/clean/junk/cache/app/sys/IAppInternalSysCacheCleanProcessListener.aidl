package com.ihs.device.clean.junk.cache.app.sys;

import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;

interface IAppInternalSysCacheCleanProcessListener {
   void onStarted();
   void onSucceeded(in long dataSize);
   void onFailed(int code, in String failMsg);
}
