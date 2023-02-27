package com.ihs.device.clean.junk.cache.nonapp.pathrule.agent;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.ihs.app.framework.HSApplication;
import com.ihs.device.clean.junk.IJunkService;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCacheManager;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCacheManager.PathFileCacheTaskListener;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.IPathFileCacheCleanListener.Stub;
import com.ihs.device.clean.junk.service.JunkService;
import com.ihs.device.common.async.BindServiceHelper;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PathFileCacheCleanTaskAgent {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private PathFileCacheTaskListener taskListener;
    private Handler callBackHandler;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(List<HSPathFileCache> toCleanList, @NonNull PathFileCacheTaskListener listener) {
        start(toCleanList, listener, null);
    }

    public void start(final List<HSPathFileCache> toCleanList, @NonNull PathFileCacheTaskListener listener, Handler callBackHandler) {
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
                    IJunkService.Stub.asInterface(iBinder).cleanPathFileCache(toCleanList, new Stub() {
                        @Override
                        public void onStarted() throws RemoteException {
                            callBackOnStarted();
                        }

                        @Override
                        public void onProgressUpdated(int processedCount, int total, HSPathFileCache adData) throws RemoteException {
                            callBackOnProgressUpdated(processedCount, total, adData);
                        }

                        @Override
                        public void onSucceeded(List<HSPathFileCache> appJunkList, long dataSize) throws RemoteException {
                            callBackOnSucceeded(appJunkList, dataSize);
                            bindServiceHelper.unbindService();
                        }

                        @Override
                        public void onFailed(int code, String failMsg) throws RemoteException {
                            callBackOnFailed(code, failMsg);
                            bindServiceHelper.unbindService();
                        }
                    });
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
                    IJunkService.Stub.asInterface(iBinder).cancelCleanPathFileCache();
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

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSPathFileCache junkResidual) {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onProgressUpdated(processedCount, total, junkResidual);
                    }
                }
            });
        }
    }

    private void callBackOnSucceeded(final List<HSPathFileCache> apps, final long dataSize) {
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

    private void callBackOnFailed(final @HSPathFileCacheManager.FailCode int code, final String failMsg) {
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
