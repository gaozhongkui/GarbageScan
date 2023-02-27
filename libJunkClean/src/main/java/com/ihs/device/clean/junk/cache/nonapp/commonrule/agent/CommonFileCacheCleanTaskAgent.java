package com.ihs.device.clean.junk.cache.nonapp.commonrule.agent;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.ihs.app.framework.HSApplication;
import com.ihs.device.clean.junk.IJunkService;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCacheManager;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCacheManager.FileCleanTaskListener;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.ICommonFileCacheCleanListener.Stub;
import com.ihs.device.clean.junk.service.JunkService;
import com.ihs.device.common.async.BindServiceHelper;
import com.ihs.device.common.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File Clean Task Agent 将 Clean 请求发送到 第二进程
 */
public class CommonFileCacheCleanTaskAgent {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private FileCleanTaskListener taskListener;
    private Handler callBackHandler;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(List<HSCommonFileCache> toCleanList, @NonNull FileCleanTaskListener taskListener) {
        start(toCleanList, taskListener, null);
    }

    public void start(final List<HSCommonFileCache> toCleanList, @NonNull FileCleanTaskListener listener, Handler callBackHandler) {
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
                    IJunkService.Stub.asInterface(iBinder).cleanCommonFileCache(toCleanList, new Stub() {
                        List<HSCommonFileCache> result = new ArrayList<HSCommonFileCache>();

                        @Override
                        public void onStarted() throws RemoteException {
                            callBackOnStarted();
                        }

                        @Override
                        public void onProgressUpdated(int processedCount, int total, HSCommonFileCache commonFileCache) throws RemoteException {
                            result.add(commonFileCache);
                            callBackOnProgressUpdated(processedCount, total, commonFileCache);
                        }

                        @Override
                        public void onSucceeded(long dataSize) throws RemoteException {
                            callBackOnSucceeded(result, dataSize);
                            bindServiceHelper.unbindService();
                        }

                        @Override
                        public void onFailed(int code, String failMsg) throws RemoteException {
                            callBackOnFailed(code, failMsg);
                            bindServiceHelper.unbindService();
                        }
                    });
                } catch (Exception e) {
                    callBackOnFailed(HSCommonFileCacheManager.FAIL_EXCEPTION, e.getMessage());
                    bindServiceHelper.unbindService();
                }
            }

            @Override
            public void onServiceUnbound() {
                callBackOnFailed(HSCommonFileCacheManager.FAIL_SERVICE_DISCONNECTED, "Service Disconnected");
                bindServiceHelper.unbindService();
            }
        });
    }

    public void cancel() {
        callBackOnFailed(HSCommonFileCacheManager.FAIL_CANCEL, "Canceled");
        final BindServiceHelper bindServiceHelper = new BindServiceHelper();
        bindServiceHelper.bindService(new Intent(HSApplication.getContext(), JunkService.class), new BindServiceHelper.BindServiceListener() {
            @Override
            public void onServiceBound(IBinder iBinder) {
                try {
                    IJunkService.Stub.asInterface(iBinder).cancelCleanCommonFileCache();
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

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSCommonFileCache junkFile) {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onProgressUpdated(processedCount, total, junkFile);
                    }
                }
            });
        }
    }

    private void callBackOnSucceeded(final List<HSCommonFileCache> result, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (taskListener != null) {
                        taskListener.onSucceeded(result, dataSize);
                        callBackHandler = null;
                        taskListener = null;
                    }
                }
            });
        }
    }

    private void callBackOnFailed(final @HSCommonFileCacheManager.FailCode int code, final String failMsg) {
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