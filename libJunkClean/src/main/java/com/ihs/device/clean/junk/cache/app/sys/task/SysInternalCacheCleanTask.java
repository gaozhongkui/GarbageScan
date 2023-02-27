package com.ihs.device.clean.junk.cache.app.sys.task;

import android.os.Handler;
import android.support.annotation.BinderThread;
import android.support.annotation.NonNull;

import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCacheManager;
import com.ihs.device.clean.junk.cache.app.sys.IAppInternalSysCacheCleanProcessListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.async.AsyncProcessor.OnProcessListener;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AppExternalCacheCleanTask 运行在第二进程, Listener 一对一
 */
@BinderThread
public class SysInternalCacheCleanTask {

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private SysInternalCacheCleanProcessor processor;
    private IAppInternalSysCacheCleanProcessListener iAppInternalSysCacheCleanProcessListener;
    private Handler callBackHandler;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(@NonNull IAppInternalSysCacheCleanProcessListener listener) {
        start(listener, null);
    }

    public void start(@NonNull IAppInternalSysCacheCleanProcessListener listener, Handler callBackHandler) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        this.iAppInternalSysCacheCleanProcessListener = listener;
        this.callBackHandler = Utils.getValidHandler(callBackHandler);

        callBackOnStarted();

        HSLog.i("libDevice", "Sys Internal Cache Clean -----------");
        processor = new SysInternalCacheCleanProcessor(new OnProcessListener<Void, Long>() {

            @Override
            public void onStarted() {
            }

            @Override
            public void onProgressUpdated(Void aVoid) {
            }

            @Override
            public void onSucceeded(Long size) {
                callBackOnSucceeded(size);
                HSLog.i("libDevice", "Sys Internal Cache Clean onSucceeded:" + size);
            }

            @Override
            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                callBackOnFailed(failCode, e.getMessage());
                HSLog.i("libDevice", "Sys Internal Cache Clean onFailed:" + failCode + " err:" + e.getMessage());
            }
        });

        processor.executeOnExecutor(JunkServiceImpl.getInstance().getAppSysCacheThreadPool());
    }

    public void cancel() {
        callBackOnFailed(AsyncProcessor.FAIL_CANCEL, "Canceled");
        if (processor != null) {
            processor.cancel(true);
            processor = null;
        }
    }

    protected void callBackOnFailed(final @AsyncProcessor.FailCode int code, final String errMsg) {
        @HSAppSysCacheManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iAppInternalSysCacheCleanProcessListener != null) {
                        try {
                            iAppInternalSysCacheCleanProcessListener.onFailed(failCode, errMsg);
                            callBackHandler = null;
                            iAppInternalSysCacheCleanProcessListener = null;
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnStarted() {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iAppInternalSysCacheCleanProcessListener != null) {
                        try {
                            iAppInternalSysCacheCleanProcessListener.onStarted();
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnSucceeded(final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iAppInternalSysCacheCleanProcessListener != null) {
                        try {
                            iAppInternalSysCacheCleanProcessListener.onSucceeded(dataSize);
                            callBackHandler = null;
                            iAppInternalSysCacheCleanProcessListener = null;
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
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
