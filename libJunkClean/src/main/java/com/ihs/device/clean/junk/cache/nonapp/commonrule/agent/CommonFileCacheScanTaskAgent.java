package com.ihs.device.clean.junk.cache.nonapp.commonrule.agent;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.ihs.app.framework.HSApplication;
import com.ihs.device.clean.junk.IJunkService;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCacheManager;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCacheManager.FileScanTaskListener;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.ICommonFileCacheScanListener;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.ICommonFileCacheScanListener.Stub;
import com.ihs.device.clean.junk.service.JunkService;
import com.ihs.device.common.async.BindServiceHelper;
import com.ihs.device.common.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File Scan Task Agent 将 Scan 请求发送到 第二进程
 */
public class CommonFileCacheScanTaskAgent {
    private final Map<FileScanTaskListener, Handler> callbackListenerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ICommonFileCacheScanListener iProcessListener;

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * start In MultiProcess
     * <p/>
     * 在独立第二进程上启动
     */

    public void start(final boolean withCompletedProcess, final List<String> extensions, final long fileMinSize) {
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
                        Map<String, List<HSCommonFileCache>> result = new HashMap<String, List<HSCommonFileCache>>();

                        @Override
                        public void onStarted() throws RemoteException {
                            callBackOnStarted();
                        }

                        @Override
                        public void onProgressUpdated(int processedCount, int total, HSCommonFileCache commonFileCache) throws RemoteException {
                            String extension = commonFileCache.getFileExtension();
                            List<HSCommonFileCache> commonFileCacheList = result.get(extension);
                            if (commonFileCacheList == null) {
                                commonFileCacheList = new ArrayList<>();
                            }
                            commonFileCacheList.add(commonFileCache);
                            result.put(extension, commonFileCacheList);
                            callBackOnProgressUpdated(processedCount, total, commonFileCache);
                        }

                        @SuppressWarnings({"unchecked"})
                        @Override
                        public void onSucceeded(long dataSize) throws RemoteException {
                            for (List<HSCommonFileCache> valueList : result.values()) {
                                Collections.sort(valueList, new Comparator<HSCommonFileCache>() {
                                    @Override
                                    public int compare(HSCommonFileCache lhs, HSCommonFileCache rhs) {
                                        return Utils.compare(rhs.getSize(), lhs.getSize());
                                    }
                                });
                            }
                            callBackOnSucceeded(result, dataSize);
                            bindServiceHelper.unbindService();
                        }

                        @Override
                        public void onFailed(int code, String failMsg) throws RemoteException {
                            callBackOnFailed(code, failMsg);
                            bindServiceHelper.unbindService();
                        }
                    };
                    IJunkService.Stub.asInterface(iBinder).scanCommonFileCache(withCompletedProcess, extensions, fileMinSize, iProcessListener);
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

    /**
     * cancel
     * <p/>
     * cancel 独立进程上的 boost
     */
    public void cancel() {
        callBackOnFailed(HSCommonFileCacheManager.FAIL_CANCEL, "Canceled");
        final BindServiceHelper bindServiceHelper = new BindServiceHelper();
        bindServiceHelper.bindService(new Intent(HSApplication.getContext(), JunkService.class), new BindServiceHelper.BindServiceListener() {
            @Override
            public void onServiceBound(IBinder iBinder) {
                try {
                    IJunkService.Stub.asInterface(iBinder).cancelScanCommonFileCache();
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

    public void addListener(FileScanTaskListener noProgressTaskListener) {
        addListener(noProgressTaskListener, null);
    }

    public void addListener(FileScanTaskListener taskListener, Handler handler) {
        if (taskListener == null) {
            return;
        }
        callbackListenerMap.put(taskListener, Utils.getValidHandler(handler));
    }

    public void removeListener(FileScanTaskListener taskListener) {
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
            for (final FileScanTaskListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onStarted();
                    }
                });
            }
        }
    }

    private void callBackOnProgressUpdated(final int processedCount, final int total, final HSCommonFileCache commonFileCache) {
        if (isRunning.get()) {
            for (final FileScanTaskListener listener : callbackListenerMap.keySet()) {
                Handler handler = callbackListenerMap.get(listener);
                if (handler == null) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onProgressUpdated(processedCount, total, commonFileCache);
                    }
                });
            }
        }
    }

    private void callBackOnSucceeded(final Map<String, List<HSCommonFileCache>> result, final long dataSize) {
        if (isRunning.compareAndSet(true, false)) {
            for (final FileScanTaskListener listener : callbackListenerMap.keySet()) {
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

    private void callBackOnFailed(final @HSCommonFileCacheManager.FailCode int code, final String failMsg) {
        if (isRunning.compareAndSet(true, false)) {
            for (final FileScanTaskListener listener : callbackListenerMap.keySet()) {
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
