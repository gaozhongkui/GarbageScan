package com.ihs.device.clean.junk.cache.nonapp.pathrule.task;

import android.os.Handler;

import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCacheManager;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.IPathFileCacheCleanListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.async.AsyncProcessor.OnProcessListener;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Arthur on 16/6/1
 */
public class PathFileCacheCleanTask {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private IPathFileCacheCleanListener iPathFileCacheCleanListener;
    private PathFileCacheCleanProcessor processor;
    private Handler callBackHandler;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(List<HSPathFileCache> toCleanList, IPathFileCacheCleanListener listener) {
        start(toCleanList, listener, null);
    }

    /**
     * start 的多线程实现,一般要求速度快
     */
    public void start(final List<HSPathFileCache> toCleanList, IPathFileCacheCleanListener listener, Handler callBackHandler) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        this.iPathFileCacheCleanListener = listener;
        this.callBackHandler = Utils.getValidHandler(callBackHandler);

        HSLog.i("libDevice", "PathFileClean-----------");
        processor = new PathFileCacheCleanProcessor(new OnProcessListener<PathFileCacheCleanTaskProgress, List<HSPathFileCache>>() {
            private long dataSize = 0L;
            private int processedCount = 0;

            @Override
            public void onStarted() {
                callBackOnStarted();
            }

            @Override
            public void onProgressUpdated(PathFileCacheCleanTaskProgress progress) {
                HSPathFileCache pathFileCache = progress.pathFileCache;
                dataSize += pathFileCache.getSize();
                processedCount = progress.processedCount;
                callBackOnProgressUpdated(processedCount, progress.total, pathFileCache);
                HSLog.i("libDevice", "PathFileClean: " + processedCount + "/" + progress.total + " size:" + pathFileCache.getSize());
            }

            @Override
            public void onSucceeded(List<HSPathFileCache> result) {
                callBackOnSucceeded(result, dataSize);
                HSLog.i("libDevice", "PathFileClean onCompleted");
            }

            @Override
            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                callBackOnFailed(failCode, e.getMessage());
            }
        });
        processor.executeOnExecutor(JunkServiceImpl.getInstance().getPathFileCacheThreadPool(), toCleanList);
    }

    public void cancel() {
        callBackOnFailed(AsyncProcessor.FAIL_CANCEL, "Canceled");
        if (processor != null) {
            processor.cancel(true);
            processor = null;
        }
    }

    private void callBackOnStarted() {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iPathFileCacheCleanListener != null) {
                        try {
                            iPathFileCacheCleanListener.onStarted();
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSPathFileCache pathFileCache) {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iPathFileCacheCleanListener != null) {
                        try {
                            iPathFileCacheCleanListener.onProgressUpdated(processedCount, total, pathFileCache);
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnSucceeded(final List<HSPathFileCache> pathFileCacheList, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iPathFileCacheCleanListener != null) {
                        try {
                            iPathFileCacheCleanListener.onSucceeded(pathFileCacheList, dataSize);
                            callBackHandler = null;
                            iPathFileCacheCleanListener = null;
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnFailed(final @AsyncProcessor.FailCode int code, final String errMsg) {
        @HSPathFileCacheManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iPathFileCacheCleanListener != null) {
                        try {
                            iPathFileCacheCleanListener.onFailed(failCode, errMsg);
                            callBackHandler = null;
                            iPathFileCacheCleanListener = null;
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    @HSPathFileCacheManager.FailCode
    private int getManagerFailCode(@AsyncProcessor.FailCode int failCode) {
        @HSPathFileCacheManager.FailCode int transferFailCode = HSPathFileCacheManager.FAIL_UNKNOWN;
        switch (failCode) {
            case AsyncProcessor.FAIL_CANCEL:
                transferFailCode = HSPathFileCacheManager.FAIL_CANCEL;
                break;
            case AsyncProcessor.FAIL_EXCEPTION:
            case AsyncProcessor.FAIL_REJECTED_EXECUTION:
                transferFailCode = HSPathFileCacheManager.FAIL_EXCEPTION;
                break;
            case AsyncProcessor.FAIL_IS_RUNNING:
                transferFailCode = HSPathFileCacheManager.FAIL_IS_RUNNING;
                break;
            case AsyncProcessor.FAIL_UNKNOWN:
                transferFailCode = HSPathFileCacheManager.FAIL_UNKNOWN;
                break;
        }
        return transferFailCode;
    }
}
