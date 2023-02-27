package com.ihs.device.clean.junk.cache.app.nonsys.junk.task;

import android.os.Handler;

import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCacheManager;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.IAppJunkCacheCleanListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.async.AsyncProcessor.OnProcessListener;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppJunkCacheCleanTask {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private IAppJunkCacheCleanListener iListener;
    private AppJunkCacheCleanProcessor processor;
    private Handler callBackHandler;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(List<HSAppJunkCache> toCleanList, IAppJunkCacheCleanListener listener) {
        start(toCleanList, listener, null);
    }

    /**
     * start 的多线程实现,一般要求速度快
     */
    public void start(final List<HSAppJunkCache> toCleanList, IAppJunkCacheCleanListener listener, Handler callBackHandler) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        this.iListener = listener;
        this.callBackHandler = Utils.getValidHandler(callBackHandler);

        HSLog.i("libDevice", "AppJunk Clean-----------");
        processor = new AppJunkCacheCleanProcessor(new OnProcessListener<AppJunkCacheCleanTaskProgress, List<HSAppJunkCache>>() {
            private long dataSize = 0L;
            private int processedCount = 0;

            @Override
            public void onStarted() {
                callBackOnStarted();
            }

            @Override
            public void onProgressUpdated(AppJunkCacheCleanTaskProgress progress) {
                HSAppJunkCache appJunkCache = progress.appJunk;
                dataSize += appJunkCache.getSize();
                processedCount = progress.processedCount;
                callBackOnProgressUpdated(processedCount, progress.total, appJunkCache);
                HSLog.i("libDevice", "AppJunk Clean: " + processedCount + "/" + progress.total + " pkg:" + appJunkCache.getPackageName() + " size:" + dataSize);
            }

            @Override
            public void onSucceeded(List<HSAppJunkCache> result) {
                callBackOnSucceeded(result, dataSize);
                HSLog.i("libDevice", "AppJunk Clean onCompleted");
            }

            @Override
            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                callBackOnFailed(failCode, e.getMessage());
            }
        });
        processor.executeOnExecutor(JunkServiceImpl.getInstance().getAppJunkCacheThreadPool(), toCleanList);
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
                    if (iListener != null) {
                        try {
                            iListener.onStarted();
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSAppJunkCache appJunkCache) {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iListener != null) {
                        try {
                            iListener.onProgressUpdated(processedCount, total, appJunkCache);
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnSucceeded(final List<HSAppJunkCache> apps, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iListener != null) {
                        try {
                            iListener.onSucceeded(apps, dataSize);
                            callBackHandler = null;
                            iListener = null;
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnFailed(final @AsyncProcessor.FailCode int code, final String errMsg) {
        @HSAppJunkCacheManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iListener != null) {
                        try {
                            iListener.onFailed(failCode, errMsg);
                            callBackHandler = null;
                            iListener = null;
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    @HSAppJunkCacheManager.FailCode
    private int getManagerFailCode(@AsyncProcessor.FailCode int failCode) {
        @HSAppJunkCacheManager.FailCode int transferFailCode = HSAppJunkCacheManager.FAIL_UNKNOWN;
        switch (failCode) {
            case AsyncProcessor.FAIL_CANCEL:
                transferFailCode = HSAppJunkCacheManager.FAIL_CANCEL;
                break;
            case AsyncProcessor.FAIL_EXCEPTION:
                transferFailCode = HSAppJunkCacheManager.FAIL_EXCEPTION;
                break;
            case AsyncProcessor.FAIL_IS_RUNNING:
                transferFailCode = HSAppJunkCacheManager.FAIL_IS_RUNNING;
                break;
            case AsyncProcessor.FAIL_REJECTED_EXECUTION:
                transferFailCode = HSAppJunkCacheManager.FAIL_EXCEPTION;
                break;
            case AsyncProcessor.FAIL_UNKNOWN:
                transferFailCode = HSAppJunkCacheManager.FAIL_UNKNOWN;
                break;
        }
        return transferFailCode;
    }
}
