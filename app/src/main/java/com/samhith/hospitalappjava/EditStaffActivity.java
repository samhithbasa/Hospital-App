package com.samhith.hospitalappjava;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class EditStaffActivity extends AppCompatActivity {

    private EditText nameEditText, roleEditText, departmentEditText,
            emailEditText, phoneEditText, joinDateEditText, addressEditText;
    private Button updateBtn, backBtn;
    private DatabaseHelper dbHelper;
    private int staffId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_staff);

        nameEditText = findViewById(R.id.nameEditText);
        roleEditText = findViewById(R.id.roleEditText);
        departmentEditText = findViewById(R.id.departmentEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        joinDateEditText = findViewById(R.id.joinDateEditText);
        addressEditText = findViewById(R.id.addressEditText);
        updateBtn = findViewById(R.id.updateBtn);
        backBtn = findViewById(R.id.backBtn);

        dbHelper = new DatabaseHelper(this);

        staffId = getIntent().getIntExtra("STAFF_ID", -1);

        if (staffId == -1) {
            Toast.makeText(this, "Invalid staff member", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        joinDateEditText.setFocusable(false);
        joinDateEditText.setOnClickListener(v -> showDatePicker());

        loadStaffDetails();

        updateBtn.setOnClickListener(v -> updateStaff());
        backBtn.setOnClickListener(v -> finish());
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

    private void loadStaffDetails() {
        Staff staff = dbHelper.getStaffById(staffId);
        if (staff != null) {
            nameEditText.setText(staff.getName());
            roleEditText.setText(staff.getRole());
            departmentEditText.setText(staff.getDepartment());
            emailEditText.setText(staff.getEmail());
            phoneEditText.setText(staff.getPhone());
            joinDateEditText.setText(staff.getJoinDate());
            addressEditText.setText(staff.getAddress());
        } else {
            Toast.makeText(this, "Staff record not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateStaff() {
        String name = nameEditText.getText().toString().trim();
        String role = roleEditText.getText().toString().trim();
        String department = departmentEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String joinDate = joinDateEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();

        if (name.isEmpty() || role.isEmpty() || department.isEmpty() ||
                email.isEmpty() || phone.isEmpty() || joinDate.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Pattern.matches("^[0-9]{10}$", phone)) {
            Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        Staff staff = new Staff();
        staff.setId(staffId);
        staff.setName(name);
        staff.setRole(role);
        staff.setDepartment(department);
        staff.setEmail(email);
        staff.setPhone(phone);
        staff.setJoinDate(joinDate);
        staff.setAddress(address);

        Staff dbStaff = dbHelper.getStaffById(staffId);
        if (dbStaff != null) {
            staff.setPhotoPath(dbStaff.getPhotoPath());
        }

        boolean success = dbHelper.updateStaff(staff);

        if (success) {
            Toast.makeText(this, "Staff updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update staff", Toast.LENGTH_SHORT).show();
        }
    }
}