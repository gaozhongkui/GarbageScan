package com.ihs.device.clean.junk.appinfo.agent;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.ihs.app.framework.HSApplication;
import com.ihs.device.clean.junk.IJunkService;
import com.ihs.device.clean.junk.appinfo.HSAppInfoManager;
import com.ihs.device.clean.junk.appinfo.HSAppInfoManager.AppInfoTaskListener;
import com.ihs.device.clean.junk.appinfo.HSAppInfoManager.AppInfoTaskNoProgressListener;
import com.ihs.device.clean.junk.appinfo.IAppInfoProcessListener;
import com.ihs.device.clean.junk.appinfo.IAppInfoProcessListener.Stub;
import com.ihs.device.clean.junk.service.JunkService;
import com.ihs.device.common.HSAppFilter;
import com.ihs.device.common.HSAppInfo;
import com.ihs.device.common.async.BindServiceHelper;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppInfoScanTaskAgent {
    private final Map<AppInfoTaskNoProgressListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private IAppInfoProcessListener iProcessListener;

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
                        public void onProgressUpdated(int processedCount, int total, HSAppInfo appInfo) throws RemoteException {
                            callBackOnProgressUpdated(processedCount, total, appInfo);
                        }

                        @Override
                        public void onSucceeded(List<HSAppInfo> appInfoApps, long dataSize) throws RemoteException {
                            callBackOnSucceeded(appInfoApps, dataSize);

                        }

                        @Override
                        public void onFailed(int code, String failMsg) throws RemoteException {
                            callBackOnFailed(code, failMsg);
                            bindServiceHelper.unbindService();
                        }
                    };
                    IJunkService.Stub.asInterface(iBinder).scanAppInfo(withCompletedProcess, hsAppFilter, iProcessListener);
                } catch (Exception e) {
                    callBackOnFailed(HSAppInfoManager.FAIL_EXCEPTION, e.getMessage());
                    bindServiceHelper.unbindService();
                }
            }

            @Override
            public void onServiceUnbound() {
                callBackOnFailed(HSAppInfoManager.FAIL_SERVICE_DISCONNECTED, "Service Disconnected");
                bindServiceHelper.unbindService();
            }
        });
    }

    public void cancel() {
        callBackOnFailed(HSAppInfoManager.FAIL_CANCEL, "Canceled");
        final BindServiceHelper bindServiceHelper = new BindServiceHelper();
        bindServiceHelper.bindService(new Intent(HSApplication.getContext(), JunkService.class), new BindServiceHelper.BindServiceListener() {
            @Override
            public void onServiceBound(IBinder iBinder) {
                try {
                    IJunkService.Stub.asInterface(iBinder).cancelScanAppInfo();
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

    public void addListener(AppInfoTaskNoProgressListener listener) {
        addListener(listener, null);
    }

    public void addListener(AppInfoTaskNoProgressListener listener, Handler handler) {
        if (listener == null) {
            return;
        }
        callbackListenerMap.put(listener, Utils.getValidHandler(handler));
    }

    public void removeListener(AppInfoTaskNoProgressListener listener) {
        if (listener == null) {
            return;
        }
        callbackListenerMap.remove(listener);
        if (callbackListenerMap.isEmpty()) {
            cancel();
        }
    }

    public void removeAllListeners() {
        callbackListenerMap.clear();
        cancel();
    }

    private void callBackOnStarted() {
        if (isRunning.get()) {
            for (final AppInfoTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                if (listener == null || !(listener instanceof AppInfoTaskListener)) {
                    continue;
                }
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                final AppInfoTaskListener appInfoTaskListener = (AppInfoTaskListener) listener;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        appInfoTaskListener.onStarted();
                    }
                });
            }
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSAppInfo HSAppInfo) {
        if (isRunning.get()) {
            for (final AppInfoTaskNoProgressListener listener : callbackListenerMap.keySet()) {
                if (listener == null || !(listener instanceof AppInfoTaskListener)) {
                    continue;
                }
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                final AppInfoTaskListener appInfoTaskListener = (AppInfoTaskListener) listener;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        appInfoTaskListener.onProgressUpdated(processedCount, total, HSAppInfo);
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final List<HSAppInfo> apps, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final AppInfoTaskNoProgressListener listener : callbackListenerMap.keySet()) {
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

    private void callBackOnFailed(final @HSAppInfoManager.FailCode int code, final String failMsg) {
        if (isRunning.compareAndSet(true, false)) {
            for (final AppInfoTaskNoProgressListener listener : callbackListenerMap.keySet()) {
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
