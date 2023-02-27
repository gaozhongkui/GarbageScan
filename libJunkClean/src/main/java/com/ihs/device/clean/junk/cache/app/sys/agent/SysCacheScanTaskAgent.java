package com.ihs.device.clean.junk.cache.app.sys.agent;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.ihs.app.framework.HSApplication;
import com.ihs.device.clean.junk.IJunkService;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCacheManager;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCacheManager.AppSysCacheTaskListener;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCacheManager.AppSysCacheTaskNoProgressListener;
import com.ihs.device.clean.junk.cache.app.sys.IAppSysCacheProcessListener;
import com.ihs.device.clean.junk.cache.app.sys.IAppSysCacheProcessListener.Stub;
import com.ihs.device.clean.junk.service.JunkService;
import com.ihs.device.common.HSAppFilter;
import com.ihs.device.common.async.BindServiceHelper;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SysCacheScanTaskAgent {

    private final Map<AppSysCacheTaskNoProgressListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private IAppSysCacheProcessListener iProcessListener;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(final boolean withCompletedProcess, final HSAppFilter hsAppFilter) {
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
                        public void onProgressUpdated(int processedCount, int total, HSAppSysCache appSysCache) throws RemoteException {
                            callBackOnProgressUpdated(processedCount, total, appSysCache);
                        }

                        @Override
                        public void onSucceeded(List<HSAppSysCache> junkAppCaches, long dataSize) throws RemoteException {
                            callBackOnSucceeded(junkAppCaches, dataSize);
                            bindServiceHelper.unbindService();
                        }

                        @Override
                        public void onFailed(int code, String failMsg) throws RemoteException {
                            callBackOnFailed(code, failMsg);
                            bindServiceHelper.unbindService();
                        }
                    };
                    IJunkService.Stub.asInterface(iBinder).scanAppSysCache(withCompletedProcess, hsAppFilter, iProcessListener);
                } catch (Exception e) {
                    callBackOnFailed(HSAppSysCacheManager.FAIL_EXCEPTION, e.getMessage());
                    bindServiceHelper.unbindService();
                }
            }

            @Override
            public void onServiceUnbound() {
                callBackOnFailed(HSAppSysCacheManager.FAIL_SERVICE_DISCONNECTED, "Service Disconnected");
                bindServiceHelper.unbindService();
            }
        });
    }

    public void cancel() {
        callBackOnFailed(HSAppSysCacheManager.FAIL_CANCEL, "Canceled");
        final BindServiceHelper bindServiceHelper = new BindServiceHelper();
        bindServiceHelper.bindService(new Intent(HSApplication.getContext(), JunkService.class), new BindServiceHelper.BindServiceListener() {
            @Override
            public void onServiceBound(IBinder iBinder) {
                try {
                    IJunkService.Stub.asInterface(iBinder).cancelScanAppSysCache();
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

    public void addListener(AppSysCacheTaskNoProgressListener taskListener) {
        addListener(taskListener, null);
    }

    public void addListener(AppSysCacheTaskNoProgressListener taskListener, Handler handler) {
        if (taskListener == null) {
            return;
        }
        callbackListenerMap.put(taskListener, Utils.getValidHandler(handler));
    }

    public void removeListener(AppSysCacheTaskNoProgressListener taskListener) {
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
            for (final AppSysCacheTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                if (listener == null || !(listener instanceof AppSysCacheTaskListener)) {
                    continue;
                }
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                final AppSysCacheTaskListener taskListener = (AppSysCacheTaskListener) listener;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        taskListener.onStarted();
                    }
                });
            }
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSAppSysCache appSysCache) {
        if (isRunning.get()) {
            for (final AppSysCacheTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                if (listener == null || !(listener instanceof AppSysCacheTaskListener)) {
                    continue;
                }
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                final AppSysCacheTaskListener taskListener = (AppSysCacheTaskListener) listener;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        taskListener.onProgressUpdated(processedCount, total, appSysCache);
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final List<HSAppSysCache> apps, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final AppSysCacheTaskNoProgressListener memoryTaskListener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(memoryTaskListener);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (memoryTaskListener != null) {
                            memoryTaskListener.onSucceeded(apps, dataSize);
                        }
                    }
                });
            }
            removeAllListeners();
        }
    }

    private void callBackOnFailed(final @HSAppSysCacheManager.FailCode int code, final String failMsg) {
        if (isRunning.compareAndSet(true, false)) {
            for (final AppSysCacheTaskNoProgressListener memoryTaskListener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(memoryTaskListener);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (memoryTaskListener != null) {
                            memoryTaskListener.onFailed(code, failMsg);
                        }
                    }
                });
            }
            removeAllListeners();
        }
    }
}
