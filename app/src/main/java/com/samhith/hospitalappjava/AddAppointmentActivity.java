package com.samhith.hospitalappjava;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddAppointmentActivity extends AppCompatActivity {
    private Spinner spinnerPatients, spinnerDoctors;
    private EditText etDate, etTime, etPurpose;
    private DatabaseHelper databaseHelper;
    private List<Patient> patientList;
    private List<Staff> doctorList;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_appointment);

        databaseHelper = new DatabaseHelper(this);
        userId = getIntent().getIntExtra("USER_ID", -1);

        spinnerPatients = findViewById(R.id.spinnerPatients);
        spinnerDoctors = findViewById(R.id.spinnerDoctors);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etPurpose = findViewById(R.id.etPurpose);
        Button btnSave = findViewById(R.id.btnSave);

        setCurrentDateTime();
        loadPatientsAndDoctors();

        // Show Material Date Picker
        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> showDatePicker());

        // Show Time Picker Dialog
        etTime.setFocusable(false);
        etTime.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> saveAppointment());
    }

    private void setCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Date now = new Date();
        etDate.setText(dateFormat.format(now));
        etTime.setText(timeFormat.format(now));
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

    private void loadPatientsAndDoctors() {
        patientList = databaseHelper.getPatientsByUserId(userId);
        List<String> patientNames = new ArrayList<>();
        for (Patient p : patientList) {
            patientNames.add(p.getName() + " (ID: " + p.getId() + ")");
        }
        ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, patientNames);
        patientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPatients.setAdapter(patientAdapter);

        doctorList = new ArrayList<>();
        List<Staff> allStaff = databaseHelper.getAllStaff();
        List<String> doctorNames = new ArrayList<>();
        for (Staff staff : allStaff) {
            if ("doctor".equalsIgnoreCase(staff.getRole())) {
                doctorList.add(staff);
                doctorNames.add(staff.getName() + " (" + staff.getDepartment() + ")");
            }
        }
        ArrayAdapter<String> doctorAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, doctorNames);
        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctors.setAdapter(doctorAdapter);
    }

    private void saveAppointment() {
        if (spinnerPatients.getSelectedItemPosition() < 0 || spinnerDoctors.getSelectedItemPosition() < 0) {
            Toast.makeText(this, "Please select both patient and doctor", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String purpose = etPurpose.getText().toString().trim();

        if (date.isEmpty() || time.isEmpty() || purpose.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int patientId = patientList.get(spinnerPatients.getSelectedItemPosition()).getId();
        int doctorId = doctorList.get(spinnerDoctors.getSelectedItemPosition()).getId();

        long result = databaseHelper.addAppointment(patientId, doctorId, date, time, purpose, userId);

        if (result != -1) {
            scheduleReminder(patientList.get(spinnerPatients.getSelectedItemPosition()).getName(), date, time);
            Toast.makeText(this, "Appointment added successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error adding appointment", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleReminder(String patientName, String date, String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date appointmentDate = sdf.parse(date + " " + time);
            if (appointmentDate != null) {
                long triggerAtMillis = appointmentDate.getTime() - (10 * 60 * 1000); // 10 minutes before

                if (triggerAtMillis < System.currentTimeMillis()) {
                    triggerAtMillis = System.currentTimeMillis() + 1000; // If already passed, set for 1s later
                }

                Intent intent = new Intent(this, ReminderReceiver.class);
                intent.putExtra("PATIENT_NAME", patientName);
                intent.putExtra("APPOINTMENT_TIME", time);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this, (int) System.currentTimeMillis(), intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
