package com.ihs.device.clean.junk.service;

import android.os.IBinder;
import android.os.RemoteException;

import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.junk.IJunkService;
import com.ihs.device.clean.junk.appinfo.IAppInfoProcessListener;
import com.ihs.device.clean.junk.appinfo.task.AppInfoScanTask;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.IAppJunkCacheCleanListener;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.IAppJunkCacheScanListener;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.task.AppJunkCacheCleanTask;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.task.AppJunkCacheScanTask;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;
import com.ihs.device.clean.junk.cache.app.sys.IAppInternalSysCacheCleanProcessListener;
import com.ihs.device.clean.junk.cache.app.sys.IAppSysCacheProcessListener;
import com.ihs.device.clean.junk.cache.app.sys.task.SysCacheScanTask;
import com.ihs.device.clean.junk.cache.app.sys.task.SysExternalCacheCleanTask;
import com.ihs.device.clean.junk.cache.app.sys.task.SysExternalCacheScanTask;
import com.ihs.device.clean.junk.cache.app.sys.task.SysInternalCacheCleanTask;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.ICommonFileCacheCleanListener;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.ICommonFileCacheScanListener;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.task.CommonFileCacheCleanTask;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.task.CommonFileCacheScanTask;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.IPathFileCacheCleanListener;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.IPathFileCacheScanListener;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.task.PathFileCacheCleanTask;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.task.PathFileCacheScanTask;
import com.ihs.device.common.HSAppFilter;
import com.ihs.device.common.async.ThreadPoolFactory;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class JunkServiceImpl extends IJunkService.Stub {

    private static class SingletonHolder {
        private static final JunkServiceImpl INSTANCE = new JunkServiceImpl();
    }

    public static JunkServiceImpl getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private final ThreadPoolExecutor appInfoThreadPool;
    private final ThreadPoolExecutor appSysCacheThreadPool;
    private final ThreadPoolExecutor appJunkCacheThreadPool;
    private final ThreadPoolExecutor appDataCacheThreadPool;
    private final ThreadPoolExecutor pathFileCacheThreadPool;
    private final ThreadPoolExecutor commonFileCacheThreadPool;

    /**
     * 1 appInfo
     */
    private AppInfoScanTask appInfoScanTask;
    /**
     * 2 app cache
     */
    // 2.1 sys
    private SysCacheScanTask sysCacheScanTask;
    private SysExternalCacheScanTask sysExternalCacheScanTask;
    private SysInternalCacheCleanTask sysInternalCacheCleanTask;
    private SysExternalCacheCleanTask sysExternalCacheCleanTask;

    // 2.2.2. app junk
    private AppJunkCacheScanTask appJunkCacheScanTask;
    private AppJunkCacheCleanTask appJunkCacheCleanTask;
    /**
     * 3 non app cache
     */
    // 3.1 file | folder
    private CommonFileCacheScanTask commonFileCacheScanTask;
    private CommonFileCacheCleanTask commonFileCacheCleanTask;
    // 3.2 ad
    private PathFileCacheScanTask pathFileCacheScanTask;
    private PathFileCacheCleanTask pathFileCacheCleanTask;

    private JunkServiceImpl() {
        appInfoThreadPool = ThreadPoolFactory.newThreadPool(ThreadPoolFactory.getCpuCoreCount()/2 + 1);
        appSysCacheThreadPool = ThreadPoolFactory.newThreadPool(ThreadPoolFactory.getCpuCoreCount()/2 + 1);
        commonFileCacheThreadPool = ThreadPoolFactory.newThreadPool(ThreadPoolFactory.getCpuCoreCount()/2 + 1);
        appJunkCacheThreadPool = ThreadPoolFactory.newThreadPool(ThreadPoolFactory.getCpuCoreCount()/2 + 1);
        appDataCacheThreadPool = ThreadPoolFactory.newThreadPool(ThreadPoolFactory.getCpuCoreCount()/2 + 1);
        pathFileCacheThreadPool = ThreadPoolFactory.newThreadPool(ThreadPoolFactory.getCpuCoreCount()/2 + 1);
    }

    public ThreadPoolExecutor getAppInfoThreadPool() {
        return appInfoThreadPool;
    }

    public ThreadPoolExecutor getAppSysCacheThreadPool() {
        return appSysCacheThreadPool;
    }

    public ThreadPoolExecutor getCommonFileCacheThreadPool() {
        return commonFileCacheThreadPool;
    }

    public ThreadPoolExecutor getAppJunkCacheThreadPool() {
        return appJunkCacheThreadPool;
    }

    public ThreadPoolExecutor getAppDataCacheThreadPool() {
        return appDataCacheThreadPool;
    }

    public ThreadPoolExecutor getPathFileCacheThreadPool() {
        return pathFileCacheThreadPool;
    }

    /**
     * ------------1. memory ------------
     */

    @Override
    public void scanAppInfo(boolean withCompletedProcess, HSAppFilter filter, IAppInfoProcessListener listener) throws RemoteException {
        if (withCompletedProcess) {
            cancelScanAppInfo();
        } else {
            if (appInfoScanTask != null && appInfoScanTask.isRunning()) {
                appInfoScanTask.addListener(listener);
                return;
            }
        }
        appInfoScanTask = new AppInfoScanTask();
        appInfoScanTask.addListener(listener);
        appInfoScanTask.start(filter);
    }

    @Override
    public void cancelScanAppInfo() throws RemoteException {
        if (appInfoScanTask != null && appInfoScanTask.isRunning()) {
            appInfoScanTask.cancel();
        }
    }

    /**
     * --------------2. app cache --------------
     */
    // 2.1 sys cache
    @Override
    public void scanAppSysCache(boolean withCompletedProcess, HSAppFilter filter, IAppSysCacheProcessListener listener) throws RemoteException {
        if (withCompletedProcess) {
            cancelScanAppSysCache();
        } else {
            if (sysCacheScanTask != null && sysCacheScanTask.isRunning()) {
                sysCacheScanTask.addListener(listener);
                return;
            }
        }
        sysCacheScanTask = new SysCacheScanTask();
        sysCacheScanTask.addListener(listener);
        sysCacheScanTask.start(filter);
    }

    @Override
    public void cancelScanAppSysCache() throws RemoteException {
        if (sysCacheScanTask != null && sysCacheScanTask.isRunning()) {
            sysCacheScanTask.cancel();
        }
    }

    @Override
    public void scanAppSysExternalCache(boolean withCompletedProcess, HSAppFilter filter, IAppSysCacheProcessListener listener) throws RemoteException {
        if (withCompletedProcess) {
            cancelScanAppSysExternalCache();
        } else {
            if (sysExternalCacheScanTask != null && sysExternalCacheScanTask.isRunning()) {
                sysExternalCacheScanTask.addListener(listener);
                return;
            }
        }
        sysExternalCacheScanTask = new SysExternalCacheScanTask();
        sysExternalCacheScanTask.addListener(listener);
        sysExternalCacheScanTask.start(filter);
    }

    @Override
    public void cancelScanAppSysExternalCache() throws RemoteException {
        if (sysExternalCacheScanTask != null && sysExternalCacheScanTask.isRunning()) {
            sysExternalCacheScanTask.cancel();
        }
    }

    @Override
    public void cleanAppSysInternalCache(IAppInternalSysCacheCleanProcessListener listener) throws RemoteException {
        cancelCleanAppSysInternalCache();
        sysInternalCacheCleanTask = new SysInternalCacheCleanTask();
        sysInternalCacheCleanTask.start(listener);
    }

    @Override
    public void cancelCleanAppSysInternalCache() throws RemoteException {
        if (sysInternalCacheCleanTask != null && sysInternalCacheCleanTask.isRunning()) {
            sysInternalCacheCleanTask.cancel();
        }
    }

    @Override
    public void cleanAppSysExternalCache(List<HSAppSysCache> toCleanList, IAppSysCacheProcessListener listener) throws RemoteException {
        cancelCleanAppSysExternalCache();
        sysExternalCacheCleanTask = new SysExternalCacheCleanTask();
        sysExternalCacheCleanTask.start(toCleanList, listener);
    }

    @Override
    public void cancelCleanAppSysExternalCache() throws RemoteException {
        if (sysExternalCacheCleanTask != null && sysExternalCacheCleanTask.isRunning()) {
            sysExternalCacheCleanTask.cancel();
        }
    }

    @Override
    public void scanAppJunkCache(boolean withCompletedProcess, IAppJunkCacheScanListener listener) throws RemoteException {
        if (withCompletedProcess) {
            cancelScanAppJunkCache();
        } else {
            if (appJunkCacheScanTask != null && appJunkCacheScanTask.isRunning()) {
                appJunkCacheScanTask.addListener(listener);
                return;
            }
        }
        appJunkCacheScanTask = new AppJunkCacheScanTask();
        appJunkCacheScanTask.addListener(listener);
        appJunkCacheScanTask.start();
    }

    @Override
    public void cancelScanAppJunkCache() throws RemoteException {
        if (appJunkCacheScanTask != null && appJunkCacheScanTask.isRunning()) {
            appJunkCacheScanTask.cancel();
        }
    }

    @Override
    public void cleanAppJunkCache(List<HSAppJunkCache> toCleanList, IAppJunkCacheCleanListener listener) throws RemoteException {
        cancelCleanAppJunkCache();
        appJunkCacheCleanTask = new AppJunkCacheCleanTask();
        appJunkCacheCleanTask.start(toCleanList, listener);
    }

    @Override
    public void cancelCleanAppJunkCache() throws RemoteException {
        if (appJunkCacheCleanTask != null && appJunkCacheCleanTask.isRunning()) {
            appJunkCacheCleanTask.cancel();
        }
    }

    @Override
    public void scanCommonFileCache(boolean withCompletedProcess, List<String> extensions, long fileMinSize, ICommonFileCacheScanListener listener)
            throws RemoteException {
        if (withCompletedProcess) {
            cancelScanCommonFileCache();
        } else {
            if (commonFileCacheScanTask != null && commonFileCacheScanTask.isRunning()) {
                commonFileCacheScanTask.addListener(listener);
                return;
            }
        }
        commonFileCacheScanTask = new CommonFileCacheScanTask();
        commonFileCacheScanTask.addListener(listener);
        commonFileCacheScanTask.start(extensions, fileMinSize);
    }

    @Override
    public void cancelScanCommonFileCache() throws RemoteException {
        if (commonFileCacheScanTask != null && commonFileCacheScanTask.isRunning()) {
            commonFileCacheScanTask.cancel();
        }
    }

    @Override
    public void cleanCommonFileCache(List<HSCommonFileCache> toCleanList, ICommonFileCacheCleanListener listener) throws RemoteException {
        cancelCleanCommonFileCache();
        commonFileCacheCleanTask = new CommonFileCacheCleanTask();
        commonFileCacheCleanTask.start(toCleanList, listener);
    }

    @Override
    public void cancelCleanCommonFileCache() throws RemoteException {
        if (commonFileCacheCleanTask != null && commonFileCacheCleanTask.isRunning()) {
            commonFileCacheCleanTask.cancel();
        }
    }

    // 3.2 ad
    @Override
    public void scanPathFileCache(boolean withCompletedProcess, IPathFileCacheScanListener listener) throws RemoteException {
        if (withCompletedProcess) {
            cancelScanPathFileCache();
        } else {
            if (pathFileCacheScanTask != null && pathFileCacheScanTask.isRunning()) {
                pathFileCacheScanTask.addListener(listener);
                return;
            }
        }
        pathFileCacheScanTask = new PathFileCacheScanTask();
        pathFileCacheScanTask.addListener(listener);
        pathFileCacheScanTask.start();
    }

    @Override
    public void cancelScanPathFileCache() throws RemoteException {
        if (pathFileCacheScanTask != null && pathFileCacheScanTask.isRunning()) {
            pathFileCacheScanTask.cancel();
        }
    }

    @Override
    public void cleanPathFileCache(List<HSPathFileCache> toCleanList, IPathFileCacheCleanListener listener) throws RemoteException {
        pathFileCacheCleanTask = new PathFileCacheCleanTask();
        pathFileCacheCleanTask.start(toCleanList, listener);
    }

    @Override
    public void cancelCleanPathFileCache() throws RemoteException {
        if (pathFileCacheCleanTask != null && pathFileCacheCleanTask.isRunning()) {
            pathFileCacheCleanTask.cancel();
        }
    }

    @Override
    public void setBinder(final IBinder client) throws RemoteException {
        try {
            client.linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    HSLog.i("libDevice", "---------binderDied----------");
                }
            }, 0);
        } catch (Exception e) {
            if (HSLog.isDebugging()) {
                throw e;
            }
            e.printStackTrace();
        }
    }

}