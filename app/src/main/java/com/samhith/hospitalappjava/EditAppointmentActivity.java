package com.samhith.hospitalappjava;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditAppointmentActivity extends AppCompatActivity {
    private EditText etPatientId, etDoctorId, etDate, etTime, etPurpose, etStatus;
    private Button btnUpdate;
    private DatabaseHelper databaseHelper;
    private int appointmentId;
    private String originalStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_appointment);

        databaseHelper = new DatabaseHelper(this);
        appointmentId = getIntent().getIntExtra("APPOINTMENT_ID", -1);

        etPatientId = findViewById(R.id.etPatientId);
        etDoctorId = findViewById(R.id.etDoctorId);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etPurpose = findViewById(R.id.etPurpose);
        etStatus = findViewById(R.id.etStatus);
        btnUpdate = findViewById(R.id.btnUpdate);

        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> showDatePicker());

        etTime.setFocusable(false);
        etTime.setOnClickListener(v -> showTimePicker());

        loadAppointmentData();

        btnUpdate.setOnClickListener(v -> updateAppointment());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Appointment Date")
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String formattedDate = sdf.format(new Date(selection));
            etDate.setText(formattedDate);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    etTime.setText(formattedTime);
                },
                9, 0, true);
        timePickerDialog.show();
    }

    private void loadAppointmentData() {
        Appointment appointment = databaseHelper.getAppointmentById(appointmentId);
        if (appointment != null) {
            originalStatus = appointment.getStatus();
            etPatientId.setText(String.valueOf(appointment.getPatientId()));
            etDoctorId.setText(String.valueOf(appointment.getDoctorId()));
            etDate.setText(appointment.getDate());
            etTime.setText(appointment.getTime());
            etPurpose.setText(appointment.getPurpose());
            etStatus.setText(appointment.getStatus());
        }
    }

    private void updateAppointment() {
        try {
            int patientId = Integer.parseInt(etPatientId.getText().toString());
            int doctorId = Integer.parseInt(etDoctorId.getText().toString());
            String date = etDate.getText().toString();
            String time = etTime.getText().toString();
            String purpose = etPurpose.getText().toString();
            String status = etStatus.getText().toString().toLowerCase();

            if (!status.equals("scheduled") && !status.equals("completed") && !status.equals("canceled")) {
                Toast.makeText(this, "Status must be: scheduled, completed, or canceled", Toast.LENGTH_SHORT).show();
                return;
            }

            Appointment appointment = new Appointment();
            appointment.setId(appointmentId);
            appointment.setPatientId(patientId);
            appointment.setDoctorId(doctorId);
            appointment.setDate(date);
            appointment.setTime(time);
            appointment.setPurpose(purpose);
            appointment.setStatus(status);

            if (!status.equalsIgnoreCase(originalStatus) &&
                    (status.equals("completed") || status.equals("canceled"))) {
                String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                appointment.setStatusUpdateTime(currentTime);
            }

            boolean updated = databaseHelper.updateAppointment(appointment);
            if (updated) {
                Toast.makeText(this, "Appointment updated", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error updating appointment", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid IDs", Toast.LENGTH_SHORT).show();
        }
    }
}