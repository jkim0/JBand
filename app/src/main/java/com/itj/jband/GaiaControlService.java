package com.itj.jband;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.csr.gaia.android.library.DataConnectionListener;
import com.csr.gaia.android.library.Gaia;
import com.csr.gaia.android.library.Gaia.EventId;
import com.csr.gaia.android.library.Gaia.Status;
import com.csr.gaia.android.library.GaiaCommand;
import com.csr.gaia.android.library.GaiaLink;

import java.io.IOException;

public class GaiaControlService extends Service {
    private static final String TAG = GaiaControlService.class.getSimpleName();

    public static final String ACTION_START_GAIA_SERVICE = "com.itj.jband.ACTION_START";
    public static final String ACTION_STOP_GAIA_SERVICE = "com.itj.jband.ACTION_STOP";

    private ServiceHandler mServiceHandler;

    private GaiaLink mGaiaLink;
    private GaiaCommand mCommand;
    private boolean mIsConnected;
    private byte[] mPacketPayloadBuffer;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if (!onHandleIntent((Intent)msg.obj))
                stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread(GaiaControlService.class.getSimpleName() + "-Hendler",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceHandler = new ServiceHandler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        msg.sendToTarget();

        return START_STICKY;
    }

    private boolean onHandleIntent(Intent intent) {
        startGaia();
        return true;
    }

    Handler mGaiaReceiveHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "msg.what = " + msg.what + " arg1 = " + msg.arg1 + " arg2 = " + msg.arg2);
            switch (GaiaLink.Message.valueOf(msg.what)) {
                case DEBUG:
                    break;
                case UNHANDLED:
                    break;
                case CONNECTED:
                    mIsConnected = true;
                    break;
                case DISCONNECTED:
                    // Disconnect the link locally.
                    try {
                        mGaiaLink.disconnect();
                    } catch (IOException e) {
                        Log.d(TAG, "Disconnect failed: " + e.getMessage());
                    } finally {
                        // Ensure state is correct even if the disconnect failed.
                        mIsConnected = false;
                    }
                    break;
                case ERROR:
                    break;
            }
        }
    };

    private void startGaia() {
        mGaiaLink = new GaiaLink();

        try {
            mGaiaLink.listen();
        } catch (IOException ex) {
            Log.d(TAG, "Ex = ", ex);
        }

        mGaiaLink.setReceiveHandler(mGaiaReceiveHandler);

        // Listener that allows the GaiaLink to update this Activity to indicate when data is being sent or received.
        mGaiaLink.setDataConnectionListener(new DataConnectionListener() {
            /**
             * This will be called when data starts or stops being sent or received on the GaiaLink.
             * @param isDataTransferInProgress True if data is being sent or received on the link.
             */
            public void update(boolean isDataTransferInProgress) {
                Log.d(TAG, "DataConnectionListener::update isDataTransferInProgress = " + isDataTransferInProgress);
            }
        });

        // Allocate the buffer for building packets to be sent over the air.
        mPacketPayloadBuffer = new byte[GaiaLink.MAX_PACKET_PAYLOAD];
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
