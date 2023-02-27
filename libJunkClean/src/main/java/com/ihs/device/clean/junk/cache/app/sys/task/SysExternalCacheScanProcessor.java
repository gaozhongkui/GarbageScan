package com.ihs.device.clean.junk.cache.app.sys.task;

import android.database.Cursor;
import android.provider.MediaStore;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache;
import com.ihs.device.clean.junk.util.SUtils;
import com.ihs.device.common.HSAppFilter;
import com.ihs.device.common.HSAppInfoUtils;
import com.ihs.device.common.async.AsyncProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SysExternalCacheScanProcessor extends AsyncProcessor<Void, SysExternalCacheTaskProgress, List<HSAppSysCache>> {

    private HSAppFilter hsAppFilter;

    public SysExternalCacheScanProcessor(HSAppFilter hsAppFilter, OnProcessListener<SysExternalCacheTaskProgress, List<HSAppSysCache>> processListener) {
        super(processListener);

        if (hsAppFilter == null) {
            hsAppFilter = new HSAppFilter();
        }
        this.hsAppFilter = hsAppFilter;
    }

    @Override
    protected List<HSAppSysCache> doInBackground(Void... voids) {
        Map<String, HSAppSysCache> installedAppMap = getInstalledAppMap(hsAppFilter);
        String[] columns = new String[]{MediaStore.Files.FileColumns.DATA,
                                        MediaStore.Files.FileColumns.SIZE};
        String sdCardRoot = SUtils.EXTERNAL_STORAGE_DIRECTORY_ABSOLUTE_PATH;
        Cursor cursor = null;
        try {
            cursor = HSApplication.getContext().getContentResolver()
                    .query(MediaStore.Files.getContentUri("external"), columns, MediaStore.Files.FileColumns.DATA + " like ? ",
                           new String[]{sdCardRoot + "android/data/%"}, MediaStore.Files.FileColumns.DATA);
            if (cursor == null) {
                return new ArrayList<>(installedAppMap.values());
            }
            if (cursor.moveToFirst()) {
                int dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                int sizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE);
                do {
                    if (!isRunning()) {
                        return new ArrayList<>(installedAppMap.values());
                    }
                    String filePath = cursor.getString(dataIndex).toLowerCase();
//                    HSLog.i("LWJlog", "filePath = " + filePath);
                    if (filePath.startsWith(sdCardRoot + "android/data") && filePath.contains("/cache/")) {
                        String pkgName = null;
                        try {
                            pkgName = filePath.substring(filePath.indexOf("android/data") + "android/data".length() + 1, filePath.indexOf("/cache/"));
                        } catch (Exception e) {
                            if (HSLog.isDebugging()) {
                                throw e;
                            }
                            e.printStackTrace();
                        }
                        if (pkgName == null) {
                            continue;
                        }
                        if (installedAppMap.containsKey(pkgName)) {
                            HSAppSysCache appSysCache = installedAppMap.get(pkgName);
                            appSysCache.setSize(appSysCache.getSize() + cursor.getLong(sizeIndex));
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception ignore) {
            if (HSLog.isDebugging()) {
                throw ignore;
            }
            ignore.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return new ArrayList<>(installedAppMap.values());
    }

    private Map<String, HSAppSysCache> getInstalledAppMap(HSAppFilter hsAppFilter) {
        if (hsAppFilter == null) {
            hsAppFilter = new HSAppFilter();
        }
        List<HSAppSysCache> installedAppInfoList = HSAppInfoUtils.getInstalledAppInfoList(HSAppSysCache.class, hsAppFilter);
        Map<String, HSAppSysCache> appInfoMap = new HashMap<>();
        for (HSAppSysCache appRunningInfo : installedAppInfoList) {
            appInfoMap.put(appRunningInfo.getPackageName(), appRunningInfo);
        }
        return appInfoMap;
    }
}
