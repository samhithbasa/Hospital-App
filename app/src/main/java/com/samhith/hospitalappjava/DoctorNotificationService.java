package com.samhith.hospitalappjava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class DoctorNotificationService extends Service {
    private static final String TAG = "DoctorNotifyService";
    private static final String CHANNEL_ID = "DoctorAppointments";
    private ListenerRegistration appointmentListener;
    private DatabaseHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        dbHelper = new DatabaseHelper(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String persistentEmail = prefs.getString("persistent_doctor_email", "");
        Log.d(TAG, "onStartCommand persistentEmail: " + persistentEmail);

        if (!persistentEmail.isEmpty()) {
            startDoctorAppointmentListener(persistentEmail.toLowerCase().trim());
            
            // Running as a foreground service to ensure it stays alive
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Hospital App Active")
                    .setContentText("Listening for new appointment requests...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
            startForeground(101, notification);
        } else {
            stopSelf();
        }

        return START_STICKY;
    }

    private void startDoctorAppointmentListener(String doctorEmail) {
        Log.d(TAG, "Background listener disabled as requested by user. Using SMS + In-App flow.");
    }

    private void showAppointmentNotification(int appointmentId, String patientName, String patientPhone, String date, String time, String purpose, String doctorEmail) {
        Log.d(TAG, "Showing notification for appointment: " + appointmentId + " patient: " + patientName);
        // Accept Intent
        Intent acceptIntent = new Intent(this, AppointmentActionReceiver.class);
        acceptIntent.setAction(AppointmentActionReceiver.ACTION_ACCEPT);
        acceptIntent.putExtra("appointment_id", appointmentId);
        acceptIntent.putExtra("patient_phone", patientPhone);
        acceptIntent.putExtra("doctor_email", doctorEmail);
        acceptIntent.putExtra("patient_name", patientName);
        acceptIntent.putExtra("date", date);
        acceptIntent.putExtra("time", time);
        acceptIntent.putExtra("notification_id", appointmentId);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, appointmentId, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        // Cancel Intent
        Intent cancelIntent = new Intent(this, AppointmentActionReceiver.class);
        cancelIntent.setAction(AppointmentActionReceiver.ACTION_CANCEL);
        cancelIntent.putExtra("appointment_id", appointmentId);
        cancelIntent.putExtra("notification_id", appointmentId);
        cancelIntent.putExtra("patient_phone", patientPhone);
        cancelIntent.putExtra("doctor_email", doctorEmail);
        cancelIntent.putExtra("patient_name", patientName);
        cancelIntent.putExtra("date", date);
        cancelIntent.putExtra("time", time);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, appointmentId + 1000, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("New Appointment Request")
                .setContentText("Patient: " + patientName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_input_add, "Accept", acceptPendingIntent)
                .addAction(android.R.drawable.ic_delete, "Cancel", cancelPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(appointmentId, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Doctor Appointments",
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (appointmentListener != null) appointmentListener.remove();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
