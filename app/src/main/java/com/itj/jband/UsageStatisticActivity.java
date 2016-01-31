package com.itj.jband;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.zip.Inflater;

public class UsageStatisticActivity extends AppCompatActivity {
    private static final String TAG = UsageStatisticActivity.class.getSimpleName();

    private ViewPager mViewPager;
    private TextView mMonthlyStatistics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_statistic);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Calendar calendar = Calendar.getInstance();

        mMonthlyStatistics = (TextView)findViewById(R.id.textview_monthly_statistics);
        mMonthlyStatistics.setText(String.format(getString(R.string.label_text_monthly_statistics), calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1));

        Button buttonIntialize = (Button)findViewById(R.id.button_initialize);
        buttonIntialize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runInitialize();
            }
        });

        ImageButton buttonLeft = (ImageButton)findViewById(R.id.image_button_left);
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.arrowScroll(View.FOCUS_LEFT);
            }
        });
        ImageButton buttonRight = (ImageButton)findViewById(R.id.image_button_right);
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.arrowScroll(View.FOCUS_RIGHT);
            }
        });

        ViewPager pager = (ViewPager)findViewById(R.id.container);
        StatisticsAdapter adapter = new StatisticsAdapter(this);
        pager.setAdapter(adapter);
        pager.setCurrentItem(adapter.getCount() - 1);
        mViewPager = pager;
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

    private void runInitialize() {
        InitializeDialogFragment fragment = new InitializeDialogFragment();
        fragment.setOnPositiveClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                doInitialize();
            }
        });
        fragment.show(getSupportFragmentManager(), InitializeDialogFragment.class.getSimpleName());
    }

    public void doInitialize() {
        // TODO
        // implement initializing code.
    }

    public static class InitializeDialogFragment extends DialogFragment {
        private DialogInterface.OnClickListener mPositiveClickListener;
        private DialogInterface.OnClickListener mNegativeClickListener;

        public void setOnPositiveClickListener(DialogInterface.OnClickListener listener) {
            mPositiveClickListener = listener;
        }

        public void setOnNegativeClickListener(DialogInterface.OnClickListener listener) {
            mNegativeClickListener = listener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_popup_usage_statistics)
                    .setMessage(R.string.msg_popup_usage_statistics)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mPositiveClickListener.onClick(dialogInterface, i);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null);
            return builder.show();
        }
    }

    private class StatisticsAdapter extends PagerAdapter {
        private Context mContext;
        private LayoutInflater mInflater;

        public StatisticsAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LinearLayout item = (LinearLayout)mInflater.inflate(R.layout.total_usage_layout, null);

            TextView hour = (TextView)item.findViewById(R.id.text_view_hour);
            TextView min = (TextView)item.findViewById(R.id.text_view_min);

            hour.setText(String.format(getString(R.string.string_format_hour), position));
            ((ViewPager) container).addView(item);
            return item;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager)container).removeView((View)object);
        }
    }
}
