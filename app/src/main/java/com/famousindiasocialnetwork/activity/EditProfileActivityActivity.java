package com.famousindiasocialnetwork.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.request.UserUpdateRequest;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.network.response.ProfileResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.FirebaseUploader;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.google.firebase.iid.FirebaseInstanceId;
import com.kbeanie.multipicker.api.CameraImagePicker;
import com.kbeanie.multipicker.api.ImagePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenImage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivityActivity extends AppCompatActivity implements ImagePickerCallback {
    private static String DATA_EXTRA_USER = "UserProfile";
    private static String DATA_EXTRA_FORCE_EDIT = "UserProfileForceEdit";
    private final int REQUEST_CODE_PERMISSION = 55;

    private ImageView userImage;
    private EditText userName;
    private ProgressBar progress;
    private Button done;

    ProfileResponse profileMe;
    private DrService drService;
    private SharedPreferenceUtil sharedPreferenceUtil;

    private String pickerPath;
    private ImagePicker imagePicker;
    private CameraImagePicker cameraPicker;
    private File mediaFile;
    private TextView tvTitle;
    private boolean updated, foreceEdit;
    private boolean updateInProgress;
    List<String> list;
    ArrayAdapter<String> SpinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile_activity);
        drService = ApiUtils.getClient().create(DrService.class);
        sharedPreferenceUtil = new SharedPreferenceUtil(this);
        initUi();
        profileMe = getIntent().getParcelableExtra(DATA_EXTRA_USER);
        foreceEdit = getIntent().getBooleanExtra(DATA_EXTRA_FORCE_EDIT, false);
        if (foreceEdit) {
            Toast.makeText(this, "You need to provide some basic information", Toast.LENGTH_LONG).show();
        }
        setDetails();
        addGender();
    }

    private void addGender() {
        list = new ArrayList<String>(); // List of Items
        list.add("Male");
        list.add("Female");
        list.add("Other");
        SpinnerAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, list) {
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.parseColor("#E30D81"));
                return v;
            }

            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View v = super.getDropDownView(position, convertView,
                        parent);
                v.setBackgroundColor(Color.parseColor("#ffffff"));
                ((TextView) v).setTextColor(Color.parseColor("#000000"));
                return v;
            }
        };
        SpinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        // Set Adapter in the spinner
        ((Spinner) findViewById(R.id.spinner)).setAdapter(SpinnerAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (requestCode == Picker.PICK_IMAGE_DEVICE) {
                if (imagePicker == null) {
                    imagePicker = new ImagePicker(this);
                    imagePicker.setImagePickerCallback(this);
                }
                imagePicker.submit(data);
            } else if (requestCode == Picker.PICK_IMAGE_CAMERA) {
                if (cameraPicker == null) {
                    cameraPicker = new CameraImagePicker(this);
                    cameraPicker.setImagePickerCallback(this);
                    cameraPicker.reinitialize(pickerPath);
                }
                cameraPicker.submit(data);
            }
        }
    }

    private void setDetails() {
        userName.setText(profileMe.getName());
        Glide.with(this)
                .load(profileMe.getImage())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(this, 8))))
                .into(userImage);
    }


    private void initUi() {
        findViewById(R.id.ll_top).setVisibility(View.GONE);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_keyboard_arrow_left);
            toolbar.setTitleTextAppearance(this, R.style.MontserratBoldTextAppearance);
            actionBar.setTitle("Edit profile");
        }

        tvTitle = findViewById(R.id.tv_title);
        tvTitle.setTypeface(Helper.getMontserratBold(this));
        tvTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        userImage = findViewById(R.id.userImage);
        userName = findViewById(R.id.fullName);
        progress = findViewById(R.id.progress);
        ImageView pickImage = findViewById(R.id.pickImage);
        pickImage.bringToFront();
        pickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkStoragePermissions()) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(EditProfileActivityActivity.this);
                    alertDialog.setMessage("Get icon_picture from");
                    alertDialog.setPositiveButton("Camera", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                            cameraPicker = new CameraImagePicker(EditProfileActivityActivity.this);
                            cameraPicker.shouldGenerateMetadata(true);
                            cameraPicker.shouldGenerateThumbnails(true);
                            cameraPicker.setImagePickerCallback(EditProfileActivityActivity.this);
                            pickerPath = cameraPicker.pickImage();
                        }
                    });
                    alertDialog.setNegativeButton("Gallery", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                            imagePicker = new ImagePicker(EditProfileActivityActivity.this);
                            imagePicker.shouldGenerateMetadata(true);
                            imagePicker.shouldGenerateThumbnails(true);
                            imagePicker.setImagePickerCallback(EditProfileActivityActivity.this);
                            imagePicker.pickImage();
                        }
                    });
                    alertDialog.create().show();
                } else {
                    ActivityCompat.requestPermissions(EditProfileActivityActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION);
                }
            }
        });

        done = findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(userName.getText())) {
                    Toast.makeText(EditProfileActivityActivity.this, "Name cant be left empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateProfile();
            }
        });
    }

    private boolean checkStoragePermissions() {
        return
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateProfile() {
        setUpdateProgress(true);
        drService.createUpdateUser(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null),
                new UserUpdateRequest(profileMe != null ? profileMe.getGender() : String.valueOf("m"),
                        userName.getText().toString(),
                        profileMe.getImage(),
                        FirebaseInstanceId.getInstance().getToken(),
                        profileMe.getNotification_on_like(),
                        profileMe.getNotification_on_dislike(),
                        profileMe.getNotification_on_comment()

                ), 1).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setUpdateProgress(false);
                if (response.isSuccessful()) {
                    profileMe.setGender(response.body().getGender());
                    profileMe.setName(response.body().getName());
                    profileMe.setImage(response.body().getImage());
                    Helper.saveProfileMe(sharedPreferenceUtil, profileMe);
                    Toast.makeText(EditProfileActivityActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    sharedPreferenceUtil.setBooleanPreference(Constants.KEY_UPDATED, true);
                    updated = true;
                    onBackPressed();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setUpdateProgress(false);
                Toast.makeText(EditProfileActivityActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                t.getMessage();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (updateInProgress) {
            Toast.makeText(this, "Processing, Please wait!", Toast.LENGTH_LONG).show();
        } else {
            if (foreceEdit) {
                if (updated)
                    super.onBackPressed();
                else
                    Toast.makeText(this, "You need to provide some basic information", Toast.LENGTH_LONG).show();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onImagesChosen(List<ChosenImage> images) {
        setUpdateProgress(true);
        mediaFile = new File(Uri.parse(images.get(0).getOriginalPath()).getPath());
        Glide.with(this)
                .load(mediaFile)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(this, 8))).placeholder(R.drawable.placeholder))
                .into(userImage);

        FirebaseUploader firebaseUploader = new FirebaseUploader(new FirebaseUploader.UploadListener() {
            @Override
            public void onUploadFail(String message) {
                setUpdateProgress(false);
                Toast.makeText(EditProfileActivityActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUploadSuccess(String downloadUrl) {
                drService.createUpdateUser(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null),
                        new UserUpdateRequest(profileMe != null ? profileMe.getGender() : String.valueOf("m"),
                                profileMe != null ? profileMe.getName() : userName.getText().toString(),
                                downloadUrl,
                                FirebaseInstanceId.getInstance().getToken(),
                                profileMe.getNotification_on_like(),
                                profileMe.getNotification_on_dislike(),
                                profileMe.getNotification_on_comment()), 1).enqueue(new Callback<UserResponse>() {
                    @Override
                    public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                        setUpdateProgress(false);
                        if (response.isSuccessful()) {
                            profileMe.setGender(response.body().getGender());
                            profileMe.setName(response.body().getName());
                            profileMe.setImage(response.body().getImage());
                            Helper.saveProfileMe(sharedPreferenceUtil, profileMe);
                            Toast.makeText(EditProfileActivityActivity.this, "Image Updated", Toast.LENGTH_SHORT).show();
                            sharedPreferenceUtil.setBooleanPreference(Constants.KEY_UPDATED, true);
                        } else {
                            Toast.makeText(EditProfileActivityActivity.this, "Something went wrong while updating icon_picture", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserResponse> call, Throwable t) {
                        setUpdateProgress(false);
                        t.getMessage();
                        Toast.makeText(EditProfileActivityActivity.this, "Something went wrong while updating icon_picture", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onUploadProgress(int progress) {

            }

            @Override
            public void onUploadCancelled() {

            }
        });
        firebaseUploader.setReplace(true);
        firebaseUploader.uploadImage(this, mediaFile);
    }

    private void setUpdateProgress(boolean inProgress) {
        updateInProgress = inProgress;
        progress.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);
        done.setBackground(ContextCompat.getDrawable(this, inProgress ? R.drawable.rounded_bg_color_gray : R.drawable.rounded_bg_color_primary));
        done.setClickable(!inProgress);
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // You have to save path in case your activity is killed.
        // In such a scenario, you will need to re-initialize the CameraImagePicker
        outState.putString("picker_path", pickerPath);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // After Activity recreate, you need to re-intialize these
        // two values to be able to re-intialize CameraImagePicker
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("picker_path")) {
                pickerPath = savedInstanceState.getString("picker_path");
            }
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    public static Intent newInstance(Context context, ProfileResponse userMe, boolean forceEdit) {
        Intent intent = new Intent(context, EditProfileActivityActivity.class);
        intent.putExtra(DATA_EXTRA_USER, userMe);
        intent.putExtra(DATA_EXTRA_FORCE_EDIT, forceEdit);
        return intent;
    }
}
