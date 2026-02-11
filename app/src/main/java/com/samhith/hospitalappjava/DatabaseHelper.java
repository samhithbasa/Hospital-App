package com.samhith.hospitalappjava;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteConstraintException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "hospital.db";
    private static final int DATABASE_VERSION = 8;

    // Table names - changed to public
    public static final String TABLE_USERS = "users";
    public static final String TABLE_PATIENTS = "patients";
    public static final String TABLE_APPOINTMENTS = "appointments";
    private static final String TABLE_STAFF = "staff";

    // Common column names
    private static final String KEY_ID = "id";

    // Users table columns
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NAME = "name";

    // Patients table columns
    private static final String KEY_PATIENT_NAME = "name";
    private static final String KEY_AGE = "age";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_MEDICAL_HISTORY = "medical_history";

    // Appointments table columns
    private static final String KEY_PATIENT_ID = "patient_id";
    private static final String KEY_DOCTOR_ID = "doctor_id";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIME = "time";
    private static final String KEY_PURPOSE = "purpose";
    private static final String KEY_STATUS = "status"; // scheduled, completed, canceled
    private static final String KEY_STATUS_UPDATE_TIME = "status_update_time";

    // Staff table columns
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_JOIN_DATE = "date";
    private static final String KEY_PHOTO_PATH = "photoPath";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USERNAME + " TEXT UNIQUE,"
                + KEY_PASSWORD + " TEXT,"
                + KEY_ROLE + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Create patients table with user_id
        String CREATE_PATIENTS_TABLE = "CREATE TABLE " + TABLE_PATIENTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PATIENT_NAME + " TEXT,"
                + KEY_AGE + " INTEGER,"
                + KEY_GENDER + " TEXT,"
                + KEY_ADDRESS + " TEXT,"
                + KEY_PHONE + " TEXT,"
                + KEY_MEDICAL_HISTORY + " TEXT,"
                + "user_id INTEGER" + ")";
        db.execSQL(CREATE_PATIENTS_TABLE);

        // Create appointments table with user_id
        String CREATE_APPOINTMENTS_TABLE = "CREATE TABLE " + TABLE_APPOINTMENTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PATIENT_ID + " INTEGER,"
                + KEY_DOCTOR_ID + " INTEGER,"
                + KEY_DATE + " TEXT,"
                + KEY_TIME + " TEXT,"
                + KEY_PURPOSE + " TEXT,"
                + KEY_STATUS + " TEXT,"
                + KEY_STATUS_UPDATE_TIME + " TEXT,"
                + "user_id INTEGER" + ")";
        db.execSQL(CREATE_APPOINTMENTS_TABLE);

        // Create staff table with user_id
        String CREATE_STAFF_TABLE = "CREATE TABLE " + TABLE_STAFF + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NAME + " TEXT,"
                + KEY_ROLE + " TEXT,"
                + KEY_DEPARTMENT + " TEXT,"
                + KEY_EMAIL + " TEXT,"
                + KEY_PHONE + " TEXT,"
                + KEY_JOIN_DATE + " TEXT,"
                + KEY_ADDRESS + " TEXT,"
                + "photoPath TEXT,"  // 👈 Add this line
                + "user_id INTEGER" + ")";
        db.execSQL(CREATE_STAFF_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            // Add user_id columns to existing tables
            db.execSQL("ALTER TABLE " + TABLE_PATIENTS + " ADD COLUMN user_id INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN user_id INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_STAFF + " ADD COLUMN user_id INTEGER DEFAULT 0");
        }
        if (oldVersion < 8) {
            // Add photoPath column to staff table
            db.execSQL("ALTER TABLE " + TABLE_STAFF + " ADD COLUMN photoPath TEXT");
        } else {
            // For complete rebuild if needed
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATIENTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPOINTMENTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_STAFF);
            onCreate(db);
        }
    }

    // User management methods
    public long addUser(String username, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, password);
        values.put(KEY_ROLE, role);

        try {
            return db.insertOrThrow(TABLE_USERS, null, values);
        } catch (SQLiteConstraintException e) {
            return -1; // user already exists
        }
    }

    public int getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{KEY_ID},
                KEY_USERNAME + "=?",
                new String[]{username},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            cursor.close();
            return id;
        }
        return -1; // Return -1 if user not found
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " +
                        KEY_USERNAME + "=? AND " + KEY_PASSWORD + "=?",
                new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public String getUserRole(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_ROLE},
                KEY_USERNAME + "=?", new String[]{username},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String role = cursor.getString(0);
            cursor.close();
            return role;
        }
        return null;
    }

    // Patient management methods
    public long addPatient(String name, int age, String gender, String address,
                           String phone, String medicalHistory, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PATIENT_NAME, name);
        values.put(KEY_AGE, age);
        values.put(KEY_GENDER, gender);
        values.put(KEY_ADDRESS, address);
        values.put(KEY_PHONE, phone);
        values.put(KEY_MEDICAL_HISTORY, medicalHistory);
        values.put("user_id", userId);

        return db.insert(TABLE_PATIENTS, null, values);
    }

    public List<Patient> getPatientsByUserId(int userId) {
        List<Patient> patientList = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_PATIENTS + " WHERE user_id = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                Patient patient = new Patient();
                patient.setId(cursor.getInt(0));
                patient.setName(cursor.getString(1));
                patient.setAge(cursor.getInt(2));
                patient.setGender(cursor.getString(3));
                patient.setAddress(cursor.getString(4));
                patient.setPhone(cursor.getString(5));
                patient.setMedicalHistory(cursor.getString(6));
                patientList.add(patient);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return patientList;
    }

    public boolean updatePatient(int id, String name, int age, String gender,
                                 String address, String phone, String medicalHistory) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PATIENT_NAME, name);
        values.put(KEY_AGE, age);
        values.put(KEY_GENDER, gender);
        values.put(KEY_ADDRESS, address);
        values.put(KEY_PHONE, phone);
        values.put(KEY_MEDICAL_HISTORY, medicalHistory);

        int rowsAffected = db.update(TABLE_PATIENTS, values,
                KEY_ID + " = ?",
                new String[]{String.valueOf(id)});
        return rowsAffected > 0;
    }



    public List<Patient> getAllPatients() {
        List<Patient> patientList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PATIENTS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Patient patient = new Patient();
                patient.setId(cursor.getInt(0));
                patient.setName(cursor.getString(1));
                patient.setAge(cursor.getInt(2));
                patient.setGender(cursor.getString(3));
                patient.setAddress(cursor.getString(4));
                patient.setPhone(cursor.getString(5));
                patient.setMedicalHistory(cursor.getString(6));
                patientList.add(patient);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return patientList;
    }

    public Patient getPatientById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PATIENTS,
                new String[]{KEY_ID, KEY_PATIENT_NAME, KEY_AGE, KEY_GENDER,
                        KEY_ADDRESS, KEY_PHONE, KEY_MEDICAL_HISTORY},
                KEY_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);

        if (cursor.moveToFirst()) {
            Patient patient = new Patient();
            patient.setId(cursor.getInt(0));
            patient.setName(cursor.getString(1));
            patient.setAge(cursor.getInt(2));
            patient.setGender(cursor.getString(3));
            patient.setAddress(cursor.getString(4));
            patient.setPhone(cursor.getString(5));
            patient.setMedicalHistory(cursor.getString(6));
            cursor.close();
            return patient;
        }
        return null;
    }

    public boolean deletePatient(int patientId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // First delete related appointments to maintain referential integrity
        db.delete(TABLE_APPOINTMENTS, KEY_PATIENT_ID + " = ?",
                new String[]{String.valueOf(patientId)});

        // Then delete the patient
        int rowsAffected = db.delete(TABLE_PATIENTS, KEY_ID + " = ?",
                new String[]{String.valueOf(patientId)});

        return rowsAffected > 0;
    }

    // Appointment management methods
    public long addAppointment(int patientId, int doctorId, String date,
                               String time, String purpose, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PATIENT_ID, patientId);
        values.put(KEY_DOCTOR_ID, doctorId);
        values.put(KEY_DATE, date);
        values.put(KEY_TIME, time);
        values.put(KEY_PURPOSE, purpose);
        values.put(KEY_STATUS, "scheduled");
        values.put(KEY_STATUS_UPDATE_TIME, "");
        values.put("user_id", userId);

        return db.insert(TABLE_APPOINTMENTS, null, values);
    }

    public List<Appointment> getAppointmentsByUserId(int userId) {
        List<Appointment> appointmentList = new ArrayList<>();
        String query = "SELECT a.*, p.name AS patient_name, s.name AS doctor_name " +
                "FROM " + TABLE_APPOINTMENTS + " a " +
                "LEFT JOIN " + TABLE_PATIENTS + " p ON a." + KEY_PATIENT_ID + " = p." + KEY_ID + " " +
                "LEFT JOIN " + TABLE_STAFF + " s ON a." + KEY_DOCTOR_ID + " = s." + KEY_ID + " " +
                "WHERE a.user_id = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                Appointment appointment = new Appointment();
                appointment.setId(cursor.getInt(0));
                appointment.setPatientId(cursor.getInt(1));
                appointment.setDoctorId(cursor.getInt(2));
                appointment.setDate(cursor.getString(3));
                appointment.setTime(cursor.getString(4));
                appointment.setPurpose(cursor.getString(5));
                appointment.setStatus(cursor.getString(6));
                appointment.setStatusUpdateTime(cursor.getString(7));
                appointment.setPatientName(cursor.getString(8));
                appointment.setDoctorName(cursor.getString(9));
                appointmentList.add(appointment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return appointmentList;
    }


    // Add these methods to DatabaseHelper.java

    public List<Patient> getPatientsByDoctorId(int doctorId) {
        List<Patient> patientList = new ArrayList<>();
        String query = "SELECT DISTINCT p.* FROM " + TABLE_PATIENTS + " p " +
                "INNER JOIN " + TABLE_APPOINTMENTS + " a ON p." + KEY_ID + " = a." + KEY_PATIENT_ID + " " +
                "WHERE a." + KEY_DOCTOR_ID + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(doctorId)});

        if (cursor.moveToFirst()) {
            do {
                Patient patient = new Patient();
                patient.setId(cursor.getInt(0));
                patient.setName(cursor.getString(1));
                patient.setAge(cursor.getInt(2));
                patient.setGender(cursor.getString(3));
                patient.setAddress(cursor.getString(4));
                patient.setPhone(cursor.getString(5));
                patient.setMedicalHistory(cursor.getString(6));
                patientList.add(patient);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return patientList;
    }

    public List<Appointment> getAppointmentsByDoctorId(int doctorId) {
        List<Appointment> appointmentList = new ArrayList<>();
        String query = "SELECT a.*, p.name AS patient_name, s.name AS doctor_name " +
                "FROM " + TABLE_APPOINTMENTS + " a " +
                "LEFT JOIN " + TABLE_PATIENTS + " p ON a.patient_id = p.id " +
                "LEFT JOIN " + TABLE_STAFF + " s ON a.doctor_id = s.id " +
                "WHERE a.doctor_id = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(doctorId)});

        if (cursor.moveToFirst()) {
            do {
                Appointment appointment = new Appointment();
                appointment.setId(cursor.getInt(0));
                appointment.setPatientId(cursor.getInt(1));
                appointment.setDoctorId(cursor.getInt(2));
                appointment.setDate(cursor.getString(3));
                appointment.setTime(cursor.getString(4));
                appointment.setPurpose(cursor.getString(5));
                appointment.setStatus(cursor.getString(6));
                appointment.setStatusUpdateTime(cursor.getString(7));
                appointment.setPatientName(cursor.getString(8));
                appointment.setDoctorName(cursor.getString(9));
                appointmentList.add(appointment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return appointmentList;
    }


    public List<Appointment> getAllAppointments() {
        List<Appointment> appointmentList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_APPOINTMENTS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Appointment appointment = new Appointment();
                appointment.setId(cursor.getInt(0));
                appointment.setPatientId(cursor.getInt(1));
                appointment.setDoctorId(cursor.getInt(2));
                appointment.setDate(cursor.getString(3));
                appointment.setTime(cursor.getString(4));
                appointment.setPurpose(cursor.getString(5));
                appointment.setStatus(cursor.getString(6));
                appointment.setStatusUpdateTime(cursor.getString(7));
                appointmentList.add(appointment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return appointmentList;
    }

    public Appointment getAppointmentById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_APPOINTMENTS,
                new String[]{KEY_ID, KEY_PATIENT_ID, KEY_DOCTOR_ID, KEY_DATE,
                        KEY_TIME, KEY_PURPOSE, KEY_STATUS, KEY_STATUS_UPDATE_TIME},
                KEY_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);

        if (cursor.moveToFirst()) {
            Appointment appointment = new Appointment();
            appointment.setId(cursor.getInt(0));
            appointment.setPatientId(cursor.getInt(1));
            appointment.setDoctorId(cursor.getInt(2));
            appointment.setDate(cursor.getString(3));
            appointment.setTime(cursor.getString(4));
            appointment.setPurpose(cursor.getString(5));
            appointment.setStatus(cursor.getString(6));
            appointment.setStatusUpdateTime(cursor.getString(7));
            cursor.close();
            return appointment;
        }
        return null;
    }

    public boolean updateAppointment(Appointment appointment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PATIENT_ID, appointment.getPatientId());
        values.put(KEY_DOCTOR_ID, appointment.getDoctorId());
        values.put(KEY_DATE, appointment.getDate());
        values.put(KEY_TIME, appointment.getTime());
        values.put(KEY_PURPOSE, appointment.getPurpose());
        values.put(KEY_STATUS, appointment.getStatus());
        values.put(KEY_STATUS_UPDATE_TIME, appointment.getStatusUpdateTime());

        int rowsAffected = db.update(TABLE_APPOINTMENTS, values,
                KEY_ID + " = ?",
                new String[]{String.valueOf(appointment.getId())});
        return rowsAffected > 0;
    }

    public List<Appointment> getAllAppointmentsWithNames() {
        List<Appointment> appointmentList = new ArrayList<>();
        String selectQuery = "SELECT a.*, p.name as patient_name, s.name as doctor_name " +
                "FROM " + TABLE_APPOINTMENTS + " a " +
                "LEFT JOIN " + TABLE_PATIENTS + " p ON a." + KEY_PATIENT_ID + " = p." + KEY_ID + " " +
                "LEFT JOIN " + TABLE_STAFF + " s ON a." + KEY_DOCTOR_ID + " = s." + KEY_ID;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Appointment appointment = new Appointment();
                appointment.setId(cursor.getInt(0));
                appointment.setPatientId(cursor.getInt(1));
                appointment.setDoctorId(cursor.getInt(2));
                appointment.setDate(cursor.getString(3));
                appointment.setTime(cursor.getString(4));
                appointment.setPurpose(cursor.getString(5));
                appointment.setStatus(cursor.getString(6));
                appointment.setStatusUpdateTime(cursor.getString(7));
                appointment.setPatientName(cursor.getString(8)); // patient_name
                appointment.setDoctorName(cursor.getString(9));  // doctor_name
                appointmentList.add(appointment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return appointmentList;
    }

    public Appointment getAppointmentWithNamesById(int id) {
        String selectQuery = "SELECT a.*, p.name as patient_name, s.name as doctor_name " +
                "FROM " + TABLE_APPOINTMENTS + " a " +
                "LEFT JOIN " + TABLE_PATIENTS + " p ON a." + KEY_PATIENT_ID + " = p." + KEY_ID + " " +
                "LEFT JOIN " + TABLE_STAFF + " s ON a." + KEY_DOCTOR_ID + " = s." + KEY_ID + " " +
                "WHERE a." + KEY_ID + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            Appointment appointment = new Appointment();
            appointment.setId(cursor.getInt(0));
            appointment.setPatientId(cursor.getInt(1));
            appointment.setDoctorId(cursor.getInt(2));
            appointment.setDate(cursor.getString(3));
            appointment.setTime(cursor.getString(4));
            appointment.setPurpose(cursor.getString(5));
            appointment.setStatus(cursor.getString(6));
            appointment.setStatusUpdateTime(cursor.getString(7));
            appointment.setPatientName(cursor.getString(8)); // patient_name
            appointment.setDoctorName(cursor.getString(9));  // doctor_name
            cursor.close();
            return appointment;
        }
        return null;
    }

    public List<Appointment> getAppointmentsByStatus(String status) {
        List<Appointment> appointmentList = new ArrayList<>();

        String query = "SELECT a.*, p.name AS patient_name, s.name AS doctor_name " +
                "FROM " + TABLE_APPOINTMENTS + " a " +
                "LEFT JOIN " + TABLE_PATIENTS + " p ON a.patient_id = p.id " +
                "LEFT JOIN " + TABLE_STAFF + " s ON a.doctor_id = s.id " +
                "WHERE a.status = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{status});

        if (cursor.moveToFirst()) {
            do {
                Appointment appointment = new Appointment();
                appointment.setId(cursor.getInt(0));
                appointment.setPatientId(cursor.getInt(1));
                appointment.setDoctorId(cursor.getInt(2));
                appointment.setDate(cursor.getString(3));
                appointment.setTime(cursor.getString(4));
                appointment.setPurpose(cursor.getString(5));
                appointment.setStatus(cursor.getString(6));
                appointment.setStatusUpdateTime(cursor.getString(7));
                appointment.setPatientName(cursor.getString(8));
                appointment.setDoctorName(cursor.getString(9));
                appointmentList.add(appointment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return appointmentList;
    }



    public boolean deleteAppointment(int appointmentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_APPOINTMENTS, KEY_ID + " = ?",
                new String[]{String.valueOf(appointmentId)});
        return rowsAffected > 0;
    }

    // Staff management methods
    public List<Staff> getAllStaff() {
        List<Staff> staffList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_STAFF;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Staff staff = new Staff();
                staff.setId(cursor.getInt(0));
                staff.setName(cursor.getString(1));
                staff.setRole(cursor.getString(2));
                staff.setDepartment(cursor.getString(3));
                staff.setEmail(cursor.getString(4));
                staff.setPhone(cursor.getString(5));
                staff.setJoinDate(cursor.getString(6));
                staff.setAddress(cursor.getString(7));
                staff.setPhotoPath(cursor.getString(8)); // photoPath
                staffList.add(staff);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return staffList;
    }

    public Staff getStaffByUserId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STAFF,
                new String[]{KEY_ID, KEY_NAME, KEY_ROLE, KEY_DEPARTMENT,
                        KEY_EMAIL, KEY_PHONE, KEY_JOIN_DATE, KEY_ADDRESS, KEY_PHOTO_PATH},
                "user_id = ?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            Staff staff = new Staff();
            staff.setId(cursor.getInt(0));
            staff.setName(cursor.getString(1));
            staff.setRole(cursor.getString(2));
            staff.setDepartment(cursor.getString(3));
            staff.setEmail(cursor.getString(4));
            staff.setPhone(cursor.getString(5));
            staff.setJoinDate(cursor.getString(6));
            staff.setAddress(cursor.getString(7));
            staff.setPhotoPath(cursor.getString(8)); // photoPath
            cursor.close();
            return staff;
        }
        return null;
    }

    public boolean deleteStaff(int staffId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_STAFF, KEY_ID + " = ?",
                new String[]{String.valueOf(staffId)});
        return rowsAffected > 0;
    }

    public boolean updateStaff(Staff staff) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, staff.getName());
        values.put(KEY_ROLE, staff.getRole());
        values.put(KEY_DEPARTMENT, staff.getDepartment());
        values.put(KEY_EMAIL, staff.getEmail());
        values.put(KEY_PHONE, staff.getPhone());
        values.put(KEY_JOIN_DATE, staff.getJoinDate());
        values.put(KEY_ADDRESS, staff.getAddress());
        values.put(KEY_PHOTO_PATH, staff.getPhotoPath());

        int rowsAffected = db.update(TABLE_STAFF, values,
                KEY_ID + " = ?",
                new String[]{String.valueOf(staff.getId())});
        return rowsAffected > 0;
    }

    public Staff getStaffById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STAFF,
                new String[]{KEY_ID, KEY_NAME, KEY_ROLE, KEY_DEPARTMENT,
                        KEY_EMAIL, KEY_PHONE, KEY_JOIN_DATE, KEY_ADDRESS, KEY_PHOTO_PATH},
                KEY_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);

        if (cursor.moveToFirst()) {
            Staff staff = new Staff();
            staff.setId(cursor.getInt(0));
            staff.setName(cursor.getString(1));
            staff.setRole(cursor.getString(2));
            staff.setDepartment(cursor.getString(3));
            staff.setEmail(cursor.getString(4));
            staff.setPhone(cursor.getString(5));
            staff.setJoinDate(cursor.getString(6));
            staff.setAddress(cursor.getString(7));
            staff.setPhotoPath(cursor.getString(8)); // ✅ now loaded
            cursor.close();
            return staff;
        }
        return null;
    }


    public long addStaff(String name, String role, String department,
                         String email, String phone, String joinDate,
                         String address, String photoPath, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_ROLE, role);
        values.put(KEY_DEPARTMENT, department);
        values.put(KEY_EMAIL, email);
        values.put(KEY_PHONE, phone);
        values.put(KEY_JOIN_DATE, joinDate);
        values.put(KEY_ADDRESS, address);
        values.put(KEY_PHOTO_PATH, photoPath);
        values.put("user_id", userId);

        return db.insert(TABLE_STAFF, null, values);
    }

}