package com.itj.jband;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class DeviceManageActivity extends AppCompatActivity {
    private static final String TAG = DeviceManageActivity.class.getSimpleName();

    private ListView mListView;

    private BluetoothAdapter mBTAdapter;
    private ArrayAdapter<BluetoothDevice> mAdapter;

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_CODE_LOCATION = 1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_manage);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        mListView = (ListView)findViewById(R.id.device_list);
        mAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1, new ArrayList<BluetoothDevice>());
        mListView.setAdapter(mAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBtEventReceiver, filter);

        if (mBTAdapter == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
        } else {
            initializeBluetooth();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeBluetooth() {
        if(mBTAdapter.isEnabled()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    startDiscovery();
                }
            });
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeBluetooth();
            } else {
                Log.d(TAG, "failed to grant permission.");
                finish();
            }
        }
    }

    private final BroadcastReceiver mBtEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action = " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getUuids() != null) {
                    for (ParcelUuid id : device.getUuids()) {
                        Log.d(TAG, "device name = " + device + " uuid = " + id.toString());
                    }
                }
                mAdapter.add(device);
            }
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBtEventReceiver);
        super.onDestroy();
    }

    private void startDiscovery() {
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
        }

        final boolean result = mBTAdapter.startDiscovery();
        Log.d(TAG, "result = " + result);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startDiscovery();
            } else {
                Log.d(TAG, "can not enable bt.");
                finish();
            }
        }
    }
}
