package com.ihs.device.clean.junk.cache.nonapp.commonrule.task;

import android.os.Handler;

import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCacheManager;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.ICommonFileCacheCleanListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.async.AsyncProcessor.OnProcessListener;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommonFileCacheCleanTask {

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ICommonFileCacheCleanListener processListener;
    private CommonFileCacheCleanProcessor processor;
    private Handler callBackHandler;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(List<HSCommonFileCache> cleanList, ICommonFileCacheCleanListener listener) {
        start(cleanList, listener, null);
    }

    /**
     * start 的多线程实现,一般要求速度快
     */
    public void start(List<HSCommonFileCache> cleanList, ICommonFileCacheCleanListener listener, Handler callBackHandler) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        this.processListener = listener;
        this.callBackHandler = Utils.getValidHandler(callBackHandler);

        HSLog.i("libDevice", "CommonFileCache clean-----------");
        processor = new CommonFileCacheCleanProcessor(new OnProcessListener<CommonFileCacheTaskProgress, List<HSCommonFileCache>>() {
            private long dataSize = 0L;
            private int processedCount = 0;
            private int total = 0;

            @Override
            public void onStarted() {
                callBackOnStarted();
            }

            @Override
            public void onProgressUpdated(CommonFileCacheTaskProgress progress) {
                HSCommonFileCache commonFileCache = progress.commonFileCache;
                dataSize += commonFileCache.getSize();
                processedCount = progress.processedCount;
                total = progress.total;
                callBackOnProgressUpdated(processedCount, total, commonFileCache);
                HSLog.i("libDevice", "CommonFileCache clean: " + processedCount + "/" + total + " filePath:" + commonFileCache.getFilePath() + " size:" +
                    commonFileCache.getSize());
            }

            @Override
            public void onSucceeded(List<HSCommonFileCache> result) {
                callBackOnSucceeded(dataSize);
                HSLog.i("libDevice", "CommonFileCache clean onCompleted");
            }

            @Override
            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                callBackOnFailed(failCode, e.getMessage());
            }
        });
        processor.executeOnExecutor(JunkServiceImpl.getInstance().getCommonFileCacheThreadPool(), cleanList);
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
                    if (processListener != null) {
                        try {
                            processListener.onStarted();
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSCommonFileCache commonFileCache) {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (processListener != null) {
                        try {
                            processListener.onProgressUpdated(processedCount, total, commonFileCache);
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnSucceeded(final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (processListener != null) {
                        try {
                            processListener.onSucceeded(dataSize);
                            callBackHandler = null;
                            processListener = null;
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnFailed(final @AsyncProcessor.FailCode int code, final String errMsg) {
        @HSCommonFileCacheManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (processListener != null) {
                        try {
                            processListener.onFailed(failCode, errMsg);
                            callBackHandler = null;
                            processListener = null;
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    @HSCommonFileCacheManager.FailCode
    private int getManagerFailCode(@AsyncProcessor.FailCode int failCode) {
        @HSCommonFileCacheManager.FailCode int transferFailCode = HSCommonFileCacheManager.FAIL_UNKNOWN;
        switch (failCode) {
            case AsyncProcessor.FAIL_CANCEL:
                transferFailCode = HSCommonFileCacheManager.FAIL_CANCEL;
                break;
            case AsyncProcessor.FAIL_EXCEPTION:
            case AsyncProcessor.FAIL_REJECTED_EXECUTION:
                transferFailCode = HSCommonFileCacheManager.FAIL_EXCEPTION;
                break;
            case AsyncProcessor.FAIL_IS_RUNNING:
                transferFailCode = HSCommonFileCacheManager.FAIL_IS_RUNNING;
                break;
            case AsyncProcessor.FAIL_UNKNOWN:
                transferFailCode = HSCommonFileCacheManager.FAIL_UNKNOWN;
                break;
        }
        return transferFailCode;
    }
}