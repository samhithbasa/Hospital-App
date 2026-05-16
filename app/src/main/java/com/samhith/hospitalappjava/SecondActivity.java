package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.PendingIntent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class SecondActivity extends AppCompatActivity {

    private static final String TAG = "SecondActivity";
    private int userId;
    private String userRole;
    private String username;
    private TextView userRoleText;
    private DatabaseHelper dbHelper;
    private com.google.firebase.firestore.ListenerRegistration statusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply Dark Mode Preference before super.onCreate and setContentView
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // Initialize Views
        MaterialCardView cardPatients = findViewById(R.id.cardPatients);
        MaterialCardView cardStaff = findViewById(R.id.cardStaff);
        MaterialCardView cardAppointments = findViewById(R.id.cardAppointments);
        MaterialCardView cardReports = findViewById(R.id.cardReports);
        View logoutBtn = findViewById(R.id.logoutBtn);
        SwitchMaterial darkModeSwitch = findViewById(R.id.darkModeSwitch);
        userRoleText = findViewById(R.id.userRoleText);
        TextView tvAppointmentsTitle = findViewById(R.id.tvAppointmentsTitle);

        // Update switch state without triggering the listener
        darkModeSwitch.setOnCheckedChangeListener(null);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isDarkMode", isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            // recreate() is usually called by setDefaultNightMode, but let's be sure
            // Actually, calling it here can sometimes cause a loop or double recreation.
            // But if it's not changing, we might need it.
        });

        // Get Intent Data
        userId = getIntent().getIntExtra("USER_ID", -1);
        userRole = getIntent().getStringExtra("USER_ROLE");
        username = getIntent().getStringExtra("USERNAME");

        if (userId == -1 || userRole == null) {
            Log.e(TAG, "Invalid user session");
            Toast.makeText(this, "Error: Invalid user session", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Apply Role-Based Visibility and UI
        setupRoleUI(cardPatients, cardStaff, cardAppointments, cardReports, tvAppointmentsTitle);

        // Click Listeners
        cardPatients.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManagePatientsActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USER_ROLE", userRole);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
        });

        cardStaff.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageStaffActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USER_ROLE", userRole);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
        });

        cardAppointments.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageAppointmentsActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USER_ROLE", userRole);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
        });

        cardReports.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportsActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USER_ROLE", userRole);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
        });

        logoutBtn.setOnClickListener(v -> {
            // Clear ALL session data including persistent doctor email
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            prefs.edit()
                 .remove("current_username")
                 .remove("current_role")
                 .remove("current_user_id")
                 .remove("persistent_doctor_email")
                 .apply();
            
            // Stop the notification service
            stopService(new Intent(this, DoctorNotificationService.class));
            
            finish();
            startActivity(new Intent(this, MainActivity.class));
        });

        if ("doctor".equalsIgnoreCase(userRole)) {
            // Service handles notifications in background
        } else {
            // If NOT a doctor, ensure the service is stopped
            stopService(new Intent(this, DoctorNotificationService.class));
        }

        // Real-time security listener: Force logout if account deactivated
        setupSecurityListener();
    }

    private void setupSecurityListener() {
        if (username == null) return;
        
        statusListener = FirebaseFirestore.getInstance()
                .collection("backup_users")
                .document(username)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Status listener error", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Boolean isActive = snapshot.getBoolean("isActive");
                        if (Boolean.FALSE.equals(isActive)) {
                            Log.w(TAG, "Account deactivated. Forcing logout.");
                            handleForcedLogout("Your account has been deactivated.");
                        }
                    } else if (snapshot != null && !snapshot.exists()) {
                        // Document deleted entirely
                        Log.w(TAG, "Account record deleted. Forcing logout.");
                        handleForcedLogout("Your account record was not found.");
                    }
                });
    }

    private void handleForcedLogout(String message) {
        // Stop listener
        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }

        // Clear session
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit()
             .remove("current_username")
             .remove("current_role")
             .remove("current_user_id")
             .apply();

        // Show message and exit
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        if (statusListener != null) {
            statusListener.remove();
        }
        super.onDestroy();
    }

    private void setupRoleUI(MaterialCardView cardPatients, MaterialCardView cardStaff, 
                             MaterialCardView cardAppointments, MaterialCardView cardReports,
                             TextView tvAppointmentsTitle) {
        
        String capitalizedRole = userRole.substring(0, 1).toUpperCase() + userRole.substring(1);
        
        // Fetch staff name for personalized greeting
        dbHelper = new DatabaseHelper(this);
        Staff staff = dbHelper.getStaffByEmail(username);
        if (staff != null && staff.getName() != null) {
            userRoleText.setText("Welcome, " + staff.getName());
        } else {
            userRoleText.setText(capitalizedRole + " Portal");
        }

        if ("doctor".equalsIgnoreCase(userRole)) {
            // Doctors only see their appointments
            cardPatients.setVisibility(View.GONE);
            cardStaff.setVisibility(View.GONE);
            cardReports.setVisibility(View.GONE);
            tvAppointmentsTitle.setText("My Schedule");
            
            // Re-center appointments card if it's the only one
            // (The GridLayout handles this somewhat, but we can refine)
        } else if ("receptionist".equalsIgnoreCase(userRole) || "admin".equalsIgnoreCase(userRole)) {
            // Receptionists and Admins see everything
            cardPatients.setVisibility(View.VISIBLE);
            cardStaff.setVisibility(View.VISIBLE);
            cardReports.setVisibility(View.VISIBLE);
            tvAppointmentsTitle.setText("Appointments");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId == -1) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}