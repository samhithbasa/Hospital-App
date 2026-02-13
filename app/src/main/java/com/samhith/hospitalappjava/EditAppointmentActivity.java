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
import java.text.ParseException;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.net.Uri;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;

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

                // Fetch patient name for the reminder
                String patientName = "Patient";
                Patient p = databaseHelper.getPatientById(patientId);
                if (p != null)
                    patientName = p.getName();

                scheduleReminder(appointmentId, patientName, date, time);

                finish();
            } else {
                Toast.makeText(this, "Error updating appointment", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid IDs", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleReminder(int appointmentId, String patientName, String date, String time) {
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
                        this, appointmentId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    // Check if we can schedule exact alarms on Android 12+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis,
                                    pendingIntent);
                            Log.d("EditAppointment", "Alarm scheduled for: " + appointmentDate);
                            Toast.makeText(this, "Reminder updated", Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            Log.w("EditAppointment", "Cannot schedule exact alarms - permission denied");
                            new AlertDialog.Builder(this)
                                    .setTitle("Permission Required")
                                    .setMessage(
                                            "To receive timely appointment reminders, please allow 'Alarms & reminders' permission in Settings.")
                                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                                        Intent settingsIntent = new Intent(
                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                        settingsIntent.setData(Uri.parse("package:" + getPackageName()));
                                        startActivity(settingsIntent);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            Toast.makeText(this, "Reminder NOT set. Permission required.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                        Log.d("EditAppointment", "Alarm scheduled for: " + appointmentDate);
                        Toast.makeText(this, "Reminder updated", Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}