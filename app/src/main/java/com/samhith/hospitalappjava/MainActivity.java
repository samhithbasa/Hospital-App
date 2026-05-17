package com.samhith.hospitalappjava;

import android.content.Context;
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
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private com.google.android.material.button.MaterialButton loginButton;
    private TextView signUpButton, errorText;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply Dark Mode Preference
        android.content.SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUP);
        errorText = findViewById(R.id.errorText);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);

        dbHelper = new DatabaseHelper(this);
        FirebaseFirestore.getInstance().enableNetwork();

        // One-time database wipe logic
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("is_db_wiped_v1", false)) {
            dbHelper.clearAllData();
            prefs.edit().putBoolean("is_db_wiped_v1", true).apply();
        }

        requestNotificationPermission();

        // Start Doctor Notification Service if a persistent session exists
        String persistentEmail = prefs.getString("persistent_doctor_email", "");
        if (!persistentEmail.isEmpty()) {
            Intent serviceIntent = new Intent(this, DoctorNotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }

        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());
        loginButton.setOnClickListener(v -> handleLogin());
        signUpButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegisterActivity.class)));

        // Admin check
        if (dbHelper.getUserRole("admin") == null) {
            dbHelper.addUser("admin", "admin123", "admin", -1);
        }

        // Auto-Login
        String savedUser = prefs.getString("current_username", null);
        String savedRole = prefs.getString("current_role", null);
        int savedUserId = prefs.getInt("current_user_id", -1);
        if (savedUser != null && savedRole != null && savedUserId != -1) {
            loginSuccess(savedUser, savedRole);
        }
    }

    private void handleLogin() {
        String email = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorText.setText("Please enter email and password");
            errorText.setVisibility(android.view.View.VISIBLE);
            return;
        }

        final String finalEmail = email.toLowerCase();

        // 1. Local Login
        if (dbHelper.checkUser(email, password) || dbHelper.checkUser(finalEmail, password)) {
            String role = dbHelper.getUserRole(email);
            if (role == null) role = dbHelper.getUserRole(finalEmail);
            loginSuccess(email, role);
        } else {
            // 2. Cloud Login
            if (isNetworkAvailable()) {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errorText.setText("Invalid credentials format.");
                    errorText.setVisibility(android.view.View.VISIBLE);
                    return;
                }

                errorText.setText("Authenticating with cloud...");
                errorText.setVisibility(android.view.View.VISIBLE);
                verifyCloudUser(finalEmail, password);
            } else {
                errorText.setText("Invalid credentials or no internet.");
                errorText.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    private void verifyCloudUser(String email, String password) {
        FirebaseFirestore.getInstance().collection("backup_users")
                .document(email)
                .get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String cloudPassword = doc.getString("password");
                        if (cloudPassword != null && cloudPassword.equals(password)) {
                            if (Boolean.TRUE.equals(doc.getBoolean("isActive"))) {
                                String role = doc.getString("role");
                                if (dbHelper.getUserRole(email) == null) {
                                    dbHelper.addUser(email, password, role, -1);
                                }
                                fetchStaffProfileAndLogin(email, role);
                            } else {
                                errorText.setText("Account deactivated.");
                                errorText.setVisibility(android.view.View.VISIBLE);
                            }
                        } else {
                            errorText.setText("Invalid credentials.");
                            errorText.setVisibility(android.view.View.VISIBLE);
                        }
                    } else {
                        errorText.setText("Account not found.");
                        errorText.setVisibility(android.view.View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    errorText.setText("Cloud verification error.");
                    errorText.setVisibility(android.view.View.VISIBLE);
                });
    }

    private void fetchStaffProfileAndLogin(String email, String role) {
        if (!"admin".equals(role)) {
            FirebaseFirestore.getInstance().collection("backup_staff")
                    .document(email)
                    .get(Source.SERVER)
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && dbHelper.getStaffByEmail(email) == null) {
                            dbHelper.addStaff(
                                doc.getString("name"),
                                doc.getString("role"),
                                doc.getString("department"),
                                doc.getString("email"),
                                doc.getString("phone"),
                                doc.getString("join_date"),
                                doc.getString("address"),
                                doc.getString("specialization"),
                                doc.getString("photoPath"),
                                doc.getLong("id") != null ? doc.getLong("id").intValue() : -1
                            );
                        }
                        loginSuccess(email, role);
                    })
                    .addOnFailureListener(e -> loginSuccess(email, role));
        } else {
            loginSuccess(email, role);
        }
    }

    private void loginSuccess(String email, String role) {
        int userId = dbHelper.getUserId(email);
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                .putString("current_username", email)
                .putString("current_role", role)
                .putInt("current_user_id", userId)
                .apply();

        if ("doctor".equalsIgnoreCase(role)) {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putString("persistent_doctor_email", email).apply();
            Intent serviceIntent = new Intent(this, DoctorNotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }

        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        intent.putExtra("USERNAME", email);
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("USER_ID", userId);

        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Syncing data...");
        pd.setCancelable(false);
        pd.show();

        FirestoreSyncManager.downloadAllData(dbHelper, userId, () -> {
            pd.dismiss();
            startActivity(intent);
            finish();
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        final EditText input = new EditText(this);
        input.setHint("Enter email");
        builder.setView(input);
        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) Toast.makeText(this, "Email sent!", Toast.LENGTH_SHORT).show();
                        });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.SEND_SMS}, 101);
            }
        }
    }
}
