package com.ihs.device.clean.junk.appinfo.task;

import android.os.Handler;
import android.support.annotation.BinderThread;

import com.ihs.device.clean.junk.appinfo.HSAppInfoManager;
import com.ihs.device.clean.junk.appinfo.IAppInfoProcessListener;
import com.ihs.device.clean.junk.service.JunkServiceImpl;
import com.ihs.device.common.HSAppFilter;
import com.ihs.device.common.HSAppInfo;
import com.ihs.device.common.HSAppInfoUtils;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.device.common.async.AsyncProcessor.OnProcessListener;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.common.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@BinderThread
public class AppInfoScanTask {
    private final List<AsyncProcessor> processorList = new CopyOnWriteArrayList<>();

    private final Map<IAppInfoProcessListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * start 的多线程实现,一般要求速度快
     */
    public void start(final HSAppFilter hsAppFilter) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        processorList.clear();

        callBackOnStarted();
        HSLog.i("libDevice", "AppInfoScan-----------");
        Thread deepThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final AtomicLong dataSize = new AtomicLong(0);
                    final AtomicInteger processedCount = new AtomicInteger(0);
                    final List<HSAppInfo> resultList = new ArrayList<>();

                    final List<HSAppInfo> appInfoAppList = HSAppInfoUtils.getInstalledAppInfoList(HSAppInfo.class,
                            hsAppFilter == null ? new HSAppFilter() : hsAppFilter);
                    final int totalAppCount = appInfoAppList.size();
                    if (totalAppCount == 0) {
                        callBackOnSucceeded(resultList, 0);
                        HSLog.i("libDevice", "AppInfoScan onSucceeded:" + resultList.size());
                        return;
                    }
                    for (final HSAppInfo appInfo : appInfoAppList) {
                        if (!isRunning.get()) {
                            HSLog.i("libDevice", "AppInfoScan  onCanceled");
                            break;
                        }
                        HSLog.i("libDevice", "appinfo:" + appInfo.getAppName());
                        AppInfoScanProcessor processor = new AppInfoScanProcessor(new OnProcessListener<Void, HSAppInfo>() {
                            @Override
                            public void onStarted() {
                            }

                            @Override
                            public void onProgressUpdated(Void aVoid) {
                            }

                            @Override
                            public void onSucceeded(HSAppInfo result) {
                                processedCount.incrementAndGet();
                                if (result != null) {
                                    dataSize.addAndGet(result.getSize());
                                    resultList.add(result);
                                    callBackOnProgressUpdated(processedCount.get(), totalAppCount, result);
                                    HSLog.i("libDevice", processedCount.get() + "/" + totalAppCount + " pkg:" + result.getPackageName() + " total:size:" + dataSize.get());
                                }

                                if (processedCount.get() == totalAppCount) {
                                    Collections.sort(resultList, new Comparator<HSAppInfo>() {
                                        @Override
                                        public int compare(HSAppInfo lhs, HSAppInfo rhs) {
                                            return Utils.compare(rhs.getSize(), lhs.getSize());
                                        }
                                    });
                                    HSLog.i("libDevice", "AppInfoScan onSucceeded:" + resultList.size());
                                    callBackOnSucceeded(resultList, dataSize.get());
                                }
                            }

                            @Override
                            public void onFailed(@AsyncProcessor.FailCode int failCode, Exception e) {
                                callBackOnFailed(failCode, e.getMessage());
                            }
                        });
                        processor.executeOnExecutor(JunkServiceImpl.getInstance().getAppInfoThreadPool(), appInfo);
                        processorList.add(processor);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callBackOnFailed(AsyncProcessor.FAIL_EXCEPTION, e.getMessage());
                    HSLog.i("libDevice", "MemoryScan Exception:" + e.getMessage());
                }
            }
        });
        deepThread.start();
    }

    public void cancel() {
        callBackOnFailed(AsyncProcessor.FAIL_CANCEL, "Canceled");
        for (AsyncProcessor processor : processorList) {
            try {
                if (processor != null) {
                    processor.cancel(true);
                }
            } catch (Exception e) {
                HSLog.i("libDevice", "err:" + e.getMessage());
            }
        }
        processorList.clear();
    }

    public void addListener(IAppInfoProcessListener listener) {
        addListener(listener, null);
    }

    public void addListener(IAppInfoProcessListener listener, Handler handler) {
        if (listener == null) {
            return;
        }
        callbackListenerMap.put(listener, Utils.getValidHandler(handler));
    }

    public void removeListener(IAppInfoProcessListener listener) {
        if (listener == null) {
            return;
        }
        callbackListenerMap.remove(listener);
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
            for (final IAppInfoProcessListener listener : callbackListenerMap.keySet()) {
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

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSAppInfo appInfoApp) {
        if (isRunning.get()) {
            for (final IAppInfoProcessListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                listener.onProgressUpdated(processedCount, total, appInfoApp);
                            } catch (Exception e) {
                                HSLog.i("libDevice", "exception:" + e.getMessage());
                            }
                        }
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final List<HSAppInfo> apps, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final IAppInfoProcessListener listener : callbackListenerMap.keySet()) {
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
        @HSAppInfoManager.FailCode final int failCode = getManagerFailCode(code);
        if (isRunning.compareAndSet(true, false)) {
            for (final IAppInfoProcessListener listener : callbackListenerMap.keySet()) {
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

    @HSAppInfoManager.FailCode
    private int getManagerFailCode(@AsyncProcessor.FailCode int failCode) {
        @HSAppInfoManager.FailCode int transferFailCode = HSAppInfoManager.FAIL_UNKNOWN;
        switch (failCode) {
            case AsyncProcessor.FAIL_CANCEL:
                transferFailCode = HSAppInfoManager.FAIL_CANCEL;
                break;
            case AsyncProcessor.FAIL_EXCEPTION:
            case AsyncProcessor.FAIL_REJECTED_EXECUTION:
                transferFailCode = HSAppInfoManager.FAIL_EXCEPTION;
                break;
            case AsyncProcessor.FAIL_IS_RUNNING:
                transferFailCode = HSAppInfoManager.FAIL_IS_RUNNING;
                break;
            case AsyncProcessor.FAIL_UNKNOWN:
                transferFailCode = HSAppInfoManager.FAIL_UNKNOWN;
                break;
        }
        return transferFailCode;
    }
}
