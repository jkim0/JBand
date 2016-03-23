package com.itj.jband;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaLink;
import com.csr.gaia.library.GaiaPacket;
import com.itj.jband.schedule.ScheduleManageService;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public class GaiaControlService extends Service {
    private static final String TAG = GaiaControlService.class.getSimpleName();

    private static final boolean DEBUG = true;
    private static final boolean SUPPORT_DIRECT_CONNECTION = false;

    public static final String ACTION_START_GAIA_SERVICE = "com.itj.jband.ACTION_GAIA_START";
    public static final String ACTION_STOP_GAIA_SERVICE = "com.itj.jband.ACTION_GAIA_STOP";

    private ServiceHandler mServiceHandler;

    private GaiaLink mGaiaLink;
    private GaiaLink.Transport mTransposrt = GaiaLink.Transport.BT_GAIA;
    private boolean mIsConnected;
    private boolean mSleepMode = false;

    private Handler mDeviceHandler = new Handler();

    private int mStartId = 0;

    private IBinder mBinder = new GaiaControlServiceProxy(this);
    private HashMap<IGaiaEventListener, GaiaEventListenerProxy> mEventListeners = new HashMap<>();

    // Handler that receives messages from the thread
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
        HandlerThread thread = new HandlerThread(GaiaControlService.class.getSimpleName() + "-Hendler",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceHandler = new ServiceHandler(thread.getLooper());

        mGaiaLink = GaiaLink.getInstance();
        mGaiaLink.setReceiveHandler(mGaiaReceiveHandler);
        mGaiaLink.setLogHandler(mGaiaReceiveHandler);

        if (SUPPORT_DIRECT_CONNECTION) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        for (ParcelUuid uuid : device.getUuids()) {
                            if (uuid.equals(ParcelUuid.fromString("00001107-D102-11E1-9B23-00025B00A5A5"))) {
                                Log.d(TAG, "received intent = " + intent.getAction() + " device = " + device.getName()
                                        + "has Gaia UUID, so try to connect directly.");
                                connect(device);
                                break;
                            }
                        }
                    }
                }
            }, filter);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        registerReceiver(mBroadcastReceiver, filter);

        Intent serviceIntent = new Intent(this, ScheduleManageService.class);
        startService(serviceIntent);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_UUID)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String uuidStr = intent.getStringExtra(BluetoothDevice.EXTRA_UUID);
                Log.d(TAG, "device name = " + device.getName() + " uuid = " + uuidStr);
                ParcelUuid[] uuids = device.getUuids();
                if (uuids != null) {
                    for (ParcelUuid uuid : uuids) {
                        Log.d(TAG, "uuid2 = " + uuid + " uuid = " + uuid.getUuid());
                    }
                }
            } else if (action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                onSmsReceived(intent);
            }
        }
    };

    @Override
    public void onDestroy() {
        shutDownGaia();
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private void shutDownGaia() {
        if (mGaiaLink != null) {
            if (mGaiaLink.isConnected()) {
                mGaiaLink.disconnect();
            }
            mGaiaLink.setReceiveHandler(null);
            mGaiaLink = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand startId = " + startId);
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        msg.sendToTarget();

        return START_STICKY;
    }

    private boolean onHandleIntent(Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        Log.d(TAG, "onHandleIntent action = " + action);
        if (action.equals(ACTION_START_GAIA_SERVICE) && mStartId <= 0) {
            // TODO
            return true;
        } else if (action.equals(ACTION_STOP_GAIA_SERVICE)) {
            // TODO
            // disconnect already connected bluetooth device and stop service.
            if (mStartId > 0) {
                stopSelf(mStartId);
                mStartId = 0;
            }
        }

        return false;
    }

    private Handler mGaiaReceiveHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "msg.what = " + msg.what + " arg1 = " + msg.arg1 + " arg2 = " + msg.arg2);
            switch (GaiaLink.Message.valueOf(msg.what)) {
                case CONNECTED:
                    if (DEBUG)
                        Log.d(TAG, "CONNECTED");
                    handleGaiaConnection(true);
                    break;
                case DISCONNECTED:
                    if (DEBUG)
                        Log.d(TAG, "DISCONNECTED");
                    handleGaiaConnection(false);
                    break;
                case PACKET:
                    GaiaPacket packet = (GaiaPacket) msg.obj;
                    handlePackets(packet);
                    break;
                case STREAM:;
                    break;
                case ERROR:
                    if (DEBUG)
                        Log.d(TAG, "ERROR");
                    GaiaError error = (GaiaError) msg.obj;
                    handleError(error);
                    break;
                case DEBUG:
                    handleLog((String)msg.obj);
                    break;
            }
        }
    };

    private void handleLog(String message) {
        synchronized (mEventListeners) {
            for (GaiaEventListenerProxy proxy : mEventListeners.values()) {
                proxy.onLog(message);
            }
        }
    }

    /**
     * Event handler triggered when the GAIA link connects.
     */
    private void handleGaiaConnection(boolean connected) {
        Toast.makeText(this, mGaiaLink.getName() + " connected", Toast.LENGTH_SHORT);
        mIsConnected = connected;

        if (connected) {
            getDeveiceInfo();
        } else {
            // FIXME
            // do anything else.
        }

        synchronized (mEventListeners) {
            for (GaiaEventListenerProxy proxy : mEventListeners.values()) {
                proxy.onConnectionStateChanged(connected);
            }
        }
    }

    /**
     * Send GAIA commands to retrieve information about the remote device.
     */
    private void getDeveiceInfo() {
        // Send the request commands. If the remote device responds, the values will be received as a GAIA message
        // that will be processed in handleTheUnhandled() to update the local variables holding state about the remote device.
        if (mIsConnected) {
            sendGaiaPacket(Gaia.COMMAND_GET_API_VERSION);
            sendGaiaPacket(Gaia.COMMAND_GET_LED_CONTROL);
            sendGaiaPacket(Gaia.COMMAND_GET_CURRENT_RSSI);
            sendGaiaPacket(Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL);
            //mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_REQ_BT_STATUS, true);
            // TODO
            // set default sleep mode
            mSleepMode = Utils.getSavedSleepMode(this);
            if (mSleepMode) {
                //sendGaiaPacket(Gaia.COMMAND_SET_SLEEP_MODE);
            }

            mGaiaLink.registerNotification(Gaia.EventId.CHARGER_CONNECTION);
        }
    }

    private void handleError(GaiaError error) {
        switch (error.getType()) {
            case UNSUPPORTED_TRANSPORT:
                if (DEBUG) {
                    Log.d(TAG, "handleError::error - Unsupported_transport");
                }
                break;
            case ILLEGAL_ARGUMENT:
                if (DEBUG) {
                    Log.d(TAG, "handleERror::error - Illeagal argument.");
                }
                break;
            case DEVICE_UNKNOWN_ADDRESS:
                if (DEBUG) {
                    Log.d(TAG, "handleError::error - Unknown device address");
                }
                break;
            case CONNECTION_FAILED:
                if (DEBUG) {
                    Log.d(TAG, "handleError::error - Failed to connect");
                }
                break;
            case ALREADY_CONNECTED:
                if (DEBUG) {
                    Log.d(TAG, "handleError::error - device is aleady connected");
                }
                break;
            case BLUETOOTH_NOT_SUPPORTED:
                if (DEBUG) {
                    Log.d(TAG, "handleError::error - bluetooth is not supported on device");
                }
                // This case has already been tested in this activity by extending of ModelActivity.
                break;
            case SENDING_FAILED:
                String message;
                if (error.getCommand() > 0) {
                    message = "Send command " + error.getCommand() + " failed";

                }
                else {
                    message = "Send command failed";
                }
                if (DEBUG)
                    Log.w(TAG, "handleError::error - " + message + ": " + error.getStringException());
                break;
        }
    }

    /**
     * To check the status of an acknowledgement packet.
     *
     * @param packet
     *            the packet to check.
     *
     * @return true if the status is SUCCESS and the packet is an acknowledgment, false otherwise.
     */
    private boolean checkStatus(GaiaPacket packet) {
        if (!packet.isAcknowledgement()) {
            return false;
        }

        Log.d(TAG, "checkStatus() status = " + packet.getStatus().toString());
        switch (packet.getStatus()) {
            case SUCCESS:
                return true;
            case NOT_SUPPORTED:
                receivePacketCommandNotSupported(packet);
                break;
            case AUTHENTICATING:
            case INCORRECT_STATE:
            case INSUFFICIENT_RESOURCES:
            case INVALID_PARAMETER:
            case NOT_AUTHENTICATED:
            default:
                if (DEBUG)
                    Log.w(TAG, "Status " + packet.getStatus().toString() + " with the command " + packet.getCommand());
        }
        return false;
    }

    /**
     * When we received a packet about a command which is not supported by the device.
     *
     * @param packet
     *            the concerned packet.
     */
    private void receivePacketCommandNotSupported(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL:
                if (DEBUG)
                    Log.w(TAG, "Received \"COMMAND_GET_CURRENT_BATTERY_LEVEL\" not supported.");
                //mListAdapter.setValue(Information.BATTERY_LEVEL.ordinal(), getString(R.string.info_not_supported));
                break;

            case Gaia.COMMAND_GET_CURRENT_RSSI:
                if (DEBUG)
                    Log.w(TAG, "Received \"COMMAND_GET_CURRENT_RSSI\" not supported.");
                //mListAdapter.setValue(Information.SIGNAL_LEVEL.ordinal(), getString(R.string.info_not_supported));
                break;

            case Gaia.COMMAND_GET_API_VERSION:
                if (DEBUG)
                    Log.w(TAG, "Received \"COMMAND_GET_API_VERSION\" not supported.");
                //mListAdapter.setValue(Information.API_VERSION.ordinal(), getString(R.string.info_not_supported));
                break;

            case Gaia.COMMAND_EVENT_NOTIFICATION:
                if (DEBUG)
                    Log.w(TAG, "Received \"COMMAND_EVENT_NOTIFICATION\" not supported.");
                //mListAdapter.setValue(Information.BATTERY_STATUS.ordinal(), getString(R.string.info_not_supported));
                break;
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_GET_LED_CONTROL to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_LED_CONTROL.
     */
    private void receiveCurrentLedControl(GaiaPacket packet) {
        if (checkStatus(packet)) {
            final boolean ledActivated = packet.getBoolean();
            if (DEBUG) {
                Log.d(TAG, "receiveCurrentLedControl activated = " + ledActivated);
            }
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_GET_CURRENT_BATTERY_LEVEL to manage the application
     * depending on information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_CURRENT_BATTERY_LEVEL.
     */
    private void receiveCurrentBatteryLevel(GaiaPacket packet) {
        if (checkStatus(packet)) {
            final int level = Utils.extractIntField(packet.getPayload(), 1, 2, false);
            if (DEBUG) {
                Log.d(TAG, "receiveGetGurrentBatteryLevel lebel = " + level);
            }
            // we need to retrieve this information constantly
            //mHandler.postDelayed(mRunnableBattery, WAITING_TIME);
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_GET_CURRENT_RSSI to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_CURRENT_RSSI.
     */
    private void receiveCurrentRSSI(GaiaPacket packet) {
        if (checkStatus(packet)) {
            final int level = packet.getByte(1);
            if (DEBUG) {
                Log.d(TAG, "receiveCurrentRSSI rssiLevel = " + level);
            }
            // we need to retrieve this information constantly
            //mHandler.postDelayed(mRunnableRSSI, WAITING_TIME);
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_GET_CURRENT_RSSI to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_CURRENT_RSSI.
     */
    private void receiveApiVersion(GaiaPacket packet) {
        if (checkStatus(packet)) {
            final String apiVersion = packet.getByte(1) + "." + packet.getByte(2) + "." + packet.getByte(3);
            if (DEBUG) {
                Log.d(TAG, "receiveApiVersion version = " + apiVersion);
            }
        }
    }

    /**
     * To handle notifications coming from the Gaia device.
     */
    private void handleNotification(GaiaPacket packet) {
        Gaia.EventId event = packet.getEvent();
        switch (event) {
            case CHARGER_CONNECTION:
                byte[] payload = packet.getPayload();
                boolean inCharge = false;
                if (payload != null && payload.length > 0) {
                    inCharge = payload[1] == 0x01;
                }
                if (DEBUG)
                    Log.d(TAG, "handleNotification::CHARGER_CONNECTION in charge ? " + inCharge);
                break;
            default:
                if (DEBUG)
                    Log.i(TAG, "Received event: " + event);
        }
    }

    /**
     * To manage packets from Gaia device which are "PACKET" directly by the library.
     *
     * @param packet
     *            The message coming from the handler which calls this method.
     */
    private void handlePackets(GaiaPacket packet) {
        boolean validate;
        switch (packet.getCommand()) {
            case Gaia.COMMAND_GET_LED_CONTROL:
                validate = checkStatus(packet);
                if (DEBUG)
                    Log.i(TAG, "Received \"COMMAND_GET_LED_CONTROL\" packet with a " + validate + " status.");
                receiveCurrentLedControl(packet);
                break;
            case Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL:
                validate = checkStatus(packet);
                if (DEBUG)
                    Log.i(TAG, "Received \"COMMAND_GET_CURRENT_BATTERY_LEVEL\" packet with a " + validate + " status.");
                receiveCurrentBatteryLevel(packet);
                break;

            case Gaia.COMMAND_GET_CURRENT_RSSI:
                validate = checkStatus(packet);
                if (DEBUG)
                    Log.i(TAG, "Received \"COMMAND_GET_CURRENT_RSSI\" packet with a " + validate + " status.");
                receiveCurrentRSSI(packet);
                break;

            case Gaia.COMMAND_GET_API_VERSION:
                validate = checkStatus(packet);
                if (DEBUG)
                    Log.i(TAG, "Received \"COMMAND_GET_API_VERSION\" packet with a " + validate + " status.");
                receiveApiVersion(packet);
                break;

            case Gaia.COMMAND_EVENT_NOTIFICATION:
                if (DEBUG)
                    Log.i(TAG, "Received \"Notification\" packet.");
                handleNotification(packet);
                break;

            case Gaia.COMMAND_SET_SLEEP_MODE:
                validate = checkStatus(packet);
                if (DEBUG) {
                    Log.d(TAG, "Received \"COMMAND_SET_SLEEP_MODE\" packet with a " + validate + " status.");
                }
                break;

            default:
                if (DEBUG)
                    Log.d(TAG, "Received packet - command: " + Utils.getIntToHexadecimal(packet.getCommandId())
                            + " - payload: " + Utils.getStringFromBytes(packet.getPayload()));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    private class GaiaControlServiceProxy extends IGaiaControlService.Stub {
        private WeakReference<GaiaControlService> mService;

        public GaiaControlServiceProxy(GaiaControlService service) {
            mService = new WeakReference<GaiaControlService>(service);
        }

        @Override
        public void connect(BluetoothDevice device) throws RemoteException {
            mService.get().connect(device);
        }

        @Override
        public void disconnect() throws RemoteException {
            mService.get().disconnect();
        }

        @Override
        public boolean isConnected() throws RemoteException {
            return mService.get().isConnected();
        }

        @Override
        public void setSleepMode(boolean mode) throws RemoteException {
            mService.get().setSleepMode(mode);
        }

        @Override
        public void sendCommand(int vendorId, int commandId, byte[] payload) {
            mService.get().sendCommand(vendorId, commandId, payload);
        }

        @Override
        public void registerEventListener(IGaiaEventListener listener) throws RemoteException {
            mService.get().registerEventListener(listener);
        }

        @Override
        public void unregisterEventListener(IGaiaEventListener listener) throws RemoteException {
            mService.get().unregisterEventListener(listener);
        }
    }

    private void connect(BluetoothDevice device) {
        Log.d(TAG, "connect device = " + device.toString() + " name = " + device.getName());
        if (mGaiaLink.isConnected()) {
            if (mGaiaLink.getBluetoothDevice().equals(device)) {
                Log.d(TAG, "Gaia link is aleady connected so ignore.");
                return;
            } else {
                Log.d(TAG, "Gaia link is aleady connected so disconnect first.");
                mGaiaLink.disconnect();
            }
        }

        if (device != null && device.getUuids() != null) {
            for (ParcelUuid id : device.getUuids()) {
                Log.d(TAG, "device name = " + device + " uuid = " + id.toString());
            }
        }/* else if (device.getUuids() == null) {
            Log.d(TAG, "device has not uuids so request uuids before connect");
            device.fetchUuidsWithSdp();
            return;
        }*/

        mGaiaLink.connect(device, mTransposrt);
    }

    private void disconnect() {
        Log.d(TAG, "disconnect aleady connected = " + mGaiaLink.isConnected());
        mGaiaLink.disconnect();
    }

    private boolean isConnected() {
        return mGaiaLink.isConnected();
    }

    private void setSleepMode(boolean mode) {
        Log.d(TAG, "setSleepMode mode = " + mode + " oldMode = " + mSleepMode);
        // TODO
        // send sleep command to device
        if (mSleepMode == mode) {
            Log.d(TAG, "igore this request because requested mode is same with oldMode.");
            return;
        }

        mSleepMode = mode;
        Utils.saveSleepMode(this, mode);
        sendGaiaPacket(Gaia.COMMAND_SET_SLEEP_MODE);
    }

    private void sendCommand(int vendorId, int commandId, byte[] payload) {
        Log.d(TAG, "sendCommand vendorId = " + vendorId + " commandId = " + commandId);
        mGaiaLink.sendCommand(vendorId, commandId, payload);
    }

    private void registerEventListener(IGaiaEventListener listener) {
        synchronized (mEventListeners) {
            if (mEventListeners.containsKey(listener)) {
                throw new IllegalStateException("listener is already registered.");
            }

            GaiaEventListenerProxy proxy = new GaiaEventListenerProxy(listener);
            mEventListeners.put(listener, proxy);
        }
    }

    private void unregisterEventListener(IGaiaEventListener listener) {
        synchronized (mEventListeners) {
            if (mEventListeners.containsKey(listener)) {
                mEventListeners.remove(listener);
            } else {
                throw new IllegalStateException("there is no registered listener.");
            }
        }
    }

    private class GaiaEventListenerProxy implements IGaiaEventListener.Stub.DeathRecipient {
        private IGaiaEventListener mListener;

        public GaiaEventListenerProxy(IGaiaEventListener listener) {
            mListener = listener;
        }

        @Override
        public void binderDied() {
            synchronized (mEventListeners) {
                mEventListeners.remove(mListener);
            }
        }

        private void onConnectionStateChanged(boolean state) {
            try {
                mListener.onConnectionStateChanged(state);
            } catch (RemoteException ex) {
                Log.d(TAG, "failed to notify changed connection state.");
            }
        }

        private void onLog(String message) {
            try {
                mListener.onLog(message);
            } catch (RemoteException ex) {
                Log.d(TAG, "failed to notify log.");
            }
        }
    }

    /**
     * To send a packet to the device.
     *
     * @param command
     *            the command to send to the device.
     * @param payload
     *            the additional information for the command.
     */
    private void sendGaiaPacket(int command, int... payload) {
        mGaiaLink.sendCommand(Gaia.VENDOR_CSR, command, payload);
    }

    private void onSmsReceived(Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "onSmsReceived");
        }
        Bundle data = intent.getExtras();
        if (data == null) {
            Log.d(TAG, "ignore this cause extra data is null");
            return;
        }

        Object[] pdus = (Object[])data.get("pdus");
        if (pdus == null ) {
            Log.d(TAG, "ignore this cause pdu data is null");
            return;
        }

        String format = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            format = data.getString("format");
        }

        String smsMessage = "";
        String smsFrom = null;
        for (int i = 0; i < pdus.length; i++) {
            SmsMessage message;
            if (TextUtils.isEmpty(format)) {
                message = SmsMessage.createFromPdu((byte[]) pdus[i]);
            } else {
                message = SmsMessage.createFromPdu((byte[]) pdus[i], format);
            }

            if (smsFrom == null) {
                smsFrom = message.getOriginatingAddress();
            }

            smsMessage += message.getMessageBody();
        }

        Log.d(TAG, "sms message received : from : " + smsFrom + ", message : " + smsMessage);
    }
}
