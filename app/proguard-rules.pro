# Keep Room entities and generated code
-keep class com.codetivelab.fieldcalc.data.database.** { *; }
-keepclassmembers class * { @androidx.room.* <methods>; }
# Compose is handled by default AGP rules
