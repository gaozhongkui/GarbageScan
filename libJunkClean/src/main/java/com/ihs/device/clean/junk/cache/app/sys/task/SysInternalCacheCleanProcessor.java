package com.ihs.device.clean.junk.cache.app.sys.task;

import android.content.pm.IPackageDataObserver;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StatFs;

import com.ihs.app.framework.HSApplication;
import com.ihs.device.common.async.AsyncProcessor;
import com.ihs.commons.utils.HSLog;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

public class SysInternalCacheCleanProcessor extends AsyncProcessor<Void, Void, Long> {

    public SysInternalCacheCleanProcessor(OnProcessListener<Void, Long> processListener) {
        super(processListener);
    }

    @Override
    protected Long doInBackground(Void... params) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        long size = (long) stat.getBlockCount() * (long) stat.getBlockSize();
        try {
            Method freeStorageAndNotifyMethod = HSApplication.getContext().getPackageManager().getClass()
                    .getMethod("freeStorageAndNotify", long.class, IPackageDataObserver.class);
            freeStorageAndNotifyMethod.setAccessible(true);

            freeStorageAndNotifyMethod.invoke(HSApplication.getContext().getPackageManager(), size, new IPackageDataObserver.Stub() {
                @Override
                public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
                    HSLog.i("libDevice", "packageName:" + packageName + " succeeded:" + succeeded);
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }
}
