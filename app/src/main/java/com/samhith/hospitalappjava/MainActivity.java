package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private Button loginButton, signUpButton;
    private TextView errorText, signupSuccessText;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUP);
        errorText = findViewById(R.id.errorText);
        signupSuccessText = findViewById(R.id.signup_success);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);

        dbHelper = new DatabaseHelper(this);

        requestNotificationPermission();

        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());

        // Add admin user if not exists
        if (dbHelper.getUserRole("admin") == null) {
            dbHelper.addUser("admin", "admin123", "admin");
        }

        // Login functionality
        // Login functionality
        loginButton.setOnClickListener(view -> {
            String email = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                errorText.setText(R.string.fill_all_fields);
                return;
            }

            // 1. Try Local Login first
            if (dbHelper.checkUser(email, password)) {
                loginSuccess(email);
            } else {
                // 2. Try Firebase Auth (Cloud Login)
                Toast.makeText(this, "Verifying with cloud...", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Cloud login success!
                                // Now we need to restore data to local DB so the app works
                                Toast.makeText(this, "Cloud login successful! Syncing data...", Toast.LENGTH_LONG)
                                        .show();

                                // Trigger Restore
                                OneTimeWorkRequest restoreRequest = new OneTimeWorkRequest.Builder(RestoreWorker.class)
                                        .build();
                                WorkManager.getInstance(this).enqueue(restoreRequest);

                                // Monitor restore progress or just let user wait?
                                // For better UX, we'll watch the worker
                                WorkManager.getInstance(this).getWorkInfoByIdLiveData(restoreRequest.getId())
                                        .observe(this, workInfo -> {
                                            if (workInfo != null && workInfo.getState().isFinished()) {
                                                if (workInfo.getState() == androidx.work.WorkInfo.State.SUCCEEDED) {
                                                    // Ensure user exists locally now (restored from backup_users)
                                                    // The password hash should matched.
                                                    // Proceed to dashboard
                                                    loginSuccess(email);
                                                } else {
                                                    errorText.setText("Sync failed. Check internet.");
                                                }
                                            }
                                        });

                            } else {
                                errorText.setText("Invalid credentials or user not found.");
                            }
                        });
            }
        });

        // Sign-up functionality with password validation
        signUpButton.setOnClickListener(view -> {
            String email = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                signupSuccessText.setText(R.string.fill_all_fields);
                signupSuccessText
                        .setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                return;
            }

            // Check: password must not contain username (email)
            if (password.toLowerCase().contains(email.toLowerCase())) {
                signupSuccessText.setText("Password should not contain email.");
                signupSuccessText
                        .setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                return;
            }

            // Optional: Check for minimum password length
            if (password.length() < 8) {
                signupSuccessText.setText("Password must be at least 8 characters long.");
                signupSuccessText
                        .setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                return;
            }

            // Create Firebase Auth user first
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Firebase Auth created, now create local user
                            long result = dbHelper.addUser(email, password, "staff");

                            if (result != -1) {
                                signupSuccessText.setText(R.string.signup_success);
                                signupSuccessText
                                        .setTextColor(ContextCompat.getColor(MainActivity.this,
                                                android.R.color.holo_green_dark));
                                usernameEditText.setText("");
                                passwordEditText.setText("");
                            } else {
                                signupSuccessText.setText("Local user creation failed (exists?)");
                                signupSuccessText
                                        .setTextColor(ContextCompat.getColor(MainActivity.this,
                                                android.R.color.holo_red_dark));
                            }
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage()
                                    : "Unknown error";
                            signupSuccessText.setText("Registration failed: " + error);
                            signupSuccessText
                                    .setTextColor(
                                            ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                        }
                    });
        });
    }

    private void loginSuccess(String email) {
        String role = dbHelper.getUserRole(email);
        int userId = dbHelper.getUserId(email);

        // Fallback for role if null (e.g. freshly restored/created)
        if (role == null)
            role = "staff";

        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        intent.putExtra("USERNAME", email);
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);

        // Trigger automatic cloud sync after successful login
        Toast.makeText(this, "Syncing data from cloud...", Toast.LENGTH_SHORT).show();
        OneTimeWorkRequest restoreRequest = new OneTimeWorkRequest.Builder(RestoreWorker.class).build();
        WorkManager.getInstance(this).enqueue(restoreRequest);

        finish();
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText input = new EditText(this);
        input.setHint("Enter your email");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this,
                                        "Password reset email sent! Please check your inbox and spam folder.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                String errorMsg = task.getException() != null ? task.getException().getMessage()
                                        : "Unknown error";
                                Toast.makeText(MainActivity.this,
                                        "Error sending reset email: " + errorMsg +
                                                "\n\nPlease verify:\n1. Email is registered\n2. Internet connection\n3. Check spam folder",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.POST_NOTIFICATIONS }, 101);
            }
        }
    }
}
