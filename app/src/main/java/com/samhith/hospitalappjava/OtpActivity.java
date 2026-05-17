package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class OtpActivity extends AppCompatActivity {

    private TextInputEditText etOtp;
    private String receivedOtp;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        dbHelper = new DatabaseHelper(this);
        etOtp = findViewById(R.id.etOtp);
        MaterialButton btnVerify = findViewById(R.id.btnVerify);
        TextView tvResendOtp = findViewById(R.id.tvResendOtp);
        TextView tvOtpDescription = findViewById(R.id.tvOtpDescription);

        String email = getIntent().getStringExtra("EMAIL");
        receivedOtp = getIntent().getStringExtra("OTP");

        tvOtpDescription.setText("We've sent a 6-digit code to " + email + ". Please enter it below to activate your account.");

        btnVerify.setOnClickListener(v -> verifyOtp());
        tvResendOtp.setOnClickListener(v -> resendOtp(email));
    }

    private void resendOtp(String email) {
        String newOtp = String.format(Locale.getDefault(), "%06d", new Random().nextInt(999999));
        receivedOtp = newOtp;

        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Resending verification code...");
        pd.setCancelable(false);
        pd.show();

        EmailService.sendOtpEmail(email, newOtp, new EmailService.EmailCallback() {
            @Override
            public void onSuccess() {
                pd.dismiss();
                Toast.makeText(OtpActivity.this, "A new code has been sent!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                pd.dismiss();
                Toast.makeText(OtpActivity.this, "Failed to resend: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verifyOtp() {
        String enteredOtp = etOtp.getText().toString().trim();
        if (enteredOtp.equals(receivedOtp)) {
            saveUserToDatabase();
        } else {
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserToDatabase() {
        String name = getIntent().getStringExtra("NAME");
        String email = getIntent().getStringExtra("EMAIL");
        String password = getIntent().getStringExtra("PASSWORD");
        String role = getIntent().getStringExtra("ROLE");
        String specialization = getIntent().getStringExtra("SPECIALIZATION");
        String phone = getIntent().getStringExtra("PHONE");
        String address = getIntent().getStringExtra("ADDRESS");
        String photoPath = getIntent().getStringExtra("PHOTO_PATH");
        if (photoPath != null && photoPath.isEmpty()) {
            photoPath = null;
        }
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        long staffId = dbHelper.addStaff(name, role, "General", email, phone, todayDate, address, specialization, photoPath, -1);

        if (staffId != -1) {
            boolean userCreated = dbHelper.addUser(email, password, role);
            if (userCreated) {
                int userId = dbHelper.getUserId(email);
                dbHelper.updateStaffUserId((int)staffId, userId);

                Staff s = dbHelper.getStaffByEmail(email);
                if (s != null) {
                    FirestoreSyncManager.syncStaff(s);
                    FirestoreSyncManager.syncUser(email, password, role, (int)staffId);
                }

                Toast.makeText(this, "Account activated! Please login.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(OtpActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Failed to create login credentials", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Failed to create staff profile", Toast.LENGTH_SHORT).show();
        }
    }
}
