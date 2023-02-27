package com.ihs.device.clean.junk;

import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.connection.HSHttpConnection;
import com.ihs.commons.utils.HSError;
import com.ihs.commons.utils.HSLog;
import com.ihs.commons.utils.HSPreferenceHelper;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author JackSparrow
 * Create Date 01/11/2017.
 */

public class JunkDataManager {
    public static final String PREF_FILE_NAME = "LibDeviceJunkDataManager";

    private static final String PREF_APP_JUNK_LAST_CHECK_UPGRADE_TIME = "PREF_APP_JUNK_LAST_CHECK_UPGRADE_TIME";
    private static final String PREF_APP_DATA_LAST_CHECK_UPGRADE_TIME = "PREF_APP_DATA_LAST_CHECK_UPGRADE_TIME";
    private static final String PREF_AD_CACHE_LAST_CHECK_UPGRADE_TIME = "PREF_AD_CACHE_LAST_CHECK_UPGRADE_TIME";

    private static final String PREF_APP_JUNK_NORMAL_VERSION = "PREF_APP_JUNK_NORMAL_VERSION";
    private static final String PREF_APP_DATA_NORMAL_VERSION = "PREF_APP_DATA_NORMAL_VERSION";
    private static final String PREF_AD_CACHE_NORMAL_VERSION = "PREF_AD_CACHE_NORMAL_VERSION";

    private AtomicBoolean isAppJunkFileDownloading = new AtomicBoolean(false);
    private AtomicBoolean isAppDataFileDownloading = new AtomicBoolean(false);
    private AtomicBoolean isADCacheFileDownloading = new AtomicBoolean(false);

    private static volatile JunkDataManager instance;

    private JunkDataManager() {
    }

    public static JunkDataManager getInstance() {
        if (instance == null) {
            synchronized (JunkDataManager.class) {
                if (instance == null) {
                    instance = new JunkDataManager();
                }
            }
        }
        return instance;
    }

    public boolean isDataStable() {
        return !isAppJunkFileDownloading.get() && !isAppDataFileDownloading.get() && !isADCacheFileDownloading.get();
    }

    @WorkerThread
    public synchronized void checkAppJunkToUpgrade(int newAppJunkFileVersion, String appJunkFileUrl) {
        HSLog.d("JunkDataManager", "start checkAppJunkToUpgrade, newAppJunkFileVersion: " + newAppJunkFileVersion + ", appJunkFileUrl: " + appJunkFileUrl);

        if (!TextUtils.isEmpty(appJunkFileUrl)) {
            if (getCurrentAppJunkFile().exists() && newAppJunkFileVersion <= getCurrentAppJunkVersion()) {
                HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).putLong(PREF_APP_JUNK_LAST_CHECK_UPGRADE_TIME, System.currentTimeMillis());

                HSLog.d("JunkDataManager", "成功检查AppJunkFile是否需要升级，本地数据版本已经是最新的，不需要升级");
                return;
            }

            if (!isAppJunkFileDownloading.compareAndSet(false, true)) {
                HSLog.d("JunkDataManager", "AppJunkFile数据正在下载！请勿重复调用");
                return;
            }

            HSLog.d("JunkDataManager", "成功检查AppJunkFile是否需要升级，本地数据需要升级");

            try {
                File appJunkDirectory = getAppJunkDirectory();
                if (!appJunkDirectory.exists()) {
                    appJunkDirectory.mkdirs();
                }
                downloadAppJunkFile(newAppJunkFileVersion, appJunkFileUrl);
            } finally {
                isAppJunkFileDownloading.set(false);
            }
        }
    }

    @WorkerThread
    public synchronized void checkAppDataToUpgrade(int newAppDataFileVersion, String appDataFileUrl) {
        HSLog.d("JunkDataManager", "start checkAppDataToUpgrade, newAppDataFileVersion: " + newAppDataFileVersion + ", appDataFileUrl: " + appDataFileUrl);

        if (!TextUtils.isEmpty(appDataFileUrl)) {
            if (getCurrentAppDataFile().exists() && newAppDataFileVersion <= getCurrentAppDataVersion()) {
                HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).putLong(PREF_APP_DATA_LAST_CHECK_UPGRADE_TIME, System.currentTimeMillis());

                HSLog.d("JunkDataManager", "成功检查AppDataFile是否需要升级，本地数据版本已经是最新的，不需要升级");
                return;
            }

            if (!isAppDataFileDownloading.compareAndSet(false, true)) {
                HSLog.d("JunkDataManager", "AppDataFile数据正在下载！请勿重复调用");
                return;
            }

            HSLog.d("JunkDataManager", "成功检查AppDataFile是否需要升级，本地数据需要升级");

            try {
                File appDataDirectory = getAppDataDirectory();
                if (!appDataDirectory.exists()) {
                    appDataDirectory.mkdirs();
                }
                downloadAppDataFile(newAppDataFileVersion, appDataFileUrl);
            } finally {
                isAppDataFileDownloading.set(false);
            }
        }
    }

    @WorkerThread
    public synchronized void checkADCacheToUpgrade(int newADCacheFileVersion, String adCacheFileUrl) {
        HSLog.d("JunkDataManager", "start checkADCacheToUpgrade, newADCacheFileVersion: " + newADCacheFileVersion + ", adCacheFileUrl: " + adCacheFileUrl);

        if (!TextUtils.isEmpty(adCacheFileUrl)) {
            if (getCurrentADCacheFile().exists() && newADCacheFileVersion <= getCurrentADCacheVersion()) {
                HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).putLong(PREF_AD_CACHE_LAST_CHECK_UPGRADE_TIME, System.currentTimeMillis());

                HSLog.d("JunkDataManager", "成功检查ADCacheFile是否需要升级，本地数据版本已经是最新的，不需要升级");
                return;
            }

            if (!isADCacheFileDownloading.compareAndSet(false, true)) {
                HSLog.d("JunkDataManager", "ADCacheFile数据正在下载！请勿重复调用");
                return;
            }

            HSLog.d("JunkDataManager", "成功检查ADCacheFile是否需要升级，本地数据需要升级");

            try {
                File adCacheDirectory = getADCacheDirectory();
                if (!adCacheDirectory.exists()) {
                    adCacheDirectory.mkdirs();
                }
                downloadADCacheFile(newADCacheFileVersion, adCacheFileUrl);
            } finally {
                isADCacheFileDownloading.set(false);
            }
        }
    }

    private void downloadAppJunkFile(final int newAppJunkFileVersion, String appJunkFileUrl) {
        HSLog.d("JunkDataManager", "start download AppJunkFile");

        HSHttpConnection connection = new HSHttpConnection(appJunkFileUrl);
        connection.setConnectionFinishedListener(new HSHttpConnection.OnConnectionFinishedListener() {
            @Override
            public void onConnectionFinished(HSHttpConnection hsHttpConnection) {
                if (hsHttpConnection.isSucceeded()) {
                    HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).putLong(PREF_APP_JUNK_LAST_CHECK_UPGRADE_TIME, System.currentTimeMillis());
                    HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).putInt(PREF_APP_JUNK_NORMAL_VERSION, newAppJunkFileVersion);

                    keepSpecifiedFile(getAppJunkDirectory(), getDownloadAppJunkFile());

                    HSLog.d("JunkDataManager", "download AppJunkFile file success");
                } else {
                    failed(new HSError(hsHttpConnection.getResponseCode(), hsHttpConnection.getResponseMessage()));
                }
            }

            @Override
            public void onConnectionFailed(HSHttpConnection hsHttpConnection, HSError hsError) {
                failed(hsError);
            }

            private void failed(HSError hsError) {
                HSLog.e("JunkDataManager", "download AppJunkFile failed:" + hsError.getMessage());
            }
        });
        connection.setDownloadFile(getAppJunkFile(newAppJunkFileVersion));
        connection.startSync();
    }

    private void downloadAppDataFile(final int newAppDataFileVersion, String appDataFileUrl) {
        HSLog.d("JunkDataManager", "start download AppDataFile");

        HSHttpConnection connection = new HSHttpConnection(appDataFileUrl);
        connection.setConnectionFinishedListener(new HSHttpConnection.OnConnectionFinishedListener() {
            @Override
            public void onConnectionFinished(HSHttpConnection hsHttpConnection) {
                if (hsHttpConnection.isSucceeded()) {
                    HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).putLong(PREF_APP_DATA_LAST_CHECK_UPGRADE_TIME, System.currentTimeMillis());
                    HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).putInt(PREF_APP_DATA_NORMAL_VERSION, newAppDataFileVersion);

                    keepSpecifiedFile(getAppDataDirectory(), getDownloadAppDataFile());

                    HSLog.d("JunkDataManager", "download AppDataFile success");
                } else {
                    failed(new HSError(hsHttpConnection.getResponseCode(), hsHttpConnection.getResponseMessage()));
                }
            }

            @Override
            public void onConnectionFailed(HSHttpConnection hsHttpConnection, HSError hsError) {
                failed(hsError);
            }

            private void failed(HSError hsError) {
                HSLog.e("JunkDataManager", "download AppDataFile failed:" + hsError.getMessage());
            }
        });
        connection.setDownloadFile(getAppDataFile(newAppDataFileVersion));
        connection.startSync();
    }

    private void downloadADCacheFile(final int newADCacheFileVersion, String adCacheFileUrl) {
        HSLog.d("JunkDataManager", "start download ADCacheFile");

        HSHttpConnection connection = new HSHttpConnection(adCacheFileUrl);
        connection.setConnectionFinishedListener(new HSHttpConnection.OnConnectionFinishedListener() {
            @Override
            public void onConnectionFinished(HSHttpConnection hsHttpConnection) {
                if (hsHttpConnection.isSucceeded()) {
                    HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).putLong(PREF_AD_CACHE_LAST_CHECK_UPGRADE_TIME, System.currentTimeMillis());
                    HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).putInt(PREF_AD_CACHE_NORMAL_VERSION, newADCacheFileVersion);

                    keepSpecifiedFile(getADCacheDirectory(), getDownloadADCacheFile());

                    HSLog.d("JunkDataManager", "download ADCacheFile success");
                } else {
                    failed(new HSError(hsHttpConnection.getResponseCode(), hsHttpConnection.getResponseMessage()));
                }
            }

            @Override
            public void onConnectionFailed(HSHttpConnection hsHttpConnection, HSError hsError) {
                failed(hsError);
            }

            private void failed(HSError hsError) {
                HSLog.e("JunkDataManager", "download ADCacheFile failed:" + hsError.getMessage());
            }
        });
        connection.setDownloadFile(getADCacheFile(newADCacheFileVersion));
        connection.startSync();
    }

    public static File getDownloadAppJunkFile() {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator +
            "AppJunkFile" + File.separator + "aj_" + getCurrentAppJunkVersion());
    }

    public static File getDownloadAppDataFile() {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator +
            "AppDataFile" + File.separator + "ad_" + getCurrentAppDataVersion());
    }

    public static File getDownloadADCacheFile() {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator +
            "ADCacheFile" + File.separator + "pr_" + getCurrentADCacheVersion());
    }

    public static long getAppJunkLastCheckUpgradeTime() {
        return HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).getLong(PREF_APP_JUNK_LAST_CHECK_UPGRADE_TIME, 0);
    }

    public static long getAppDataLastCheckUpgradeTime() {
        return HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).getLong(PREF_APP_DATA_LAST_CHECK_UPGRADE_TIME, 0);
    }

    public static long getADCacheLastCheckUpgradeTime() {
        return HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).getLong(PREF_AD_CACHE_LAST_CHECK_UPGRADE_TIME, 0);
    }

    public static int getCurrentAppJunkVersion() {
        return HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).getInt(PREF_APP_JUNK_NORMAL_VERSION, 30001);
    }

    public static int getCurrentAppDataVersion() {
        return HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).getInt(PREF_APP_DATA_NORMAL_VERSION, 30001);
    }

    public static int getCurrentADCacheVersion() {
        return HSPreferenceHelper.create(HSApplication.getContext(), PREF_FILE_NAME).getInt(PREF_AD_CACHE_NORMAL_VERSION, 30001);
    }

    private static File getCurrentAppJunkFile() {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator +
            "AppJunkFile" + File.separator + "aj_" + getCurrentAppJunkVersion());
    }

    private static File getCurrentAppDataFile() {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator +
            "AppDataFile" + File.separator + "ad_" + getCurrentAppDataVersion());
    }

    private static File getCurrentADCacheFile() {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator +
            "ADCacheFile" + File.separator + "pr_" + getCurrentADCacheVersion());
    }

    private static File getAppJunkFile(int appJunkFileVersion) {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator +
            "AppJunkFile" + File.separator + "aj_" + appJunkFileVersion);
    }

    private static File getAppDataFile(int appDataFileVersion) {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator +
            "AppDataFile" + File.separator + "ad_" + appDataFileVersion);
    }

    private static File getADCacheFile(int adCacheFileVersion) {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator +
            "ADCacheFile" + File.separator + "pr_" + adCacheFileVersion);
    }

    private static File getAppJunkDirectory() {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator + "AppJunkFile");
    }

    private static File getAppDataDirectory() {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator + "AppDataFile");
    }

    private static File getADCacheDirectory() {
        return new File(HSApplication.getContext().getFilesDir().getPath() + File.separator + "ADCacheFile");
    }

    private static void keepSpecifiedFile(File root, File specifiedFile) {
        try {
            if (root == null || !root.exists()) {
                return;
            }

            File files[] = root.listFiles();
            if (files == null) {
                return;
            }

            for (File file : files) {
                if (file.isDirectory()) {
                    keepSpecifiedFile(file, specifiedFile);
                    file.delete();
                } else if (file.exists()) {
                    if (!TextUtils.equals(file.getName(), specifiedFile.getName())) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            if (HSLog.isDebugging()) {
                throw e;
            }
            e.printStackTrace();
        }

    }
}
