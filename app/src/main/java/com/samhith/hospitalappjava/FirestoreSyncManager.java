package com.samhith.hospitalappjava;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import java.util.HashMap;
import java.util.Map;

public class FirestoreSyncManager {
    private static final String TAG = "FirestoreSyncManager";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void syncPatient(Patient p) {
        Map<String, Object> patientMap = new HashMap<>();
        patientMap.put("id", p.getId());
        patientMap.put("name", p.getName());
        patientMap.put("age", p.getAge());
        patientMap.put("gender", p.getGender());
        patientMap.put("address", p.getAddress());
        patientMap.put("phone", p.getPhone());
        patientMap.put("medical_history", p.getMedicalHistory());

        // Use Phone as document ID for consistency across devices
        db.collection("backup_patients").document(p.getPhone())
                .set(patientMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Patient synced successfully: " + p.getName()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync patient: " + p.getName(), e));
    }

    public static void syncAppointment(Appointment a, String patientPhone, String doctorEmail) {
        Map<String, Object> appointmentMap = new HashMap<>();
        appointmentMap.put("id", a.getId());
        appointmentMap.put("patient_id", a.getPatientId());
        appointmentMap.put("doctor_id", a.getDoctorId());
        // Use a combination of fields as document ID, force lowercase for consistency
        String cleanEmail = doctorEmail.toLowerCase().trim();
        String cleanPhone = patientPhone.trim();
        appointmentMap.put("doctor_email", cleanEmail);
        appointmentMap.put("patient_phone", cleanPhone);
        appointmentMap.put("date", a.getDate());
        appointmentMap.put("time", a.getTime());
        appointmentMap.put("purpose", a.getPurpose());
        appointmentMap.put("status", a.getStatus());
        appointmentMap.put("status_update_time", a.getStatusUpdateTime());

        // CRITICAL: Document ID must be IDENTICAL everywhere
        String cleanDate = a.getDate().replace("/", "-").replace(".", "-").trim();
        String cleanTime = a.getTime().replace(":", "-").trim();
        String docId = cleanPhone + "_" + cleanEmail + "_" + cleanDate + "_" + cleanTime;
        
        db.collection("backup_appointments").document(docId)
                .set(appointmentMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment synced successfully: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync appointment: " + docId, e));
    }

    public static void syncStaff(Staff s) {
        Map<String, Object> staffMap = new HashMap<>();
        staffMap.put("id", s.getId());
        staffMap.put("name", s.getName());
        staffMap.put("role", s.getRole());
        staffMap.put("department", s.getDepartment());
        staffMap.put("email", s.getEmail());
        staffMap.put("phone", s.getPhone());
        staffMap.put("join_date", s.getJoinDate());
        staffMap.put("address", s.getAddress());
        staffMap.put("specialization", s.getSpecialization());
        staffMap.put("photoPath", s.getPhotoPath());

        // Use Email as document ID for consistency across devices
        db.collection("backup_staff").document(s.getEmail())
                .set(staffMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Staff synced successfully: " + s.getName()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync staff: " + s.getName(), e));
    }

    public static void syncUser(String username, String password, String role, int staffId) {
        syncUserWithCallback(username, password, role, staffId, null);
    }

    public static void syncUserWithCallback(String username, String password, String role, int staffId, OnSyncCompleteListener listener) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("password", password);
        userMap.put("role", role);
        userMap.put("staff_id", staffId);
        userMap.put("isActive", true); // Default to active on creation

        db.collection("backup_users").document(username)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User synced successfully: " + username);
                    if (listener != null) listener.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync user: " + username, e);
                    if (listener != null) listener.onComplete(false);
                });
    }

    public interface OnSyncCompleteListener {
        void onComplete(boolean success);
    }

    public static void deletePatient(String phone) {
        db.collection("backup_patients").document(phone).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Patient deleted from cloud: " + phone))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete patient from cloud: " + phone, e));
    }
    
    public static void deleteAppointment(String patientPhone, String doctorEmail, String date, String time) {
        String docId = patientPhone + "_" + doctorEmail + "_" + date.replace("/", "-") + "_" + time.replace(":", "-");
        db.collection("backup_appointments").document(docId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment deleted from cloud: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete appointment from cloud: " + docId, e));
    }
    
    public static void deleteStaff(String email) {
        db.collection("backup_staff").document(email).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Staff deleted from cloud: " + email))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete staff from cloud: " + email, e));
    }
    
    public static void deleteUser(String username) {
        db.collection("backup_users").document(username).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User deleted from cloud: " + username))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete user from cloud: " + username, e));
    }

    public static void deactivateUser(String username) {
        db.collection("backup_users").document(username)
                .update("isActive", false)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User deactivated in cloud: " + username))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to deactivate user: " + username, e));
    }

    public static void downloadAllData(DatabaseHelper dbHelper, int currentUserId, Runnable onComplete) {
        Log.d(TAG, "Starting full data sync down...");
        
        // 0. Clear local data first to ensure deletions are synced
        dbHelper.clearSyncData();
        
        final int[] tasksPending = {3};
        Runnable checkComplete = () -> {
            tasksPending[0]--;
            if (tasksPending[0] <= 0) {
                Log.d(TAG, "All sync tasks completed.");
                if (onComplete != null) onComplete.run();
            }
        };

        // Ensure network is enabled before starting download
        db.enableNetwork().addOnCompleteListener(netTask -> {
            // 1. Staff
            db.collection("backup_staff").get()
                .addOnSuccessListener(snapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            String email = doc.getString("email");
                            if (email != null && dbHelper.getStaffIdByEmail(email) == -1) {
                                    dbHelper.addStaff(
                                        doc.getString("name"),
                                        doc.getString("role"),
                                        doc.getString("department"),
                                        email,
                                        doc.getString("phone"),
                                        doc.getString("join_date"),
                                        doc.getString("address"),
                                        doc.getString("specialization"),
                                        doc.getString("photoPath"),
                                        currentUserId
                                    );
                            }
                        } catch (Exception ex) { Log.e(TAG, "Staff sync error", ex); }
                    }
                    checkComplete.run();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "Staff fail", e); checkComplete.run(); });

            // 2. Patients
            db.collection("backup_patients").get()
                .addOnSuccessListener(snapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            String phone = doc.getString("phone");
                            if (phone != null && dbHelper.getPatientIdByPhone(phone) == -1) {
                                Long age = doc.getLong("age");
                                dbHelper.addPatient(
                                    doc.getString("name"),
                                    age != null ? age.intValue() : 0,
                                    doc.getString("gender"),
                                    doc.getString("address"),
                                    phone,
                                    doc.getString("medical_history"),
                                    currentUserId
                                );
                            }
                        } catch (Exception ex) { Log.e(TAG, "Patient sync error", ex); }
                    }
                    checkComplete.run();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "Patient fail", e); checkComplete.run(); });

            // 3. Appointments
            db.collection("backup_appointments").get()
                .addOnSuccessListener(snapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            String pPhone = doc.getString("patient_phone");
                            String dEmail = doc.getString("doctor_email");
                            
                            if (pPhone != null && dEmail != null) {
                                // Resolve local IDs from natural keys
                                int pId = dbHelper.getPatientIdByPhone(pPhone);
                                int dId = dbHelper.getStaffIdByEmail(dEmail);
                                
                                if (pId != -1 && dId != -1) {
                                    dbHelper.addAppointmentFromSync(
                                        pId,
                                        dId,
                                        doc.getString("date"),
                                        doc.getString("time"),
                                        doc.getString("purpose"),
                                        doc.getString("status"),
                                        doc.getString("status_update_time"),
                                        currentUserId
                                    );
                                }
                            }
                        } catch (Exception ex) { Log.e(TAG, "Appointment sync error", ex); }
                    }
                    checkComplete.run();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "Appt fail", e); checkComplete.run(); });
        });
    }

    public static void syncPendingAppointmentsForDoctor(android.content.Context context, String doctorEmail, int currentUserId, Runnable onComplete) {
        String cleanEmail = doctorEmail.toLowerCase().trim();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        db.collection("backup_appointments")
                .whereEqualTo("doctor_email", cleanEmail)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            String pPhone = doc.getString("patient_phone");
                            String dEmail = doc.getString("doctor_email");

                            if (pPhone != null && dEmail != null) {
                                int pId = dbHelper.getPatientIdByPhone(pPhone);
                                int dId = dbHelper.getStaffIdByEmail(dEmail);

                                if (pId != -1 && dId != -1) {
                                    dbHelper.addAppointmentFromSync(
                                            pId, dId,
                                            doc.getString("date"),
                                            doc.getString("time"),
                                            doc.getString("purpose"),
                                            doc.getString("status"),
                                            doc.getString("status_update_time"),
                                            currentUserId
                                    );
                                }
                            }
                        } catch (Exception ex) { Log.e(TAG, "Doctor sync error", ex); }
                    }
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fail sync doctor", e);
                    if (onComplete != null) onComplete.run();
                });
    }
}
