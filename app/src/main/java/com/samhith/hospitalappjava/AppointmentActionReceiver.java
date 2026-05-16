package com.samhith.hospitalappjava;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;
import androidx.core.app.NotificationManagerCompat;

public class AppointmentActionReceiver extends BroadcastReceiver {
    public static final String ACTION_ACCEPT = "com.samhith.hospitalappjava.ACTION_ACCEPT";
    public static final String ACTION_CANCEL = "com.samhith.hospitalappjava.ACTION_CANCEL";
    private static final String TAG = "AppActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        android.util.Log.d(TAG, "Notification Action Received: " + intent.getAction());
        int appointmentId = intent.getIntExtra("appointment_id", -1);
        String patientPhone = intent.getStringExtra("patient_phone");
        String patientName = intent.getStringExtra("patient_name");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");
        String doctorEmail = intent.getStringExtra("doctor_email");
        int notificationId = intent.getIntExtra("notification_id", -1);

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String action = intent.getAction();

        if (ACTION_ACCEPT.equals(action)) {
            // Resolve local ID if not valid
            if (appointmentId == -1 || dbHelper.getAppointmentById(appointmentId) == null) {
                appointmentId = dbHelper.getAppointmentIdByNaturalKey(patientPhone, doctorEmail, date, time);
            }

            if (appointmentId != -1) {
                // Update status to scheduled
                dbHelper.updateAppointmentStatus(appointmentId, "scheduled");
                
                // Sync to Firestore
                Appointment app = dbHelper.getAppointmentWithNamesById(appointmentId);
                if (app != null) {
                    FirestoreSyncManager.syncAppointment(app, patientPhone, doctorEmail);
                }
            }

            // Send Confirmation SMS
            if (patientPhone != null && !patientPhone.isEmpty()) {
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    String message = "Hello " + patientName + ", your appointment at Trinity Care on " + date + " at " + time + " has been CONFIRMED by the doctor.";
                    smsManager.sendTextMessage(patientPhone, null, message, null, null);
                    Toast.makeText(context, "Appointment Accepted & SMS Sent", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Accepted, but SMS failed", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (ACTION_CANCEL.equals(action)) {
            // Resolve local ID if not valid
            if (appointmentId == -1 || dbHelper.getAppointmentById(appointmentId) == null) {
                appointmentId = dbHelper.getAppointmentIdByNaturalKey(patientPhone, doctorEmail, date, time);
            }

            if (appointmentId != -1) {
                // Update status to canceled
                dbHelper.updateAppointmentStatus(appointmentId, "canceled");
                
                // Sync to Firestore
                Appointment app = dbHelper.getAppointmentWithNamesById(appointmentId);
                if (app != null) {
                    FirestoreSyncManager.syncAppointment(app, patientPhone, doctorEmail);
                }
                if (patientPhone != null && !patientPhone.isEmpty()) {
                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        String message = "Hello " + patientName + ", unfortunately your appointment at Trinity Care on " + date + " at " + time + " has been CANCELED by the doctor.";
                        smsManager.sendTextMessage(patientPhone, null, message, null, null);
                        Toast.makeText(context, "Appointment Canceled & SMS Sent", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Canceled, but SMS failed", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(context, "Appointment Canceled", Toast.LENGTH_SHORT).show();
            }
        }

        // Dismiss the notification
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }
    }
}
