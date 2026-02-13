package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.concurrent.TimeUnit;

public class SecondActivity extends AppCompatActivity {

    private static final String TAG = "SecondActivity";
    private int userId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Button managePatientsBtn = findViewById(R.id.managePatientsBtn);
        Button manageStaffBtn = findViewById(R.id.manageStaffBtn);
        Button manageAppointmentsBtn = findViewById(R.id.manageAppointmentsBtn);
        Button viewReportsBtn = findViewById(R.id.viewReportsBtn);
        Button logoutBtn = findViewById(R.id.logoutBtn);
        Button backBtn = findViewById(R.id.backBtn);
        SwitchMaterial darkModeSwitch = findViewById(R.id.darkModeSwitch);

        // Dark Mode Logic
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        darkModeSwitch.setChecked(isDarkMode);

        // Schedule Cloud Backup
        PeriodicWorkRequest backupRequest = new PeriodicWorkRequest.Builder(
                BackupWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueue(backupRequest);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isDarkMode", isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        userId = getIntent().getIntExtra("USER_ID", -1);

        if (userId == -1) {
            Log.e(TAG, "Invalid user ID received");
            Toast.makeText(this, "Error: Invalid user session", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        userRole = getIntent().getStringExtra("USER_ROLE");
        if (userRole == null) {
            Log.e(TAG, "Missing user role");
            Toast.makeText(this, "Error: Missing user role", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        managePatientsBtn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(SecondActivity.this, ManagePatientsActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching ManagePatientsActivity: " + e.getMessage());
                Toast.makeText(this, "Error opening Patient Management", Toast.LENGTH_SHORT).show();
            }
        });

        manageStaffBtn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, ManageStaffActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching ManageStaffActivity: " + e.getMessage());
                Toast.makeText(this, "Error opening Staff Management", Toast.LENGTH_SHORT).show();
            }
        });

        manageAppointmentsBtn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, ManageAppointmentsActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching ManageAppointmentsActivity: " + e.getMessage());
                Toast.makeText(this, "Error opening Appointment Management", Toast.LENGTH_SHORT).show();
            }
        });

        viewReportsBtn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(SecondActivity.this, ReportsActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching ReportsActivity: " + e.getMessage());
                Toast.makeText(this, "Error opening Reports", Toast.LENGTH_SHORT).show();
            }
        });

        backBtn.setOnClickListener(v -> {
            startActivity(new Intent(SecondActivity.this, MainActivity.class));
            finish();
        });

        logoutBtn.setOnClickListener(v -> {
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId == -1) {
            Log.e(TAG, "Invalid user ID detected in onResume");
            Toast.makeText(this, "Error: Invalid user session", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}