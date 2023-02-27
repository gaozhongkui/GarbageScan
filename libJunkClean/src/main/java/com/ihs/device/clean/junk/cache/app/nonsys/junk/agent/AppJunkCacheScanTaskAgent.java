package com.ihs.device.clean.junk.cache.app.nonsys.junk.agent;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.ihs.app.framework.HSApplication;
import com.ihs.device.clean.junk.IJunkService;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCacheManager;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCacheManager.AppJunkCacheCleanTaskListener;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCacheManager.AppJunkCacheScanTaskListener;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCacheManager.AppJunkCacheTaskNoProgressListener;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.IAppJunkCacheScanListener;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.IAppJunkCacheScanListener.Stub;
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
public class AppJunkCacheScanTaskAgent {
    private final Map<AppJunkCacheTaskNoProgressListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private IAppJunkCacheScanListener iProcessListener;

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * start In MultiProcess
     * <p/>
     * 在独立第二进程上启动
     */
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
                        public void onProgressUpdated(int processedCount, HSAppJunkCache appJunkCache) throws RemoteException {
                            callBackOnProgressUpdated(processedCount, appJunkCache);
                        }

                        @Override
                        public void onSucceeded(List<HSAppJunkCache> result, long dataSize) throws RemoteException {
                            callBackOnSucceeded(result, dataSize);
                            bindServiceHelper.unbindService();
                        }

                        @Override
                        public void onFailed(int code, String failMsg) throws RemoteException {
                            callBackOnFailed(code, failMsg);
                            bindServiceHelper.unbindService();
                        }
                    };
                    IJunkService.Stub.asInterface(iBinder).scanAppJunkCache(withCompletedProcess, iProcessListener);
                } catch (Exception e) {
                    callBackOnFailed(HSAppJunkCacheManager.FAIL_EXCEPTION, e.getMessage());
                    bindServiceHelper.unbindService();
                }
            }

            @Override
            public void onServiceUnbound() {
                callBackOnFailed(HSAppJunkCacheManager.FAIL_SERVICE_DISCONNECTED, "Service Disconnected");
                bindServiceHelper.unbindService();
            }
        });
    }

    /**
     * cancel
     * <p/>
     * cancel 独立进程上的 boost
     */
    public void cancel() {
        callBackOnFailed(HSAppJunkCacheManager.FAIL_CANCEL, "Canceled");
        final BindServiceHelper bindServiceHelper = new BindServiceHelper();
        bindServiceHelper.bindService(new Intent(HSApplication.getContext(), JunkService.class), new BindServiceHelper.BindServiceListener() {
            @Override
            public void onServiceBound(IBinder iBinder) {
                try {
                    IJunkService.Stub.asInterface(iBinder).cancelCleanAppJunkCache();
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

    public void addListener(AppJunkCacheTaskNoProgressListener taskListener) {
        addListener(taskListener, null);
    }

    public void addListener(AppJunkCacheTaskNoProgressListener taskListener, Handler handler) {
        if (taskListener == null) {
            return;
        }
        callbackListenerMap.put(taskListener, Utils.getValidHandler(handler));
    }

    public void removeListener(AppJunkCacheTaskNoProgressListener taskListener) {
        if (taskListener == null) {
            return;
        }
        callbackListenerMap.remove(taskListener);
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
            for (final AppJunkCacheTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                if (listener == null || !(listener instanceof AppJunkCacheCleanTaskListener)) {
                    continue;
                }
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                final AppJunkCacheCleanTaskListener JunkResidualTaskListener = (AppJunkCacheCleanTaskListener) listener;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        JunkResidualTaskListener.onStarted();
                    }
                });
            }
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final HSAppJunkCache appJunkCache) {
        if (isRunning.get()) {
            for (final AppJunkCacheTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                if (listener == null || !(listener instanceof AppJunkCacheScanTaskListener)) {
                    continue;
                }
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                final AppJunkCacheScanTaskListener taskListener = (AppJunkCacheScanTaskListener) listener;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        taskListener.onProgressUpdated(processedCount, appJunkCache);
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final List<HSAppJunkCache> apps, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final AppJunkCacheTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.onSucceeded(apps, dataSize);
                        }
                    }
                });
            }
            removeAllListeners();
        }
    }

    private void callBackOnFailed(final @HSAppJunkCacheManager.FailCode int code, final String failMsg) {
        if (isRunning.compareAndSet(true, false)) {
            for (final AppJunkCacheTaskNoProgressListener JunkResidualTaskListener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(JunkResidualTaskListener);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (JunkResidualTaskListener != null) {
                            JunkResidualTaskListener.onFailed(code, failMsg);
                        }
                    }
                });
            }
            removeAllListeners();
        }
    }
}
