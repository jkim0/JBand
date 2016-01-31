package com.itj.jband;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ListView mListView;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private DrawerLayout mDrawerLayout;
    private LinearLayout mPageMark;
    private int mPrevPosition;

    private static final int VIEW_COUNT = 3;

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_CODE_LOCATION = 1;
    private BluetoothAdapter mBTAdapter;
    private Handler mHandler = new Handler();

    // for test
    private static int mSteps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mPageMark = (LinearLayout)findViewById(R.id.page_mark);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mPageMark.getChildAt(mPrevPosition).setBackgroundResource(R.drawable.page_not);    //이전 페이지에 해당하는 페이지 표시 이미지 변경
                mPageMark.getChildAt(position).setBackgroundResource(R.drawable.page_select);        //현재 페이지에 해당하는 페이지 표시 이미지 변경
                mPrevPosition = position;
            }
        });
        mViewPager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClicked view = " + view);
                Intent intent = new Intent(MainActivity.this, ActivityDetailActivity.class);
                startActivity(intent);
            }
        });

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                Intent targetIntent = null;
                if (id == R.id.nav_schedume_managements) {
                    targetIntent = new Intent(MainActivity.this, ScheduleManagementActivity.class);
                } else if (id == R.id.nav_user_managements) {
                    targetIntent = new Intent(MainActivity.this, UserManagementActivity.class);
                } else if (id == R.id.nav_check_activities) {
                } else if (id == R.id.nav_sleep_managements) {
                } else if (id == R.id.nav_usage_statistics) {
                    targetIntent = new Intent(MainActivity.this, UsageStatisticActivity.class);
                } else if (id == R.id.nav_device_manage) {
                    targetIntent = new Intent(MainActivity.this, DeviceManageActivity.class);
                }
                if (targetIntent != null) {
                    startActivity(targetIntent);
                }

                mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        initPageMark();

        mListView = (ListView)findViewById(R.id.event_list);

        for (String item : getResources().getStringArray(R.array.main_list_items)) {
            Log.d(TAG, "item = " + item);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.main_list_items));
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // TODO
                // enter detail screen
            }
        });

        Intent serviceIntent = new Intent(this, SensorService.class);
        serviceIntent.setAction(SensorService.ACTION_START_SENSOR_MONITORING);
        startService(serviceIntent);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
        } else {
            initializeBluetooth();
        }*/
    }

    private void initializeBluetooth() {
        if(mBTAdapter.isEnabled()) {
            Intent serviceIntent = new Intent(this, GaiaControlService.class);
            startService(serviceIntent);
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
                Toast.makeText(this, "failed to grant permission.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Intent serviceIntent = new Intent(this, GaiaControlService.class);
                startService(serviceIntent);
            } else {
                Log.d(TAG, "failed to enable bt.");
                Toast.makeText(this, "failed to enable bt.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initPageMark(){
        for(int i=0; i<VIEW_COUNT; i++)
        {
            ImageView iv = new ImageView(getApplicationContext());	//페이지 표시 이미지 뷰 생성
            iv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            //첫 페이지 표시 이미지 이면 선택된 이미지로
            if(i==0)
                iv.setBackgroundResource(R.drawable.page_select);
            else	//나머지는 선택안된 이미지로
                iv.setBackgroundResource(R.drawable.page_not);

            //LinearLayout에 추가
            mPageMark.addView(iv);
        }
        mPrevPosition = 0;	//이전 포지션 값 초기화
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private ISenserService mService;
        private int mId = -1;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            TextView subject = (TextView) rootView.findViewById(R.id.subject);
            TextView measureValue = (TextView) rootView.findViewById(R.id.measured_value);
            TextView additionalInfo = (TextView) rootView.findViewById(R.id.additional_info);

            Intent serviceIntent = new Intent(getActivity(), SensorService.class);
            serviceIntent.setAction(SensorService.ACTION_START_SENSOR_MONITORING);
            getActivity().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

            mId = getArguments().getInt(ARG_SECTION_NUMBER, -1);
            if (mId == 1) {
                subject.setText(getString(R.string.section_title_steps));
                measureValue.setText("" + mSteps);
                additionalInfo.setText("0m/0cal");
            } else if (mId == 2) {
                subject.setText(getString(R.string.section_title_sleeps));
                measureValue.setText("step count = 0");
                additionalInfo.setText("0h00m Deep Sleep");
            } else {
                subject.setText("");
                subject.setVisibility(View.INVISIBLE);
            }

            rootView.setClickable(true);
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClicked view = " + view);
                    Intent intent = new Intent(getActivity(), ActivityDetailActivity.class);
                    intent.putExtra("type", mId);
                    startActivity(intent);
                }
            });

            return rootView;
        }

        @Override
        public void onDestroyView() {
            if (mService != null) {
                try {
                    mService.unregisterSensorEventListener(mListener);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            getActivity().unbindService(mServiceConnection);
            super.onDestroyView();
        }

        private ServiceConnection mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mService = ISenserService.Stub.asInterface(iBinder);

                try {
                    mService.registerSensorEventListener(mListener);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mService = null;
            }
        };

        private ISensorEventListener mListener = new ISensorEventListener.Stub() {
            @Override
            public void onStepDetected() throws RemoteException {
                if (mId != 1 || getView() == null) return;
                mSteps++;
                TextView measureValue = (TextView) getView().findViewById(R.id.measured_value);
                TextView additionalInfo = (TextView) getView().findViewById(R.id.additional_info);
                measureValue.setText("" + mSteps);
                additionalInfo.setText("0m/0cal");
            }

            @Override
            public void onStepCountReceived(int count) throws RemoteException {
                if (mId != 2 || getView() == null) return;
                TextView measureValue = (TextView) getView().findViewById(R.id.measured_value);
                TextView additionalInfo = (TextView) getView().findViewById(R.id.additional_info);
                measureValue.setText("step count = " + count);
                additionalInfo.setText("0h00m Deep Sleep");
            }

            @Override
            public void onAccelerometerDataReceived(float x, float y, float z) throws RemoteException {
                if (mId != 3 || getView() == null) return;
                TextView measureValue = (TextView) getView().findViewById(R.id.measured_value);
                TextView additionalInfo = (TextView) getView().findViewById(R.id.additional_info);
                measureValue.setText("axis_x = " + x + " axis_y = " + y + " axis_z = " + z);
                additionalInfo.setText("BMI 0 Average");
            }
        };
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.page_title_steps);
                case 1:
                    return getString(R.string.page_title_sleeps);
                case 2:
                    return getString(R.string.page_title_weights);
            }
            return null;
        }
    }
}
