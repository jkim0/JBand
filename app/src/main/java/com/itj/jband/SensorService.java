package com.itj.jband;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class SensorService extends Service {
    private static final String TAG = SensorService.class.getSimpleName();

    public static String ACTION_START_SENSOR_MONITORING = "com.itj.jbanb.action.START_SENSOR_MONIORING";
    public static String ACTION_STOP_SENSOR_MONITORING = "com.itj.jband.action.STOP_SENSOR_MONITORING";

    private static final int MSG_HANDLE_INTENT = 1;

    private ServiceHanadler mServiceHandler = null;
    private SensorManager mSensorManager = null;

    private HashMap<ISensorEventListener, SensorEventListenerProxy> mEventListeners = new HashMap<ISensorEventListener, SensorEventListenerProxy>();

    private int mStepCounted = 0;

    private IBinder mBinder = new SensorServiceProxy(this);

    private class ServiceHanadler extends Handler {
        public ServiceHanadler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_HANDLE_INTENT:
                    onHandleIntent((Intent)msg.obj);
                    break;
            }
        }
    }

    public SensorService() {
        HandlerThread thread = new HandlerThread(TAG + "-Handler");
        thread.start();

        mServiceHandler = new ServiceHanadler(thread.getLooper());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            final int type = sensorEvent.sensor.getType();
            switch (type) {
                case Sensor.TYPE_STEP_COUNTER:
                    synchronized (mEventListeners) {
                        if (mStepCounted < 1) {
                            mStepCounted = (int)sensorEvent.values[0];
                        }

                        for (SensorEventListenerProxy proxy : mEventListeners.values()) {
                            proxy.onStepCountReceived((int)sensorEvent.values[0] - mStepCounted);
                        }
                    }
                    break;
                case Sensor.TYPE_STEP_DETECTOR:
                    synchronized (mEventListeners) {
                        for (SensorEventListenerProxy proxy : mEventListeners.values()) {
                            proxy.onStepDetected();
                        }
                    }
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    synchronized (mEventListeners) {
                        for (SensorEventListenerProxy proxy : mEventListeners.values()) {
                            proxy.onAccelerometerDataReceived(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                        }
                    }
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand inetn = " + intent + " flags = " + flags + " startId = " + startId);
        mServiceHandler.obtainMessage(MSG_HANDLE_INTENT, intent).sendToTarget();

        return START_STICKY;
    }

    private void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "onHandleIntent:intent is null");
            return;
        }

        final String action = intent.getAction();
        if (action.equals(ACTION_START_SENSOR_MONITORING)) {
            startSensorMonitoring();
        } else if (action.equals(ACTION_STOP_SENSOR_MONITORING)) {
            stopSensorMonitoring();
        }
    }

    private void startSensorMonitoring() {
        Log.d(TAG, "startSensorMonitoring()");
        mStepCounted = 0;
        Sensor stepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepCounter != null) {
            mSensorManager.registerListener(mSensorEventListener, stepCounter, SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.d(TAG, "This device do not support step counter sensor.");
        }

        Sensor stepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (stepDetector != null) {
            mSensorManager.registerListener(mSensorEventListener, stepDetector, SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.d(TAG, "This device do not support step detector sensor.");
        }

        Sensor accelerometor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometor != null) {
            mSensorManager.registerListener(mSensorEventListener, accelerometor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.d(TAG, "This device do not support accelerometor sensor.");
        }
    }

    private void stopSensorMonitoring() {
        mSensorManager.unregisterListener(mSensorEventListener);
        mStepCounted = 0;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.d(TAG, "onBind intent = " + intent);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class SensorServiceProxy extends ISenserService.Stub {
        private WeakReference<SensorService> mService;
        public SensorServiceProxy(SensorService service) {
            mService = new WeakReference<SensorService>(service);
        }

        @Override
        public void registerSensorEventListener(ISensorEventListener listener) throws RemoteException {
            mService.get().registerSensorEventListener(listener);
        }

        @Override
        public void unregisterSensorEventListener(ISensorEventListener listener) throws RemoteException {
            mService.get().unregisterSensorEventListener(listener);
        }
    }

    private class SensorEventListenerProxy implements ISensorEventListener.Stub.DeathRecipient {
        private ISensorEventListener mListener;

        public SensorEventListenerProxy(ISensorEventListener listener) {
            mListener = listener;
        }

        public void onAccelerometerDataReceived(float x, float y, float z) {
            try {
                mListener.onAccelerometerDataReceived(x, y, z);
            } catch (RemoteException ex) {
                Log.e(TAG, "failed notify onAccelerometerDataReceived.");
            }
        }

        public void onStepCountReceived(int count) {
            try {
                mListener.onStepCountReceived(count);
            } catch (RemoteException ex) {
                Log.e(TAG, "failed notify stopCountReceived.");
            }
        }

        public void onStepDetected() {
            try {
                mListener.onStepDetected();
            } catch (RemoteException ex) {
                Log.e(TAG, "failed notify onStepDetected.");
            }
        }

        @Override
        public void binderDied() {
            synchronized (mEventListeners) {
                mEventListeners.remove(this);
            }
        }
    }

    private void registerSensorEventListener(final ISensorEventListener listener) {
        synchronized (mEventListeners) {
            SensorEventListenerProxy proxy = new SensorEventListenerProxy(listener);
            mEventListeners.put(listener, proxy);
        }
    }

    private void unregisterSensorEventListener(ISensorEventListener listener) {
        synchronized (mEventListeners) {
            SensorEventListenerProxy proxy = mEventListeners.get(listener);
            mEventListeners.remove(proxy);
        }
    }
}
