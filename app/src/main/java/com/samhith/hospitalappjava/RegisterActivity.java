package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Locale;
import java.util.Random;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import java.util.regex.Pattern;
import android.net.Uri;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etSpecialization, etPhone, etAddress;
    private LinearLayout doctorFields;
    private MaterialButtonToggleGroup roleToggleGroup;
    private String selectedRole = "doctor";
    private DatabaseHelper dbHelper;
    
    private ImageView ivRuleLength, ivRuleUppercase, ivRuleNumber, ivRuleSpecial;
    private TextView tvRuleLength, tvRuleUppercase, tvRuleNumber, tvRuleSpecial;
    private boolean isPasswordValid = false;
    
    private ImageView profileImageView;
    private Uri selectedImageUri;
    private String encodedImageStr = "";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedImageUri = result.getUriContent();
                    if (croppedImageUri != null) {
                        String base64Str = ImageUtils.encodeImageToBase64(this, croppedImageUri);
                        if (base64Str != null) {
                            encodedImageStr = base64Str;
                            profileImageView.setImageURI(croppedImageUri);
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
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etSpecialization = findViewById(R.id.etSpecialization);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        doctorFields = findViewById(R.id.doctorFields);
        roleToggleGroup = findViewById(R.id.roleToggleGroup);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);
        profileImageView = findViewById(R.id.profileImageView);

        ivRuleLength = findViewById(R.id.ivRuleLength);
        ivRuleUppercase = findViewById(R.id.ivRuleUppercase);
        ivRuleNumber = findViewById(R.id.ivRuleNumber);
        ivRuleSpecial = findViewById(R.id.ivRuleSpecial);

        tvRuleLength = findViewById(R.id.tvRuleLength);
        tvRuleUppercase = findViewById(R.id.tvRuleUppercase);
        tvRuleNumber = findViewById(R.id.tvRuleNumber);
        tvRuleSpecial = findViewById(R.id.tvRuleSpecial);
        
        setupPasswordWatcher();

        roleToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnRoleDoctor) {
                    selectedRole = "doctor";
                    doctorFields.setVisibility(View.VISIBLE);
                } else {
                    selectedRole = "receptionist";
                    doctorFields.setVisibility(View.GONE);
                }
            }
        });

        btnRegister.setOnClickListener(v -> handleRegistration());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });
        
        profileImageView.setOnClickListener(v -> openImagePicker());
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

    private void setupPasswordWatcher() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                
                boolean hasLength = password.length() >= 8;
                boolean hasUppercase = Pattern.compile("[A-Z]").matcher(password).find();
                boolean hasNumber = Pattern.compile("[0-9]").matcher(password).find();
                boolean hasSpecial = Pattern.compile("[^a-zA-Z0-9]").matcher(password).find();
                
                updateRuleUI(tvRuleLength, ivRuleLength, hasLength);
                updateRuleUI(tvRuleUppercase, ivRuleUppercase, hasUppercase);
                updateRuleUI(tvRuleNumber, ivRuleNumber, hasNumber);
                updateRuleUI(tvRuleSpecial, ivRuleSpecial, hasSpecial);
                
                isPasswordValid = hasLength && hasUppercase && hasNumber && hasSpecial;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateRuleUI(TextView tv, ImageView iv, boolean isValid) {
        if (isValid) {
            iv.setImageResource(R.drawable.ic_check_circle);
            tv.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            iv.setImageResource(R.drawable.ic_circle_outline);
            tv.setTextColor(Color.GRAY);
        }
    }

    private void handleRegistration() {
        final String name = etName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim().toLowerCase();
        final String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all common fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isPasswordValid) {
            Toast.makeText(this, "Please ensure password meets all requirements", Toast.LENGTH_LONG).show();
            return;
        }

        if (dbHelper.getStaffIdByEmail(email) != -1) {
            Toast.makeText(this, "Email already registered. Please login.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String specialization;
        final String phone;
        final String address;

        if (selectedRole.equals("doctor")) {
            specialization = etSpecialization.getText().toString().trim();
            phone = etPhone.getText().toString().trim();
            address = etAddress.getText().toString().trim();

            if (specialization.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Please fill all doctor-specific fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (phone.length() != 10) {
                Toast.makeText(this, "Mobile number must be exactly 10 digits", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            specialization = "";
            phone = "";
            address = "";
        }

        final String otp = String.format(Locale.getDefault(), "%06d", new Random().nextInt(999999));
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Sending verification code to " + email + "...");
        pd.setCancelable(false);
        pd.show();

        EmailService.sendOtpEmail(email, otp, new EmailService.EmailCallback() {
            @Override
            public void onSuccess() {
                pd.dismiss();
                Toast.makeText(RegisterActivity.this, "Verification code sent!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
                intent.putExtra("NAME", name);
                intent.putExtra("EMAIL", email);
                intent.putExtra("PASSWORD", password);
                intent.putExtra("ROLE", selectedRole);
                intent.putExtra("SPECIALIZATION", specialization);
                intent.putExtra("PHONE", phone);
                intent.putExtra("ADDRESS", address);
                intent.putExtra("OTP", otp);
                intent.putExtra("PHOTO_PATH", encodedImageStr);
                startActivity(intent);
            }

            @Override
            public void onFailure(String error) {
                pd.dismiss();
                Toast.makeText(RegisterActivity.this, "Failed to send email: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
