package com.samhith.hospitalappjava;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditMyProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etAddress;
    private DatabaseHelper dbHelper;
    private String username;
    private Staff staff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_my_profile);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);

        MaterialButton btnSave = findViewById(R.id.btnSave);
        MaterialButton btnCancel = findViewById(R.id.btnCancel);

        dbHelper = new DatabaseHelper(this);

        username = getIntent().getStringExtra("USERNAME");

        if (username == null) {
            Toast.makeText(this, "Error loading details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProfileData();

        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadProfileData() {
        staff = dbHelper.getStaffByEmail(username);
        if (staff != null) {
            etName.setText(staff.getName());
            etPhone.setText(staff.getPhone());
            etAddress.setText(staff.getAddress());
        } else {
            Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveProfile() {
        String newName = etName.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newAddress = etAddress.getText().toString().trim();

        if (newName.isEmpty() || newPhone.isEmpty() || newAddress.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPhone.length() != 10) {
            Toast.makeText(this, "Mobile number must be exactly 10 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        if (staff != null) {
            staff.setName(newName);
            staff.setPhone(newPhone);
            staff.setAddress(newAddress);

            boolean success = dbHelper.updateStaff(staff);
            if (success) {
                FirestoreSyncManager.syncStaff(staff);
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
