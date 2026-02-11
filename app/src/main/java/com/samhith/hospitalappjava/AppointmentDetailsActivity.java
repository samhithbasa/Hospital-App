package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AppointmentDetailsActivity extends AppCompatActivity {
    private TextView tvDate, tvTime, tvPurpose, tvStatus;
    private DatabaseHelper databaseHelper;
    private int appointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        databaseHelper = new DatabaseHelper(this);
        appointmentId = getIntent().getIntExtra("APPOINTMENT_ID", -1);

        TextView tvPatientName = findViewById(R.id.tvPatientName);
        TextView tvDoctorName = findViewById(R.id.tvDoctorName);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvPurpose = findViewById(R.id.tvPurpose);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button backBtn = findViewById(R.id.backBtn);

        loadAppointmentDetails(tvPatientName, tvDoctorName);

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(AppointmentDetailsActivity.this, EditAppointmentActivity.class);
            intent.putExtra("APPOINTMENT_ID", appointmentId);
            startActivity(intent);
        });

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

            // Set color based on status
            if (status.equalsIgnoreCase("completed")) {
                tvStatus.setTextColor(getResources().getColor(R.color.green));
            } else if (status.equalsIgnoreCase("canceled")) {
                tvStatus.setTextColor(getResources().getColor(R.color.red));
            } else if (status.equalsIgnoreCase("scheduled")) {
                tvStatus.setTextColor(getResources().getColor(R.color.orange));
            } else {
                tvStatus.setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }

    private void deleteAppointment() {
        boolean deleted = databaseHelper.deleteAppointment(appointmentId);
        if (deleted) {
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