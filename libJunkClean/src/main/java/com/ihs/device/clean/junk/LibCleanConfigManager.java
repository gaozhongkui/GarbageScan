package com.ihs.device.clean.junk;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;

import com.ihs.commons.config.HSConfig;
import com.ihs.commons.connection.HSHttpConnection;
import com.ihs.commons.utils.HSLog;
import com.ihs.commons.utils.HSMapUtils;
import com.ihs.commons.utils.HSPlistParser;

import java.io.ByteArrayInputStream;
import java.util.Map;


public class LibCleanConfigManager {

    private static class SingletonHolder {
        private static final LibCleanConfigManager INSTANCE = new LibCleanConfigManager();
    }

    private static final String APP_JUNK_STRUCTURE_VERSION = "Version3";
    private static final String APP_DATA_STRUCTURE_VERSION = "Version3";
    private static final String AD_CACHE_STRUCTURE_VERSION = "Version3";

    private static final String LIB_CLEAN_CONFIG_URL = "http://cdn.appcloudbox.net/services/libs/libclean/configs/libclean.cfg";

    public static LibCleanConfigManager getInstance() {
        return LibCleanConfigManager.SingletonHolder.INSTANCE;
    }

    private LibCleanConfigManager() {
    }

    public void startSessionInMainLooper() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            startSession();
        } else {
            new Handler(Looper.myLooper()).post(new Runnable() {
                @Override
                public void run() {
                    startSession();
                }
            });
        }
    }

    @MainThread
    private void startSession() {
        Map<String, ?> libCleanMap = HSConfig.getMap("libClean");
        if (libCleanMap != null) {
            int newAppJunkVersionInConfig = HSMapUtils.optInteger(libCleanMap, 0, "AppJunk", APP_JUNK_STRUCTURE_VERSION, "fileVersion");
            int newAppDataVersionInConfig = HSMapUtils.optInteger(libCleanMap, 0, "AppData", APP_DATA_STRUCTURE_VERSION, "fileVersion");
            int newADCacheVersionInConfig = HSMapUtils.optInteger(libCleanMap, 0, "ADCache", AD_CACHE_STRUCTURE_VERSION, "fileVersion");

            if (newAppJunkVersionInConfig > JunkDataManager.getCurrentAppJunkVersion() ||
                newAppDataVersionInConfig > JunkDataManager.getCurrentAppDataVersion() ||
                newADCacheVersionInConfig > JunkDataManager.getCurrentADCacheVersion()) {

                HSLog.d("LibCleanConfigManager", "检查到需要紧急升级配置文件，进行相关操作");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        fetchVersionInfoToUpgrade(HSConfig.optString(LIB_CLEAN_CONFIG_URL, "libClean", "libConfigUrl"));
                    }
                }).start();

                HSLog.d("LibCleanConfigManager", "start force upgrade");
                return;
            }
        }

        long appJunkLastUpgradeTime = JunkDataManager.getAppJunkLastCheckUpgradeTime();
        long appDataLastUpgradeTime = JunkDataManager.getAppDataLastCheckUpgradeTime();
        long adCacheLastUpgradeTime = JunkDataManager.getADCacheLastCheckUpgradeTime();
        long minUpgradeTime = appJunkLastUpgradeTime < appDataLastUpgradeTime ? appJunkLastUpgradeTime : appDataLastUpgradeTime;
        minUpgradeTime = minUpgradeTime < adCacheLastUpgradeTime ? minUpgradeTime : adCacheLastUpgradeTime;

        if (System.currentTimeMillis() - minUpgradeTime > HSConfig.optInteger(7, "libClean", "regularUpgradeDays") * 24 * 3600 * 1000) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    fetchVersionInfoToUpgrade(HSConfig.optString(LIB_CLEAN_CONFIG_URL, "libClean", "libConfigUrl"));
                }
            }).start();

        } else {
            HSLog.d("LibCleanConfigManager", "在7天内已经检查过是否需要升级垃圾数据文件");
        }
    }

    private void fetchVersionInfoToUpgrade(final String configUrl) {
        HSLog.d("LibCleanConfigManager", "start fetchVersionInfoToUpgrade");

        HSHttpConnection connection = new HSHttpConnection(configUrl);
        connection.startSync();

        if (!connection.isSucceeded() || connection.getResponseCode() != 200) {
            HSLog.d("LibCleanConfigManager", "fetchVersionInfoToUpgrade connection failed");
            return;
        }

        Map<String, ?> data = null;
        if (connection.getBody() != null) {
            data = HSPlistParser.parse(new ByteArrayInputStream(connection.getBody()), HSPlistParser.isPAEncrypted(configUrl));
        }

        if (data == null) {
            HSLog.d("LibCleanConfigManager", "fetchVersionInfoToUpgrade data == null");
            return;
        }

        for (Map.Entry entry : data.entrySet()) {
            HSLog.d("LibCleanConfigManager", "key: " + entry.getKey() + ", value: " + entry.getValue());
        }

        JunkDataManager.getInstance().checkAppJunkToUpgrade(
                HSMapUtils.optInteger(data, 0, "Data", "AppJunk", APP_JUNK_STRUCTURE_VERSION, "fileVersion"),
                HSMapUtils.optString(data, "", "Data", "AppJunk", APP_JUNK_STRUCTURE_VERSION, "fileUrl"));

        JunkDataManager.getInstance().checkAppDataToUpgrade(
                HSMapUtils.optInteger(data, 0, "Data", "AppData", APP_DATA_STRUCTURE_VERSION, "fileVersion"),
                HSMapUtils.optString(data, "", "Data", "AppData", APP_DATA_STRUCTURE_VERSION, "fileUrl"));

        JunkDataManager.getInstance().checkADCacheToUpgrade(
                HSMapUtils.optInteger(data, 0, "Data", "ADCache", AD_CACHE_STRUCTURE_VERSION, "fileVersion"),
                HSMapUtils.optString(data, "", "Data", "ADCache", AD_CACHE_STRUCTURE_VERSION, "fileUrl"));
    }
}