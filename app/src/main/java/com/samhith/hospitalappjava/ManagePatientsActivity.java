package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class ManagePatientsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int userId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_patients);

        // Get user info from intent
        userId = getIntent().getIntExtra("USER_ID", -1);
        userRole = getIntent().getStringExtra("USER_ROLE");

        if (userId == -1 || userRole == null) {
            Toast.makeText(this, "Invalid user info. Returning.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        ListView patientsListView = findViewById(R.id.patientsListView);
        Button addPatientBtn = findViewById(R.id.addPatientBtn);
        Button backBtn = findViewById(R.id.backBtn);
        TextView emptyView = findViewById(R.id.emptyView);

        // Set empty view
        patientsListView.setEmptyView(emptyView);

        // Load patient data
        refreshPatientList(patientsListView);

        // Button click listeners
        addPatientBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPatientActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        backBtn.setOnClickListener(v -> finish());

        // List item click listener
        patientsListView.setOnItemClickListener((parent, view, position, id) -> {
            Patient selectedPatient = (Patient) parent.getItemAtPosition(position);
            showPatientDetailsDialog(selectedPatient);
        });
    }

    private void refreshPatientList(ListView patientsListView) {
        List<Patient> patientList;
        if (userRole.equals("admin")) {
            patientList = dbHelper.getAllPatients();
        } else if (userRole.equals("doctor")) {
            patientList = dbHelper.getPatientsByDoctorId(userId);
        } else {
            patientList = dbHelper.getPatientsByUserId(userId);
        }

        PatientAdapter adapter = new PatientAdapter(this, patientList);
        patientsListView.setAdapter(adapter);
    }

    private void showPatientDetailsDialog(Patient patient) {
        new AlertDialog.Builder(this)
                .setTitle("Patient Details")
                .setMessage("Name: " + patient.getName() + "\n" +
                        "Age: " + patient.getAge() + "\n" +
                        "Gender: " + patient.getGender() + "\n" +
                        "Address: " + patient.getAddress() + "\n" +
                        "Phone: " + patient.getPhone() + "\n" +
                        "Medical History: " + patient.getMedicalHistory())
                .setPositiveButton("Edit", (dialog, which) -> {
                    Intent intent = new Intent(this, EditPatientActivity.class);
                    intent.putExtra("PATIENT_ID", patient.getId());
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                })
                .setNegativeButton("Delete", (dialog, which) ->
                        new AlertDialog.Builder(this)
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete this patient?")
                                .setPositiveButton("Yes", (dialogInterface, i) -> {
                                    dbHelper.deletePatient(patient.getId());
                                    refreshPatientList((ListView) findViewById(R.id.patientsListView));
                                })
                                .setNegativeButton("No", null)
                                .show())
                .setNeutralButton("Back", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPatientList((ListView) findViewById(R.id.patientsListView));
    }
}