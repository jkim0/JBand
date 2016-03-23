package com.itj.jband;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created by Loyid on 2016-02-20.
 */
public class GaiaControlManager {
    private static final String TAG = GaiaControlManager.class.getSimpleName();

    private Context mContext;

    private static GaiaControlManager sInstance = null;
    private IGaiaControlService mService = null;

    private Handler mHandler = new Handler();

    private GaiaControlManager(Context context) {
        mContext = context;
        bindToService();
    }

    public static GaiaControlManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GaiaControlManager(context.getApplicationContext());
        }

        return sInstance;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IGaiaControlService.Stub.asInterface(service);
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
        final boolean result = mContext.bindService(new Intent(mContext, GaiaControlService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        if (result) {
            mHandler.removeCallbacks(mRebindToServiceRunnable);
        } else {
            mHandler.postDelayed(mRebindToServiceRunnable, 1000);
        }
    }

    public boolean isConnected() {
        if (mService == null) {
            Log.d(TAG, "failed to call isConnected() - service is not connected.");
            return false;
        }

        try {
            return mService.isConnected();
        } catch (RemoteException ex) {
            Log.e(TAG, "failed to call isConnected().", ex);
        }

        return false;
    }

    public void connect(BluetoothDevice device) {
        if (mService == null) {
            Log.d(TAG, "failed to call connect() - service is not connected.");
        }

        try {
            mService.connect(device);
        } catch (RemoteException ex) {
            Log.e(TAG, "failed to call connect().", ex);
        }
    }

    public void disconnect() {
        if (mService == null) {
            Log.d(TAG, "failed to call disconnect() - service is not connected.");
        }

        try {
            mService.disconnect();
        } catch (RemoteException ex) {
            Log.e(TAG, "failed to call disconnect().", ex);
        }
    }

    public void setSleepMode(boolean isSleep) {
        if (mService == null) {
            Log.d(TAG, "failed to call setSleepMode() - service is not connected.");
        }

        try {
            mService.setSleepMode(isSleep);
        } catch (RemoteException ex) {
            Log.e(TAG, "failed to call setSleepMode().", ex);
        }
    }

    public void sendCommand(int vendorId, int commandId, byte[] payload) {
        if (mService == null) {
            Log.d(TAG, "failed to call setSleepMode() - service is not connected.");
        }

        try {
            mService.sendCommand(vendorId, commandId, payload);
        } catch (RemoteException ex) {
            Log.e(TAG, "failed to call setSleepMode().", ex);
        }
    }

    public void registerEventListener(GaiaEventListener listener) {
        if (mService == null) {
            Log.d(TAG, "failed to call registerEventListener() - service is not connected.");
        }

        try {
            mService.registerEventListener(listener);
        } catch (RemoteException ex) {
            Log.e(TAG, "failed to registerEventListener().", ex);
        }
    }

    public void unregisterEventListener(GaiaEventListener listener) {
        if (mService == null) {
            Log.d(TAG, "failed to call unregisterEventListener() - service is not connected.");
        }

        try {
            mService.unregisterEventListener(listener);
        } catch (RemoteException ex) {
            Log.e(TAG, "failed to call unregisterEventListener().", ex);
        }
    }

    public static abstract class GaiaEventListener extends IGaiaEventListener.Stub {
        @Override
        public abstract void onConnectionStateChanged(boolean state) throws RemoteException;

        @Override
        public abstract void onLog(String message) throws RemoteException;
    }
}
