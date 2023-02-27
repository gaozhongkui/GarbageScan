package com.ihs.device.clean.junk.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.ihs.commons.utils.HSLog;

public class JunkService extends Service {

    public void onCreate() {
        super.onCreate();
        HSLog.i("libDevice", "JunkService onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    public IBinder onBind(Intent paramIntent) {
        return JunkServiceImpl.getInstance();
    }
}