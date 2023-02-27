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
import com.ihs.device.clean.junk.cache.app.sys.IAppSysCacheProcessListener.Stub;
import com.ihs.device.clean.junk.service.JunkService;
import com.ihs.device.common.async.BindServiceHelper;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * App External Cache Clean Task Agent 将 Clean 请求发送到 第二进程
 */
public class SysExternalCacheCleanTaskAgent {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private AppSysCacheTaskListener taskListener;
    private Handler callBackHandler;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(List<HSAppSysCache> toCleanList, AppSysCacheTaskListener taskListener) {
        start(toCleanList, taskListener, null);
    }

    public void start(final List<HSAppSysCache> toCleanList, AppSysCacheTaskListener listener, Handler callBackHandler) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        this.taskListener = listener;
        this.callBackHandler = Utils.getValidHandler(callBackHandler);

        final BindServiceHelper bindServiceHelper = new BindServiceHelper();
        bindServiceHelper.bindService(new Intent(HSApplication.getContext(), JunkService.class), new BindServiceHelper.BindServiceListener() {
            @Override
            public void onServiceBound(IBinder iBinder) {
                if (!isRunning.get()) {
                    bindServiceHelper.unbindService();
                    return;
                }
                try {
                    IJunkService.Stub.asInterface(iBinder).cleanAppSysExternalCache(toCleanList, new Stub() {
                        @Override
                        public void onStarted() throws RemoteException {
                            callBackOnStarted();
                        }

                        @Override
                        public void onProgressUpdated(int processedCount, int total, HSAppSysCache appSysCache) throws RemoteException {
                            callBackOnProgressUpdated(processedCount, total, appSysCache);
                        }

                        @Override
                        public void onSucceeded(List<HSAppSysCache> junkAppCacheList, long dataSize) throws RemoteException {
                            callBackOnSucceeded(junkAppCacheList, dataSize);
                            bindServiceHelper.unbindService();
                        }

                        @Override
                        public void onFailed(int code, String failMsg) throws RemoteException {
                            callBackOnFailed(code, failMsg);
                            bindServiceHelper.unbindService();
                        }
                    });
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

    /**
     * <p>
     * cancel 独立进程上的 boost
     */
    public void cancel() {
        callBackOnFailed(HSAppSysCacheManager.FAIL_CANCEL, "Canceled");
        final BindServiceHelper bindServiceHelper = new BindServiceHelper();
        bindServiceHelper.bindService(new Intent(HSApplication.getContext(), JunkService.class), new BindServiceHelper.BindServiceListener() {
            @Override
            public void onServiceBound(IBinder iBinder) {
                try {
                    IJunkService.Stub.asInterface(iBinder).cancelCleanAppSysExternalCache();
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

    private void callBackOnStarted() {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onStarted();
                    }
                }
            });
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSAppSysCache memoryApp) {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onProgressUpdated(processedCount, total, memoryApp);
                    }
                }
            });
        }
    }

    private void callBackOnSucceeded(final List<HSAppSysCache> apps, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onSucceeded(apps, dataSize);
                        callBackHandler = null;
                        taskListener = null;
                    }
                }
            });
        }
    }

    private void callBackOnFailed(final @HSAppSysCacheManager.FailCode int code, final String failMsg) {
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onFailed(code, failMsg);
                        callBackHandler = null;
                        taskListener = null;
                    }
                }
            });
        }
    }
}
