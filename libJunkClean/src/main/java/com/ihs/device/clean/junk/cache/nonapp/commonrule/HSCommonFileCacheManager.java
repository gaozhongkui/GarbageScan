package com.ihs.device.clean.junk.cache.nonapp.commonrule;

import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.ihs.device.clean.junk.cache.nonapp.commonrule.agent.CommonFileCacheCleanTaskAgent;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.agent.CommonFileCacheScanTaskAgent;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

public class HSCommonFileCacheManager {
    /**
     * 扫描HSCommonFileCache监听，不提供进度
     */
    public interface FileScanTaskNoProgressListener {
        /**
         * 扫描成功后回调
         *
         * @param result
         *         扫描结果，Map中存储的键为垃圾类型，值为对应类型垃圾列表
         * @param dataSize
         *         垃圾总大小
         */
        void onSucceeded(Map<String, List<HSCommonFileCache>> result, long dataSize);

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
     * 扫描HSCommonFileCache监听，提供进度
     */
    public interface FileScanTaskListener extends FileScanTaskNoProgressListener {
        /**
         * 扫描开始后回调
         */
        void onStarted();

        /**
         * 扫描进度回调
         *
         * @param processedCount
         *         返回已经进行的进度数
         * @param total
         *         总的进度数
         * @param commonFileCache
         *         返回当前扫描出的HSCommonFileCache
         */
        void onProgressUpdated(int processedCount, int total, HSCommonFileCache commonFileCache);
    }

    /**
     * 清理HSCommonFileCache监听，提供进度
     */
    public interface FileCleanTaskListener {
        /**
         * 清理开始后回调
         */
        void onStarted();

        /**
         * 清理进度回调
         *
         * @param processedCount
         *         返回已经清理的进度数
         * @param total
         *         总的进度数
         * @param commonFileCache
         *         返回当前清理的HSCommonFileCache
         */
        void onProgressUpdated(int processedCount, int total, HSCommonFileCache commonFileCache);

        /**
         * 清理成功后回调
         *
         * @param commonFileCacheList
         *         清理垃圾结果列表
         * @param dataSize
         *         清理垃圾总大小
         */
        void onSucceeded(List<HSCommonFileCache> commonFileCacheList, long dataSize);

        /**
         * 清理失败后回调
         *
         * @param failCode
         *         清理失败后的错误码
         * @param failMsg
         *         清理失败后的错误信息
         */
        void onFailed(@FailCode int failCode, String failMsg);
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
        private static final HSCommonFileCacheManager INSTANCE = new HSCommonFileCacheManager();
    }

    public final static int FAIL_UNKNOWN = 0x0;
    public final static int FAIL_CANCEL = 0x1;
    public final static int FAIL_IS_RUNNING = 0x2;
    public final static int FAIL_CLEAN_LIST_EMPTY = 0x3;
    public final static int FAIL_EXCEPTION = 0x4;
    public final static int FAIL_SERVICE_DISCONNECTED = 0x5;

    public static HSCommonFileCacheManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // scan task
    private CommonFileCacheScanTaskAgent commonFileCacheScanTaskAgent;
    // clean task
    private CommonFileCacheCleanTaskAgent commonFileCacheCleanTaskAgent;

    private HSCommonFileCacheManager() {
    }

    /**
     * 开始扫描HSCommonFileCache类型垃圾，不带有进度
     *
     * @param extensions
     *         扫描垃圾类型后缀列表
     * @param taskListener
     *         扫描结果监听
     */
    public synchronized void startScanWithoutProgress(List<String> extensions, FileScanTaskListener taskListener) {
        startScanWithoutProgress(extensions, taskListener, null);
    }

    public synchronized void startScanWithoutProgress(List<String> extensions, FileScanTaskListener taskListener, Handler handler) {
        if (commonFileCacheScanTaskAgent != null && commonFileCacheScanTaskAgent.isRunning()) {
            commonFileCacheScanTaskAgent.addListener(taskListener, handler);
            return;
        }
        startScanInner(false, extensions, 0, taskListener, handler);
    }

    /**
     * 开始扫描HSCommonFileCache类型垃圾，带有进度
     *
     * @param extensions
     *         扫描垃圾类型后缀列表
     * @param taskListener
     *         扫描结果监听
     */
    public void startScanWithCompletedProgress(List<String> extensions, FileScanTaskListener taskListener) {
        startScanWithCompletedProgress(extensions, taskListener, null);
    }

    public void startScanWithCompletedProgress(List<String> extensions, FileScanTaskListener taskListener, Handler handler) {
        startScanInner(true, extensions, 0, taskListener, handler);
    }

    /**
     * 开始扫描HSCommonFileCache类型垃圾，不带有进度
     *
     * @param extensions
     *         扫描垃圾类型后缀列表
     * @param fileMinSize
     *         扫描最小文件大小
     * @param taskListener
     *         扫描结果监听
     */
    public synchronized void startScanWithoutProgress(List<String> extensions, long fileMinSize, FileScanTaskListener taskListener) {
        startScanWithoutProgress(extensions, fileMinSize, taskListener, null);
    }

    public synchronized void startScanWithoutProgress(List<String> extensions, long fileMinSize, FileScanTaskListener taskListener, Handler handler) {
        if (commonFileCacheScanTaskAgent != null && commonFileCacheScanTaskAgent.isRunning()) {
            commonFileCacheScanTaskAgent.addListener(taskListener, handler);
            return;
        }
        startScanInner(false, extensions, fileMinSize, taskListener, handler);
    }

    /**
     * 开始扫描HSCommonFileCache类型垃圾，带有进度
     *
     * @param extensions
     *         扫描垃圾类型后缀列表
     * @param fileMinSize
     *         扫描最小文件大小
     * @param taskListener
     *         扫描结果监听
     */
    public void startScanWithCompletedProgress(List<String> extensions, long fileMinSize, FileScanTaskListener taskListener) {
        startScanWithCompletedProgress(extensions, fileMinSize, taskListener, null);
    }

    public void startScanWithCompletedProgress(List<String> extensions, long fileMinSize, FileScanTaskListener taskListener, Handler handler) {
        startScanInner(true, extensions, fileMinSize, taskListener, handler);
    }

    /**
     * 停止HSCommonFileCache类型垃圾扫描
     *
     * @param taskListener
     *         停止扫描结果监听
     */
    public synchronized void stopScan(@NonNull FileScanTaskListener taskListener) {
        if (commonFileCacheScanTaskAgent != null) {
            commonFileCacheScanTaskAgent.removeListener(taskListener);
        }
    }

    /**
     * 清理所有的HSCommonFileCache类型垃圾
     *
     * @param cleanTaskListener
     *         清理结果监听
     */
    public void startFullClean(FileCleanTaskListener cleanTaskListener) {
        startCleanInner(null, cleanTaskListener, null);
    }

    /**
     * 清理选择的HSCommonFileCache类型垃圾
     *
     * @param cleanList
     *         选择清理的HSCommonFileCache类型垃圾列表
     * @param listener
     *         清理结果监听
     */
    public void startClean(List<HSCommonFileCache> cleanList, FileCleanTaskListener listener) {
        startClean(cleanList, listener, null);
    }

    public void startClean(List<HSCommonFileCache> cleanList, final FileCleanTaskListener taskListener, Handler handler) {
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
     * 停止当前所有的清理活动
     */
    public synchronized void stopClean() {
        if (commonFileCacheCleanTaskAgent != null) {
            commonFileCacheCleanTaskAgent.cancel();
            commonFileCacheCleanTaskAgent = null;
        }
    }

    private synchronized void startScanInner(boolean withCompletedProcess, List<String> extensions, long fileMinSize, FileScanTaskListener taskListener,
            Handler handler) {
        if (commonFileCacheScanTaskAgent != null && commonFileCacheScanTaskAgent.isRunning()) {
            HSLog.i("libDevice", "is Scanning && cancel last scan");
            commonFileCacheScanTaskAgent.cancel();
        }
        commonFileCacheScanTaskAgent = new CommonFileCacheScanTaskAgent();
        commonFileCacheScanTaskAgent.addListener(taskListener, handler);
        commonFileCacheScanTaskAgent.start(withCompletedProcess, extensions, fileMinSize);
    }

    private synchronized void startCleanInner(List<HSCommonFileCache> cleanList, final FileCleanTaskListener taskListener, Handler handler) {
        if (commonFileCacheCleanTaskAgent != null && commonFileCacheCleanTaskAgent.isRunning()) {
            HSLog.i("libDevice", "ExternalCacheClean is Cleaning");
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
        commonFileCacheCleanTaskAgent = new CommonFileCacheCleanTaskAgent();
        commonFileCacheCleanTaskAgent.start(cleanList, taskListener, handler);
    }
}
