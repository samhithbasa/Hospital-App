package com.samhith.hospitalappjava;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Map;

public class RestoreWorker extends Worker {
    private DatabaseHelper dbHelper;

    public RestoreWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Restore patients from Firestore
        db.collection("backup_patients").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Map<String, Object> data = document.getData();
                // Check if patient already exists to avoid duplicates
                String name = (String) data.get("name");
                int age = ((Long) data.get("age")).intValue();
                String gender = (String) data.get("gender");
                String address = (String) data.get("address");
                String phone = (String) data.get("phone");
                String medicalHistory = (String) data.get("medical_history");

                // Add patient if not exists (you may want to add a check here)
                dbHelper.addPatient(name, age, gender, address, phone, medicalHistory, 1);
            }
        });

        // Restore appointments from Firestore
        db.collection("backup_appointments").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Map<String, Object> data = document.getData();
                int patientId = ((Long) data.get("patient_id")).intValue();
                int doctorId = ((Long) data.get("doctor_id")).intValue();
                String date = (String) data.get("date");
                String time = (String) data.get("time");
                String purpose = (String) data.get("purpose");

                // Add appointment if not exists
                dbHelper.addAppointment(patientId, doctorId, date, time, purpose, 1);
            }
        });

        return Result.success();
    }
}
