package com.itj.jband.schedule;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created by Loyid on 2016-02-21.
 */
public class ScheduleManager {
    private static final String TAG = ScheduleManager.class.getSimpleName();

    private static ScheduleManager sInstance = null;
    private IScheduleManageService mService = null;
    private Context mContext;

    private Handler mHandler = new Handler();

    private ScheduleManager(Context context) {
        mContext = context;
        bindToService();
    }

    public static ScheduleManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ScheduleManager(context.getApplicationContext());
        }

        return sInstance;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IScheduleManageService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mHandler.removeCallbacks(mRebindToServiceRunnable);
            mHandler.postDelayed(mRebindToServiceRunnable, 1000);
        }
    };

    private Runnable mRebindToServiceRunnable = new Runnable() {
        @Override
        public void run() {
            bindToService();
        }
    };

    private void bindToService() {
        final boolean result = mContext.bindService(new Intent(mContext, ScheduleManageService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        if (result) {
            mHandler.removeCallbacks(mRebindToServiceRunnable);
        } else {
            mHandler.postDelayed(mRebindToServiceRunnable, 1000);
        }
    }

    public void updateSchedule(Schedule schedule) {
        if (mService == null) {
            Log.d(TAG, "failed to call updateSchedule() - service is not connected.");
            return;
        }

        try {
            mService.updateSchedule(schedule);
        } catch (RemoteException ex) {
            Log.e(TAG, "failed to call updateSchedule().", ex);
        }
    }

    public void cancelSchedule(Schedule schedule) {
        if (mService == null) {
            Log.d(TAG, "failed to call cancelSchedule() - service is not connected.");
            return;
        }

        try {
            mService.cancelSchedule(schedule);
        } catch (RemoteException ex) {
            Log.e(TAG, "failed to call cancelSchedule().", ex);
        }
    }
}
