package com.ihs.device.clean.junk.cache.nonapp.pathrule;

import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.ihs.device.clean.junk.cache.nonapp.pathrule.agent.PathFileCacheCleanTaskAgent;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.agent.PathFileCacheScanTaskAgent;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class HSPathFileCacheManager {
    /**
     * 扫描HSPathFileCache类型垃圾监听，不提供进度
     */
    public interface PathFileCacheTaskNoProgressListener {
        /**
         * 扫描结束，并成功后回调
         *
         * @param pathFileCacheList
         *         扫描出的HSPathFileCache类型垃圾列表
         * @param dataSize
         *         扫描出的HSPathFileCache垃圾总大小
         */
        void onSucceeded(List<HSPathFileCache> pathFileCacheList, long dataSize);

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
     * 清理HSPathFileCache类型垃圾监听，提供进度
     */
    public interface PathFileCacheTaskListener extends PathFileCacheTaskNoProgressListener {
        /**
         * 清理开始后回调
         */
        void onStarted();

        /**
         * 清理进度回调
         *
         * @param processedCount
         *         返回已经进行的进度数
         * @param total
         *         总的进度数
         * @param pathFileCache
         *         返回当前清理的HSPathFileCache
         */
        void onProgressUpdated(int processedCount, int total, HSPathFileCache pathFileCache);
    }

    /**
     * 扫描HSPathFileCache类型垃圾监听，提供进度
     */
    public interface PathFileCacheScanTaskListener extends PathFileCacheTaskNoProgressListener {
        /**
         * 扫描开始后回调
         */
        void onStarted();

        /**
         * 扫描进度回调
         *
         * @param processedCount
         *         返回已经进行的进度数
         * @param pathFileCache
         *         返回当前扫描出的HSPathFileCache
         */
        void onProgressUpdated(int processedCount, HSPathFileCache pathFileCache);
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
        private static final HSPathFileCacheManager INSTANCE = new HSPathFileCacheManager();
    }

    public final static int FAIL_UNKNOWN = 0x0;
    public final static int FAIL_CANCEL = 0x1;
    public final static int FAIL_IS_RUNNING = 0x2;
    public final static int FAIL_CLEAN_LIST_EMPTY = 0x3;
    public final static int FAIL_EXCEPTION = 0x4;
    public final static int FAIL_SERVICE_DISCONNECTED = 0x5;

    public static HSPathFileCacheManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Scan & Clean task
     */
    // scan task Agent
    private PathFileCacheScanTaskAgent pathFileCacheScanTaskAgent;
    // clean task Agent
    private PathFileCacheCleanTaskAgent pathFileCacheCleanTaskAgent;

    private HSPathFileCacheManager() {
    }

    /**
     * 扫描HSPathFileCache类型垃圾，提供进度
     *
     * @param taskListener
     *         扫描结果监听
     */
    public void startScanWithCompletedProgress(PathFileCacheScanTaskListener taskListener) {
        startScanWithCompletedProgress(taskListener, null);
    }

    public void startScanWithCompletedProgress(PathFileCacheScanTaskListener taskListener, Handler handler) {
        startScanInner(true, taskListener, handler);
    }

    /**
     * 扫描HSPathFileCache类型垃圾，不提供进度
     *
     * @param taskListener
     *         扫描结果监听
     */
    public void startScanWithoutProgress(PathFileCacheTaskNoProgressListener taskListener) {
        startScanWithoutProgress(taskListener, null);
    }

    public synchronized void startScanWithoutProgress(PathFileCacheTaskNoProgressListener taskListener, Handler handler) {
        if (pathFileCacheScanTaskAgent != null && pathFileCacheScanTaskAgent.isRunning()) {
            HSLog.i("libDevice", "is Scanning");
            pathFileCacheScanTaskAgent.addListener(taskListener, handler);
            return;
        }
        startScanInner(false, taskListener, handler);
    }

    /**
     * 停止扫描HSPathFileCache类型垃圾
     *
     * @param taskListener
     *         要停止的扫描监听
     */
    public synchronized void stopScan(@NonNull PathFileCacheTaskNoProgressListener taskListener) {
        if (pathFileCacheScanTaskAgent != null) {
            pathFileCacheScanTaskAgent.removeListener(taskListener);
        }
    }

    /**
     * 清理所有的HSPathFileCache类型垃圾
     *
     * @param taskListener
     *         清理结果监听
     */
    public void startFullClean(PathFileCacheTaskListener taskListener) {
        startCleanInner(null, taskListener, null);
    }

    /**
     * 清理选择的HSPathFileCache类型垃圾
     *
     * @param cleanList
     *         选择的HSPathFileCache类型垃圾列表
     * @param taskListener
     *         清理结果监听
     */
    public void startClean(List<HSPathFileCache> cleanList, PathFileCacheTaskListener taskListener) {
        startClean(cleanList, taskListener, null);
    }

    public void startClean(List<HSPathFileCache> cleanList, final PathFileCacheTaskListener taskListener, Handler handler) {
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
     * 停止所有HSPathFileCache类型的清理
     */
    public synchronized void stopClean() {
        if (pathFileCacheCleanTaskAgent != null) {
            pathFileCacheCleanTaskAgent.cancel();
            pathFileCacheCleanTaskAgent = null;
        }
    }

    private synchronized void startScanInner(boolean withCompletedProcess, PathFileCacheTaskNoProgressListener taskListener, Handler handler) {
        if (pathFileCacheScanTaskAgent != null && pathFileCacheScanTaskAgent.isRunning()) {
            HSLog.i("libDevice", "is Scanning && cancel last scan");
            pathFileCacheScanTaskAgent.cancel();
        }
        pathFileCacheScanTaskAgent = new PathFileCacheScanTaskAgent();
        pathFileCacheScanTaskAgent.addListener(taskListener, handler);
        pathFileCacheScanTaskAgent.start(withCompletedProcess);
    }

    private synchronized void startCleanInner(List<HSPathFileCache> cleanList, final PathFileCacheTaskListener taskListener, Handler handler) {
        if (pathFileCacheCleanTaskAgent != null && pathFileCacheCleanTaskAgent.isRunning()) {
            Utils.getValidHandler(handler).post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onFailed(FAIL_IS_RUNNING, "PathFileCache is Cleaning");
                    }
                }
            });
            return;
        }
        pathFileCacheCleanTaskAgent = new PathFileCacheCleanTaskAgent();
        pathFileCacheCleanTaskAgent.start(cleanList, taskListener, handler);
    }
}
