package com.samhith.hospitalappjava;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.regex.Pattern;

public class AddPatientActivity extends AppCompatActivity {

    private EditText nameEditText, ageEditText, addressEditText, phoneEditText, medicalHistoryEditText;
    private AutoCompleteTextView genderSpinner;
    private Button saveButton;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_patient);

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText);
        ageEditText = findViewById(R.id.ageEditText);
        genderSpinner = findViewById(R.id.genderSpinner);
        addressEditText = findViewById(R.id.addressEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        medicalHistoryEditText = findViewById(R.id.medicalHistoryEditText);
        saveButton = findViewById(R.id.saveButton);

        // Set up gender dropdown
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);
        genderSpinner.setText(adapter.getItem(0).toString(), false); // Set default value

        dbHelper = new DatabaseHelper(this);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String name = nameEditText.getText().toString().trim();
                    String ageStr = ageEditText.getText().toString().trim();
                    String gender = genderSpinner.getText().toString().trim();
                    String address = addressEditText.getText().toString().trim();
                    String phone = phoneEditText.getText().toString().trim();
                    String medicalHistory = medicalHistoryEditText.getText().toString().trim();

                    if (name.isEmpty() || ageStr.isEmpty()) {
                        Toast.makeText(AddPatientActivity.this,
                                "Please fill required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Phone number validation
                    if (!phone.isEmpty() && (!Pattern.matches("^[0-9]{10}$", phone))) {
                        Toast.makeText(AddPatientActivity.this,
                                "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int age = Integer.parseInt(ageStr);

                    if (dbHelper == null) {
                        dbHelper = new DatabaseHelper(AddPatientActivity.this);
                    }

                    int userId = getIntent().getIntExtra("USER_ID", -1);

                    long result = dbHelper.addPatient(name, age, gender, address, phone, medicalHistory, userId);

                    if (result != -1) {
                        Toast.makeText(AddPatientActivity.this,
                                "Patient added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddPatientActivity.this,
                                "Failed to add patient", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(AddPatientActivity.this,
                            "Please enter a valid age", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(AddPatientActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace(); // This will help in debugging
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}