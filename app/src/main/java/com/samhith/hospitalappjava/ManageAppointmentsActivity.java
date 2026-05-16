package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentChange;
import java.util.ArrayList;
import java.util.List;

public class ManageAppointmentsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AppointmentAdapter adapter;
    private DatabaseHelper databaseHelper;
    private Button btnAddAppointment;
    private SearchView searchView;
    private List<Appointment> originalAppointments;
    private int userId;
    private String userRole;
    private ListenerRegistration cloudListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_appointments);

        // Get user info from intent
        userId = getIntent().getIntExtra("USER_ID", -1);
        userRole = getIntent().getStringExtra("USER_ROLE");

        if (userId == -1 || userRole == null) {
            Toast.makeText(this, "Invalid user info. Returning.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerViewAppointments);
        btnAddAppointment = findViewById(R.id.btnAddAppointment);
        searchView = findViewById(R.id.searchView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAppointments(newText);
                return true;
            }
        });

        if ("doctor".equalsIgnoreCase(userRole)) {
            btnAddAppointment.setVisibility(View.GONE);
        } else {
            btnAddAppointment.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddAppointmentActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            });
        }

        Button backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        loadAppointments();
    }

    private void setupRealTimeSync() {
        String email = getIntent().getStringExtra("USERNAME");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        com.google.firebase.firestore.Query query;
        if ("doctor".equalsIgnoreCase(userRole)) {
            query = db.collection("backup_appointments").whereEqualTo("doctor_email", email.toLowerCase().trim());
        } else {
            query = db.collection("backup_appointments");
        }

        cloudListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) return;
            if (snapshots != null) {
                boolean needsRefresh = false;
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.REMOVED) {
                        // Handle deletion from cloud
                        String pPhone = dc.getDocument().getString("patient_phone");
                        String dEmail = dc.getDocument().getString("doctor_email");
                        String date = dc.getDocument().getString("date");
                        String time = dc.getDocument().getString("time");
                        if (pPhone != null && dEmail != null && date != null && time != null) {
                            int id = databaseHelper.getAppointmentIdByNaturalKey(pPhone, dEmail, date, time);
                            if (id != -1) {
                                databaseHelper.deleteAppointment(id);
                                needsRefresh = true;
                            }
                        }
                    } else {
                        // Handle added or modified
                        needsRefresh = true; // For simplicity, trigger a reload for any cloud change
                    }
                }
                if (needsRefresh) loadAppointmentsFromLocal();
            }
        });
    }

    private void loadAppointments() {
        if ("doctor".equalsIgnoreCase(userRole)) {
            String email = getIntent().getStringExtra("USERNAME");
            FirestoreSyncManager.syncPendingAppointmentsForDoctor(this, email, userId, this::loadAppointmentsFromLocal);
        } else {
            loadAppointmentsFromLocal();
        }
        setupRealTimeSync();
    }

    private void loadAppointmentsFromLocal() {
        runOnUiThread(() -> {
            if ("doctor".equalsIgnoreCase(userRole)) {
                String email = getIntent().getStringExtra("USERNAME");
                originalAppointments = databaseHelper.getAppointmentsByDoctorEmail(email);
                originalAppointments.sort((a1, a2) -> {
                    if ("pending".equalsIgnoreCase(a1.getStatus()) && !"pending".equalsIgnoreCase(a2.getStatus())) return -1;
                    if (!"pending".equalsIgnoreCase(a1.getStatus()) && "pending".equalsIgnoreCase(a2.getStatus())) return 1;
                    return 0;
                });
            } else {
                originalAppointments = databaseHelper.getAllAppointmentsWithNames();
            }
            adapter = new AppointmentAdapter(originalAppointments, this, this::onAppointmentClick);
            recyclerView.setAdapter(adapter);
        });
    }

    private void filterAppointments(String query) {
        List<Appointment> filteredList = new ArrayList<>();
        query = query.toLowerCase();

        for (Appointment appointment : originalAppointments) {
            if (appointment.getPatientName().toLowerCase().contains(query) ||
                    appointment.getDoctorName().toLowerCase().contains(query)) {
                filteredList.add(appointment);
            }
        }

        adapter = new AppointmentAdapter(filteredList, this, this::onAppointmentClick);
        recyclerView.setAdapter(adapter);
    }

    private void onAppointmentClick(Appointment appointment) {
        Intent intent = new Intent(this, AppointmentDetailsActivity.class);
        intent.putExtra("APPOINTMENT_ID", appointment.getId());
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USER_ROLE", userRole);
        intent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME")); // Pass email!
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cloudListener != null) {
            cloudListener.remove();
        }
    }
}