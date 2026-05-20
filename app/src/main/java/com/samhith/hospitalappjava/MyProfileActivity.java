package com.samhith.hospitalappjava;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;

import com.google.android.material.button.MaterialButton;

public class MyProfileActivity extends AppCompatActivity {

    private static final String TAG = "MyProfileActivity";

    private ImageView profileImageView;
    private TextView tvName, tvRole, tvEmail, tvPhone, tvAddress;
    private DatabaseHelper dbHelper;
    private String username;
    private int userId;
    private Staff staff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        profileImageView = findViewById(R.id.profileImageView);
        tvName = findViewById(R.id.tvName);
        tvRole = findViewById(R.id.tvRole);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        MaterialButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnEditProfile = findViewById(R.id.btnEditProfile);

        dbHelper = new DatabaseHelper(this);

        username = getIntent().getStringExtra("USERNAME");
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (username == null) {
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProfileData();

        profileImageView.setOnClickListener(v -> {
            ImagePickerBottomSheet.newInstance(croppedUri -> {
                String base64Str = ImageUtils.encodeImageToBase64(this, croppedUri);
                if (base64Str != null) {
                    profileImageView.setImageURI(croppedUri); // Show local preview
                    updateProfilePhoto(base64Str);
                } else {
                    Toast.makeText(this, "Failed to load cropped image", Toast.LENGTH_SHORT).show();
                }
            }).show(getSupportFragmentManager(), "image_picker");
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditMyProfileActivity.class);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data in case it was edited
        if (username != null) {
            loadProfileData();
        }
    }

    private void loadProfileData() {
        staff = dbHelper.getStaffByEmail(username);
        if (staff != null) {
            tvName.setText(staff.getName());
            tvRole.setText(staff.getRole());
            tvEmail.setText(staff.getEmail());
            tvPhone.setText(staff.getPhone());
            tvAddress.setText(staff.getAddress());

            if (staff.getPhotoPath() != null && !staff.getPhotoPath().isEmpty()) {
                try {
                    String photoPath = staff.getPhotoPath();
                    if (photoPath.startsWith(ImageUtils.BASE64_PREFIX)) {
                        android.graphics.Bitmap bitmap = ImageUtils.decodeBase64ToBitmap(photoPath);
                        if (bitmap != null) {
                            profileImageView.setImageBitmap(bitmap);
                        }
                    } else {
                        profileImageView.setImageURI(Uri.parse(photoPath));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading profile image", e);
                }
            }
        } else {
            Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }



    private void updateProfilePhoto(String newPhotoPath) {
        if (staff != null) {
            staff.setPhotoPath(newPhotoPath);
            boolean success = dbHelper.updateStaff(staff);
            if (success) {
                FirestoreSyncManager.syncStaff(staff);
                Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update photo", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
