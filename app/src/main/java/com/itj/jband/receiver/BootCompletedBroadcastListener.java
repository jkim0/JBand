package com.itj.jband.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.itj.jband.GaiaControlService;
import com.itj.jband.UsageStatisticActivity;
import com.itj.jband.schedule.ScheduleManageService;

public class BootCompletedBroadcastListener extends BroadcastReceiver {
    private static final String TAG =BootCompletedBroadcastListener.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent serviceIntent = new Intent(context, GaiaControlService.class);
            serviceIntent.setAction(GaiaControlService.ACTION_START_GAIA_SERVICE);
            context.startService(serviceIntent);
        }
    }
}
