// Archivo de compilación raíz: declara los plugins que usa el módulo :app
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Plugin de Google Services: lee app/google-services.json para conectar Firebase
    id("com.google.gms.google-services") version "4.4.2" apply false
}
