package com.ihs.device.clean.junk.cache.app.sys.task;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Build;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;
import com.ihs.device.common.async.AsyncProcessor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class SysCacheScanProcessor extends AsyncProcessor<HSAppSysCache, Void, HSAppSysCache> {

    public SysCacheScanProcessor(OnProcessListener<Void, HSAppSysCache> processListener) {
        super(processListener);
    }

    @Override
    protected HSAppSysCache doInBackground(HSAppSysCache... appSysCaches) {
        HSAppSysCache appSysCache = null;
        if (appSysCaches != null && appSysCaches.length > 0) {
            appSysCache = appSysCaches[0];
        }
        if (!isRunning()) {
            return appSysCache;
        }
        return fillJunkCacheSize(appSysCache);
    }

    private HSAppSysCache fillJunkCacheSize(final HSAppSysCache appSysCache) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                StorageManager storageManager = (StorageManager) HSApplication.getContext().getSystemService(Context.STORAGE_SERVICE);
                if (storageManager == null) {
                    return appSysCache;
                }
                List storageVolumeList = (List) storageManager.getClass().getDeclaredMethod("getStorageVolumes", new Class[0]).invoke(storageManager);
                if (storageVolumeList == null || storageVolumeList.isEmpty() || storageVolumeList.get(0) == null) {
                    return appSysCache;
                }
                Object storageVolume = storageVolumeList.get(0);

                String uuidStr = (String) storageVolume.getClass().getDeclaredMethod("getUuid", new Class[0]).invoke(storageVolume);
                UUID uuid = uuidStr == null ? (UUID) storageManager.getClass().getDeclaredField("UUID_DEFAULT").get(storageManager) : UUID.fromString(uuidStr);

                @SuppressLint("WrongConstant")
                Object storageStateManager = HSApplication.getContext().getSystemService("storagestats");
                if (storageStateManager == null) {
                    return appSysCache;
                }
                Object statesPackage = storageStateManager.getClass().getDeclaredMethod("queryStatsForPackage",
                        new Class[]{ UUID.class, String.class, UserHandle.class }).invoke(storageStateManager, uuid, appSysCache.getPackageName(), Process.myUserHandle());

                Long cacheBytes = (Long) statesPackage.getClass().getDeclaredMethod("getCacheBytes", new Class[0]).invoke(statesPackage);

                appSysCache.setSize(cacheBytes);
                appSysCache.setInternalCacheSize(cacheBytes);
                appSysCache.setExternalCacheSize(0);
            } else {
                PackageManager packageManager = HSApplication.getContext().getPackageManager();
                Method getPackageSizeInfoMethod = packageManager.getClass().getMethod("getPackageSizeInfo", String.class, IPackageStatsObserver.class);
                getPackageSizeInfoMethod.setAccessible(true);
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                getPackageSizeInfoMethod.invoke(packageManager, appSysCache.getPackageName(), new IPackageStatsObserver.Stub() {
                    @Override
                    public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
                        if (succeeded) {
                            appSysCache.setSize(pStats.cacheSize + pStats.externalCacheSize);
                            appSysCache.setInternalCacheSize(pStats.cacheSize);
                            appSysCache.setExternalCacheSize(pStats.externalCacheSize);
                        }
                        countDownLatch.countDown();
                    }
                });
                countDownLatch.await();
            }
        } catch (Exception ignore) {
            HSLog.i("libDevice", "ignore:" + appSysCache.getAppName() + " pkgName:" + appSysCache.getPackageName() + " err:" + ignore.getMessage());
        }
        return appSysCache;
    }
}
