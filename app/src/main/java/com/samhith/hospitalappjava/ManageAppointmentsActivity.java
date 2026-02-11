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

        btnAddAppointment.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddAppointmentActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        Button backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        loadAppointments();
    }

    private void loadAppointments() {
        if (userRole.equals("admin")) {
            originalAppointments = databaseHelper.getAllAppointmentsWithNames();
        } else if (userRole.equals("doctor")) {
            originalAppointments = databaseHelper.getAppointmentsByDoctorId(userId);
        } else {
            originalAppointments = databaseHelper.getAppointmentsByUserId(userId);
        }

        adapter = new AppointmentAdapter(originalAppointments, this, this::onAppointmentClick);
        recyclerView.setAdapter(adapter);
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
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }
}