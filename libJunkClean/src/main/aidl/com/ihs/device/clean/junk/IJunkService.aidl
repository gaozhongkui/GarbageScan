package com.ihs.device.clean.junk;

import com.ihs.device.common.HSAppFilter;

import com.ihs.device.common.HSAppInfo;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;

import com.ihs.device.clean.junk.appinfo.IAppInfoProcessListener;

import com.ihs.device.clean.junk.cache.app.sys.IAppSysCacheProcessListener;
import com.ihs.device.clean.junk.cache.app.sys.IAppInternalSysCacheCleanProcessListener;

import com.ihs.device.clean.junk.cache.app.nonsys.junk.IAppJunkCacheScanListener;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.IAppJunkCacheCleanListener;

import com.ihs.device.clean.junk.cache.nonapp.commonrule.ICommonFileCacheScanListener;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.ICommonFileCacheCleanListener;

import com.ihs.device.clean.junk.cache.nonapp.pathrule.IPathFileCacheScanListener;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.IPathFileCacheCleanListener;

interface IJunkService {

    // app info
    void scanAppInfo(in boolean withCompletedProcess, in HSAppFilter filter, in IAppInfoProcessListener listener);
    void cancelScanAppInfo();

    // app sys cache
    void scanAppSysCache(in boolean withCompletedProcess, in HSAppFilter filter,in IAppSysCacheProcessListener listener);
    void cancelScanAppSysCache();

    void scanAppSysExternalCache(in boolean withCompletedProcess, in HSAppFilter filter,in IAppSysCacheProcessListener listener);
    void cancelScanAppSysExternalCache();

    void cleanAppSysInternalCache(in IAppInternalSysCacheCleanProcessListener listener);
    void cancelCleanAppSysInternalCache();

    void cleanAppSysExternalCache(in List<HSAppSysCache> toCleanList,in IAppSysCacheProcessListener listener);
    void cancelCleanAppSysExternalCache();

    // app junk
    void scanAppJunkCache(in boolean withCompletedProcess, in IAppJunkCacheScanListener listener);
    void cancelScanAppJunkCache();

    void cleanAppJunkCache(in List<HSAppJunkCache> toCleanList,in IAppJunkCacheCleanListener listener);
    void cancelCleanAppJunkCache();

    //  File
    void scanCommonFileCache(in boolean withCompletedProcess, in List<String> extensions,in long fileMinSize,in ICommonFileCacheScanListener listener);
    void cancelScanCommonFileCache();

    void cleanCommonFileCache(in List<HSCommonFileCache> toCleanList,in ICommonFileCacheCleanListener listener);
    void cancelCleanCommonFileCache();

    //  PathFileCache
    void scanPathFileCache(in boolean withCompletedProcess, in IPathFileCacheScanListener listener);
    void cancelScanPathFileCache();

    void cleanPathFileCache(in List<HSPathFileCache> toCleanList,in IPathFileCacheCleanListener listener);
    void cancelCleanPathFileCache();

    void setBinder(IBinder client);

}
