package com.ihs.device.clean.junk.cache.nonapp.pathrule.task;

import android.os.Handler;
import android.os.RemoteException;

import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCacheManager;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.IPathFileCacheScanListener;
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

public class PathFileCacheScanTask {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Map<IPathFileCacheScanListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private prs processor;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        HSLog.i("libDevice", "PathFileScan-----------");
        processor = new prs(new OnProcessListener<prsp, List<HSPathFileCache>>() {
            @Override
            public void onStarted() {
                callBackOnStarted();
            }

            public void onProgressUpdated(prsp progress) {
                callBackOnProgressUpdated(progress.processedCount, progress.pathFileCache);
            }

            @Override
            public void onSucceeded(List<HSPathFileCache> result) {
                Collections.sort(result, new Comparator<HSPathFileCache>() {
                    @Override
                    public int compare(HSPathFileCache lhs, HSPathFileCache rhs) {
                        return Utils.compare(rhs.getSize(), lhs.getSize());
                    }
                });

                long totalDataSize = 0L;
                for (HSPathFileCache pathFileCache : result) {
                    totalDataSize += pathFileCache.getSize();
                }
                callBackOnSucceeded(result, totalDataSize);
                HSLog.i("libDevice", "PathFileScan onCompleted");
            }

            @Override
            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                callBackOnFailed(failCode, e.getMessage());
            }
        });
        processor.executeOnExecutor(JunkServiceImpl.getInstance().getPathFileCacheThreadPool());
    }

    public void cancel() {
        callBackOnFailed(AsyncProcessor.FAIL_CANCEL, "Canceled");
        if (processor != null) {
            HSLog.i("libDevice", "cancel");
            processor.cancel(true);
        }
    }

    public void addListener(IPathFileCacheScanListener listener) {
        addListener(listener, null);
    }

    public void addListener(IPathFileCacheScanListener listener, Handler handler) {
        if (listener == null) {
            return;
        }
        HSLog.i("libDevice", "4444-----:" + listener);
        callbackListenerMap.put(listener, Utils.getValidHandler(handler));
    }

    public void removeListener(IPathFileCacheScanListener listener) {
        if (listener == null) {
            return;
        }
        callbackListenerMap.remove(listener);
        HSLog.i("libDevice", "callbackListenerMap:" + callbackListenerMap.size() + " " + listener);
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
            for (final IPathFileCacheScanListener listener : callbackListenerMap.keySet()) {
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

    private void callBackOnProgressUpdated(final int processedCount, final HSPathFileCache pathFileCache) {
        if (isRunning.get()) {
            for (final IPathFileCacheScanListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                listener.onProgressUpdated(processedCount, pathFileCache);
                            } catch (RemoteException e) {
                                HSLog.i("libDevice", "exception:" + e.getMessage());
                            }
                        }
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final List<HSPathFileCache> apps, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final IPathFileCacheScanListener listener : callbackListenerMap.keySet()) {
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
        @HSPathFileCacheManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            for (final IPathFileCacheScanListener listener : callbackListenerMap.keySet()) {
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
