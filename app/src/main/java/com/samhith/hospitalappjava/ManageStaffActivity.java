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
import java.util.ArrayList;
import java.util.List;

public class ManageStaffActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int userId;
    private String userRole;

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
    }

    private void refreshStaffList(ListView staffListView) {
        List<Staff> staffList = dbHelper.getAllStaff();  // ✅ show all
        StaffAdapter adapter = new StaffAdapter(this, staffList);
        staffListView.setAdapter(adapter);
    }


    private void showStaffDetailsDialog(Staff staff) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Staff Details")
                .setMessage("Name: " + staff.getName() + "\n" +
                        "Role: " + staff.getRole() + "\n" +
                        "Department: " + staff.getDepartment() + "\n" +
                        "Email: " + staff.getEmail() + "\n" +
                        "Phone: " + staff.getPhone() + "\n" +
                        "Join Date: " + staff.getJoinDate() + "\n" +
                        "Address: " + staff.getAddress())
                .setPositiveButton("Edit", (dialog, which) -> {
                    Intent intent = new Intent(this, EditStaffActivity.class);
                    intent.putExtra("STAFF_ID", staff.getId());
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                });
                builder.setNegativeButton("Delete", (dialog, which) ->
                    new AlertDialog.Builder(this)
                            .setTitle("Confirm Delete")
                            .setMessage("Are you sure you want to delete this staff member?")
                            .setPositiveButton("Yes", (dialogInterface, i) -> {
                                dbHelper.deleteStaff(staff.getId());
                                refreshStaffList((ListView) findViewById(R.id.staffListView));
                            })
                            .setNegativeButton("No", null)
                            .show());


        builder.setNeutralButton("Back", null)
                .create()
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStaffList((ListView) findViewById(R.id.staffListView));
    }
}