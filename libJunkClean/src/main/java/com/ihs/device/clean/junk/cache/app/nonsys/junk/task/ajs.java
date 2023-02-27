package com.ihs.device.clean.junk.cache.app.nonsys.junk.task;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.junk.JunkDataManager;
import com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache;
import com.ihs.device.clean.junk.util.SUtils;
import com.ihs.device.common.async.AsyncProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ajs extends AsyncProcessor<Void, ajsp, List<HSAppJunkCache>> { //proguard AppJunkCacheScanProcessor

    static {
        try {
            System.loadLibrary("jdc");
        } catch (UnsatisfiedLinkError error) {
            if (HSLog.isDebugging()) {
                throw error;
            }
            error.printStackTrace();
        }
    }

    public native List<HSAppJunkCache> a(byte[] ajBytes, byte[] adBytes, String storagePath);

    public ajs(OnProcessListener<ajsp, List<HSAppJunkCache>> processListener) {
        super(processListener);
    }

    @Override
    protected List<HSAppJunkCache> doInBackground(Void... params) {
        if (!SUtils.isStoragePermissionGranted(HSApplication.getContext())) {
            return new ArrayList<>();
        }

        List<HSAppJunkCache> caches = null;
        File appJunkFile = JunkDataManager.getDownloadAppJunkFile();
        File appDataFile = JunkDataManager.getDownloadAppDataFile();
        if (JunkDataManager.getInstance().isDataStable() && appJunkFile.exists() && appDataFile.exists()) {
            HSLog.d("ajs", "use total aj&ad file");

            try {
                caches = a(SUtils.d(appJunkFile), SUtils.d(appDataFile), SUtils.EXTERNAL_STORAGE_DIRECTORY_ABSOLUTE_PATH);
            } catch (Exception exception) {
                if (HSLog.isDebugging()) {
                    throw exception;
                }
                exception.printStackTrace();
            } catch (Error error) {
                if (HSLog.isDebugging()) {
                    throw error;
                }
                error.printStackTrace();
            }

            if (caches != null) {
                HSLog.d("ajs", "ajs download file can scan out junk");
                return caches;
            }
        }

        HSLog.d("ajs", "use local aj&ad file");

        try {
            caches = a(SUtils.b(HSApplication.getContext(), "aj"), SUtils.b(HSApplication.getContext(), "ad"), SUtils.EXTERNAL_STORAGE_DIRECTORY_ABSOLUTE_PATH);
        } catch (Exception exception) {
            if (HSLog.isDebugging()) {
                throw exception;
            }
            exception.printStackTrace();
        } catch (Error error) {
            if (HSLog.isDebugging()) {
                throw error;
            }
            error.printStackTrace();
        }
        return caches == null ? new ArrayList<HSAppJunkCache>() : caches;
    }
}
