package com.ihs.device.clean.junk.cache.app.sys.task;

import android.os.Handler;

import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCacheManager;
import com.ihs.device.clean.junk.cache.app.sys.IAppSysCacheProcessListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.HSAppFilter;
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

public class SysExternalCacheScanTask {
    private final Map<IAppSysCacheProcessListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private SysExternalCacheScanProcessor processor;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(final HSAppFilter hsAppFilter) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }
        callBackOnStarted();

        processor = new SysExternalCacheScanProcessor(hsAppFilter, new OnProcessListener<SysExternalCacheTaskProgress, List<HSAppSysCache>>() {
            @Override
            public void onStarted() {
            }

            @Override
            public void onProgressUpdated(SysExternalCacheTaskProgress taskProgress) {
                // HSLog.i("libDevice", "processedCount.get():" + taskProgress.processedCount + "/" + taskProgress.total + " result:" + taskProgress.appSysCache.getAppName());
            }

            @Override
            public void onSucceeded(List<HSAppSysCache> result) {
                Collections.sort(result, new Comparator<HSAppSysCache>() {
                    @Override
                    public int compare(HSAppSysCache lhs, HSAppSysCache rhs) {
                        return Utils.compare(rhs.getSize(), lhs.getSize());
                    }
                });
                long dataSize = 0;
                for (HSAppSysCache appSysCache : result) {
                    dataSize += appSysCache.getSize();
                }
                callBackOnSucceeded(result, dataSize);
            }

            @Override
            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                callBackOnFailed(failCode, e.getMessage());
            }
        });
        processor.executeOnExecutor(JunkServiceImpl.getInstance().getAppSysCacheThreadPool());

    }

    public void cancel() {
        callBackOnFailed(AsyncProcessor.FAIL_CANCEL, "Canceled");
        if (processor != null) {
            processor.cancel(true);
        }
    }

    public void addListener(IAppSysCacheProcessListener listener) {
        addListener(listener, null);
    }

    public void addListener(IAppSysCacheProcessListener listener, Handler handler) {
        if (listener == null) {
            return;
        }
        callbackListenerMap.put(listener, Utils.getValidHandler(handler));
    }

    public void removeListener(IAppSysCacheProcessListener listener) {
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
            for (final IAppSysCacheProcessListener listener : callbackListenerMap.keySet()) {
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

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSAppSysCache junkAppCache) {
        if (isRunning.get()) {
            for (final IAppSysCacheProcessListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                listener.onProgressUpdated(processedCount, total, junkAppCache);
                            } catch (Exception e) {
                                HSLog.i("libDevice", "exception:" + e.getMessage());
                            }
                        }
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final List<HSAppSysCache> apps, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final IAppSysCacheProcessListener listener : callbackListenerMap.keySet()) {
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
        @HSAppSysCacheManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            for (final IAppSysCacheProcessListener listener : callbackListenerMap.keySet()) {
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

    @HSAppSysCacheManager.FailCode
    private int getManagerFailCode(@AsyncProcessor.FailCode int failCode) {
        @HSAppSysCacheManager.FailCode int transferFailCode = HSAppSysCacheManager.FAIL_UNKNOWN;
        switch (failCode) {
            case AsyncProcessor.FAIL_CANCEL:
                transferFailCode = HSAppSysCacheManager.FAIL_CANCEL;
                break;
            case AsyncProcessor.FAIL_EXCEPTION:
            case AsyncProcessor.FAIL_REJECTED_EXECUTION:
                transferFailCode = HSAppSysCacheManager.FAIL_EXCEPTION;
                break;
            case AsyncProcessor.FAIL_IS_RUNNING:
                transferFailCode = HSAppSysCacheManager.FAIL_IS_RUNNING;
                break;
            case AsyncProcessor.FAIL_UNKNOWN:
                transferFailCode = HSAppSysCacheManager.FAIL_UNKNOWN;
                break;
        }
        return transferFailCode;
    }
}
