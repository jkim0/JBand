package com.itj.jband;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AuthenticationActivity extends AppCompatActivity {
    private static final String TAG = AuthenticationActivity.class.getSimpleName();

    private EditText mEditTextPhoneNumber;
    private EditText mEditTextAuthNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mEditTextPhoneNumber = (EditText)findViewById(R.id.editbox_phone_number);
        mEditTextAuthNumber = (EditText)findViewById(R.id.editbox_auth_number);

        Button buttonSend = (Button)findViewById(R.id.button_send_auth_number);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phoneNumber = mEditTextPhoneNumber.getText().toString();
                sendAuthNumber(phoneNumber);
            }
        });

        Button buttonAuth = (Button)findViewById(R.id.button_auth);
        buttonAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runAuthentication();
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendAuthNumber(String phoneNumber) {
        if (phoneNumber == null | phoneNumber.length() == 0) {
            AuthFailDialogFragment fragment = new AuthFailDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString("message", getString(R.string.msg_phone_number_error));
            fragment.setArguments(bundle);
            fragment.show(getSupportFragmentManager(), AuthFailDialogFragment.class.getSimpleName());
        }
    }

    private void runAuthentication() {
        String authNumber = mEditTextAuthNumber.getText().toString();
        if (authNumber == null || authNumber.length() < 6) {
            AuthFailDialogFragment fragment = new AuthFailDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString("message", getString(R.string.msg_auth_fail_error));
            fragment.setArguments(bundle);
            fragment.show(getSupportFragmentManager(), AuthenticationActivity.class.getSimpleName());
            return;
        }
        setResult(RESULT_OK);
        finish();
    }

    public static class AuthFailDialogFragment extends DialogFragment {
        private String mMessage = "";
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_popup_auth_error)
                    .setMessage(getArguments().getString("message"))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dismiss();
                        }
                    });
            return builder.show();
        }
    }
}
