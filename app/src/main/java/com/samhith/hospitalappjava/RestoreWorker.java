package com.samhith.hospitalappjava;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.tasks.Tasks;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class RestoreWorker extends Worker {
    private DatabaseHelper dbHelper;
    private int userId;

    public RestoreWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        dbHelper = new DatabaseHelper(context);
        // In a real app, you'd retrieve the logged-in user ID from SharedPreferences or
        // WorkerParameters
        userId = 1;
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        try {
            // Restore patients synchronously
            Tasks.await(db.collection("backup_patients").get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Map<String, Object> data = document.getData();
                    // Check if patient already exists to avoid duplicates
                    String name = (String) data.get("name");
                    Long ageLong = (Long) data.get("age");
                    int age = ageLong != null ? ageLong.intValue() : 0;
                    String gender = (String) data.get("gender");
                    String address = (String) data.get("address");
                    String phone = (String) data.get("phone");
                    String medicalHistory = (String) data.get("medical_history");

                    // Add patient using the Correct signature for DatabaseHelper
                    dbHelper.addPatient(name, age, gender, address, phone, medicalHistory, userId);
                }
            }));

            // Restore appointments synchronously
            Tasks.await(db.collection("backup_appointments").get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Map<String, Object> data = document.getData();
                    Long pIdLong = (Long) data.get("patient_id");
                    int patientId = pIdLong != null ? pIdLong.intValue() : 0;

                    Long dIdLong = (Long) data.get("doctor_id");
                    int doctorId = dIdLong != null ? dIdLong.intValue() : 0;

                    String date = (String) data.get("date");
                    String time = (String) data.get("time");
                    String purpose = (String) data.get("purpose");

                    // Add appointment using the Correct signature for DatabaseHelper
                    dbHelper.addAppointment(patientId, doctorId, date, time, purpose, userId);
                }
            }));

            return Result.success();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}
