package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class ManageStaffActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int userId;
    private String userRole;
    private ListenerRegistration staffListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_staff);

        // Get user info from intent
        userId = getIntent().getIntExtra("USER_ID", -1);
        userRole = getIntent().getStringExtra("USER_ROLE");

        if (userId == -1 || userRole == null) {
            Toast.makeText(this, "Invalid user info. Returning.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        ListView staffListView = findViewById(R.id.staffListView);
        Button addStaffBtn = findViewById(R.id.addStaffBtn);
        Button backBtn = findViewById(R.id.backBtn);
        TextView emptyView = findViewById(R.id.emptyView);

        // Set empty view
        staffListView.setEmptyView(emptyView);

        // Load staff data
        refreshStaffList(staffListView);

        // Button click listeners
        addStaffBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddStaffActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        backBtn.setOnClickListener(v -> finish());

        // List item click listener
        staffListView.setOnItemClickListener((parent, view, position, id) -> {
            Staff selectedStaff = (Staff) parent.getItemAtPosition(position);
            showStaffDetailsDialog(selectedStaff);
        });

        setupRealTimeSync(staffListView);
    }

    private void setupRealTimeSync(ListView staffListView) {
        staffListener = FirebaseFirestore.getInstance()
                .collection("backup_staff")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        boolean needsRefresh = false;
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.REMOVED) {
                                String email = dc.getDocument().getString("email");
                                if (email != null) {
                                    int id = dbHelper.getStaffIdByEmail(email);
                                    if (id != -1) {
                                        dbHelper.deleteStaff(id);
                                        needsRefresh = true;
                                    }
                                }
                            } else {
                                needsRefresh = true;
                            }
                        }
                        if (needsRefresh) refreshStaffList(staffListView);
                    }
                });
    }

    private void refreshStaffList(ListView staffListView) {
        List<Staff> allStaff = dbHelper.getAllStaff();
        List<Staff> doctorsOnly = new ArrayList<>();
        
        for (Staff s : allStaff) {
            if ("doctor".equalsIgnoreCase(s.getRole())) {
                doctorsOnly.add(s);
            }
        }
        
        StaffAdapter adapter = new StaffAdapter(this, doctorsOnly);
        staffListView.setAdapter(adapter);
    }


    private void showStaffDetailsDialog(Staff staff) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_item_details, null);
        builder.setView(dialogView);
        
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogContent = dialogView.findViewById(R.id.dialogContent);
        Button btnBack = dialogView.findViewById(R.id.btnBack);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnEdit = dialogView.findViewById(R.id.btnEdit);
        
        dialogTitle.setText("Staff Profile");
        
        StringBuilder content = new StringBuilder();
        content.append("Name: ").append(staff.getName()).append("\n");
        content.append("Role: ").append(staff.getRole()).append("\n");
        content.append("Department: ").append(staff.getDepartment()).append("\n");
        content.append("Email: ").append(staff.getEmail()).append("\n");
        content.append("Phone: ").append(staff.getPhone()).append("\n");
        content.append("Join Date: ").append(staff.getJoinDate()).append("\n");
        content.append("Address: ").append(staff.getAddress());
        if (staff.getSpecialization() != null && !staff.getSpecialization().isEmpty()) {
            content.append("\nSpecialization: ").append(staff.getSpecialization());
        }
        
        dialogContent.setText(content.toString());
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        btnBack.setOnClickListener(v -> dialog.dismiss());
        
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, EditStaffActivity.class);
            intent.putExtra("STAFF_ID", staff.getId());
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });
        
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteStaff(staff);
        });
        
        dialog.show();
    }

    private void confirmDeleteStaff(Staff staff) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this staff member?")
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
                    pd.setMessage("Deleting staff and user account...");
                    pd.setCancelable(false);
                    pd.show();
                    
                    if (dbHelper.deleteStaff(staff.getId())) {
                        FirestoreSyncManager.deleteStaff(staff.getEmail());
                        
                        if (staff.getEmail() != null && !staff.getEmail().isEmpty()) {
                            dbHelper.deleteUser(staff.getEmail());
                            FirestoreSyncManager.deactivateUser(staff.getEmail()); 
                        }
                        
                        new android.os.Handler().postDelayed(() -> {
                            pd.dismiss();
                            Toast.makeText(this, "Staff member deleted successfully", Toast.LENGTH_SHORT).show();
                            refreshStaffList((ListView) findViewById(R.id.staffListView));
                        }, 1500);
                    } else {
                        pd.dismiss();
                        Toast.makeText(this, "Failed to delete staff member", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStaffList((ListView) findViewById(R.id.staffListView));
    }
}