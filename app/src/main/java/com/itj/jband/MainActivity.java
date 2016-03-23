package com.itj.jband;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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

import com.itj.jband.schedule.Schedule;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ListView mListView;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private DrawerLayout mDrawerLayout;
    private LinearLayout mPageMark;
    private TextView mUserName;
    private TextView mUserInfo;
    private ImageView mUserPhoto;
    private int mPrevPosition;

    private static final int VIEW_COUNT = 3;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;
    private static final int REQUSET_DEVICE_SELECT = 3;

    private BluetoothAdapter mBTAdapter;
    private Handler mHandler = new Handler();

    private GaiaControlManager mGaiaControlManager = null;

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
                    targetIntent = new Intent(MainActivity.this, ActivityCheckActivity.class);
                } else if (id == R.id.nav_sleep_managements) {
                    targetIntent = new Intent(MainActivity.this, SleepManagementActivity.class);
                } else if (id == R.id.nav_usage_statistics) {
                    targetIntent = new Intent(MainActivity.this, UsageStatisticActivity.class);
                } else if (id == R.id.nav_device_manage) {
                    targetIntent = new Intent(MainActivity.this, DeviceManageActivity.class);
                }

                if (targetIntent != null) {
                    if (id == R.id.nav_device_manage) {
                        startActivityForResult(targetIntent, REQUSET_DEVICE_SELECT);
                    } else {
                        startActivity(targetIntent);
                    }
                }

                mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        View headerView = navigationView.getHeaderView(0);
        mUserName = (TextView)headerView.findViewById(R.id.user_name);
        mUserInfo = (TextView)headerView.findViewById(R.id.user_info);
        mUserPhoto = (ImageView)headerView.findViewById(R.id.user_photo);

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
                if (i == 0) {
                    Intent targetIntent = new Intent(MainActivity.this, SleepManagementActivity.class);
                    startActivity(targetIntent);
                } else if (i == 1) {
                    Intent targetIntent = new Intent(MainActivity.this, ActivityCheckActivity.class);
                    startActivity(targetIntent);
                } else if (i == 2) {
                    final boolean inSleep = Utils.getSavedSleepMode(MainActivity.this);
                    mGaiaControlManager.setSleepMode(!inSleep);
                }
            }
        });

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        ArrayList<String> missingPermissions = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.RECEIVE_SMS);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            initializeBluetooth();
        }

        if (missingPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[missingPermissions.size()]), REQUEST_CODE_PERMISSION);
        }
    }

    private void initializeBluetooth() {
        if(mBTAdapter.isEnabled()) {
            startGaiaControlService();
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
        if (requestCode == REQUEST_CODE_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                Log.d(TAG, "permission = " + permissions[i] + " grantResult = " + (grantResults[i] == PackageManager.PERMISSION_GRANTED));
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        initializeBluetooth();
                    } else {
                        Log.d(TAG, "failed to grant bluetooth permission.");
                        Toast.makeText(this, "failed to grant bluetooth permission.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                initializeBluetooth();
            } else {
                Log.d(TAG, "failed to enable bt.");
                Toast.makeText(this, "failed to enable bt.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUSET_DEVICE_SELECT) {
            if (resultCode == RESULT_OK) {
                BluetoothDevice device = data.getParcelableExtra("device");
                mGaiaControlManager.connect(device);
            }
        }
    }

    private void startGaiaControlService() {
        Log.d(TAG, "startGaiaControlService");
        Intent serviceIntent = new Intent(this, GaiaControlService.class);
        serviceIntent.setAction(GaiaControlService.ACTION_START_GAIA_SERVICE);
        startService(serviceIntent);

        mGaiaControlManager = GaiaControlManager.getInstance(this);
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
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        String name = sp.getString("name", null);
        String phoneNumber = sp.getString("phone_number", null);
        if (!TextUtils.isEmpty(phoneNumber)) {
            name += "(" + phoneNumber + ")";
        }
        final int gender = sp.getInt("gender", 0);
        String strGender = getString(R.string.label_radio_button_male);
        if (gender == 1) {
            strGender = getString(R.string.label_radio_button_female);
        }

        final float height = sp.getFloat("height", (float) 0.0);
        String strHeight = String.valueOf(height);

        final float weight = sp.getFloat("weight", (float) 0.0);
        String strWeight = String.valueOf(weight);

        String photoPath = sp.getString("user_photo_file", null);
        if (!TextUtils.isEmpty(photoPath)) {
            File img = new File(photoPath);
            Bitmap bitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
            BitmapDrawable d = new BitmapDrawable(getResources(), bitmap);
            mUserPhoto.setImageDrawable(d);
        }

        Log.d(TAG, "onResume name = " + name + " mUserName = " + mUserName);
        mUserName.setText(name);
        mUserInfo.setText(strGender + ", " + strHeight + "cm, " + strWeight + "kg");
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

            return rootView;
        }
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
