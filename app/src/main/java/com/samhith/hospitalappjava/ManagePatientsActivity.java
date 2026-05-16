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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class ManagePatientsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int userId;
    private String userRole;
    private ListenerRegistration patientListener;

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

        setupRealTimeSync(patientsListView);
    }

    private void setupRealTimeSync(ListView patientsListView) {
        patientListener = FirebaseFirestore.getInstance()
                .collection("backup_patients")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        boolean needsRefresh = false;
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.REMOVED) {
                                String phone = dc.getDocument().getString("phone");
                                if (phone != null) {
                                    int id = dbHelper.getPatientIdByPhone(phone);
                                    if (id != -1) {
                                        dbHelper.deletePatient(id);
                                        needsRefresh = true;
                                    }
                                }
                            } else {
                                needsRefresh = true;
                            }
                        }
                        if (needsRefresh) refreshPatientList(patientsListView);
                    }
                });
    }

    private void refreshPatientList(ListView patientsListView) {
        List<Patient> patientList = new java.util.ArrayList<>();
        if ("doctor".equalsIgnoreCase(userRole)) {
            // Doctors see patients they have appointments with
            String username = getIntent().getStringExtra("USERNAME");
            if (username != null && !username.isEmpty()) {
                int staffId = dbHelper.getStaffIdForUser(username);
                if (staffId != -1) {
                    patientList = dbHelper.getPatientsByDoctorId(staffId);
                } else {
                    Toast.makeText(this, "Doctor profile not found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Session error: Username missing.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Receptionists/Admins see all patients
            patientList = dbHelper.getAllPatients();
        }

        PatientAdapter adapter = new PatientAdapter(this, patientList);
        patientsListView.setAdapter(adapter);
    }

    private void showPatientDetailsDialog(Patient patient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_item_details, null);
        builder.setView(dialogView);
        
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogContent = dialogView.findViewById(R.id.dialogContent);
        Button btnBack = dialogView.findViewById(R.id.btnBack);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnEdit = dialogView.findViewById(R.id.btnEdit);
        
        dialogTitle.setText("Patient Profile");
        
        StringBuilder content = new StringBuilder();
        content.append("Name: ").append(patient.getName()).append("\n");
        content.append("Age: ").append(patient.getAge()).append("\n");
        content.append("Gender: ").append(patient.getGender()).append("\n");
        content.append("Address: ").append(patient.getAddress()).append("\n");
        content.append("Phone: ").append(patient.getPhone()).append("\n");
        content.append("Medical History: ").append(patient.getMedicalHistory());
        
        dialogContent.setText(content.toString());
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        btnBack.setOnClickListener(v -> dialog.dismiss());
        
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, EditPatientActivity.class);
            intent.putExtra("PATIENT_ID", patient.getId());
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });
        
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeletePatient(patient);
        });
        
        dialog.show();
    }

    private void confirmDeletePatient(Patient patient) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this patient?")
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    if (dbHelper.deletePatient(patient.getId())) {
                        FirestoreSyncManager.deletePatient(patient.getPhone());
                        Toast.makeText(this, "Patient deleted", Toast.LENGTH_SHORT).show();
                        refreshPatientList((ListView) findViewById(R.id.patientsListView));
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPatientList((ListView) findViewById(R.id.patientsListView));
    }
}