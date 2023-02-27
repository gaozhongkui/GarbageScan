package com.ihs.device.clean.junk.appinfo.task;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Build;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.ihs.app.framework.HSApplication;
import com.ihs.device.common.HSAppInfo;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.commons.utils.HSLog;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * MemoryAppScanProcessor Created by sharp on 15/8/10.
 */
public class AppInfoScanProcessor extends AsyncProcessor<HSAppInfo, Void, HSAppInfo> {

    public AppInfoScanProcessor(@NonNull OnProcessListener<Void, HSAppInfo> asyncTaskListener) {
        super(asyncTaskListener);
    }

    @Override
    protected HSAppInfo doInBackground(HSAppInfo... appInfoApps) {
        HSAppInfo appInfoApp = null;
        if (appInfoApps != null && appInfoApps.length > 0) {
            appInfoApp = appInfoApps[0];
        }
        if (!isRunning()) {
            return appInfoApp;
        }

        return fillAppInfo(appInfoApp);
    }

    private HSAppInfo fillAppInfo(final HSAppInfo appInfo) {

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                StorageManager storageManager = (StorageManager) HSApplication.getContext().getSystemService(Context.STORAGE_SERVICE);
                if (storageManager == null) {
                    return appInfo;
                }
                List storageVolumeList = (List) storageManager.getClass().getDeclaredMethod("getStorageVolumes", new Class[0]).invoke(storageManager);
                if (storageVolumeList == null || storageVolumeList.isEmpty() || storageVolumeList.get(0) == null) {
                    return appInfo;
                }
                Object storageVolume = storageVolumeList.get(0);

                String uuidStr = (String) storageVolume.getClass().getDeclaredMethod("getUuid", new Class[0]).invoke(storageVolume);
                UUID uuid = uuidStr == null ? (UUID) storageManager.getClass().getDeclaredField("UUID_DEFAULT").get(storageManager) : UUID.fromString(uuidStr);

                @SuppressLint("WrongConstant")
                Object storageStateManager = HSApplication.getContext().getSystemService("storagestats");
                if (storageStateManager == null) {
                    return appInfo;
                }
                Object statesPackage = storageStateManager.getClass().getDeclaredMethod("queryStatsForPackage",
                        new Class[]{ UUID.class, String.class, UserHandle.class }).invoke(storageStateManager, uuid, appInfo.getPackageName(), Process.myUserHandle());

                Long cacheBytes = (Long) statesPackage.getClass().getDeclaredMethod("getCacheBytes", new Class[0]).invoke(statesPackage);
                Long appBytes = (Long) statesPackage.getClass().getDeclaredMethod("getAppBytes", new Class[0]).invoke(statesPackage);
                Long dataBytes = (Long) statesPackage.getClass().getDeclaredMethod("getDataBytes", new Class[0]).invoke(statesPackage);

                appInfo.setSize(dataBytes + appBytes + cacheBytes);

            } else {

                PackageManager packageManager = HSApplication.getContext().getPackageManager();
                Method getPackageSizeInfoMethod = packageManager.getClass().getMethod("getPackageSizeInfo", String.class, IPackageStatsObserver.class);
                HSLog.i("libDevice", "getPackageSizeInfoMethod:" + getPackageSizeInfoMethod);
                getPackageSizeInfoMethod.setAccessible(true);
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                getPackageSizeInfoMethod.invoke(packageManager, appInfo.getPackageName(), new IPackageStatsObserver.Stub() {
                    @Override
                    public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
                        if (succeeded) {
                            appInfo.getAppName();
                            appInfo.setSize(pStats.dataSize + pStats.cacheSize + pStats.codeSize + pStats.externalCacheSize + pStats.externalCodeSize +
                                    pStats.externalDataSize + pStats.externalMediaSize + pStats.externalObbSize);
                        }
                        synchronized (countDownLatch) {
                            countDownLatch.countDown();
                        }
                    }
                });
                countDownLatch.await();
            }
        } catch (Exception ignore) {
            HSLog.i("libDevice", "ignore:" + appInfo.getAppName() + " pkgName:" + appInfo.getPackageName() + " err:" + ignore.getMessage());
        }
        return appInfo;
    }
    //
    // private HSAppInfo getPacakgeStats(final HSAppInfo appInfo) {
    //     try {
    //         PackageManager packageManager = HSApplication.getContext().getPackageManager();
    //         Method getPackageSizeInfo = packageManager.getClass().getDeclaredMethod("getPackageSizeInfo", String.class, int.class, IPackageStatsObserver.class);
    //         getPackageSizeInfo.setAccessible(true);
    //         final CountDownLatch countDownLatch = new CountDownLatch(1);
    //         HSLog.i("libDevice", "appinfo:" + appInfo.getAppName() + " " + appInfo.getPackageName());
    //         Object obj = getPackageSizeInfo
    //                 .invoke(packageManager, appInfo.getPackageName(), android.os.Process.myUid() / 100000, new IPackageStatsObserver.Stub() {
    //                     @Override
    //                     public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
    //                         HSLog.i("libDevice", "succeeded" + succeeded + " pStats：" + pStats.cacheSize);
    //                         if (succeeded) {
    //                             appInfo.setSize(pStats.dataSize + pStats.cacheSize + pStats.codeSize + pStats.externalCacheSize + pStats.externalCodeSize +
    //                                             pStats.externalDataSize + pStats.externalMediaSize + pStats.externalObbSize);
    //                         }
    //                         synchronized (countDownLatch) {
    //                             countDownLatch.countDown();
    //                         }
    //                     }
    //                 });
    //         HSLog.i("libDevice", " --------> getPackageSizeInfo():" + obj);
    //         countDownLatch.await();
    //     } catch (Exception ignore) {
    //         HSLog.i("libDevice", "ignore:" + appInfo.getAppName() + " pkgName:" + appInfo.getPackageName() + " err:" + ignore.getMessage());
    //     }
    //     return appInfo;
    // }
}
