package com.samhith.hospitalappjava;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private TextView tvPatientCount, tvStaffCount, tvAppointmentCount;
    private TextView tvDoctorCount, tvNurseCount;
    private TextView tvUpcomingAppointments, tvCompletedAppointments, tvCanceledAppointments;
    private int userId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        databaseHelper = new DatabaseHelper(this);

        tvPatientCount = findViewById(R.id.tvPatientCount);
        tvStaffCount = findViewById(R.id.tvStaffCount);
        tvDoctorCount = findViewById(R.id.tvDoctorCount);
        tvNurseCount = findViewById(R.id.tvNurseCount);
        tvAppointmentCount = findViewById(R.id.tvAppointmentCount);
        tvUpcomingAppointments = findViewById(R.id.tvUpcomingAppointments);
        tvCompletedAppointments = findViewById(R.id.tvCompletedAppointments);
        tvCanceledAppointments = findViewById(R.id.tvCanceledAppointments);

        userId = getIntent().getIntExtra("USER_ID", -1);
        userRole = getIntent().getStringExtra("USER_ROLE");

        if (userId == -1 || userRole == null) {
            Toast.makeText(this, "Invalid user session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadReports();
    }

    private void loadReports() {
        loadBasicCounts();
        loadUpcomingAppointments();
        loadCompletedAppointments();
        loadCanceledAppointments();
    }

    private void loadBasicCounts() {
        List<Patient> patients;
        List<Appointment> appointments;

        List<Staff> staffList = databaseHelper.getAllStaff();
        int doctorCount = 0;
        int nurseCount = 0;

        for (Staff s : staffList) {
            if (s.getRole().equalsIgnoreCase("doctor")) doctorCount++;
            else if (s.getRole().equalsIgnoreCase("nurse")) nurseCount++;
        }

        if ("admin".equalsIgnoreCase(userRole)) {
            patients = databaseHelper.getAllPatients();
            appointments = databaseHelper.getAllAppointmentsWithNames();
        } else if ("doctor".equalsIgnoreCase(userRole)) {
            patients = databaseHelper.getPatientsByDoctorId(userId);
            appointments = databaseHelper.getAppointmentsByDoctorId(userId);
        } else {
            patients = databaseHelper.getPatientsByUserId(userId);
            appointments = databaseHelper.getAppointmentsByUserId(userId);
        }

        tvPatientCount.setText(getString(R.string.patient_count, patients.size()));
        tvStaffCount.setText(getString(R.string.staff_count, staffList.size()));
        tvDoctorCount.setText(getString(R.string.doctor_count, doctorCount));
        tvNurseCount.setText(getString(R.string.nurse_count, nurseCount));
        tvAppointmentCount.setText(getString(R.string.appointment_count, appointments.size()));
    }

    private void loadUpcomingAppointments() {
        List<Appointment> appointments = getFilteredAppointments("scheduled");
        displayAppointments(appointments, tvUpcomingAppointments, true);
    }

    private void loadCompletedAppointments() {
        List<Appointment> appointments = getFilteredAppointments("completed");
        displayAppointments(appointments, tvCompletedAppointments, false);
    }

    private void loadCanceledAppointments() {
        List<Appointment> appointments = getFilteredAppointments("canceled");
        displayAppointments(appointments, tvCanceledAppointments, false);
    }

    private List<Appointment> getFilteredAppointments(String status) {
        if ("admin".equalsIgnoreCase(userRole)) {
            return databaseHelper.getAppointmentsByStatus(status);
        } else if ("doctor".equalsIgnoreCase(userRole)) {
            return filterByStatus(databaseHelper.getAppointmentsByDoctorId(userId), status);
        } else {
            return filterByStatus(databaseHelper.getAppointmentsByUserId(userId), status);
        }
    }

    private void displayAppointments(List<Appointment> appointments, TextView targetView, boolean isUpcoming) {
        if (appointments.isEmpty()) {
            targetView.setText(isUpcoming ? R.string.no_upcoming_appointments : R.string.no_completed_appointments);
        } else {
            StringBuilder builder = new StringBuilder();
            for (Appointment a : appointments) {
                builder.append(formatAppointmentDetails(a, isUpcoming));
            }
            targetView.setText(builder.toString());
        }
    }

    private List<Appointment> filterByStatus(List<Appointment> all, String status) {
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment a : all) {
            if (status.equalsIgnoreCase(a.getStatus())) {
                filtered.add(a);
            }
        }
        return filtered;
    }

    private String formatAppointmentDetails(Appointment a, boolean isUpcoming) {
        String patient = a.getPatientName() != null ? a.getPatientName() : "Unknown";
        String doctor = a.getDoctorName() != null ? a.getDoctorName() : "Unknown";
        String date = a.getDate() != null ? a.getDate() : "N/A";
        String timeInfo = isUpcoming
                ? getString(R.string.scheduled_for_time, a.getTime() != null ? a.getTime() : "N/A")
                : getString(R.string.status_updated_at, a.getStatusUpdateTime() != null ? a.getStatusUpdateTime() : "N/A");

        return "• " + patient + "\n" +
                "  " + getString(R.string.with_doctor, doctor) + "\n" +
                "  " + getString(R.string.on_date, date) + "\n" +
                "  " + timeInfo + "\n\n";
    }
}
