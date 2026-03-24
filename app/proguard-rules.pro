# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# -----------------------------------------------------------------------
# REGLAS PARA MICAJAPLUS
# -----------------------------------------------------------------------

# 1. Mantener intactas todas las clases dentro del paquete 'models'
-keep class com.example.micaja.models.** { *; }

# 2. Mantener los nombres de las variables exactos para que hagan match con el JSON de Python
-keepclassmembers class com.example.micaja.models.** {
    <fields>;
}

# 3. (Opcional pero recomendado) Reglas básicas si usas Retrofit y Gson
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}