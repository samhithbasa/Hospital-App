package com.samhith.hospitalappjava;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "appointment_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String patientName = intent.getStringExtra("PATIENT_NAME");
        String patientPhone = intent.getStringExtra("PATIENT_PHONE");
        String appointmentTime = intent.getStringExtra("APPOINTMENT_TIME");
        String message = "Reminder: You have an appointment at " + appointmentTime;

        // Send SMS if permission is granted and phone number is available
        if (patientPhone != null && !patientPhone.isEmpty()) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, 
                    android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
                    smsManager.sendTextMessage(patientPhone, null, message, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Appointment Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Appointment Reminder")
                .setContentText("Appointment with " + patientName + " at " + appointmentTime)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
