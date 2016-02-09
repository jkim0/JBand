package com.itj.jband;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

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

    private boolean isStorePskey47 = false;

    private Handler mDeviceHandler = new Handler();

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
        //startGaia();
        return false;//true;
    }

    Handler mGaiaReceiveHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "msg.what = " + msg.what + " arg1 = " + msg.arg1 + " arg2 = " + msg.arg2);
            switch (GaiaLink.Message.valueOf(msg.what)) {
                case DEBUG:
                    Log.d(TAG, "here");
                    break;
                case UNHANDLED:
                    Log.d(TAG, "what");
                    handleTheUnhandled(msg);
                    break;
                case CONNECTED:
                    Log.d(TAG, "how");
                    mIsConnected = true;
                    handleGaiaConnected();
                    break;
                case DISCONNECTED:
                    Log.d(TAG, "where");
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
                    Log.d(TAG, "now");
                    break;
                default:
                    Log.d(TAG, "go");
                    break;
            }
        }
    };

    /**
     * Event handler triggered when the GAIA link connects.
     */
    private void handleGaiaConnected() {
        Toast.makeText(this, mGaiaLink.getName() + " connected", Toast.LENGTH_SHORT);
        mIsConnected = true;

        mDeviceHandler.removeCallbacks(updateDeviceInfo);
        mDeviceHandler.postDelayed(updateDeviceInfo, 1000);
    }

    /**
     * Get information about the remote device.
     */
    private Runnable updateDeviceInfo = new Runnable() {
        public void run() {
            getDeveiceInfo();
        }
    };

    /**
     * Send GAIA commands to retrieve information about the remote device.
     */
    private void getDeveiceInfo() {
        // Send the request commands. If the remote device responds, the values will be received as a GAIA message
        // that will be processed in handleTheUnhandled() to update the local variables holding state about the remote device.
        if (mIsConnected) {
            try {
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_TTS_LANGUAGE);
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_API_VERSION);
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_CURRENT_RSSI);
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL);
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_RETRIEVE_FULL_PS_KEY, 0x02, 0x99); // PSKEY_FEATURE_BLOCK
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_RETRIEVE_FULL_PS_KEY, 0x02, 0x93); // PSKEY_USER_CONFIGURATION_9
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_RETRIEVE_FULL_PS_KEY, 0x02, 0xb9); // PSKEY_FEATURE_BLOCK
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_LED_CONTROL);
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_BOOT_MODE);
                /*Below code is just for demo, set PSKEY configuration data 47 as initial value
                 *0x0000 means whole partitions are unused.
                 */
                byte[] Pskeydata47 = new byte[4];
                Pskeydata47[0] = (byte) 0x00;
                Pskeydata47[1] = (byte) 0x2f;
                Pskeydata47[2] = (byte) 0x00;
                Pskeydata47[3] = (byte) 0x00;
                isStorePskey47 = true;
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_STORE_PS_KEY, Pskeydata47);
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_MOUNTED_PARTITIONS);
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_LED_CONTROL);
            }
            catch (IOException e) {
                Toast.makeText(this, "Failed to retrieve info from remote device.", Toast.LENGTH_SHORT);
                Log.d(TAG,"sendCommand failed in getDeviceInfo: " + e.getMessage());
            }
        }
    }

    /**
     * Handle a command not handled by GaiaLink.
     * @param msg The Message object containing the command.
     */
    private void handleTheUnhandled(Message msg) {
        Log.d(TAG, "handleTheUnhandled msg = " + msg);
        mCommand = (GaiaCommand) msg.obj;

        Gaia.Status status = mCommand.getStatus();
        int command_id = mCommand.getCommand();

        // Handle acks for commands we have sent.
        if (mCommand.isAcknowledgement()) {
            if (status == Gaia.Status.SUCCESS) {
                Log.d(TAG, Integer.toHexString(mCommand.getVendorId()) + ":" + Integer.toHexString(mCommand.getCommandId()) + " = " + mCommand.getPayload().length);

                // Act on an acknowledgement
                switch (command_id) {
                    case Gaia.COMMAND_DEVICE_RESET:
                        try {
                            if (mIsConnected) {
                                //isRebootDevice = true;
                                mGaiaLink.disconnect();
                                Toast.makeText(this, "Device reboot started, will reconnect it in 6 seconds", Toast.LENGTH_SHORT).show();
                                mIsConnected = false;
                            }
                        }
                        catch (IOException e) {
                            Log.d(TAG, "Disconnect failed: " + e.getMessage());
                        }
                        break;

                    case Gaia.COMMAND_GET_BOOT_MODE:
                        int bootmode = 0;
                        if (mCommand.getByte(0) == 0x00) {
                            bootmode = mCommand.getByte(1);
                        }
                        Log.d(TAG, "get boot mode = " + bootmode);
                        break;

                    case Gaia.COMMAND_GET_STORAGE_PARTITION_STATUS:
                        Log.d(TAG, "get starage partition status");
                        break;

                    case Gaia.COMMAND_OPEN_STORAGE_PARTITION:
                        Log.d(TAG, "open storage partition");
                        break;

                    case Gaia.COMMAND_WRITE_STORAGE_PARTITION:
                        Log.d(TAG, "write storage partition");
                        break;

                    case Gaia.COMMAND_CLOSE_STORAGE_PARTITION:
                        Log.d(TAG, "close storage partition");
                        break;

                    case Gaia.COMMAND_RETRIEVE_FULL_PS_KEY:
                        Log.d(TAG, "retireve full ps key");
                        break;

                    case Gaia.COMMAND_GET_MOUNTED_PARTITIONS:
                        Log.d(TAG, "get mounted partitions");
                        break;
                    case Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL:
                        Log.d(TAG, "get current battery level");
                        break;
                    case Gaia.COMMAND_GET_API_VERSION: {
                        String apiVersion = "null";
                        if (mCommand.getByte(0) == 0x00) {
                            apiVersion = "" + mCommand.getByte(1) + "." + mCommand.getByte(2) + "." + mCommand.getByte(3);
                        }
                        Log.d(TAG, "get api version = " + apiVersion);
                    }
                    break;
                    case Gaia.COMMAND_GET_CURRENT_RSSI:
                        int rssi = 0;
                        if (mCommand.getByte(0) == 0x00) {
                            rssi = mCommand.getByte(1);
                        }
                        Log.d(TAG, "get current rssi = " + rssi);
                        break;
                    case Gaia.COMMAND_GET_APPLICATION_VERSION: {
                        String applicationVersion = "";
                        if (mCommand.getByte(0) == 0x00) {
                            for (Byte data : mCommand.getPayload()) {
                                String temp = Gaia.hexb(data);
                                applicationVersion += "" + Integer.valueOf(temp, 16).intValue();
                            }
                        }
                        Log.d(TAG, "get application version = " + applicationVersion);
                    }
                    break;
                    case Gaia.COMMAND_SET_TTS_LANGUAGE:
                        Log.d(TAG, "set tts language");
                        break;
                    case Gaia.COMMAND_MOUNT_STORAGE_PARTITION:
                        Log.d(TAG, "mount storage partition");
                        break;
                    case Gaia.COMMAND_GET_TTS_LANGUAGE:
                        Log.d(TAG, "get tts language");
                        break;
                    case Gaia.COMMAND_SET_LED_CONTROL:
                        Log.d(TAG, "set led control");
                        break;
                    case Gaia.COMMAND_GET_LED_CONTROL:
                        Log.d(TAG, "get led control");
                        if (mCommand.getByte(0) == 0x00) {
                            Log.d(TAG, "Get LED successfully");
                            boolean isOn = false;
                            if (mCommand.getByte(1) == 0x01)
                                isOn = true;

                            Log.d(TAG, "get ledControl ret = " + isOn);
                            if (!isOn) {
                                try {
                                    mGaiaLink.setLEDControl(true);
                                } catch (IOException ex) {
                                    Log.d(TAG, "Exception ex = " + ex);
                                }
                            }
                        }
                        break;
                    case Gaia.COMMAND_STORE_PS_KEY:
                        Log.d(TAG, "store ps key");
                        break;
                    case Gaia.COMMAND_REGISTER_NOTIFICATION:
                        Log.d(TAG, "Register notification successfully");
                        break;
                }
            }
            else {
                // Acknowledgement received with non-success result code,
                // so display the friendly message as a toast and dismiss any dialogs.
                Toast.makeText(this, "Error from remote device: command_id=" + command_id + ",status=" + Gaia.statusText(status), Toast.LENGTH_SHORT).show();
            }
        }
        else if (command_id == Gaia.COMMAND_EVENT_NOTIFICATION) {
            EventId event_id = mCommand.getEventId();
            Log.d(TAG, "Event " + event_id.toString());

            switch (event_id) {
                case CHARGER_CONNECTION:
                    if (mCommand.getBoolean())
                        Toast.makeText(this, "Charger connected", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, "Charger disconnected", Toast.LENGTH_SHORT).show();
                    break;

                case USER_ACTION:
                    int user_action = mCommand.getShort(1);
                    Log.d(TAG, String.format("HS Event 0x%04X --> %s", user_action, user_action));
                    break;
                case AV_COMMAND:
                    Log.d(TAG, "AV command : " + new String(mCommand.getPayload()));
                    break;
                case DEVICE_STATE_CHANGED:
                    Log.d(TAG, "current state : " + new String(mCommand.getPayload()));
                    break;
                case DEBUG_MESSAGE:
                    Log.d(TAG, "DEBUG_MESSAGE : " + new String(mCommand.getPayload()));
                    break;
                case KEY:
                    Log.d(TAG, "Key Event " + event_id.toString());
                    break;
                default:
                    Log.d(TAG, "Event " + event_id.toString());
                    break;
            }
        }
    }

    private void startGaia() {
        mGaiaLink = new GaiaLink();

        try {
            //mGaiaLink.listen();
            mGaiaLink.connect("00:02:5B:00:FF:03");
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
