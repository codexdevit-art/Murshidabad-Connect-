================================================================================
MURSHIDABAD CONNECT — SETUP, FIREBASE, ADMIN & BUILD GUIDE
Developer: Slstrms Technologies
Target Users: Residents of Murshidabad, West Bengal, India
================================================================================

1. DELIVERABLES & CURRENT FILE INVENTORY
--------------------------------------------------------------------------------
- Android User App Source : app/ (Kotlin, Jetpack Compose, Material 3)
- Admin Panel             : admin.html (Single-file HTML5 + CSS3 + Vanilla JS)
- Firestore Security Rules: firestore.rules
- Firestore Blueprint     : firebase-blueprint.json
- Rules Test Suite        : firestore.test.js (verified against Firestore Emulator)
- Robolectric Test Suite  : app/src/test/java/com/example/ExampleRobolectricTest.kt
- Setup Guide             : README.txt

NOTE ON RELEASE APK (output/release.apk):
- All release Kotlin compilation, resource packaging, DEX merging, and
  lintVitalRelease tasks compiled successfully.
- The final `:app:packageRelease` task did NOT generate `output/release.apk`
  because this cloud build container does not contain a release upload keystore
  (`my-upload-key.jks` / `STORE_PASSWORD` / `KEY_PASSWORD` environment variables).
- A working, debug-signed APK IS compiled at:
  `app/build/outputs/apk/debug/app-debug.apk`
- See Section 8 below for exact release signing instructions or use AI Studio's
  Settings menu to export a signed APK/AAB.

================================================================================
2. FIREBASE PROJECT CONFIGURATION (SHARED BY ANDROID APP & ADMIN PANEL)
================================================================================
Both `app/` and `admin.html` connect to the SAME newly provisioned Firebase
project and named Firestore database:

- Firebase Project ID     : gen-lang-client-0379959654
- Project Number          : 886350848238
- Android Package Name    : com.murshidabad.connect
- Android App ID          : 1:886350848238:android:a765f19795fd66389793ff
- Firestore Database ID   : ai-studio-android-murshida-a77068e6-7c4a-4a53-be40-aa155ef83318
- Web OAuth Client ID     : 886350848238-eru6tcnvaapq29st5hkl5v65r5i0e49r.apps.googleusercontent.com
- Android Config File     : app/google-services.json
- Database Resource File  : app/src/main/res/values/firebase_applet_config.xml

IMPORTANT — NAMED FIRESTORE DATABASE:
This project uses the named Firestore database ID:
  ai-studio-android-murshida-a77068e6-7c4a-4a53-be40-aa155ef83318
- Android uses `FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))`
- `admin.html` uses `getFirestore(app, FIRESTORE_DATABASE_ID)`

Registered Debug Keystore Fingerprints (already in app/google-services.json):
- SHA-1   : A3:74:FC:0E:66:ED:C0:06:1B:30:6E:93:2B:45:FA:8D:6F:E3:E0:F3
- SHA-256 : CD:F8:C1:A3:01:54:23:9A:E7:3C:76:7B:3F:B7:19:69:58:52:9C:34:9E:D5:24:7C:03:80:87:C6:AC:DA:DB:1A

When you create your release upload keystore for Google Play / release APK
builds, also add the release SHA-1 and SHA-256 fingerprints in:
  Firebase Console -> Project Settings -> Your apps -> com.murshidabad.connect -> Add fingerprint

================================================================================
3. ADMIN PANEL (admin.html) — WEB APP ID & HOSTING SETUP
================================================================================
`admin.html` already includes the real `apiKey`, `authDomain`, `projectId`,
`messagingSenderId`, and `FIRESTORE_DATABASE_ID` from Firebase project
`gen-lang-client-0379959654`.

Because a Firebase Web App (`appId` containing `:web:`) must be registered in
Firebase Console before web login works:

1. Open Firebase Console -> Select project `gen-lang-client-0379959654`.
2. Go to Project Settings (gear icon) -> General -> Your apps -> Add app -> Web (</>).
3. Register nickname e.g. `Murshidabad Connect Admin`.
4. Copy the generated `appId` (format: `1:886350848238:web:xxxxxxxxxxxxxxxx`).
5. Either:
   - Paste it directly into `DEFAULT_WEB_APP_ID` inside `admin.html`, OR
   - Open `admin.html` over HTTP/HTTPS and paste the Web App ID into the yellow
     setup banner at the top of the login card (saves to browser localStorage).
6. Serve `admin.html` over HTTP/HTTPS (Google Sign-In popup does not work over
   `file://`). For local use:
     python3 -m http.server 8080
   Then open http://localhost:8080/admin.html
7. Verify `localhost` (and any production admin hosting domain) is listed under:
   Firebase Console -> Authentication -> Settings -> Authorized domains
8. Ensure Google Sign-In provider is enabled under:
   Firebase Console -> Authentication -> Sign-in method -> Google

================================================================================
4. GOOGLE LOGIN & ROLE-BASED ADMIN ACCESS (RBAC)
================================================================================
USER APP (Android):
- Uses Jetpack Credential Manager (`GetSignInWithGoogleOption`) + Firebase Auth.
- Automatically reads Google Display Name, Email, Photo URL, and Firebase UID.
- Creates/updates `users/{uid}` in Firestore on first login.
- Anonymous auth and email/password auth are disabled by design and rules.

ADMIN PANEL (admin.html):
- Uses `GoogleAuthProvider` (`signInWithPopup`).
- Bootstrap Super Admin email configured in both `firestore.rules` and
  `admin.html`:
    rjjejsjs29@gmail.com (must have `email_verified == true` in Firebase Auth)
- Additional Admins and Managers:
  1. Sign in as Super Admin (`rjjejsjs29@gmail.com`).
  2. Navigate to "Admins & Managers" in the Admin Panel sidebar.
  3. Create an entry in `admins/{uid}` (SUPER_ADMIN / ADMIN) or `managers/{uid}`
     (MANAGER / MODERATOR) with granular per-module permissions
     (`view`, `add`, `edit`, `delete`, `publish`).
  4. All permissions are enforced server-side in `firestore.rules` as well as in
     the Admin Panel UI.

================================================================================
5. URL-ONLY IMAGE SYSTEM (NO FIREBASE STORAGE / NO BINARY UPLOADS)
================================================================================
- Firebase Storage is NOT enabled or used anywhere in this project.
- No device file pickers (`PickVisualMedia`, `<input type="file">`) or binary
  uploads exist in either app.
- User profile avatars come automatically from Google Sign-In (`photoUrl`).
- Content banners, news photos, community post photos, lost & found photos,
  and business images use HTTPS image URLs (`imageUrl`) stored in Firestore and
  rendered via Coil (Android) / `<img>` (Admin Panel) with graceful fallback UI.

================================================================================
6. FIRESTORE SECURITY RULES & EMULATOR TESTS
================================================================================
Security rules are defined in `firestore.rules` and already deployed to
`gen-lang-client-0379959654` (`ai-studio-android-murshida-a77068e6-7c4a-4a53-be40-aa155ef83318`).

To re-deploy rules manually with Firebase CLI:
  firebase deploy --only firestore:rules --project gen-lang-client-0379959654

To run the local Firestore security rule tests:
  firebase emulators:start --only firestore --project demo-no-project
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js

================================================================================
7. FIREBASE CLOUD MESSAGING (FCM) & SERVER-SIDE AUTOMATION
================================================================================
- Android app includes `MurshidabadMessagingService` in
  `app/src/main/java/com/example/fcm/MurshidabadMessagingService.kt`, declares
  `POST_NOTIFICATIONS` (Android 13+), subscribes to `all_users` and
  `murshidabad_updates` topics, and stores each user's `fcmToken` in
  `users/{uid}`.
- Admin Panel ("Push Notifications") writes notification documents into the
  `notifications` collection in Firestore (which immediately appear in the
  User App's Alerts tab).
- To deliver actual background FCM push notifications when Admin creates a
  document in `notifications`, deploy a Firebase Cloud Function (Node.js) on
  Blaze plan using `firebase-admin`:
    - Trigger: `onDocumentCreated` on `notifications/{docId}` (database:
      `ai-studio-android-murshida-a77068e6-7c4a-4a53-be40-aa155ef83318`)
    - Send via `admin.messaging().send({ topic: "all_users", notification: ... })`
      or target user's `fcmToken` when `targetAudience === "USER"`.
- Similarly, toggling a user's `status` to `BLOCKED` in Firestore immediately
  restricts Firestore writes and shows a blocked notice in the Android app; if
  you also want Firebase Auth tokens revoked server-side, add a Cloud Function
  calling `admin.auth().updateUser(uid, { disabled: true })`.

================================================================================
8. BUILDING & SIGNING THE ANDROID APK (output/release.apk)
================================================================================
Run Robolectric unit & Compose UI tests:
  gradle :app:testDebugUnitTest

Build Debug APK (already verified):
  gradle :app:assembleDebug
  Output: app/build/outputs/apk/debug/app-debug.apk

Build Signed Release APK:
1. Generate an upload keystore (keep secure and NEVER commit to Git):
     keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
2. Export signing credentials in your shell:
     export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
     export STORE_PASSWORD="YOUR_STORE_PASSWORD"
     export KEY_PASSWORD="YOUR_KEY_PASSWORD"
3. Build the release APK:
     gradle :app:assembleRelease
4. Copy to output/release.apk:
     mkdir -p output && cp app/build/outputs/apk/release/app-release.apk output/release.apk
5. Add the SHA-1 & SHA-256 of `my-upload-key.jks` (`keytool -list -v -keystore my-upload-key.jks`)
   to Firebase Console under `com.murshidabad.connect` so Google Sign-In works
   in release builds.
================================================================================
