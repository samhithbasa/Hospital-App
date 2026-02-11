package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class EditPatientActivity extends AppCompatActivity {

    private EditText nameEditText, ageEditText, genderEditText,
            addressEditText, phoneEditText, medicalHistoryEditText;
    private Button saveButton, cancelButton;
    private DatabaseHelper dbHelper;
    private int patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_patient);

        // Initialize views
        nameEditText = findViewById(R.id.editName);
        ageEditText = findViewById(R.id.editAge);
        genderEditText = findViewById(R.id.editGender);
        addressEditText = findViewById(R.id.editAddress);
        phoneEditText = findViewById(R.id.editPhone);
        medicalHistoryEditText = findViewById(R.id.editMedicalHistory);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        dbHelper = new DatabaseHelper(this);

        // Get patient ID from intent
        patientId = getIntent().getIntExtra("PATIENT_ID", -1);

        if (patientId != -1) {
            loadPatientData(patientId);
        }

        // Set up button click listeners
        saveButton.setOnClickListener(v -> savePatientChanges());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void loadPatientData(int patientId) {
        Patient patient = dbHelper.getPatientById(patientId);

        if (patient != null) {
            nameEditText.setText(patient.getName());
            ageEditText.setText(String.valueOf(patient.getAge()));
            genderEditText.setText(patient.getGender());
            addressEditText.setText(patient.getAddress());
            phoneEditText.setText(patient.getPhone());
            medicalHistoryEditText.setText(patient.getMedicalHistory());
        }
    }

    private void savePatientChanges() {
        // Get updated values from EditTexts
        String name = nameEditText.getText().toString().trim();
        int age = Integer.parseInt(ageEditText.getText().toString().trim());
        String gender = genderEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String medicalHistory = medicalHistoryEditText.getText().toString().trim();

        // Update patient in database
        boolean isUpdated = dbHelper.updatePatient(patientId, name, age, gender,
                address, phone, medicalHistory);

        if (isUpdated) {
            // Set result and finish
            setResult(RESULT_OK);
            finish();
        }
    }
}