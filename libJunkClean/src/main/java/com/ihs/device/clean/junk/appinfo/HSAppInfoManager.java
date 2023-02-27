package com.ihs.device.clean.junk.appinfo;

import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ihs.device.clean.junk.appinfo.agent.AppInfoScanTaskAgent;
import com.ihs.device.common.HSAppFilter;
import com.ihs.device.common.HSAppInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class HSAppInfoManager {

    public interface AppInfoTaskNoProgressListener {
        void onSucceeded(List<HSAppInfo> apps, long dataSize);

        void onFailed(@FailCode int code, String failMsg);
    }

    public interface AppInfoTaskListener extends AppInfoTaskNoProgressListener {
        void onStarted();

        void onProgressUpdated(int processedCount, int total, HSAppInfo appInfo);
    }

    @IntDef({FAIL_UNKNOWN,
             FAIL_CANCEL,
             FAIL_IS_RUNNING,
             FAIL_EXCEPTION,
             FAIL_SERVICE_DISCONNECTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FailCode {
    }

    private static class SingletonHolder {
        private static final HSAppInfoManager INSTANCE = new HSAppInfoManager();
    }

    public final static int FAIL_UNKNOWN = 0x0;
    public final static int FAIL_CANCEL = 0x1;
    public final static int FAIL_IS_RUNNING = 0x2;
    public final static int FAIL_EXCEPTION = 0x4;
    public final static int FAIL_SERVICE_DISCONNECTED = 0x5;

    public static HSAppInfoManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private HSAppFilter scanGlobalAppFilter = new HSAppFilter();

    private AppInfoScanTaskAgent appInfoScanTaskAgent;

    private HSAppInfoManager() {
    }

    public void setScanGlobalAppFilter(@Nullable HSAppFilter hsAppFilter) {
        if (hsAppFilter == null) {
            hsAppFilter = new HSAppFilter();
        }
        scanGlobalAppFilter = hsAppFilter;
    }

    public void startScanWithCompletedProgress(AppInfoTaskListener listener) {
        startScanWithCompletedProgress(listener, null);
    }

    public void startScanWithCompletedProgress(AppInfoTaskListener listener, Handler handler) {
        startScanInner(true, listener, handler);
    }

    public void startScanWithoutProgress(AppInfoTaskNoProgressListener scanTaskListener) {
        startScanWithoutProgress(scanTaskListener, null);
    }

    public synchronized void startScanWithoutProgress(AppInfoTaskNoProgressListener scanTaskListener, Handler handler) {
        if (appInfoScanTaskAgent != null && appInfoScanTaskAgent.isRunning()) {
            appInfoScanTaskAgent.addListener(scanTaskListener, handler);
            return;
        }
        startScanInner(false, scanTaskListener, handler);
    }

    public synchronized void stopScan(@NonNull AppInfoTaskNoProgressListener scanTaskListener) {
        if (appInfoScanTaskAgent != null) {
            appInfoScanTaskAgent.removeListener(scanTaskListener);
        }
    }

    private synchronized void startScanInner(boolean withCompletedProcess, AppInfoTaskNoProgressListener scanTaskListener, Handler handler) {
        if (appInfoScanTaskAgent != null && appInfoScanTaskAgent.isRunning()) {
            appInfoScanTaskAgent.cancel();
        }
        appInfoScanTaskAgent = new AppInfoScanTaskAgent();
        appInfoScanTaskAgent.addListener(scanTaskListener, handler);
        appInfoScanTaskAgent.start(withCompletedProcess, scanGlobalAppFilter);
    }
}