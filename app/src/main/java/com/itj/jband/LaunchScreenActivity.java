package com.itj.jband;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class LaunchScreenActivity extends AppCompatActivity {
    private static final String TAG = LaunchScreenActivity.class.getSimpleName();

    private static final int LAUNCH_MAIN_ACTIVITY_DELAY = 1000;

    private static final int REQUEST_AUTHENTICATION = 1;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_screen);

        checkNetworkStatus();
    }

    private void checkNetworkStatus() {
        checkUpgrade();
    }

    private void checkUpgrade() {
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        final boolean released = sp.getBoolean("new_version_released", true);
        if (released) {
            showUpgradeDialog();
        } else {
            lunchNextStep();
        }
    }

    private void showUpgradeDialog() {
        UpgradeDialogFragment fragment = new UpgradeDialogFragment();
        fragment.setOnPositiveClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                lunchUpgrade();
            }
        });
        fragment.setOnNegativeClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                lunchNextStep();
            }
        });
        fragment.show(getSupportFragmentManager(), UpgradeDialogFragment.class.getSimpleName());
    }

    private void lunchUpgrade() {
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("new_version_released", false);
        editor.commit();
        lunchNextStep();
    }

    private void lunchNextStep() {
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        final boolean authenticated = sp.getBoolean("authenticated", false);
        if (authenticated) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent nextIntent = new Intent(LaunchScreenActivity.this, MainActivity.class);
                    startActivity(nextIntent);
                    finish();
                }
            }, LAUNCH_MAIN_ACTIVITY_DELAY);
        } else {
            Intent nextIntent = new Intent(LaunchScreenActivity.this, ServiceAgreementActivity.class);
            startActivityForResult(nextIntent, REQUEST_AUTHENTICATION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_AUTHENTICATION) {
            if (resultCode == RESULT_OK) {
                SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean("authenticated", true);
                editor.commit();
                lunchNextStep();
            }
        }

        Log.d(TAG, "reqeust Code = " + requestCode + " result = " + resultCode);
    }

    public static class ButtonDialogFragment extends DialogFragment {
        protected DialogInterface.OnClickListener mPositiveClickListener;
        protected DialogInterface.OnClickListener mNegativeClickListener;

        public void setOnPositiveClickListener(DialogInterface.OnClickListener listener) {
            mPositiveClickListener = listener;
        }

        public void setOnNegativeClickListener(DialogInterface.OnClickListener listener) {
            mNegativeClickListener = listener;
        }
    }

    public static class NetworkErrorDialogFragment extends ButtonDialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_guide_popup);
            builder.setMessage(R.string.msg_network_status_error);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mPositiveClickListener.onClick(dialogInterface, i);
                }
            });
            return builder.show();
        }
    }

    public static class UpgradeDialogFragment extends ButtonDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_upgrade_popup)
                    .setMessage(R.string.msg_upgrade_popup)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mPositiveClickListener.onClick(dialogInterface, i);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mNegativeClickListener.onClick(dialogInterface, i);
                        }
                    });
            return builder.show();
        }
    }
}
