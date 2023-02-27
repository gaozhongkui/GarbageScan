package com.ihs.device.clean.junk.cache.app.sys.task;

import android.os.Handler;

import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCacheManager;
import com.ihs.device.clean.junk.cache.app.sys.IAppSysCacheProcessListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.HSAppFilter;
import com.ihs.device.common.HSAppInfoUtils;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.async.AsyncProcessor.OnProcessListener;
import com.ihs.device.common.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SysCacheScanTask {
    private final List<SysCacheScanProcessor> processorList = new CopyOnWriteArrayList<>();

    private final Map<IAppSysCacheProcessListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(final HSAppFilter hsAppFilter) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }
        processorList.clear();
        callBackOnStarted();
        Thread deepThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final AtomicLong dataSize = new AtomicLong(0);
                    final AtomicInteger processedCount = new AtomicInteger(0);
                    final List<HSAppSysCache> resultList = new ArrayList<>();
                    final List<HSAppSysCache> junkAppCaches = HSAppInfoUtils.getInstalledAppInfoList(HSAppSysCache.class,
                            hsAppFilter == null ? new HSAppFilter() : hsAppFilter);

                    final int totalAppCount = junkAppCaches.size();
                    if (totalAppCount == 0) {
                        callBackOnSucceeded(resultList, 0);
                        return;
                    }
                    for (final HSAppSysCache junkAppCache : junkAppCaches) {
                        if (!isRunning.get()) {
                            break;
                        }
                        SysCacheScanProcessor processor = new SysCacheScanProcessor(new OnProcessListener<Void, HSAppSysCache>() {
                            @Override
                            public void onStarted() {
                            }

                            @Override
                            public void onProgressUpdated(Void aVoid) {
                            }

                            @Override
                            public void onSucceeded(HSAppSysCache result) {
                                processedCount.incrementAndGet();
                                if (result != null) {
                                    dataSize.addAndGet(result.getSize());
                                    resultList.add(result);
                                    HSLog.i("libDevice", "processedCount.get():" + processedCount.get() + "/" + totalAppCount + " result:" + result.getAppName());
                                    callBackOnProgressUpdated(processedCount.get(), totalAppCount, result);
                                }

                                if (processedCount.get() == totalAppCount) {
                                    Collections.sort(resultList, new Comparator<HSAppSysCache>() {
                                        @Override
                                        public int compare(HSAppSysCache lhs, HSAppSysCache rhs) {
                                            return Utils.compare(rhs.getSize(), lhs.getSize());
                                        }
                                    });

                                    callBackOnSucceeded(resultList, dataSize.get());
                                }
                            }

                            @Override
                            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                                callBackOnFailed(failCode, e.getMessage());
                            }
                        });
                        processor.executeOnExecutor(JunkServiceImpl.getInstance().getAppSysCacheThreadPool(), junkAppCache);
                        processorList.add(processor);
                    }
                } catch (Exception e) {
                    if (HSLog.isDebugging()) {
                        throw e;
                    }
                    e.printStackTrace();
                    callBackOnFailed(AsyncProcessor.FAIL_EXCEPTION, e.getMessage());
                }
            }
        });
        deepThread.start();
    }

    public void cancel() {
        callBackOnFailed(AsyncProcessor.FAIL_CANCEL, "Canceled");
        for (AsyncProcessor processor : processorList) {
            try {
                if (processor != null) {
                    processor.cancel(true);
                }
            } catch (Exception e) {
                if (HSLog.isDebugging()) {
                    throw e;
                }
                e.printStackTrace();
                HSLog.i("libDevice", "err:" + e.getMessage());
            }
        }
        processorList.clear();
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
