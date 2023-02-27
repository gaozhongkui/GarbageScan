package com.ihs.device.clean.junk.cache.nonapp.pathrule.task;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.junk.JunkDataManager;
import com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache;
import com.ihs.device.clean.junk.util.SUtils;
import com.ihs.device.common.async.AsyncProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class prs extends AsyncProcessor<Void, prsp, List<HSPathFileCache>> { //proguard PathFileCacheScanProcessor

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

    public native List<HSPathFileCache> c(byte[] bytes, String storagePath);

    public prs(OnProcessListener<prsp, List<HSPathFileCache>> processListener) {
        super(processListener);
    }

    @Override
    protected List<HSPathFileCache> doInBackground(Void... params) {
        if (!SUtils.isStoragePermissionGranted(HSApplication.getContext())) {
            return new ArrayList<>();
        }

        List<HSPathFileCache> caches = null;
        File adCacheFile = JunkDataManager.getDownloadADCacheFile();
        if (JunkDataManager.getInstance().isDataStable() && adCacheFile.exists()) {
            HSLog.d("prs", "use total pr file");

            try {
                caches = c(SUtils.d(adCacheFile), SUtils.EXTERNAL_STORAGE_DIRECTORY_ABSOLUTE_PATH);
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
                HSLog.d("prs", "prs download file can scan out junk");
                return caches;
            }
        }

        HSLog.d("prs", "use local pr file");

        try {
            caches = c(SUtils.b(HSApplication.getContext(), "pr"), SUtils.EXTERNAL_STORAGE_DIRECTORY_ABSOLUTE_PATH);
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
        return caches == null ? new ArrayList<HSPathFileCache>() : caches;
    }
}
