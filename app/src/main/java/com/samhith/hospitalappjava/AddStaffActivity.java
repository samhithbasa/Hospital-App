// AddStaffActivity.java
package com.samhith.hospitalappjava;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class AddStaffActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1001;

    private EditText nameEditText, roleEditText, departmentEditText,
            emailEditText, phoneEditText, joinDateEditText, addressEditText, specializationEditText;
    private ImageView profileImageView;
    private Uri selectedImageUri;
    private String encodedImageStr = "";
    private Button saveButton;
    private DatabaseHelper dbHelper;
    private int userId;

    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedImageUri = result.getUriContent();
                    if (croppedImageUri != null) {
                        String base64Str = ImageUtils.encodeImageToBase64(this, croppedImageUri);
                        if (base64Str != null) {
                            encodedImageStr = base64Str;
                            profileImageView.setImageURI(croppedImageUri); // Show local preview
                        } else {
                            Toast.makeText(this, "Failed to load cropped image", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Exception error = result.getError();
                    if (error != null) {
                        Toast.makeText(this, "Crop failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void launchImageCropper(Uri imageUri) {
        CropImageOptions cropImageOptions = new CropImageOptions();
        cropImageOptions.guidelines = CropImageView.Guidelines.ON;
        cropImageOptions.fixAspectRatio = true;
        cropImageOptions.aspectRatioX = 1;
        cropImageOptions.aspectRatioY = 1;
        
        CropImageContractOptions cropOptions = new CropImageContractOptions(imageUri, cropImageOptions);
        cropImageLauncher.launch(cropOptions);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        dbHelper = new DatabaseHelper(this);
        userId = getIntent().getIntExtra("USER_ID", -1);

        nameEditText = findViewById(R.id.nameEditText);
        roleEditText = findViewById(R.id.roleEditText);
        departmentEditText = findViewById(R.id.departmentEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        joinDateEditText = findViewById(R.id.joinDateEditText);
        addressEditText = findViewById(R.id.addressEditText);
        specializationEditText = findViewById(R.id.specializationEditText);
        profileImageView = findViewById(R.id.profileImageView);
        saveButton = findViewById(R.id.saveButton);

        phoneEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        setCurrentJoinDate();

        joinDateEditText.setFocusable(false);
        joinDateEditText.setOnClickListener(v -> showDatePicker());

        profileImageView.setOnClickListener(v -> openImagePicker());
        saveButton.setOnClickListener(v -> saveStaff());
    }

    private void setCurrentJoinDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        joinDateEditText.setText(dateFormat.format(new Date()));
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Join Date")
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String formattedDate = sdf.format(new Date(selection));
            joinDateEditText.setText(formattedDate);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri tempUri = data.getData();
            if (tempUri != null) {
                launchImageCropper(tempUri);
            }
        }
    }

    private void saveStaff() {
        String name = nameEditText.getText().toString().trim();
        String role = roleEditText.getText().toString().trim();
        String department = departmentEditText.getText().toString().trim();
        String emailInput = emailEditText.getText().toString().trim();
        String email = emailInput.toLowerCase();
        String phone = phoneEditText.getText().toString().trim();
        String joinDate = joinDateEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String specialization = specializationEditText.getText().toString().trim();
        String photoPath = encodedImageStr.isEmpty() ? null : encodedImageStr;

        if (name.isEmpty() || role.isEmpty() || department.isEmpty() ||
                email.isEmpty() || phone.isEmpty() || joinDate.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (role.equalsIgnoreCase("doctor") && specialization.isEmpty()) {
            Toast.makeText(this, "Specialization is required for doctors", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Pattern.matches("^[0-9]{10}$", phone)) {
            Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        long result = dbHelper.addStaff(name, role, department, email, phone, joinDate, address, specialization, photoPath, userId);

        if (result != -1) {
            // Create Staff object for syncing
            Staff staff = new Staff();
            staff.setId((int) result);
            staff.setName(name);
            staff.setRole(role);
            staff.setDepartment(department);
            staff.setEmail(email);
            staff.setPhone(phone);
            staff.setJoinDate(joinDate);
            staff.setAddress(address);
            staff.setSpecialization(specialization);
            staff.setPhotoPath(photoPath);
            
            // Sync to Firestore
            FirestoreSyncManager.syncStaff(staff);

            Toast.makeText(this, "Staff member added successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error adding staff member", Toast.LENGTH_SHORT).show();
        }
    }
}