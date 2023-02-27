package com.ihs.device.clean.junk.cache.app.nonsys.junk.task;

import android.os.Handler;
import android.os.RemoteException;

import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCacheManager;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.IAppJunkCacheScanListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.async.AsyncProcessor.OnProcessListener;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppJunkCacheScanTask {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Map<IAppJunkCacheScanListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private ajs processor;

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * start 的多线程实现,一般要求速度快
     */
    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        HSLog.i("libDevice", "App Junk Scan-----------");
        processor = new ajs(new OnProcessListener<ajsp, List<HSAppJunkCache>>() {
            @Override
            public void onStarted() {
                callBackOnStarted();
            }

            @Override
            public void onProgressUpdated(ajsp progress) {
                callBackOnProgressUpdated(progress.processedCount, progress.appJunkCache);
            }

            @Override
            public void onSucceeded(List<HSAppJunkCache> result) {
                Collections.sort(result, new Comparator<HSAppJunkCache>() {
                    @Override
                    public int compare(HSAppJunkCache lhs, HSAppJunkCache rhs) {
                        return Utils.compare(rhs.getSize(), lhs.getSize());
                    }
                });

                long dataSize = 0L;
                for (HSAppJunkCache appJunkCache : result) {
                    dataSize += appJunkCache.getSize();
                }
                callBackOnSucceeded(result, dataSize);
                HSLog.i("libDevice", "App Junk Scan onCompleted:" + result.size());
            }

            @Override
            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                callBackOnFailed(failCode, e.getMessage());
            }
        });
        processor.executeOnExecutor(JunkServiceImpl.getInstance().getAppJunkCacheThreadPool());
    }

    public void cancel() {
        callBackOnFailed(AsyncProcessor.FAIL_CANCEL, "Canceled");
        if (processor != null) {
            processor.cancel(true);
        }
    }

    public void addListener(IAppJunkCacheScanListener listener) {
        addListener(listener, null);
    }

    public void addListener(IAppJunkCacheScanListener listener, Handler handler) {
        if (listener == null) {
            return;
        }
        callbackListenerMap.put(listener, Utils.getValidHandler(handler));
    }

    public void removeListener(IAppJunkCacheScanListener listener) {
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
            for (final IAppJunkCacheScanListener listener : callbackListenerMap.keySet()) {
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

    private void callBackOnProgressUpdated(final int processedCount, final HSAppJunkCache appJunkCache) {
        if (isRunning.get()) {
            for (final IAppJunkCacheScanListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                listener.onProgressUpdated(processedCount, appJunkCache);
                            } catch (RemoteException e) {
                                HSLog.i("libDevice", "exception:" + e.getMessage());
                            }
                        }
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final List<HSAppJunkCache> apps, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final IAppJunkCacheScanListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                listener.onSucceeded(apps, dataSize);
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

    private void callBackOnFailed(final @AsyncProcessor.FailCode int code, final String errMsg) {
        @HSAppJunkCacheManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            for (final IAppJunkCacheScanListener listener : callbackListenerMap.keySet()) {
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
                transferFailCode = HSAppJunkCacheManager.FAIL_EXCEPTION;
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
