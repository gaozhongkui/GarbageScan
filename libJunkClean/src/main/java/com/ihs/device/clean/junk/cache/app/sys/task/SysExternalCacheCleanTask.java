package com.ihs.device.clean.junk.cache.app.sys.task;

import android.os.Handler;
import android.support.annotation.BinderThread;
import android.support.annotation.NonNull;

import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCacheManager;
import com.ihs.device.clean.junk.cache.app.sys.IAppSysCacheProcessListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.async.AsyncProcessor.OnProcessListener;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * System External Cache CleanTask 运行在第二进程, Listener 一对一
 */
@BinderThread
public class SysExternalCacheCleanTask {

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private SysExternalCacheCleanProcessor processor;
    private IAppSysCacheProcessListener iAppSysCacheProcessListener;
    private Handler callBackHandler;

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(List<HSAppSysCache> toCleanList, @NonNull IAppSysCacheProcessListener listener) {
        start(toCleanList, listener, null);
    }

    public void start(List<HSAppSysCache> toCleanList, @NonNull IAppSysCacheProcessListener listener, Handler callBackHandler) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        this.iAppSysCacheProcessListener = listener;
        this.callBackHandler = Utils.getValidHandler(callBackHandler);

        callBackOnStarted();

        HSLog.i("libDevice", "System External Cache Clean-----------");
        processor = new SysExternalCacheCleanProcessor(new OnProcessListener<SysCacheProgress, List<HSAppSysCache>>() {
            private long dataSize = 0L;
            private int processedCount = 0;

            @Override
            public void onStarted() {
            }

            @Override
            public void onProgressUpdated(SysCacheProgress progress) {
                dataSize += progress.appSysCache.getSize();
                processedCount = progress.processedCount;
                callBackOnProgressUpdated(processedCount, progress.total, progress.appSysCache);
                HSLog.i("libDevice", "System External Cache Clean: " + processedCount + " pkg:" + progress.appSysCache.getPackageName() + " size:" + dataSize);
            }

            @Override
            public void onSucceeded(List<HSAppSysCache> result) {
                callBackOnSucceeded(result, dataSize);
                HSLog.i("libDevice", "System External Cache Clean onSucceeded:" + result.size());
            }

            @Override
            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                callBackOnFailed(failCode, e.getMessage());
                HSLog.i("libDevice", "System External Cache Clean onFailed:" + failCode + " err:" + e.getMessage());
                e.printStackTrace();
            }
        });

        processor.executeOnExecutor(JunkServiceImpl.getInstance().getAppSysCacheThreadPool(), toCleanList);
    }

    public void cancel() {
        callBackOnFailed(AsyncProcessor.FAIL_CANCEL, "Canceled");
        if (processor != null) {
            processor.cancel(true);
            processor = null;
        }
    }

    private void callBackOnStarted() {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iAppSysCacheProcessListener != null) {
                        try {
                            iAppSysCacheProcessListener.onStarted();
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSAppSysCache junkAppCache) {
        if (isRunning.get()) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iAppSysCacheProcessListener != null) {
                        try {
                            iAppSysCacheProcessListener.onProgressUpdated(processedCount, total, junkAppCache);
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
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
                    if (iAppSysCacheProcessListener != null) {
                        try {
                            iAppSysCacheProcessListener.onSucceeded(apps, dataSize);
                            callBackHandler = null;
                            iAppSysCacheProcessListener = null;
                        } catch (Exception e) {
                            HSLog.i("libDevice", "exception:" + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void callBackOnFailed(final @AsyncProcessor.FailCode int code, final String errMsg) {
        @HSAppSysCacheManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            callBackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (iAppSysCacheProcessListener != null) {
                        try {
                            iAppSysCacheProcessListener.onFailed(failCode, errMsg);
                            callBackHandler = null;
                            iAppSysCacheProcessListener = null;
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
