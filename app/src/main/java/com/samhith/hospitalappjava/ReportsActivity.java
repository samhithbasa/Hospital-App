package com.samhith.hospitalappjava;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private TextView tvPatientCount, tvStaffCount, tvAppointmentCount;
    private TextView tvDoctorCount, tvNurseCount;
    private TextView tvUpcomingAppointments, tvCompletedAppointments, tvCanceledAppointments;
    private int userId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        databaseHelper = new DatabaseHelper(this);

        tvPatientCount = findViewById(R.id.tvPatientCount);
        tvStaffCount = findViewById(R.id.tvStaffCount);
        tvDoctorCount = findViewById(R.id.tvDoctorCount);
        tvNurseCount = findViewById(R.id.tvNurseCount);
        tvAppointmentCount = findViewById(R.id.tvAppointmentCount);
        tvUpcomingAppointments = findViewById(R.id.tvUpcomingAppointments);
        tvCompletedAppointments = findViewById(R.id.tvCompletedAppointments);
        tvCanceledAppointments = findViewById(R.id.tvCanceledAppointments);

        userId = getIntent().getIntExtra("USER_ID", -1);
        userRole = getIntent().getStringExtra("USER_ROLE");

        if (userId == -1 || userRole == null) {
            Toast.makeText(this, "Invalid user session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadReports();

        findViewById(R.id.exportPdfBtn).setOnClickListener(v -> generatePDF());
    }

    private void loadReports() {
        loadBasicCounts();
        loadUpcomingAppointments();
        loadCompletedAppointments();
        loadCanceledAppointments();
    }

    private void loadBasicCounts() {
        List<Patient> patients;
        List<Appointment> appointments;

        List<Staff> staffList = databaseHelper.getAllStaff();
        int doctorCount = 0;
        int nurseCount = 0;

        for (Staff s : staffList) {
            if (s.getRole().equalsIgnoreCase("doctor"))
                doctorCount++;
            else if (s.getRole().equalsIgnoreCase("nurse"))
                nurseCount++;
        }

        if ("doctor".equalsIgnoreCase(userRole)) {
            String username = getIntent().getStringExtra("USERNAME");
            int staffId = databaseHelper.getStaffIdForUser(username);
            patients = databaseHelper.getPatientsByDoctorId(staffId);
            appointments = databaseHelper.getAppointmentsByDoctorId(staffId);
        } else {
            // Receptionists/Admins see all
            patients = databaseHelper.getAllPatients();
            appointments = databaseHelper.getAllAppointmentsWithNames();
        }

        tvPatientCount.setText(getString(R.string.patient_count, patients.size()));
        tvStaffCount.setText(getString(R.string.staff_count, staffList.size()));
        tvDoctorCount.setText(getString(R.string.doctor_count, doctorCount));
        tvNurseCount.setText(getString(R.string.nurse_count, nurseCount));
        tvAppointmentCount.setText(getString(R.string.appointment_count, appointments.size()));
    }

    private void loadUpcomingAppointments() {
        List<Appointment> appointments = getFilteredAppointments("scheduled");
        displayAppointments(appointments, tvUpcomingAppointments, true);
    }

    private void loadCompletedAppointments() {
        List<Appointment> appointments = getFilteredAppointments("completed");
        displayAppointments(appointments, tvCompletedAppointments, false);
    }

    private void loadCanceledAppointments() {
        List<Appointment> appointments = getFilteredAppointments("canceled");
        displayAppointments(appointments, tvCanceledAppointments, false);
    }

    private List<Appointment> getFilteredAppointments(String status) {
        if ("doctor".equalsIgnoreCase(userRole)) {
            String username = getIntent().getStringExtra("USERNAME");
            int staffId = databaseHelper.getStaffIdForUser(username);
            return filterByStatus(databaseHelper.getAppointmentsByDoctorId(staffId), status);
        } else {
            // Receptionists/Admins
            return databaseHelper.getAppointmentsByStatus(status);
        }
    }

    private void displayAppointments(List<Appointment> appointments, TextView targetView, boolean isUpcoming) {
        if (appointments.isEmpty()) {
            targetView.setText(isUpcoming ? R.string.no_upcoming_appointments : R.string.no_completed_appointments);
        } else {
            StringBuilder builder = new StringBuilder();
            for (Appointment a : appointments) {
                builder.append(formatAppointmentDetails(a, isUpcoming));
            }
            targetView.setText(builder.toString());
        }
    }

    private List<Appointment> filterByStatus(List<Appointment> all, String status) {
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment a : all) {
            if (status.equalsIgnoreCase(a.getStatus())) {
                filtered.add(a);
            }
        }
        return filtered;
    }

    private String formatAppointmentDetails(Appointment a, boolean isUpcoming) {
        String patient = a.getPatientName() != null ? a.getPatientName() : "Unknown";
        String doctor = a.getDoctorName() != null ? a.getDoctorName() : "Unknown";
        String date = a.getDate() != null ? a.getDate() : "N/A";
        String timeInfo = isUpcoming
                ? getString(R.string.scheduled_for_time, a.getTime() != null ? a.getTime() : "N/A")
                : getString(R.string.status_updated_at,
                        a.getStatusUpdateTime() != null ? a.getStatusUpdateTime() : "N/A");

        return "• " + patient + "\n" +
                "  " + getString(R.string.with_doctor, doctor) + "\n" +
                "  " + getString(R.string.on_date, date) + "\n" +
                "  " + timeInfo + "\n\n";
    }

    private void generatePDF() {
        PdfDocument document = new PdfDocument();
        final int pageWidth = 595;
        final int pageHeight = 842;

        // Inner class to track current page drawing state
        class PageState {
            PdfDocument.Page page;
            Canvas canvas;
            int pageNum = 1;
            int currentY = 135;

            PageState(PdfDocument doc) {
                startNewPage(doc);
            }

            void startNewPage(PdfDocument doc) {
                if (page != null) {
                    drawFooter(page.getCanvas(), pageNum);
                    doc.finishPage(page);
                }
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                page = doc.startPage(pageInfo);
                canvas = page.getCanvas();
                currentY = 60; // Top margin for subsequent pages
                drawHeader(canvas, pageNum);
            }

            void drawHeader(Canvas canvas, int num) {
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

                if (num == 1) {
                    // First page: Main Title
                    paint.setColor(0xFF0F172A); // Slate 900
                    paint.setTextSize(20f);
                    paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                    canvas.drawText("HOSPITAL STATUS REPORT", 40, 65, paint);

                    paint.setColor(0xFF64748B); // Slate 500
                    paint.setTextSize(9f);
                    paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                    canvas.drawText("Generated via Receptionist Portal", 40, 80, paint);

                    // Underline Accent in Hospital Blue
                    paint.setColor(0xFF0284C7); // Sky Blue 600
                    paint.setStrokeWidth(3f);
                    canvas.drawLine(40, 92, 555, 92, paint);

                    // Metadata right side
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                    String dateStr = sdf.format(new java.util.Date());
                    paint.setColor(0xFF475569); // Slate 600
                    paint.setTextSize(9f);
                    paint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText("Date: " + dateStr, 555, 70, paint);
                    canvas.drawText("Scope: SYSTEM REPORT", 555, 85, paint);
                    paint.setTextAlign(Paint.Align.LEFT); // Reset

                    currentY = 115;
                } else {
                    // Subsequent pages: Minimal header
                    paint.setColor(0xFF475569); // Slate 600
                    paint.setTextSize(10f);
                    paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                    canvas.drawText("HOSPITAL STATUS REPORT (Continued)", 40, 50, paint);

                    paint.setColor(0xFFE2E8F0); // Slate 200 divider
                    paint.setStrokeWidth(1f);
                    canvas.drawLine(40, 58, 555, 58, paint);

                    currentY = 80;
                }
            }

            void drawFooter(Canvas canvas, int num) {
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                paint.setTextSize(8f);
                paint.setColor(0xFF94A3B8); // Slate 400

                // Divider line
                paint.setStrokeWidth(0.8f);
                canvas.drawLine(40, 795, 555, 795, paint);

                // Footer content
                canvas.drawText("Confidential - For Internal Administrative Use Only", 40, 810, paint);

                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("Page " + num, 555, 810, paint);
            }

            void checkPageOverflow(PdfDocument doc, int neededHeight) {
                if (currentY + neededHeight > 780) {
                    pageNum++;
                    startNewPage(doc);
                }
            }

            void drawAppointmentSection(PdfDocument doc, String sectionTitle, List<Appointment> appointments, int accentColor, boolean isUpcoming) {
                checkPageOverflow(doc, 45);

                Paint paint = new Paint();
                paint.setAntiAlias(true);

                // Left vertical color accent bar
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(accentColor);
                canvas.drawRect(40, currentY, 44, currentY + 14, paint);

                // Section title
                paint.setColor(0xFF1E293B); // Slate 800
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                paint.setTextSize(11f);
                canvas.drawText(sectionTitle + " (" + appointments.size() + ")", 52, currentY + 11, paint);

                currentY += 22;

                // Table Header background
                checkPageOverflow(doc, 25);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFFF1F5F9); // Slate 100
                canvas.drawRect(40, currentY, 555, currentY + 18, paint);

                // Table Header text
                paint.setColor(0xFF475569); // Slate 600
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                paint.setTextSize(8f);
                canvas.drawText("Patient", 48, currentY + 12, paint);
                canvas.drawText("Doctor", 160, currentY + 12, paint);
                canvas.drawText("Date & Time", 282, currentY + 12, paint);
                canvas.drawText(isUpcoming ? "Scheduled Time" : "Updated At", 412, currentY + 12, paint);

                currentY += 23;

                // Table Rows
                if (appointments.isEmpty()) {
                    checkPageOverflow(doc, 25);
                    paint.setColor(0xFF94A3B8); // Slate 400
                    paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                    paint.setTextSize(9f);
                    canvas.drawText("No appointments in this category", 48, currentY + 12, paint);
                    currentY += 25;
                } else {
                    for (Appointment a : appointments) {
                        checkPageOverflow(doc, 23);

                        // Draw clean white background for the row
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(0xFFFFFFFF);
                        canvas.drawRect(40, currentY, 555, currentY + 20, paint);

                        // Text fields
                        paint.setColor(0xFF334155); // Slate 700
                        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                        paint.setTextSize(9f);

                        String patientName = a.getPatientName() != null ? a.getPatientName() : "Unknown";
                        String doctorName = a.getDoctorName() != null ? a.getDoctorName() : "Unknown";
                        String dateStr = a.getDate() != null ? a.getDate() : "N/A";
                        String rightColStr = isUpcoming
                            ? (a.getTime() != null ? a.getTime() : "N/A")
                            : (a.getStatusUpdateTime() != null ? a.getStatusUpdateTime() : "N/A");

                        canvas.drawText(truncate(patientName, 18), 48, currentY + 13, paint);
                        canvas.drawText("Dr. " + truncate(doctorName, 18), 160, currentY + 13, paint);
                        canvas.drawText(truncate(dateStr, 15), 282, currentY + 13, paint);
                        canvas.drawText(truncate(rightColStr, 22), 412, currentY + 13, paint);

                        // Thin separator line at the bottom of the row
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(0.5f);
                        paint.setColor(0xFFF1F5F9); // Light divider
                        canvas.drawLine(40, currentY + 20, 555, currentY + 20, paint);

                        currentY += 21;
                    }
                    currentY += 5;
                }
                currentY += 10;
            }
        }

        PageState state = new PageState(document);
        Canvas canvas = state.canvas;
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Fetch counts for cards
        List<Staff> staffList = databaseHelper.getAllStaff();
        int doctorCount = 0;
        int nurseCount = 0;
        for (Staff s : staffList) {
            if (s.getRole().equalsIgnoreCase("doctor")) doctorCount++;
            else if (s.getRole().equalsIgnoreCase("nurse")) nurseCount++;
        }

        List<Patient> patientsList;
        List<Appointment> allAppointmentsList;
        if ("doctor".equalsIgnoreCase(userRole)) {
            String username = getIntent().getStringExtra("USERNAME");
            int staffId = databaseHelper.getStaffIdForUser(username);
            patientsList = databaseHelper.getPatientsByDoctorId(staffId);
            allAppointmentsList = databaseHelper.getAppointmentsByDoctorId(staffId);
        } else {
            patientsList = databaseHelper.getAllPatients();
            allAppointmentsList = databaseHelper.getAllAppointmentsWithNames();
        }

        int patientCount = patientsList.size();
        int staffCount = staffList.size();
        int appointmentCount = allAppointmentsList.size();

        // Draw the 3-column metric cards
        int cardY = state.currentY;
        int cardHeight = 70;
        int cardWidth = 160;
        int gap = 17;
        int[] cardX = { 40, 40 + cardWidth + gap, 40 + 2 * (cardWidth + gap) };

        for (int i = 0; i < 3; i++) {
            RectF cardRect = new RectF(cardX[i], cardY, cardX[i] + cardWidth, cardY + cardHeight);

            // Card background Slate 50
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFF8FAFC);
            canvas.drawRoundRect(cardRect, 6f, 6f, paint);

            // Card border Slate 200
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(0xFFE2E8F0);
            canvas.drawRoundRect(cardRect, 6f, 6f, paint);
        }

        // Card Content Drawing
        paint.setStyle(Paint.Style.FILL);

        // Card 1: Patients
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setColor(0xFF64748B); // Slate 500
        paint.setTextSize(8f);
        canvas.drawText("TOTAL PATIENTS", cardX[0] + 12, cardY + 20, paint);

        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setColor(0xFF0F172A); // Slate 900
        paint.setTextSize(20f);
        canvas.drawText(String.valueOf(patientCount), cardX[0] + 12, cardY + 45, paint);

        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setColor(0xFF94A3B8); // Slate 400
        paint.setTextSize(8f);
        canvas.drawText("Registered Patients", cardX[0] + 12, cardY + 58, paint);

        // Card 2: Staff
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setColor(0xFF64748B); // Slate 500
        paint.setTextSize(8f);
        canvas.drawText("TOTAL STAFF", cardX[1] + 12, cardY + 20, paint);

        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setColor(0xFF0F172A); // Slate 900
        paint.setTextSize(20f);
        canvas.drawText(String.valueOf(staffCount), cardX[1] + 12, cardY + 45, paint);

        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setColor(0xFF64748B); // Slate 500
        paint.setTextSize(8f);
        canvas.drawText("Dr: " + doctorCount + " | Nurse: " + nurseCount, cardX[1] + 12, cardY + 58, paint);

        // Card 3: Appointments
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setColor(0xFF64748B); // Slate 500
        paint.setTextSize(8f);
        canvas.drawText("TOTAL APPOINTMENTS", cardX[2] + 12, cardY + 20, paint);

        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setColor(0xFF0284C7); // Sky Blue 600
        paint.setTextSize(20f);
        canvas.drawText(String.valueOf(appointmentCount), cardX[2] + 12, cardY + 45, paint);

        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setColor(0xFF94A3B8); // Slate 400
        paint.setTextSize(8f);
        canvas.drawText("Active Bookings", cardX[2] + 12, cardY + 58, paint);

        state.currentY += cardHeight + 25;

        // Fetch and draw the structured tables for each category
        List<Appointment> upcoming = getFilteredAppointments("scheduled");
        List<Appointment> completed = getFilteredAppointments("completed");
        List<Appointment> canceled = getFilteredAppointments("canceled");

        state.drawAppointmentSection(document, "Upcoming Appointments", upcoming, 0xFF0284C7, true);
        state.drawAppointmentSection(document, "Completed Appointments", completed, 0xFF10B981, false);
        state.drawAppointmentSection(document, "Canceled Appointments", canceled, 0xFFEF4444, false);

        // Draw last page footer and finish
        state.drawFooter(state.page.getCanvas(), state.pageNum);
        document.finishPage(state.page);

        String fileName = "Hospital_Report_" + System.currentTimeMillis() + ".pdf";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        try {
            if (uri != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                document.writeTo(out);
                document.close();
                out.close();
                Toast.makeText(this, "PDF Exported to Downloads", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }
}
