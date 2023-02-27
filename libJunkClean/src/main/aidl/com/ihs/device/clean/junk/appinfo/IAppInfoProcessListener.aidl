package com.ihs.device.clean.junk.appinfo;

import com.ihs.device.common.HSAppInfo;

interface IAppInfoProcessListener {
   void onStarted();
   void onProgressUpdated(in int processedCount, in int total, in HSAppInfo appInfo);
   void onSucceeded(in List<HSAppInfo> appInfoList, in long dataSize);
   void onFailed(int failCode, in String failMsg);
}
