# IronMarket

Tienda virtual Android para el nicho fitness: un marketplace donde tiendas y vendedores publican suplementos, ropa y equipo de gimnasio, y los compradores los consiguen en un solo lugar.

Proyecto académico de la asignatura **Desarrollo de Aplicaciones Móviles Nativas**, Ingeniería de Software, Fundación Universitaria Compensar (UCompensar).

- Autor: Diego Alejandro Jiménez Arias
- Docente: Flavio Alexander Navarro Carmona

## Entrega: Actividad 3, interfaces gráficas

Esta versión implementa el **frontend completo** de la app para los tres roles definidos en el documento técnico (Actividad 2). Los datos son de ejemplo y viven en memoria (`DataRepository`), así que todos los CRUD funcionan mientras la app está abierta. La conexión a Firebase corresponde a la siguiente fase.

## Tecnologías

- Kotlin y layouts XML (View system)
- Material Design 3 con tema oscuro propio y un color de acento por rol
- ViewBinding
- AndroidX Biometric (`BiometricPrompt`) para ingreso con huella y confirmación de pagos
- Cámara mediante `ActivityResultContracts.TakePicturePreview` para fotos de productos
- `RecyclerView`, `Fragment` y `BottomNavigationView`
- Android 8.0 (API 26) o superior

## Pantallas por rol

| Rol | Pantallas |
|---|---|
| Transversal | Splash screen, inicio de sesión (correo, contraseña y huella), registro con rol, recuperación de contraseña, menú inferior (Bottom Navigation) |
| Comprador | Inicio, catálogo con búsqueda y filtros, detalle de producto, carrito (CRUD), pasarela de pagos, perfil de usuario |
| Vendedor | Panel de productos (CRUD completo), historial de pedidos recibidos, perfil de vendedor |
| Administrador | Dashboard, gestión de usuarios (listado, creación, edición, suspensión y eliminación), gestión de productos (revisión, aprobación y eliminación), reporte de ventas |

## Cuentas de prueba

La contraseña de todas las cuentas es `123456`. En la pantalla de inicio de sesión hay botones que llenan el formulario automáticamente.

| Rol | Correo |
|---|---|
| Comprador | comprador@ironmarket.co |
| Vendedor | vendedor@ironmarket.co |
| Administrador | admin@ironmarket.co |

Para probar la huella, primero inicia sesión una vez con correo y contraseña. En el emulador, registra una huella en *Configuración > Seguridad* y usa los controles extendidos del emulador para simularla.

## Estructura del proyecto

```
app/src/main/
├── java/com/ironmarket/app/
│   ├── data/          # Modelos, repositorio en memoria y sesión
│   ├── util/          # Formato de moneda, validaciones y badges
│   └── ui/
│       ├── auth/      # Splash, login, registro y recuperación
│       ├── common/    # Actividad base por rol, contenedor con menú y perfil
│       ├── buyer/     # Pantallas del comprador
│       ├── seller/    # Pantallas del vendedor
│       └── admin/     # Pantallas del administrador
└── res/
    ├── layout/        # 26 layouts XML
    ├── drawable/      # Iconos vectoriales y formas
    ├── values/        # colors, themes, styles, dimens y strings
    ├── font/          # Tipografía Barlow
    ├── color/         # Selectores de color
    └── menu/          # Menú inferior de cada rol
```

## Cómo ejecutarlo

1. Abre la carpeta del proyecto en Android Studio (Ladybug o más reciente).
2. Espera a que termine la sincronización de Gradle.
3. Ejecuta la app en un emulador o en un celular con Android 8.0 o superior.

También puedes instalar directamente el APK incluido en la sección *Releases*.
