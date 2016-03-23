// IGaiaControlService.aidl
package com.itj.jband;

// Declare any non-default types here with import statements
import android.bluetooth.BluetoothDevice;
import com.itj.jband.IGaiaEventListener;

interface IGaiaControlService {
    void connect(in BluetoothDevice device);
    void disconnect();
    boolean isConnected();
    void setSleepMode(boolean mode);
    void sendCommand(int vendorId, int commandId, in byte[] payload);
    void registerEventListener(IGaiaEventListener listener);
    void unregisterEventListener(IGaiaEventListener listener);
}
