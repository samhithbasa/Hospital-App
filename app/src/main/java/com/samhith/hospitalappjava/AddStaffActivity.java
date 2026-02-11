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
import com.google.android.material.datepicker.MaterialDatePicker;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class AddStaffActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1001;

    private EditText nameEditText, roleEditText, departmentEditText,
            emailEditText, phoneEditText, joinDateEditText, addressEditText;
    private ImageView profileImageView;
    private Uri selectedImageUri;
    private Button saveButton;
    private DatabaseHelper dbHelper;
    private int userId;

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
            selectedImageUri = data.getData();
            profileImageView.setImageURI(selectedImageUri);
        }
    }

    private void saveStaff() {
        String name = nameEditText.getText().toString().trim();
        String role = roleEditText.getText().toString().trim();
        String department = departmentEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String joinDate = joinDateEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String photoPath = selectedImageUri != null ? selectedImageUri.toString() : null;

        if (name.isEmpty() || role.isEmpty() || department.isEmpty() ||
                email.isEmpty() || phone.isEmpty() || joinDate.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Pattern.matches("^[0-9]{10}$", phone)) {
            Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        long result = dbHelper.addStaff(name, role, department, email, phone, joinDate, address, photoPath, userId);

        if (result != -1) {
            Toast.makeText(this, "Staff member added successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error adding staff member", Toast.LENGTH_SHORT).show();
        }
    }
}