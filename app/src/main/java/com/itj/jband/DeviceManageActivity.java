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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class DeviceManageActivity extends AppCompatActivity {
    private static final String TAG = DeviceManageActivity.class.getSimpleName();

    private RecyclerView mListView;

    private BluetoothAdapter mBTAdapter;
    private DeviceAdapter mAdapter;

    private static final int REQUEST_ENABLE_BT = 0;

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

        mListView = (RecyclerView)findViewById(R.id.device_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mListView.setLayoutManager(layoutManager);

        mAdapter = new DeviceAdapter();
        mListView.setAdapter(mAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBtEventReceiver, filter);

        if (mBTAdapter == null) {
            return;
        }
    }

    public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
        private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<>();

        public class DeviceViewHolder extends RecyclerView.ViewHolder {
            public ImageView mIcon;
            public TextView mDeviceName;

            public DeviceViewHolder(View itemView) {
                super(itemView);
                mIcon = (ImageView)itemView.findViewById(R.id.device_icon);
                mDeviceName = (TextView)itemView.findViewById(R.id.device_name);
            }
        }

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.devcie_list_item, parent, false);
            DeviceViewHolder vh = new DeviceViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder holder, int position) {
            final BluetoothDevice device = mDeviceList.get(position);

            Log.d(TAG, "onBindViewHolder position = " + position + " address = " + device.getAddress() + " class = " + device.getBluetoothClass().getDeviceClass());
            holder.mIcon.setImageResource(R.drawable.ic_smartphone_black_24dp);
            holder.mDeviceName.setText(device.getName());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDeviceSelected(device);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onDeviceLongClicked(device);
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDeviceList.size();
        }

        public void addItem(BluetoothDevice device) {
            String address = device.getAddress();

            if (mDeviceList.contains(device)) {
                final int index = mDeviceList.indexOf(device);
                mDeviceList.set(index, device);
                notifyItemChanged(index);
            } else {
                mDeviceList.add(device);
                final int index = mDeviceList.size();
                notifyItemInserted(index);
            }
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
                mAdapter.addItem(device);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        loadConnectedDeviceList();
        startDiscovery();
    }

    private void loadConnectedDeviceList() {
        if (mBTAdapter != null) {
            Set<BluetoothDevice> devices = mBTAdapter.getBondedDevices();
            for (BluetoothDevice devcice : devices) {
                ParcelUuid[] uuids = devcice.getUuids();
                Log.d(TAG, "device = " + devcice.getName());
                for (ParcelUuid uuid : uuids) {
                    Log.d(TAG, "    Supported uuid = " + uuid.toString() + " uuidStr = " + uuid.getUuid());
                }
                mAdapter.addItem(devcice);
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBtEventReceiver);
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
        }

        super.onDestroy();
    }

    private void startDiscovery() {
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
        }

        final boolean result = mBTAdapter.startDiscovery();
        Log.d(TAG, "result = " + result);
    }

    private void onDeviceSelected(BluetoothDevice device) {
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
        }
        Intent data = new Intent();
        data.putExtra("device", device);
        setResult(RESULT_OK, data);
        finish();
    }

    private void onDeviceLongClicked(BluetoothDevice device) {
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
        }
        Intent intent = new Intent(this, GaiaDebugActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("device", device);
        startActivity(intent);
    }
}
