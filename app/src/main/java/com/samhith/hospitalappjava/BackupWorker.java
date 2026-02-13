package com.samhith.hospitalappjava;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupWorker extends Worker {
    private DatabaseHelper dbHelper;

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Example: Sync patients
        List<Patient> patients = dbHelper.getAllPatients();
        for (Patient p : patients) {
            Map<String, Object> patientMap = new HashMap<>();
            patientMap.put("name", p.getName());
            patientMap.put("age", p.getAge());
            patientMap.put("gender", p.getGender());
            patientMap.put("address", p.getAddress());
            patientMap.put("phone", p.getPhone());
            patientMap.put("medical_history", p.getMedicalHistory());

            db.collection("backup_patients").document(String.valueOf(p.getId()))
                    .set(patientMap);
        }

        // Example: Sync appointments
        List<Appointment> appointments = dbHelper.getAllAppointments();
        for (Appointment a : appointments) {
            Map<String, Object> appointmentMap = new HashMap<>();
            appointmentMap.put("patient_id", a.getPatientId());
            appointmentMap.put("doctor_id", a.getDoctorId());
            appointmentMap.put("date", a.getDate());
            appointmentMap.put("time", a.getTime());
            appointmentMap.put("purpose", a.getPurpose());
            appointmentMap.put("status", a.getStatus());

            db.collection("backup_appointments").document(String.valueOf(a.getId()))
                    .set(appointmentMap);
        }

        return Result.success();
    }
}
