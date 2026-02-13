# Hospital Management System

A comprehensive Android application for managing hospital operations including patient records, staff management, appointments, and reporting.


## 📥 Download

You can download the latest APK from the releases folder: 
**[Download HospitalApp.apk](https://github.com/samhithbasa/Hospital-App/raw/main/app/release/app-release.apk)**

## 📱 Features

### User Management
- **Multi-role Authentication**: Admin and Staff roles with secure login using **BCrypt hashing**
- **User Registration**: New staff can sign up with password validation and secure storage
- **Forgot Password**: Implementation of reset password flow via Firebase Email
- **Role-based Access Control**: Different permissions for admin and staff users

### Patient Management
- Add, view, edit, and delete patient records
- Track patient information: name, age, gender, address, phone, medical history
- User-specific patient records (staff can only see their own patients)

### Staff Management
- Comprehensive staff profiles with photo support
- Track staff details: name, role, department, email, phone, join date, address
- Staff photo management using Glide library

### Appointment System
- Schedule appointments between patients and doctors
- Track appointment status: scheduled, completed, canceled
- View appointments by user or doctor
- Edit and manage appointment details
- Status update tracking with timestamps
- **Appointment Reminders**: Local notifications 10 minutes before scheduled time
- **Push Notifications**: Integrated Firebase Cloud Messaging for system alerts

### Reports & Analytics
- Generate reports on appointments by status
- View patient and staff statistics
- Filter appointments by status (scheduled, completed, canceled)
- **PDF Export**: Export comprehensive hospital reports to the device **Downloads folder** (`/storage/emulated/0/Download/`)
- **Dark Mode**: Toggleable dark theme with state persistence
- **Cloud Backup**: Periodic background synchronization of SQLite data to Firebase Firestore
- **Cloud Restore**: Pull data from Firestore to sync across multiple devices

## 🛠️ Tech Stack

- **Language**: Java
- **Platform**: Android (API 24+, Target API 35)
- **Database**: SQLite (local) + **Firebase Firestore (Cloud Backup)**
- **Authentication**: **jBCrypt 0.4** (password hashing) + **Firebase Auth**
- **Notifications**: **Firebase Cloud Messaging (FCM)** + AlarmManager
- **UI Components**: Material Design 3, CardView, **SwitchMaterial**
- **Background Tasks**: **WorkManager** (for periodic cloud sync)
- **Image Loading**: Glide 4.16.0
- **Build System**: Gradle with Version Catalogs
- **View Binding**: Enabled for type-safe view access

## 📋 Prerequisites

- **Android Studio**: Arctic Fox or later
- **JDK**: Java 11 or higher
- **Android SDK**: API Level 24 (Android 7.0) minimum
- **Gradle**: 7.0+ (handled by wrapper)

## 🚀 Setup Instructions

### 1. Clone or Download the Project
```bash
git clone <your-repository-url>
cd HospitalAppJava
```

### 2. Open in Android Studio
1. Launch Android Studio
2. Select **File → Open**
3. Navigate to the project directory and select it
4. Wait for Gradle sync to complete

### 3. Build the Project
```bash
# Using Gradle wrapper (recommended)
./gradlew build

# On Windows
gradlew.bat build
```

### 4. Run the Application
1. Connect an Android device via USB (with USB debugging enabled) or start an emulator
2. Click the **Run** button in Android Studio or use:
```bash
./gradlew installDebug
```

### 5. Default Credentials
- **Username**: `admin`
- **Password**: `admin123`
- **Role**: Admin

## 📦 Project Structure

```
HospitalAppJava/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/samhith/hospitalappjava/
│   │   │   │   ├── MainActivity.java              # Login/Signup
│   │   │   │   ├── SecondActivity.java            # Dashboard
│   │   │   │   ├── DatabaseHelper.java            # SQLite database
│   │   │   │   ├── Patient.java                   # Patient model
│   │   │   │   ├── Staff.java                     # Staff model
│   │   │   │   ├── Appointment.java               # Appointment model
│   │   │   │   ├── ManagePatientsActivity.java    # Patient CRUD
│   │   │   │   ├── ManageStaffActivity.java       # Staff CRUD
│   │   │   │   ├── ManageAppointmentsActivity.java # Appointment CRUD
│   │   │   │   ├── ReportsActivity.java           # Reports & Analytics
│   │   │   │   └── *Adapter.java                  # RecyclerView adapters
│   │   │   ├── res/
│   │   │   │   ├── layout/                        # XML layouts
│   │   │   │   ├── drawable/                      # Images & icons
│   │   │   │   └── values/                        # Strings, colors, themes
│   │   │   └── AndroidManifest.xml
│   │   └── test/                                  # Unit tests
│   └── build.gradle                               # App-level build config
├── build.gradle                                   # Project-level build config
├── gradle.properties                              # Gradle settings
└── settings.gradle                                # Project settings
```

## 🗄️ Database Schema

The app uses SQLite with the following tables:

### Users Table
- `id` (PRIMARY KEY)
- `username` (UNIQUE)
- `password`
- `role` (admin/staff)

### Patients Table
- `id` (PRIMARY KEY)
- `name`, `age`, `gender`, `address`, `phone`
- `medical_history`
- `user_id` (foreign key)

### Staff Table
- `id` (PRIMARY KEY)
- `name`, `role`, `department`, `email`, `phone`
- `date` (join date), `address`, `photoPath`
- `user_id` (foreign key)

### Appointments Table
- `id` (PRIMARY KEY)
- `patient_id`, `doctor_id` (foreign keys)
- `date`, `time`, `purpose`
- `status` (scheduled/completed/canceled)
- `status_update_time`
- `user_id` (foreign key)

## 📱 Deployment

### Generate Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Generate Release APK (Unsigned)
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Generate Signed Release APK

1. **Create a Keystore** (first time only):
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

2. **Configure Signing in `app/build.gradle`**:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("path/to/my-release-key.jks")
            storePassword "your-store-password"
            keyAlias "my-key-alias"
            keyPassword "your-key-password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

3. **Build Signed APK**:
```bash
./gradlew assembleRelease
```

### Generate App Bundle (AAB) for Google Play
```bash
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

> **Note**: App Bundles are the recommended format for Google Play Store distribution.

## 🔒 Security Notes

- **Password Validation**: Minimum 8 characters, cannot contain username
- **User Isolation**: Staff users can only access their own data
- **Admin Access**:- Full access to all records
- **Secure Storage**: All user passwords are encrypted using BCrypt
- **Privacy**: User-specific data isolation
- **Cloud Sync**: Optional background backup to Firebase

## 🐛 Known Issues & Limitations

- None (Cloud sync enabled across devices)

## 🔮 Future Enhancements

- [x] Implement password hashing (BCrypt)
- [x] Add cloud backup/sync (Firebase)
- [x] Implement forgot password via email
- [x] Add push notifications for appointments
- [x] Export reports to PDF
- [x] Add appointment reminders
- [x] Implement dark mode

## 📄 License

This project is created for educational purposes.

## 👤 Author

**Samhith**

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

---

**Made with ❤️ for better healthcare management**
