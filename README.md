# MenusCalendar
Calendario de menus

## Copia de seguridad de recetas en Google Drive (opcional)

La app no tiene login. En el primer arranque ofrece **Conectar con Google** o **Continuar sin cuenta**; la elección no se vuelve a pedir y desde Ajustes se puede conectar, hacer copia, restaurar o desconectar en cualquier momento.

- Solo se guardan las **recetas** (no el calendario), en la carpeta oculta `appDataFolder` del Google Drive del propio usuario, con el permiso `drive.appdata`. No hay servidor propio: nadie más ve la cuenta ni los datos, y la app no guarda contraseñas ni tokens.
- Una tarea diaria de WorkManager sube la copia solo si las recetas han cambiado desde la última subida.

### Configuración necesaria en Google Cloud

1. Crear un proyecto en [Google Cloud Console](https://console.cloud.google.com/) y habilitar **Google Drive API**.
2. Configurar la pantalla de consentimiento OAuth (tipo *External*) con el permiso `https://www.googleapis.com/auth/drive.appdata`.
3. Crear un **ID de cliente OAuth de tipo Android** con el paquete `com.gabrieltagama.menuplanner` y la huella SHA-1 del certificado con el que se firma la APK (una por cada certificado: debug, release y la clave de firma de Google Play).

No hace falta ninguna clave en el código: Google identifica la app por paquete y firma.

### Firma fija de la APK debug

La APK que genera GitHub Actions se firma siempre con el mismo certificado debug, guardado como secret `DEBUG_KEYSTORE_BASE64` (keystore PKCS12 en base64; contraseñas `android`, alias `androiddebugkey`). Sin ese secret se usa la clave debug aleatoria del runner y el login con Google no funciona. Su huella SHA-1 es la que hay que registrar en el cliente OAuth Android.
