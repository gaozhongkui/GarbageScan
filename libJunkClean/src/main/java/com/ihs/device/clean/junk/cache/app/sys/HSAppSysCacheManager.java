package com.ihs.device.clean.junk.cache.app.sys;

import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.ihs.device.clean.junk.cache.app.sys.agent.SysCacheScanTaskAgent;
import com.ihs.device.clean.junk.cache.app.sys.agent.SysExternalCacheCleanTaskAgent;
import com.ihs.device.clean.junk.cache.app.sys.agent.SysExternalCacheScanTaskAgent;
import com.ihs.device.clean.junk.cache.app.sys.agent.SysInternalCacheCleanTaskAgent;
import com.ihs.device.common.HSAppFilter;
import com.ihs.device.common.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class HSAppSysCacheManager {
    /**
     * 扫描SysCache回调，无进度
     */
    public interface AppSysCacheTaskNoProgressListener {
        /**
         * @param apps
         *         返回扫描出所有的HSAppSysCache列表
         * @param dataSize
         *         HSAppSysCache总大小
         */
        void onSucceeded(List<HSAppSysCache> apps, long dataSize);

        /**
         * @param code
         *         扫描失败错误码
         * @param failMsg
         *         扫描失败错误信息
         */
        void onFailed(@FailCode int code, String failMsg);
    }

    /**
     * 扫描SysCache回调，提供进度
     */
    public interface AppSysCacheTaskListener extends AppSysCacheTaskNoProgressListener {
        /**
         * 扫描开始
         */
        void onStarted();

        /**
         * @param processedCount
         *         返回已经进行的进度数
         * @param total
         *         总的进度数
         * @param appSysCache
         *         返回当前扫描出的HSAppSysCache
         */
        void onProgressUpdated(int processedCount, int total, HSAppSysCache appSysCache);
    }

    /**
     * 扫描InternalSysCache回调，提供进度
     */
    public interface AppInternalSysCacheCleanTaskListener {
        /**
         * 扫描开始
         */
        void onStarted();

        /**
         * @param processedCount
         *         返回已经进行的进度数
         * @param total
         *         总的进度数
         * @param appSysCache
         *         返回当前扫描出的HSAppSysCache
         */
        void onProgressUpdated(int processedCount, int total, HSAppSysCache appSysCache);

        /**
         * @param dataSize
         *         扫描出InternalSysCache垃圾总大小
         */
        void onSucceeded(long dataSize);

        /**
         * @param code
         *         扫描失败错误码
         * @param failMsg
         *         扫描失败错误信息
         */
        void onFailed(@FailCode int code, String failMsg);
    }

    @IntDef({FAIL_UNKNOWN,
             FAIL_CANCEL,
             FAIL_IS_RUNNING,
             FAIL_CLEAN_LIST_EMPTY,
             FAIL_EXCEPTION,
             FAIL_SERVICE_DISCONNECTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FailCode {
    }

    private static class SingletonHolder {
        private static final HSAppSysCacheManager INSTANCE = new HSAppSysCacheManager();
    }

    public final static int FAIL_UNKNOWN = 0x0;
    public final static int FAIL_CANCEL = 0x1;
    public final static int FAIL_IS_RUNNING = 0x2;
    public final static int FAIL_CLEAN_LIST_EMPTY = 0x3;
    public final static int FAIL_EXCEPTION = 0x4;
    public final static int FAIL_SERVICE_DISCONNECTED = 0x5;

    /**
     * 获取全局唯一HSAppSysCacheManager对象
     */
    public static HSAppSysCacheManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private HSAppFilter scanGlobalAppFilter = new HSAppFilter();
    // scan task agent
    private SysCacheScanTaskAgent sysCacheScanTaskAgent;
    private SysExternalCacheScanTaskAgent sysExternalCacheScanTaskAgent;
    // clean task agent
    private SysExternalCacheCleanTaskAgent sysExternalCacheCleanTaskAgent;
    private SysInternalCacheCleanTaskAgent sysInternalCacheCleanTaskAgent;

    private HSAppSysCacheManager() {
    }

    public void setScanGlobalAppFilter(HSAppFilter hsAppFilter) {
        if (hsAppFilter == null) {
            hsAppFilter = new HSAppFilter();
        }
        scanGlobalAppFilter = hsAppFilter;
    }

    /**
     * 开始带有进度的HSAppSysCache扫描
     *
     * @param listener
     *         扫描结果回调
     */
    public void startScanWithCompletedProgress(AppSysCacheTaskListener listener) {
        startScanWithCompletedProgress(listener, null);
    }

    public void startScanWithCompletedProgress(AppSysCacheTaskListener listener, Handler handler) {
        startScanInner(true, listener, handler);
    }

    /**
     * 开始不带有进度的HSAppSysCache扫描
     *
     * @param listener
     *         扫描结果回调
     */
    public void startScanWithoutProgress(AppSysCacheTaskNoProgressListener listener) {
        startScanWithoutProgress(listener, null);
    }

    public synchronized void startScanWithoutProgress(AppSysCacheTaskNoProgressListener listener, Handler handler) {
        if (sysCacheScanTaskAgent != null && sysCacheScanTaskAgent.isRunning()) {
            sysCacheScanTaskAgent.addListener(listener, handler);
            return;
        }
        startScanInner(false, listener, handler);
    }

    /**
     * 停止HSAppSysCache扫描
     *
     * @param listener
     *         正在监听扫描结果的listener
     */
    public synchronized void stopScan(@NonNull AppSysCacheTaskNoProgressListener listener) {
        if (sysCacheScanTaskAgent != null) {
            sysCacheScanTaskAgent.removeListener(listener);
        }
    }

    /**
     * 开始带有进度的External HSAppSysCache扫描
     *
     * @param listener
     *         扫描结果回调
     */
    public void startScanExternalWithCompletedProgress(AppSysCacheTaskListener listener) {
        startScanExternalWithCompletedProgress(listener, null);
    }

    public void startScanExternalWithCompletedProgress(AppSysCacheTaskListener listener, Handler handler) {
        startScanExternalInner(true, listener, handler);
    }

    /**
     * 开始不带有进度的External HSAppSysCache扫描
     *
     * @param listener
     *         扫描结果回调
     */
    public void startScanExternalWithoutProgress(AppSysCacheTaskNoProgressListener listener) {
        startScanExternalWithoutProgress(listener, null);
    }

    public synchronized void startScanExternalWithoutProgress(AppSysCacheTaskNoProgressListener listener, Handler handler) {
        if (sysExternalCacheScanTaskAgent != null && sysExternalCacheScanTaskAgent.isRunning()) {
            sysExternalCacheScanTaskAgent.addListener(listener, handler);
            return;
        }
        startScanExternalInner(false, listener, handler);
    }

    /**
     * 停止HSAppSysCache扫描
     *
     * @param listener
     *         正在监听扫描结果的listener
     */
    public synchronized void stopScanExternal(@NonNull AppSysCacheTaskNoProgressListener listener) {
        if (sysExternalCacheScanTaskAgent != null) {
            sysExternalCacheScanTaskAgent.removeListener(listener);
        }
    }

    /**
     * 开始清理Internal Cache
     *
     * @param listener
     *         清理结果回调
     */
    public void startCleanInternalCache(AppInternalSysCacheCleanTaskListener listener) {
        startCleanInternalCache(listener, null);
    }

    public void startCleanInternalCache(final AppInternalSysCacheCleanTaskListener listener, Handler handler) {
        startCleanInternalCacheInner(listener, handler);
    }

    /**
     * 停止清理Internal Cache
     */
    public synchronized void stopCleanInternalCache() {
        if (sysInternalCacheCleanTaskAgent != null) {
            sysInternalCacheCleanTaskAgent.cancel();
            sysInternalCacheCleanTaskAgent = null;
        }
    }

    /**
     * 开始清理全部的ExternalCache
     *
     * @param listener
     *         清理结果回调
     */
    public void startFullExternalCacheClean(final AppSysCacheTaskListener listener) {
        startCleanExternalCache(null, listener, null);
    }

    /**
     * 清理选择的ExternalCache
     *
     * @param cleanList
     *         要清理的ExternalCache列表
     * @param listener
     *         清理结果回调
     */
    public void startCleanExternalCache(List<HSAppSysCache> cleanList, AppSysCacheTaskListener listener) {
        startCleanExternalCache(cleanList, listener, null);
    }

    public void startCleanExternalCache(List<HSAppSysCache> cleanList, final AppSysCacheTaskListener cleanTaskListener, Handler handler) {
        if (cleanList == null || cleanList.isEmpty()) {
            Utils.getValidHandler(handler).post(new Runnable() {
                @Override
                public void run() {
                    if (cleanTaskListener != null) {
                        cleanTaskListener.onFailed(FAIL_CLEAN_LIST_EMPTY, "CleanList is empty");
                    }
                }
            });
            return;
        }
        startCleanExternalCacheInner(cleanList, cleanTaskListener, handler);
    }

    /**
     * 停止清理 External Cache
     */
    public synchronized void stopCleanExternalCache() {
        if (sysExternalCacheCleanTaskAgent != null) {
            sysExternalCacheCleanTaskAgent.cancel();
            sysExternalCacheCleanTaskAgent = null;
        }
    }

    private synchronized void startScanInner(boolean withCompletedProcess, AppSysCacheTaskNoProgressListener scanTaskListener, Handler handler) {
        if (sysCacheScanTaskAgent != null && sysCacheScanTaskAgent.isRunning()) {
            sysCacheScanTaskAgent.cancel();
        }
        sysCacheScanTaskAgent = new SysCacheScanTaskAgent();
        sysCacheScanTaskAgent.addListener(scanTaskListener, handler);
        sysCacheScanTaskAgent.start(withCompletedProcess, scanGlobalAppFilter);
    }

    private synchronized void startScanExternalInner(boolean withCompletedProcess, AppSysCacheTaskNoProgressListener scanTaskListener, Handler handler) {
        if (sysExternalCacheScanTaskAgent != null && sysExternalCacheScanTaskAgent.isRunning()) {
            sysExternalCacheScanTaskAgent.cancel();
        }
        sysExternalCacheScanTaskAgent = new SysExternalCacheScanTaskAgent();
        sysExternalCacheScanTaskAgent.addListener(scanTaskListener, handler);
        sysExternalCacheScanTaskAgent.start(withCompletedProcess, scanGlobalAppFilter);
    }

    private synchronized void startCleanInternalCacheInner(final AppInternalSysCacheCleanTaskListener taskListener, Handler handler) {
        if (sysInternalCacheCleanTaskAgent != null && sysInternalCacheCleanTaskAgent.isRunning()) {
            Utils.getValidHandler(handler).post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onFailed(FAIL_IS_RUNNING, "InternalCacheClean is Cleaning");
                    }
                }
            });
            return;
        }
        sysInternalCacheCleanTaskAgent = new SysInternalCacheCleanTaskAgent();
        sysInternalCacheCleanTaskAgent.start(taskListener, handler);
    }

    private synchronized void startCleanExternalCacheInner(List<HSAppSysCache> cleanList, final AppSysCacheTaskListener cleanTaskListener, Handler handler) {
        if (sysExternalCacheCleanTaskAgent != null && sysExternalCacheCleanTaskAgent.isRunning()) {
            Utils.getValidHandler(handler).post(new Runnable() {
                @Override
                public void run() {
                    if (cleanTaskListener != null) {
                        cleanTaskListener.onFailed(FAIL_IS_RUNNING, "ExternalCacheClean is Cleaning");
                    }
                }
            });
            return;
        }
        sysExternalCacheCleanTaskAgent = new SysExternalCacheCleanTaskAgent();
        sysExternalCacheCleanTaskAgent.start(cleanList, cleanTaskListener, handler);
    }
}
