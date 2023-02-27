package com.ihs.device.clean.junk.cache.nonapp.commonrule.task;

import android.os.Handler;

import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCacheManager;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.ICommonFileCacheScanListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.async.AsyncProcessor.OnProcessListener;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommonFileCacheScanTask {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Map<ICommonFileCacheScanListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private CommonFileCacheScanProcessor processor;

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * start 的多线程实现,一般要求速度快
     */
    public void start(List<String> extensions, long fileMinSize) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        HSLog.i("libDevice", "HSCommonFileCache scan-----------");
        processor = new CommonFileCacheScanProcessor(fileMinSize, new OnProcessListener<CommonFileCacheTaskProgress, Map<String, List<HSCommonFileCache>>>() {
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
                HSLog.i("libDevice", "CommonFileCache scan: " + processedCount + " commonFileCache:" + commonFileCache.getFilePath() + " size:" + dataSize);
            }

            @Override
            public void onSucceeded(Map<String, List<HSCommonFileCache>> result) {
                callBackOnSucceeded(dataSize);
                HSLog.i("libDevice", "HSCommonFileCache scan onCompleted");
            }

            @Override
            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                callBackOnFailed(failCode, e.getMessage());
            }
        });
        processor.executeOnExecutor(JunkServiceImpl.getInstance().getCommonFileCacheThreadPool(), extensions);
    }

    public void cancel() {
        callBackOnFailed(AsyncProcessor.FAIL_CANCEL, "Canceled");
        if (processor != null) {
            processor.cancel(true);
        }
    }

    public void addListener(ICommonFileCacheScanListener listener) {
        addListener(listener, null);
    }

    public void addListener(ICommonFileCacheScanListener listener, Handler handler) {
        if (listener == null) {
            return;
        }
        callbackListenerMap.put(listener, Utils.getValidHandler(handler));
    }

    public void removeListener(ICommonFileCacheScanListener listener) {
        if (listener == null) {
            return;
        }
        callbackListenerMap.remove(listener);
        if (callbackListenerMap.isEmpty()) {
            cancel();
        }
    }

    public void removeAllListeners() {
        cancel();
        callbackListenerMap.clear();
    }

    private void callBackOnStarted() {
        if (isRunning.get()) {
            for (final ICommonFileCacheScanListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                listener.onStarted();
                            } catch (Exception e) {
                                HSLog.i("libDevice", "exception:" + e.getMessage());
                            }
                        }
                    }
                });
            }
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSCommonFileCache commonFileCache) {
        if (isRunning.get()) {
            for (final ICommonFileCacheScanListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                listener.onProgressUpdated(processedCount, total, commonFileCache);
                            } catch (Exception e) {
                                HSLog.i("libDevice", "exception:" + e.getMessage());
                            }
                        }
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final ICommonFileCacheScanListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                listener.onSucceeded(dataSize);
                            } catch (Exception e) {
                                e.printStackTrace();
                                HSLog.i("libDevice", "exception:" + e.getMessage());
                            }
                        }
                    }
                });
            }
            removeAllListeners();
        }
        Utils.trySystemGC();
    }

    private void callBackOnFailed(final @AsyncProcessor.FailCode int code, final String errMsg) {
        @HSCommonFileCacheManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            for (final ICommonFileCacheScanListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                listener.onFailed(failCode, errMsg);
                            } catch (Exception e) {
                                HSLog.i("libDevice", "exception:" + e.getMessage());
                            }
                        }
                    }
                });
            }
            removeAllListeners();
        }
        Utils.trySystemGC();
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
