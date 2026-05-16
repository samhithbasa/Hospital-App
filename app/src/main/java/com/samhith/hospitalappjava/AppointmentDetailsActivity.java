package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;
import androidx.appcompat.app.AppCompatActivity;

public class AppointmentDetailsActivity extends AppCompatActivity {
    private TextView tvDate, tvTime, tvPurpose, tvStatus;
    private DatabaseHelper databaseHelper;
    private int appointmentId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        databaseHelper = new DatabaseHelper(this);
        appointmentId = getIntent().getIntExtra("APPOINTMENT_ID", -1);
        userRole = getIntent().getStringExtra("USER_ROLE");

        TextView tvPatientName = findViewById(R.id.tvPatientName);
        TextView tvDoctorName = findViewById(R.id.tvDoctorName);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvPurpose = findViewById(R.id.tvPurpose);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button btnAccept = findViewById(R.id.btnAccept);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnComplete = findViewById(R.id.btnComplete);
        Button backBtn = findViewById(R.id.backBtn);

        loadAppointmentDetails(tvPatientName, tvDoctorName);

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(AppointmentDetailsActivity.this, EditAppointmentActivity.class);
            intent.putExtra("APPOINTMENT_ID", appointmentId);
            intent.putExtra("USER_ROLE", userRole);
            startActivity(intent);
        });

        if ("doctor".equalsIgnoreCase(userRole)) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            // Accept/Cancel will be shown in loadAppointmentDetails if status is pending
        } else if ("receptionist".equalsIgnoreCase(userRole)) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE); // Receptionist CAN delete
        }
        
        btnAccept.setOnClickListener(v -> updateStatus("scheduled"));
        btnCancel.setOnClickListener(v -> updateStatus("canceled"));
        btnComplete.setOnClickListener(v -> updateStatus("completed"));

        btnDelete.setOnClickListener(v -> deleteAppointment());

        backBtn.setOnClickListener(v -> finish());
    }

    private void loadAppointmentDetails(TextView tvPatientName, TextView tvDoctorName) {
        Appointment appointment = databaseHelper.getAppointmentWithNamesById(appointmentId);
        if (appointment != null) {
            tvPatientName.setText(getString(R.string.patient_label, appointment.getPatientName()));
            tvDoctorName.setText(getString(R.string.doctor_label, appointment.getDoctorName()));
            tvDate.setText(getString(R.string.date_label, appointment.getDate()));
            tvTime.setText(getString(R.string.time_label, appointment.getTime()));
            tvPurpose.setText(getString(R.string.purpose_label, appointment.getPurpose()));

            String status = appointment.getStatus();
            String statusText = getString(R.string.status_label, status);

            // Add time information based on status
            if (("completed".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)) &&
                    appointment.getStatusUpdateTime() != null &&
                    !appointment.getStatusUpdateTime().isEmpty()) {
                statusText += " (" + appointment.getStatusUpdateTime() + ")";
            } else if ("scheduled".equalsIgnoreCase(status)) {
                statusText += " (" + appointment.getTime() + ")";
            }
            tvStatus.setText(statusText);

            // Set badge color based on status
            int color;
            if (status.equalsIgnoreCase("completed")) {
                color = androidx.core.content.ContextCompat.getColor(this, R.color.green);
            } else if (status.equalsIgnoreCase("canceled")) {
                color = androidx.core.content.ContextCompat.getColor(this, R.color.red);
            } else if (status.equalsIgnoreCase("scheduled")) {
                color = androidx.core.content.ContextCompat.getColor(this, R.color.orange);
            } else if (status.equalsIgnoreCase("pending")) {
                color = androidx.core.content.ContextCompat.getColor(this, R.color.pending);
            } else {
                color = androidx.core.content.ContextCompat.getColor(this, R.color.on_surface);
            }
            tvStatus.getBackground().setTint(color);
            tvStatus.setTextColor(android.graphics.Color.WHITE);

            // Show action buttons for doctors
            if ("doctor".equalsIgnoreCase(userRole)) {
                if ("pending".equalsIgnoreCase(status)) {
                    findViewById(R.id.btnAccept).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnCancel).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnComplete).setVisibility(View.GONE);
                } else if ("scheduled".equalsIgnoreCase(status)) {
                    findViewById(R.id.btnAccept).setVisibility(View.GONE);
                    findViewById(R.id.btnCancel).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnComplete).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.btnAccept).setVisibility(View.GONE);
                    findViewById(R.id.btnCancel).setVisibility(View.GONE);
                    findViewById(R.id.btnComplete).setVisibility(View.GONE);
                }
            } else {
                findViewById(R.id.btnAccept).setVisibility(View.GONE);
                findViewById(R.id.btnCancel).setVisibility(View.GONE);
                findViewById(R.id.btnComplete).setVisibility(View.GONE);
            }
        }
    }

    private void updateStatus(String newStatus) {
        if (databaseHelper.updateAppointmentStatus(appointmentId, newStatus)) {
            Appointment app = databaseHelper.getAppointmentWithNamesById(appointmentId);
            if (app != null) {
                Patient p = databaseHelper.getPatientById(app.getPatientId());
                if (p != null) {
                    String doctorEmail = getIntent().getStringExtra("USERNAME");
                    if (doctorEmail == null || doctorEmail.isEmpty()) {
                        // Fallback to persistent email if intent extra is missing
                        doctorEmail = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("persistent_doctor_email", "");
                    }

                    FirestoreSyncManager.syncAppointment(app, p.getPhone(), doctorEmail);
                    Toast.makeText(this, "Cloud synced successfully", Toast.LENGTH_SHORT).show();
                    
                    // Send SMS to Patient
                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        String message;
                        if ("scheduled".equalsIgnoreCase(newStatus)) {
                            message = "Hello " + p.getName() + ", your appointment at Trinity Care on " + app.getDate() + " at " + app.getTime() + " has been CONFIRMED by the doctor.";
                        } else if ("completed".equalsIgnoreCase(newStatus)) {
                            message = "Hello " + p.getName() + ", your visit at Trinity Care is complete. Thank you for choosing us!";
                        } else {
                            message = "Hello " + p.getName() + ", unfortunately your appointment at Trinity Care on " + app.getDate() + " at " + app.getTime() + " has been CANCELED by the doctor.";
                        }
                        smsManager.sendTextMessage(p.getPhone(), null, message, null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
            loadAppointmentDetails(findViewById(R.id.tvPatientName), findViewById(R.id.tvDoctorName));
        }
    }

    private void deleteAppointment() {
        Appointment appointment = databaseHelper.getAppointmentById(appointmentId);
        if (appointment == null) return;

        Patient p = databaseHelper.getPatientById(appointment.getPatientId());
        Staff d = databaseHelper.getStaffById(appointment.getDoctorId());

        boolean deleted = databaseHelper.deleteAppointment(appointmentId);
        if (deleted) {
            if (p != null && d != null) {
                FirestoreSyncManager.deleteAppointment(p.getPhone(), d.getEmail(), appointment.getDate(), appointment.getTime());
            }
            Toast.makeText(this, R.string.appointment_deleted, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.error_deleting_appointment, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tvPatientName = findViewById(R.id.tvPatientName);
        TextView tvDoctorName = findViewById(R.id.tvDoctorName);
        loadAppointmentDetails(tvPatientName, tvDoctorName);
    }
}