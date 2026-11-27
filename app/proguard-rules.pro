# Keep Firestore models and enum/constant objects
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Keep Firebase & Credential Manager classes used via reflection
-keepnames class com.google.firebase.firestore.** { *; }
-keepnames class com.google.firebase.auth.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
