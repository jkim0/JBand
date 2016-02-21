package com.itj.jband.schedule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.itj.jband.Utils;
import com.itj.jband.databases.ProviderContract;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class ScheduleManageService extends Service {
    private static final String TAG = ScheduleManageService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String ACTION_UPDATE_SCHEDULE = "com.itj.jband.ACTION_UPDATE_SCHEDULE";
    private static final String EXTRA_SCHEDULE = "scheduel";

    private ServiceHandler mServiceHandler;

    private AlarmManager mAlarmManager;

    private final Object mUpdateLock = new Object();

    private int mStartId = 0;

    private IBinder mBinder = new ScheduleManageServiceProxy(this);

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if (onHandleIntent((Intent)msg.obj)) {
                mStartId = msg.arg1;
            } else {
                stopSelf(msg.arg1);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread(ScheduleManageService.class.getSimpleName() + "-Hendler",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceHandler = new ServiceHandler(thread.getLooper());

        mAlarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive action = " + action);
                updateSchedules();
            }
        }, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) {
            Log.d(TAG, "onStartCommand intent = " + intent + " startId = " + startId);
        }
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        msg.sendToTarget();

        return START_STICKY;
    }

    private boolean onHandleIntent(Intent intent) {
        if (intent == null) {
            return true;
        }

        String action = intent.getAction();
        Log.d(TAG, "onHandleIntent action = " + action);

        if (mStartId <= 0) {
            updateSchedules();
        } else if (action.equals(ACTION_UPDATE_SCHEDULE)) {
            Schedule schedule = intent.getParcelableExtra(EXTRA_SCHEDULE);
            updateSchedule(schedule);
        }

        if (mStartId <= 0) {
            return true;
        }

        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void updateSchedules() {
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mUpdateLock) {
                    updateSchedulesInternal();
                }
            }
        });
    }

    private void updateSchedulesInternal() {
        List<Schedule> scheduleList = Schedule.getSchedules(getContentResolver(), ProviderContract.ScheduleColumns.COLUMN_SCHEDULE_IS_ON + " = 1", null);
        for (Schedule schedule : scheduleList) {
            updateScheduleInternal(schedule);
        }
    }

    public void updateSchedule(final Schedule schedule) {
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mUpdateLock) {
                    updateScheduleInternal(schedule);
                }
            }
        });
    }

    public void updateScheduleInternal(Schedule schedule) {
        PendingIntent intent = createPendingIntent(schedule);

        Calendar currentTime = Calendar.getInstance();
        Calendar nextInvokeTime = schedule.getNextInvokeTime(currentTime);
        if (DEBUG) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
            Log.d(TAG, "currentTime = " + sdf.format(currentTime.getTime()));
            Log.d(TAG, "nextInvokeTime = " + sdf.format(nextInvokeTime.getTime()));
        }

        if (Utils.isKitkatOrLater()) {
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, nextInvokeTime.getTimeInMillis(), intent);
        } else {
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, nextInvokeTime.getTimeInMillis(), intent);
        }
    }

    public void cancelSchedule(final Schedule schedule) {
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mUpdateLock) {
                    PendingIntent intent = createPendingIntent(schedule);
                    mAlarmManager.cancel(intent);
                }
            }
        });
    }

    private PendingIntent createPendingIntent(Schedule schedule) {
        Intent intent = new Intent(this, ScheduleEventReceiver.class);
        intent.putExtra("schedule_id", schedule.mId);
        Bundle data = new Bundle();
        data.putParcelable("schedule", schedule);
        intent.putExtra("data", data);

        return PendingIntent.getBroadcast(this, schedule.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static class ScheduleEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getBundleExtra("data");
            if (data != null) {
                Schedule schedule = (Schedule)data.getParcelable("schedule");
                Log.d(TAG, "schedule = " + (Schedule) data.getParcelable("schedule"));

                // TODO
                // send alarm command to device
                Toast.makeText(context, "Schedule - " + schedule.mName + ":" + schedule.mNotification, Toast.LENGTH_SHORT).show();

                // update next schedule
                Intent serviceIntent = new Intent(context, ScheduleManageService.class);
                serviceIntent.setAction(ACTION_UPDATE_SCHEDULE);
                serviceIntent.putExtra(EXTRA_SCHEDULE, schedule);
                context.startService(serviceIntent);
            }

        }
    }

    private class ScheduleManageServiceProxy extends IScheduleManageService.Stub {
        private WeakReference<ScheduleManageService> mService;

        public ScheduleManageServiceProxy(ScheduleManageService service) {
            mService = new WeakReference<ScheduleManageService>(service);
        }

        @Override
        public void updateSchedule(Schedule schedule) throws RemoteException {
            mService.get().updateSchedule(schedule);
        }

        @Override
        public void cancelSchedule(Schedule schedule) throws RemoteException {
            mService.get().cancelSchedule(schedule);
        }
    }
}
