package com.itj.jband;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.csr.gaia.library.Gaia;

public class GaiaDebugActivity extends AppCompatActivity {
    private static final String TAG = GaiaDebugActivity.class.getSimpleName();

    private EditText mVendorId;
    private EditText mCommandId;
    private ScrollView mScrollView;
    private TextView mLogView;
    private Spinner mTypes;

    private Spinner mBools;
    private EditText mInt;
    private EditText mString;

    private GaiaControlManager mGCManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gaia_debug);

        mVendorId = (EditText)findViewById(R.id.debug_edit_vendor_id);
        mVendorId.setSelection(mVendorId.getText().length());
        mCommandId = (EditText)findViewById(R.id.debug_edit_command_id);
        mCommandId.setSelection(mCommandId.getText().length());
        mTypes = (Spinner)findViewById(R.id.debug_spinner_type);
        mTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mBools.setVisibility(View.VISIBLE);
                        mInt.setVisibility(View.GONE);
                        mString.setVisibility(View.GONE);
                        break;
                    case 1:
                        mBools.setVisibility(View.GONE);
                        mInt.setVisibility(View.VISIBLE);
                        mString.setVisibility(View.GONE);
                        break;
                    case 2:
                        mBools.setVisibility(View.GONE);
                        mInt.setVisibility(View.GONE);
                        mString.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mLogView = (TextView)findViewById(R.id.debug_textview_log);
        mScrollView = (ScrollView)findViewById(R.id.debug_scrollview);

        mBools = (Spinner)findViewById(R.id.debug_spinner_bool);
        mInt = (EditText)findViewById(R.id.debug_edit_int);
        mString = (EditText)findViewById(R.id.debug_edit_string);

        Button sendBtn = (Button)findViewById(R.id.debug_button_send);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand();
            }
        });

        Button clearBtn = (Button)findViewById(R.id.debug_button_clear);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogView.setText(null);
            }
        });

        mGCManager = GaiaControlManager.getInstance(this);
        mGCManager.registerEventListener(mGaiaEventListener);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("device")) {
            BluetoothDevice device = intent.getParcelableExtra("device");
            Log.d(TAG, "device = " + device);
            mLogView.setText("start with bluetooth = " + device.getName() + "(" + device.getAddress() + ")\n");
            mGCManager.connect(device);
        }
    }

    private GaiaControlManager.GaiaEventListener mGaiaEventListener = new GaiaControlManager.GaiaEventListener() {
        @Override
        public void onLog(final String message) throws RemoteException {
            Log.d(TAG, "onLog()::message = " + message);
            mLogView.append("onLog()::messate = " + message + "\n");
            mScrollView.fullScroll(View.FOCUS_DOWN);
        }

        @Override
        public void onConnectionStateChanged(final boolean state) throws RemoteException {
            Log.d(TAG, "onConnectionStateChanged()::state = " + state);
            mLogView.append("onConnectionStateChanged()::state = " + state + "\n");
            mScrollView.fullScroll(View.FOCUS_DOWN);
        }
    };

    private void sendCommand() {
        String vendorStr = mVendorId.getText().toString();
        if (vendorStr.startsWith("0x") || vendorStr.startsWith("0X")) {
            vendorStr = vendorStr.substring(2);
        }

        if (!vendorStr.matches("^[a-fA-F0-9]*$")) {
            Log.d(TAG, "failed!!");
            return;
        }

        int vendorId = Integer.parseInt(vendorStr, 16);

        String commandStr = mCommandId.getText().toString();
        if (commandStr.startsWith("0x") || commandStr.startsWith("0X")) {
            commandStr = commandStr.substring(2);
        }

        if (!commandStr.matches("^[a-fA-F0-9]*$")) {
            Log.d(TAG, "failed!!");
            return;
        }

        int commandId = Integer.parseInt(commandStr, 16);

        byte[] payload = null;
        switch (mTypes.getSelectedItemPosition()) {
            case 0: {
                int value = mBools.getSelectedItemPosition() == 0 ? Gaia.FEATURE_ENABLED : Gaia.FEATURE_DISABLED;
                payload = new byte[]{(byte) value};
                break;
            }
            case 1: {
                String str = mInt.getText().toString().trim();
                if (str != null && str.length() > 0) {
                    int value = Integer.valueOf(mInt.getText().toString());
                    payload = new byte[]{(byte) value};
                }
                break;
            }
            case 2: {
                String str = mString.getText().toString().trim();
                if (str != null && str.length() > 0) {
                    payload = str.getBytes();
                }
                break;
            }
        }
        mGCManager.sendCommand(vendorId, commandId, payload);

    }

    @Override
    protected void onDestroy() {
        mGCManager.unregisterEventListener(mGaiaEventListener);
        mGCManager = null;
        super.onDestroy();
    }
}
