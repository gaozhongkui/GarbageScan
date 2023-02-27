package com.ihs.device.clean.junk.cache.nonapp.commonrule.task;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Files.FileColumns;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.junk.cache.nonapp.commonrule.HSCommonFileCache;
import com.ihs.device.common.async.AsyncProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommonFileCacheScanProcessor extends AsyncProcessor<List<String>, CommonFileCacheTaskProgress, Map<String, List<HSCommonFileCache>>> {
    private static final int PAGE_SIZE = 100;
    private long fileMinSize = 0;

    public CommonFileCacheScanProcessor(long fileMinSize,
            OnProcessListener<CommonFileCacheTaskProgress, Map<String, List<HSCommonFileCache>>> onProcessListener) {
        super(onProcessListener);
        this.fileMinSize = fileMinSize;
    }

    @Override
    protected Map<String, List<HSCommonFileCache>> doInBackground(List<String>... params) {
        Map<String, List<HSCommonFileCache>> resultAppMap = new HashMap<>();
        if (params == null || params.length <= 0) {
            return resultAppMap;
        }
        HSLog.i("libDevice", "CommonFileCacheScanProcessor start");
        List<String> extensionList = params[0];

        String selections = FileColumns.SIZE + " >= " + fileMinSize + " and (1=2 ";
        List<String> selectionArgList = new ArrayList<>();
        for (String fileExtension : extensionList) {
            selections += " or " + FileColumns.DATA + " like ? ";
            selectionArgList.add("%." + fileExtension);
        }
        selections += ")";
        String[] selectionArgArray = new String[selectionArgList.size()];
        selectionArgList.toArray(selectionArgArray);
        if (selectionArgList.size() > 0) {
            Cursor cursor = null;
            try {

                cursor = HSApplication.getContext().getContentResolver()
                        .query(MediaStore.Files.getContentUri("external"), new String[]{FileColumns.DATA}, selections, selectionArgArray, null);
                if (cursor != null) {
                    int totalCount = cursor.getCount();
                    int currentCount = 0;
                    HSLog.i("libDevice", "CommonFileCacheScanProcessor totalCount:" + totalCount);
                    int indexData = cursor.getColumnIndex(FileColumns.DATA);

                    while (cursor.moveToNext()) {
                        if (!isRunning()) {
                            break;
                        }
                        File file = new File(cursor.getString(indexData));
                        if (!file.exists()) {
                            HSApplication.getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                        }
                        try {
                            HSCommonFileCache commonFileCache = new HSCommonFileCache(file).initDetailInfo();
                            String extension = commonFileCache.getFileExtension();
                            List<HSCommonFileCache> commonFileCacheList = resultAppMap.get(extension);
                            if (commonFileCacheList == null) {
                                commonFileCacheList = new ArrayList<>();
                            }
                            commonFileCacheList.add(commonFileCache);
                            resultAppMap.put(extension, commonFileCacheList);
                            CommonFileCacheTaskProgress boostProgress = new CommonFileCacheTaskProgress(++currentCount, totalCount, commonFileCache);
                            postOnProgressUpdated(boostProgress);
                        } catch (Exception e) {
                            if (HSLog.isDebugging()) {
                                throw e;
                            }
                            e.printStackTrace();
                        }
                    }
                }

            } catch (Exception e) {
                if (HSLog.isDebugging()) {
                    throw e;
                }
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return resultAppMap;
    }
}
