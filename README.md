# IronMarket

Fitness marketplace Android app built with Kotlin and XML layouts / Tienda virtual Android para el nicho fitness hecha en Kotlin con layouts XML.

Proyecto final de la asignatura **Desarrollo de Aplicaciones Móviles Nativas**, Ingeniería de Software, Fundación Universitaria Compensar (UCompensar).

- Autor: Diego Alejandro Jiménez Arias
- Docente: Flavio Alexander Navarro Carmona

## Funcionalidades

| Requisito | Implementación |
|---|---|
| Autenticación para los tres roles | Registro, inicio de sesión, cierre de sesión, sesión persistente y recuperación de contraseña con **Firebase Authentication** |
| Autenticación biométrica | Ingreso con huella (`BiometricPrompt`) usando credenciales cifradas con **EncryptedSharedPreferences** y Android Keystore |
| CRUD de usuarios | Panel del Administrador: crear, listar, editar rol y estado, verificar vendedores y eliminar (borrado lógico) en **Cloud Firestore** |
| CRUD de productos | Crear, listar, editar y eliminar con foto desde **cámara o galería**, guardada en **Firebase Storage** |
| Carrito y órdenes | Carrito persistido en `carts/{uid}/items` y órdenes con historial en `orders` |
| Geolocalización | `FusedLocationProviderClient` + `Geocoder` para la dirección de entrega |
| Pasarela de pagos | **ePayco** (checkout oficial en WebView) con validación de la transacción por API |

Sin el archivo `google-services.json` la app funciona en **modo local con SQLite** (`SqliteRepository`), con las mismas pantallas y datos de ejemplo. Así se puede probar sin conexión y se ejecutan las pruebas automáticas.

## Firebase

El repositorio ya incluye `app/google-services.json` del proyecto Firebase **ironmarket-f2d0a** (Authentication con correo y contraseña y Cloud Firestore con las reglas de `firebase/firestore.rules`). Al compilar, la app usa Firebase automáticamente. Firebase considera público este archivo: la protección de los datos está en las reglas de seguridad.

Para usar un proyecto propio, reemplaza ese archivo siguiendo estos pasos:

1. Entra a <https://console.firebase.google.com>, crea el proyecto **IronMarket** (puedes desactivar Google Analytics).
2. Agrega una app **Android** con el paquete `com.ironmarket.app`, descarga **google-services.json** y cópialo en la carpeta `app/` del proyecto.
3. **Authentication** → Comenzar → **Correo electrónico/contraseña** → Habilitar.
4. **Firestore Database** → Crear base de datos → modo producción → pestaña **Reglas** → pega el contenido de `firebase/firestore.rules` → Publicar.
5. **Storage** (requiere plan Blaze, que tiene cuota gratuita) → Comenzar → pega `firebase/storage.rules`. Si no lo activas, las fotos se guardan comprimidas en Firestore automáticamente.
6. En Android Studio: **Sync Project with Gradle Files** y ejecuta la app en tu celular.
7. En la app, regístrate con **admin@ironmarket.co** (queda como Administrador), luego crea una cuenta de vendedor y una de comprador. En el panel del Administrador usa **Cargar productos de ejemplo**.

## Cuentas de prueba (modo local)

Contraseña de todas: `123456`.

| Rol | Correo |
|---|---|
| Comprador | comprador@ironmarket.co |
| Vendedor | vendedor@ironmarket.co |
| Administrador | admin@ironmarket.co |

## Pasarela ePayco (modo pruebas)

La llave pública y el modo se configuran en `gradle.properties`:

```properties
EPAYCO_PUBLIC_KEY=tu_llave_publica
EPAYCO_TEST=true
```

Para pagar en modo pruebas usa las tarjetas de prueba publicadas en la documentación de ePayco (<https://docs.epayco.com>). En compilaciones de depuración hay un botón «Modo pruebas: simular pago aprobado» por si el sandbox no responde.

## Arquitectura

```
app/src/main/java/com/ironmarket/app/
├── IronMarketApp.kt      # Inicializa la fuente de datos
├── data/
│   ├── Repository.kt          # Contrato de acceso a datos (interfaz)
│   ├── FirebaseRepository.kt  # Firebase Auth + Firestore + Storage
│   ├── SqliteRepository.kt    # SQLiteOpenHelper (modo local y pruebas)
│   ├── Session.kt             # Sesión, credenciales cifradas
│   └── Models.kt, DemoData.kt
├── util/                 # Imágenes, geolocalización, biometría, corrutinas, formato
└── ui/
    ├── auth/      # Splash, login, registro, recuperación
    ├── common/    # Actividad base por rol, menú inferior, perfil
    ├── buyer/     # Inicio, catálogo, detalle, carrito, checkout, ePayco
    ├── seller/    # Productos (CRUD), formulario con cámara/galería, pedidos
    └── admin/     # Dashboard, usuarios (CRUD), productos, reporte de ventas
firebase/          # Reglas de seguridad de Firestore y Storage
```

## Pruebas

```bash
./gradlew testDebugUnitTest
```

`ScreenshotTest` (Robolectric) recorre los flujos de los tres roles contra SQLite: login, carrito, compra, CRUD de usuarios, y guarda capturas reales en `app/build/screens/`.

## Requisitos

Android 8.0 (API 26) o superior · Android Studio Ladybug o más reciente · JDK 17.
