package com.ihs.device.clean.junk.cache.app.nonsys.junk;

import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.ihs.device.clean.junk.cache.app.nonsys.junk.agent.AppJunkCacheCleanTaskAgent;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.agent.AppJunkCacheScanTaskAgent;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class HSAppJunkCacheManager {
    /**
     * 扫描HSAppJunkCache监听，不提供进度
     */
    public interface AppJunkCacheTaskNoProgressListener {
        /**
         * 扫描成功后回调
         *
         * @param appJunkList
         *         扫描出的HSAppJunkCache列表
         * @param dataSize
         *         扫描出的HSAppJunkCache总大小
         */
        void onSucceeded(List<HSAppJunkCache> appJunkList, long dataSize);

        /**
         * 扫描失败后回调
         *
         * @param failCode
         *         扫描失败后的错误码
         * @param failMsg
         *         扫描失败后的错误信息
         */
        void onFailed(@FailCode int failCode, String failMsg);
    }

    /**
     * 清理HSAppJunkCache监听，提供进度
     */
    public interface AppJunkCacheCleanTaskListener extends AppJunkCacheTaskNoProgressListener {
        /**
         * 开始清理
         */
        void onStarted();

        /**
         * 清理进度回调
         *
         * @param processedCount
         *         返回已经进行的进度数
         * @param total
         *         总的进度数
         * @param appJunkCache
         *         返回当前清理的HSAppJunkCache
         */
        void onProgressUpdated(int processedCount, int total, HSAppJunkCache appJunkCache);
    }

    /**
     * 扫描HSAppJunkCache监听，提供进度
     */
    public interface AppJunkCacheScanTaskListener extends AppJunkCacheTaskNoProgressListener {
        /**
         * 开始扫描
         */
        void onStarted();

        /**
         * 扫描进度回调
         *
         * @param processedCount
         *         返回已经进行的进度数
         * @param appJunkCache
         *         返回当前扫描的HSAppJunkCache
         */
        void onProgressUpdated(int processedCount, HSAppJunkCache appJunkCache);
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
        private static final HSAppJunkCacheManager INSTANCE = new HSAppJunkCacheManager();
    }

    public final static int FAIL_UNKNOWN = 0x0;
    public final static int FAIL_CANCEL = 0x1;
    public final static int FAIL_IS_RUNNING = 0x2;
    public final static int FAIL_CLEAN_LIST_EMPTY = 0x3;
    public final static int FAIL_EXCEPTION = 0x4;
    public final static int FAIL_SERVICE_DISCONNECTED = 0x5;

    public static HSAppJunkCacheManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Scan & Clean task
     */
    // scan task Agent
    private AppJunkCacheScanTaskAgent appJunkCacheScanTaskAgent;
    // clean task Agent
    private AppJunkCacheCleanTaskAgent appJunkCacheCleanTaskAgent;

    private HSAppJunkCacheManager() {
    }

    /**
     * 开始扫描HSAppJunkCache，带有进度
     *
     * @param taskListener
     *         扫描结果监听
     */
    public void startScanWithCompletedProgress(AppJunkCacheScanTaskListener taskListener) {
        startScanWithCompletedProgress(taskListener, null);
    }

    public void startScanWithCompletedProgress(AppJunkCacheScanTaskListener taskListener, Handler handler) {
        startScanInner(true, taskListener, handler);
    }

    /**
     * 开始扫描HSAppJunkCache，无进度
     *
     * @param taskListener
     *         扫描结果监听
     */
    public void startScanWithoutProgress(AppJunkCacheTaskNoProgressListener taskListener) {
        startScanWithoutProgress(taskListener, null);
    }

    public synchronized void startScanWithoutProgress(AppJunkCacheTaskNoProgressListener taskListener, Handler handler) {
        if (appJunkCacheScanTaskAgent != null && appJunkCacheScanTaskAgent.isRunning()) {
            appJunkCacheScanTaskAgent.addListener(taskListener, handler);
            return;
        }
        startScanInner(false, taskListener, handler);
    }

    /**
     * 停止扫描HSAppJunkCache
     *
     * @param taskListener
     *         停止的扫描监听
     */
    public synchronized void stopScan(@NonNull AppJunkCacheTaskNoProgressListener taskListener) {
        if (appJunkCacheScanTaskAgent != null) {
            appJunkCacheScanTaskAgent.removeListener(taskListener);
        }
    }

    /**
     * 清理HSAppJunkCache
     *
     * @param taskListener
     *         清理结果监听
     */
    public void startFullClean(AppJunkCacheCleanTaskListener taskListener) {
        startCleanInner(null, taskListener, null);
    }

    /**
     * 清理选定的HSAppJunkCache
     *
     * @param cleanList
     *         选定的HSAppJunkCache列表
     * @param taskListener
     *         清理结果监听
     */
    public void startClean(List<HSAppJunkCache> cleanList, AppJunkCacheCleanTaskListener taskListener) {
        startClean(cleanList, taskListener, null);
    }

    public void startClean(List<HSAppJunkCache> cleanList, final AppJunkCacheCleanTaskListener taskListener, Handler handler) {
        if (cleanList == null || cleanList.isEmpty()) {
            Utils.getValidHandler(handler).post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onFailed(FAIL_CLEAN_LIST_EMPTY, "CleanList is empty");
                    }
                }
            });
            return;
        }
        startCleanInner(cleanList, taskListener, handler);
    }

    /**
     * 停止清理HSAppJunkCache
     */
    public synchronized void stopClean() {
        if (appJunkCacheCleanTaskAgent != null) {
            appJunkCacheCleanTaskAgent.cancel();
            appJunkCacheCleanTaskAgent = null;
        }
    }

    private synchronized void startScanInner(boolean withCompletedProcess, AppJunkCacheTaskNoProgressListener taskListener, Handler handler) {
        if (appJunkCacheScanTaskAgent != null && appJunkCacheScanTaskAgent.isRunning()) {
            HSLog.i("libDevice", "is Scanning && cancel last scan");
            appJunkCacheScanTaskAgent.cancel();
        }
        appJunkCacheScanTaskAgent = new AppJunkCacheScanTaskAgent();
        appJunkCacheScanTaskAgent.addListener(taskListener, handler);
        appJunkCacheScanTaskAgent.start(withCompletedProcess);
    }

    private synchronized void startCleanInner(List<HSAppJunkCache> cleanList, final AppJunkCacheCleanTaskListener taskListener, Handler handler) {
        if (appJunkCacheCleanTaskAgent != null && appJunkCacheCleanTaskAgent.isRunning()) {
            Utils.getValidHandler(handler).post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onFailed(FAIL_IS_RUNNING, "ExternalCacheClean is Cleaning");
                    }
                }
            });
            return;
        }
        appJunkCacheCleanTaskAgent = new AppJunkCacheCleanTaskAgent();
        appJunkCacheCleanTaskAgent.start(cleanList, taskListener, handler);
    }
}
