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

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etSpecialization, etPhone, etAddress;
    private LinearLayout doctorFields;
    private MaterialButtonToggleGroup roleToggleGroup;
    private String selectedRole = "doctor";
    private DatabaseHelper dbHelper;

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
    }

    private void handleRegistration() {
        final String name = etName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim().toLowerCase();
        final String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all common fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
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
