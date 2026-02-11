# Deployment Guide - Hospital Management System

This guide provides detailed instructions for deploying the Hospital Management System Android application.

## 📦 Build Variants

The project supports two build variants:
- **Debug**: For development and testing
- **Release**: For production deployment

## 🔧 Debug Build

### Generate Debug APK
```bash
# Using Gradle wrapper
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```

**Output Location**: `app/build/outputs/apk/debug/app-debug.apk`

### Install Debug APK
```bash
# Install on connected device/emulator
./gradlew installDebug

# Or manually install
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🚀 Release Build

### Option 1: Unsigned Release APK (Testing Only)

```bash
./gradlew assembleRelease
```

**Output**: `app/build/outputs/apk/release/app-release-unsigned.apk`

> ⚠️ **Warning**: Unsigned APKs cannot be installed on most devices and are not suitable for distribution.

### Option 2: Signed Release APK (Recommended)

#### Step 1: Generate Keystore

Create a keystore file (one-time setup):

```bash
keytool -genkey -v -keystore hospital-app-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias hospital-app-key
```

You'll be prompted for:
- Keystore password (remember this!)
- Key password (remember this!)
- Your name, organization, location details

**Important**: Store the keystore file and passwords securely. You'll need them for all future updates.

#### Step 2: Configure Signing

**Option A: Using gradle.properties (Recommended)**

1. Create/edit `gradle.properties` in your project root:

```properties
# Signing configuration
RELEASE_STORE_FILE=../hospital-app-release.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=hospital-app-key
RELEASE_KEY_PASSWORD=your_key_password
```

2. Update `app/build.gradle`:

```gradle
android {
    signingConfigs {
        release {
            storeFile file(RELEASE_STORE_FILE)
            storePassword RELEASE_STORE_PASSWORD
            keyAlias RELEASE_KEY_ALIAS
            keyPassword RELEASE_KEY_PASSWORD
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

**Option B: Using Environment Variables**

```gradle
android {
    signingConfigs {
        release {
            storeFile file(System.getenv("RELEASE_STORE_FILE") ?: "release.jks")
            storePassword System.getenv("RELEASE_STORE_PASSWORD")
            keyAlias System.getenv("RELEASE_KEY_ALIAS")
            keyPassword System.getenv("RELEASE_KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

#### Step 3: Build Signed APK

```bash
./gradlew assembleRelease
```

**Output**: `app/build/outputs/apk/release/app-release.apk` (signed)

### Option 3: Android App Bundle (AAB) - Google Play Store

App Bundles are the recommended format for Google Play Store distribution.

#### Build Signed AAB

```bash
./gradlew bundleRelease
```

**Output**: `app/build/outputs/bundle/release/app-release.aab`

#### Benefits of AAB
- Smaller download size (Google Play generates optimized APKs)
- Automatic support for multiple device configurations
- Required for new apps on Google Play Store (since August 2021)

## 🛡️ ProGuard/R8 Configuration

The release build uses R8 for code shrinking and obfuscation. Current configuration:

```gradle
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

### Custom ProGuard Rules

Edit `app/proguard-rules.pro` to add custom rules:

```proguard
# Keep database models
-keep class com.samhith.hospitalappjava.Patient { *; }
-keep class com.samhith.hospitalappjava.Staff { *; }
-keep class com.samhith.hospitalappjava.Appointment { *; }

# Keep Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
```

## 📱 Distribution Methods

### 1. Direct APK Distribution
- Share the signed APK file directly
- Users need to enable "Install from Unknown Sources"
- Suitable for internal testing or small-scale deployment

### 2. Google Play Store
1. Create a Google Play Developer account ($25 one-time fee)
2. Upload the signed AAB file
3. Complete store listing (description, screenshots, etc.)
4. Submit for review

### 3. Alternative App Stores
- Amazon Appstore
- Samsung Galaxy Store
- Huawei AppGallery

## 🔍 Pre-Deployment Checklist

- [ ] Update `versionCode` and `versionName` in `app/build.gradle`
- [ ] Test on multiple devices/screen sizes
- [ ] Verify all features work in release build
- [ ] Check ProGuard hasn't broken any functionality
- [ ] Review and update app permissions in `AndroidManifest.xml`
- [ ] Prepare store assets (icon, screenshots, description)
- [ ] Test installation from APK/AAB
- [ ] Verify database migrations work correctly
- [ ] Check app size (should be optimized with R8)

## 🔄 Version Management

Update version in `app/build.gradle` before each release:

```gradle
android {
    defaultConfig {
        versionCode 2        // Increment for each release
        versionName "1.1.0"  // Semantic versioning
    }
}
```

**Version Code**: Integer that increases with each release (used by Play Store)  
**Version Name**: Human-readable version string (displayed to users)

## 🐛 Troubleshooting

### Build Fails with Signing Error
- Verify keystore path is correct
- Check passwords are correct
- Ensure keystore file has proper permissions

### App Crashes in Release but Works in Debug
- Check ProGuard rules
- Review R8 optimization settings
- Test with `minifyEnabled false` to isolate the issue

### APK Won't Install
- Ensure APK is signed
- Check device allows installation from unknown sources
- Verify minimum SDK version compatibility

## 📊 Build Outputs

After building, you'll find outputs in:

```
app/build/outputs/
├── apk/
│   ├── debug/
│   │   └── app-debug.apk
│   └── release/
│       └── app-release.apk
├── bundle/
│   └── release/
│       └── app-release.aab
└── mapping/
    └── release/
        └── mapping.txt  # ProGuard mapping file (keep for crash reports)
```

> **Important**: Keep the `mapping.txt` file for each release. It's needed to deobfuscate crash reports.

## 🔐 Security Best Practices

1. **Never commit keystore files to version control**
   - Add `*.jks` to `.gitignore`
   - Store keystore securely (encrypted backup)

2. **Use environment variables for sensitive data**
   - Don't hardcode passwords in `build.gradle`
   - Use CI/CD secrets for automated builds

3. **Implement certificate pinning** (for production)
   - Protect against man-in-the-middle attacks
   - Especially important if adding network features

4. **Enable app signing by Google Play** (recommended)
   - Google manages the signing key
   - Allows key rotation if compromised

## 📞 Support

For deployment issues, check:
- Android Studio Build Output
- Gradle Console
- `app/build/outputs/logs/`

---

**Ready to deploy? Follow the steps above and your app will be live! 🚀**
