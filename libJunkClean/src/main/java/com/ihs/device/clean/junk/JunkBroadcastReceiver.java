package com.ihs.device.clean.junk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.ihs.app.framework.HSNotificationConstant;
import com.ihs.device.clean.junk.service.JunkService;
import com.ihs.device.common.utils.Utils;

public class JunkBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (action) {
            case HSNotificationConstant.HS_SESSION_START:
                LibCleanConfigManager.getInstance().startSessionInMainLooper();

                Utils.startServiceSafely(context, new Intent(context, JunkService.class));
                break;
            case HSNotificationConstant.HS_SESSION_END:
                Utils.stopServiceSafely(context, new Intent(context, JunkService.class));
                break;
        }
    }
}
