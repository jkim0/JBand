package com.itj.jband;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import java.io.File;

public class UserManagementActivity extends AppCompatActivity {
    private static final String TAG = UserManagementActivity.class.getSimpleName();

    private ImageButton mPhoto;
    private EditText mName;
    private EditText mPhoneNumber;
    private RadioGroup mGenderGroup;
    private EditText mHeight;
    private EditText mWeight;

    private static final int REQUEST_IMAGE_FROM_ALBUM = 1;
    private static final int REQUEST_IMGEE_FROM_CAMERA = 2;
    private static final int REQUEST_CROP_FROM_CAMERA = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_management);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPhoto = (ImageButton)findViewById(R.id.image_button_photo);
        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPhotoSelectDialog();
            }
        });

        mName = (EditText)findViewById(R.id.edit_text_name);
        mPhoneNumber = (EditText)findViewById(R.id.edit_text_phone_number);
        mGenderGroup = (RadioGroup)findViewById(R.id.radio_group_gender);
        mHeight = (EditText)findViewById(R.id.edit_height);
        mWeight = (EditText)findViewById(R.id.edit_weight);
    }

    private void showPhotoSelectDialog() {
        PhotoSelectorDialogFragment fragment = new PhotoSelectorDialogFragment();
        fragment.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        importPhotoFromGallery();
                        break;
                    case 1:
                        importPhotoFromCamera();
                        break;
                }
            }
        });
        fragment.show(getSupportFragmentManager(), PhotoSelectorDialogFragment.class.getSimpleName());
    }

    public static class PhotoSelectorDialogFragment extends DialogFragment {
        private DialogInterface.OnClickListener mOnClickListener;

        public void setOnClickListener(DialogInterface.OnClickListener listener) {
            mOnClickListener = listener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_photo_selector_popup)
                    .setItems(R.array.select_photo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mOnClickListener.onClick(dialog, which);
                        }
                    });
            return builder.show();
        }
    }

    private void importPhotoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        intent.putExtra("crop", true);
        startActivityForResult(intent, REQUEST_IMAGE_FROM_ALBUM);
    }

    private void importPhotoFromCamera() {
        String filename = "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
        Uri imageCaptureUri;
        File file = new File(getAlbumStorageDir(getPackageName()), filename);
        imageCaptureUri = Uri.fromFile(file);
        mImageUri = imageCaptureUri;

        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageCaptureUri);
        startActivityForResult(intent, REQUEST_IMGEE_FROM_CAMERA);
    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the app's private pictures directory.
        File file = new File(getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    private void deletePhoto() {
        mPhoto.setImageDrawable(null);
    }

    private Uri mImageUri = null;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMGEE_FROM_CAMERA || requestCode == REQUEST_IMAGE_FROM_ALBUM) {
            if (resultCode == RESULT_OK) {
                if (requestCode == REQUEST_IMAGE_FROM_ALBUM) {
                    mImageUri = data.getData();
                }

                File file = new File(getAlbumStorageDir(getPackageName()), "temp_photo.jpg");
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(mImageUri, "image/*");
                intent.putExtra("outputX", 200);
                intent.putExtra("outputY", 200);
                intent.putExtra("aspectX", 1);
                intent.putExtra("aspectY", 1);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                intent.putExtra("scale", true);
                intent.putExtra("return-data", true);
                startActivityForResult(intent, REQUEST_CROP_FROM_CAMERA);
            }
        } else if (requestCode == REQUEST_CROP_FROM_CAMERA) {
            if (resultCode == RESULT_OK) {
                File file = new File(getAlbumStorageDir(getPackageName()), "temp_photo.jpg");
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                BitmapDrawable d = new BitmapDrawable(getResources(), bitmap);
                mPhoto.setImageDrawable(d);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeValues();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initializeValues() {
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        final String name = sp.getString("name", "");
        mName.setText(name);

        final String phoneNumber = sp.getString("phone_number", "");
        mPhoneNumber.setText(phoneNumber);

        final int gender = sp.getInt("gender", 0);
        mGenderGroup.check(gender == 0 ? R.id.radio_button_male : R.id.radio_button_female);

        final float height = sp.getFloat("height", (float)0.0);
        mHeight.setText(String.valueOf(height));

        final float weight = sp.getFloat("weight", (float)0.0);
        mWeight.setText(String.valueOf(weight));

        String photoPath = sp.getString("user_photo_file", null);
        if (!TextUtils.isEmpty(photoPath)) {
            File img = new File(photoPath);
            Bitmap bitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
            BitmapDrawable d = new BitmapDrawable(getResources(), bitmap);
            mPhoto.setImageDrawable(d);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user_info_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            saveUserInfo();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveUserInfo() {
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        final String name = mName.getText().toString();
        editor.putString("name", name);

        final String phoneNumber = mPhoneNumber.getText().toString();
        editor.putString("phone_number", phoneNumber);

        final int gender = mGenderGroup.getCheckedRadioButtonId() == R.id.radio_button_male ? 0 : 1;
        editor.putInt("gender", gender);

        final float height = Float.valueOf(mHeight.getText().toString());
        editor.putFloat("height", height);

        final float weight = Float.valueOf(mWeight.getText().toString());
        editor.putFloat("weight", weight);

        File file = new File(getAlbumStorageDir(getPackageName()), "temp_photo.jpg");
        File destFile = new File(getAlbumStorageDir(getPackageName()), "user_photo.jpg");
        file.renameTo(destFile);

        editor.putString("user_photo_file", destFile.getAbsolutePath());

        editor.commit();
        finish();
    }
}
