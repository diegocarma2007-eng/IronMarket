plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firebase se activa automáticamente cuando existe app/google-services.json.
// Sin ese archivo la app funciona en modo local con SQLite (útil para pruebas y demostraciones).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Llave pública de ePayco. Se puede reemplazar en gradle.properties (EPAYCO_PUBLIC_KEY).
val epaycoKey: String = (project.findProperty("EPAYCO_PUBLIC_KEY") as String?) ?: "491d6a0b6e992cf924edd8d3d088aff1"
val epaycoTest: String = (project.findProperty("EPAYCO_TEST") as String?) ?: "true"
val adminEmails: String = (project.findProperty("ADMIN_EMAILS") as String?) ?: "admin@ironmarket.co"

android {
    namespace = "com.ironmarket.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ironmarket.app"
        minSdk = 26          // Android 8.0, definido en el documento técnico (RNF-20)
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"

        buildConfigField("String", "EPAYCO_PUBLIC_KEY", "\"$epaycoKey\"")
        buildConfigField("boolean", "EPAYCO_TEST", epaycoTest)
        buildConfigField("String", "ADMIN_EMAILS", "\"$adminEmails\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true   // acceso seguro a las vistas de los layouts XML
        buildConfig = true   // constantes de configuración (ePayco, administradores)
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true   // necesario para las capturas con Robolectric
    }
}

dependencies {
    // Interfaz
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("io.coil-kt:coil:2.7.0")

    // Seguridad: huella y almacenamiento cifrado de credenciales
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.security:security-crypto:1.0.0")

    // Firebase: autenticación, base de datos Firestore y almacenamiento de imágenes
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Geolocalización (APIs de Google Play Services)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Pruebas y capturas de pantalla
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit-ktx:1.2.1")
}
