package com.ihs.device.clean.junk.cache.nonapp.pathrule.agent;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.ihs.app.framework.HSApplication;
import com.ihs.device.clean.junk.IJunkService;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCacheManager;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCacheManager.PathFileCacheScanTaskListener;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCacheManager.PathFileCacheTaskListener;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCacheManager.PathFileCacheTaskNoProgressListener;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.IPathFileCacheScanListener;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.IPathFileCacheScanListener.Stub;
import com.ihs.device.clean.junk.service.JunkService;
import com.ihs.device.common.async.BindServiceHelper;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Residual Scan Task Agent 将 Scan 请求发送到 第二进程
 */
public class PathFileCacheScanTaskAgent {
    private final Map<PathFileCacheTaskNoProgressListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private IPathFileCacheScanListener iProcessListener;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(final boolean withCompletedProcess) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        final BindServiceHelper bindServiceHelper = new BindServiceHelper();
        bindServiceHelper.bindService(new Intent(HSApplication.getContext(), JunkService.class), new BindServiceHelper.BindServiceListener() {
            @Override
            public void onServiceBound(IBinder iBinder) {
                if (!isRunning.get()) {
                    bindServiceHelper.unbindService();
                    return;
                }
                try {
                    iProcessListener = new Stub() {
                        @Override
                        public void onStarted() throws RemoteException {
                            callBackOnStarted();
                        }

                        @Override
                        public void onProgressUpdated(int processedCount, HSPathFileCache pathFileCache) throws RemoteException {
                            callBackOnProgressUpdated(processedCount, pathFileCache);
                        }

                        @Override
                        public void onSucceeded(List<HSPathFileCache> result, long dataSize) throws RemoteException {
                            callBackOnSucceeded(result, dataSize);
                            bindServiceHelper.unbindService();
                        }

                        @Override
                        public void onFailed(int code, String failMsg) throws RemoteException {
                            callBackOnFailed(code, failMsg);
                            bindServiceHelper.unbindService();
                        }
                    };
                    IJunkService.Stub.asInterface(iBinder).scanPathFileCache(withCompletedProcess, iProcessListener);
                } catch (Exception e) {
                    callBackOnFailed(HSPathFileCacheManager.FAIL_EXCEPTION, e.getMessage());
                    bindServiceHelper.unbindService();
                }
            }

            @Override
            public void onServiceUnbound() {
                callBackOnFailed(HSPathFileCacheManager.FAIL_SERVICE_DISCONNECTED, "Service Disconnected");
                bindServiceHelper.unbindService();
            }
        });
    }

    public void cancel() {
        callBackOnFailed(HSPathFileCacheManager.FAIL_CANCEL, "Canceled");
        final BindServiceHelper bindServiceHelper = new BindServiceHelper();
        bindServiceHelper.bindService(new Intent(HSApplication.getContext(), JunkService.class), new BindServiceHelper.BindServiceListener() {
            @Override
            public void onServiceBound(IBinder iBinder) {
                try {
                    IJunkService.Stub.asInterface(iBinder).cancelScanPathFileCache();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bindServiceHelper.unbindService();
            }

            @Override
            public void onServiceUnbound() {
                bindServiceHelper.unbindService();
            }
        });
    }

    public void addListener(PathFileCacheTaskNoProgressListener noProgressTaskListener) {
        addListener(noProgressTaskListener, null);
    }

    public void addListener(PathFileCacheTaskNoProgressListener noProgressTaskListener, Handler handler) {
        if (noProgressTaskListener == null) {
            return;
        }
        callbackListenerMap.put(noProgressTaskListener, Utils.getValidHandler(handler));
    }

    public void removeListener(PathFileCacheTaskNoProgressListener noProgressTaskListener) {
        if (noProgressTaskListener == null) {
            return;
        }
        callbackListenerMap.remove(noProgressTaskListener);
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
            for (final PathFileCacheTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                if (listener == null || !(listener instanceof PathFileCacheTaskListener)) {
                    continue;
                }
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                final PathFileCacheTaskListener JunkResidualTaskListener = (PathFileCacheTaskListener) listener;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        JunkResidualTaskListener.onStarted();
                    }
                });
            }
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final HSPathFileCache pathFileCache) {
        if (isRunning.get()) {
            for (final PathFileCacheTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                if (listener == null || !(listener instanceof PathFileCacheScanTaskListener)) {
                    continue;
                }
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                final PathFileCacheScanTaskListener taskListener = (PathFileCacheScanTaskListener) listener;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        taskListener.onProgressUpdated(processedCount, pathFileCache);
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final List<HSPathFileCache> result, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final PathFileCacheTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.onSucceeded(result, dataSize);
                        }
                    }
                });
            }
            removeAllListeners();
        }
    }

    private void callBackOnFailed(final @HSPathFileCacheManager.FailCode int code, final String failMsg) {
        if (isRunning.compareAndSet(true, false)) {
            for (final PathFileCacheTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.onFailed(code, failMsg);
                        }
                    }
                });
            }
            removeAllListeners();
        }
    }
}
