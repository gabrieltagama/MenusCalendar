# MenusCalendar
Calendario de menus

## Copia de seguridad en Google Drive (opcional)

La app no tiene login. En el primer arranque ofrece **Conectar con Google** o **Continuar sin cuenta**; la elección no se vuelve a pedir y desde Ajustes se puede conectar, hacer copia, restaurar o desconectar en cualquier momento.

- Se guardan solo las **recetas** (el calendario no se copia) en la carpeta oculta `appDataFolder` del Google Drive del propio usuario, con el permiso `drive.appdata`. No hay servidor propio: nadie más ve la cuenta ni los datos, y la app no guarda contraseñas ni tokens.
- Una tarea diaria de WorkManager sube la copia solo si algo ha cambiado desde la última subida.
- Restaurar funciona como importar un fichero compartido, pero solo con las recetas: se añade lo que no existe y se actualiza solo lo que es más antiguo que la copia. Si una copia antigua trae calendario, se ignora.

## Exportar e importar JSON

Cada plato se reconoce primero por su id y, si el id no existe en el dispositivo, por su **nombre y tipo** (sin tener en cuenta mayúsculas, tildes, signos ni espacios). Así, al reinstalar la app e importar un fichero anterior, o al importar el de otra persona, las recetas que ya existen se **actualizan** (si la versión importada es más reciente) en lugar de duplicarse, y los días del calendario apuntan al plato local. Si el fichero trae varias recetas con el mismo nombre y tipo, se queda la más reciente.

### Configuración necesaria en Google Cloud

1. Crear un proyecto en [Google Cloud Console](https://console.cloud.google.com/) y habilitar **Google Drive API**.
2. Configurar la pantalla de consentimiento OAuth (tipo *External*) con el permiso `https://www.googleapis.com/auth/drive.appdata`.
3. Crear un **ID de cliente OAuth de tipo Android** con el paquete `com.gabrieltagama.menuplanner` y la huella SHA-1 del certificado con el que se firma la APK (una por cada certificado: debug, release y la clave de firma de Google Play).

No hace falta ninguna clave en el código: Google identifica la app por paquete y firma.

### Firma fija de la APK debug

La APK que genera GitHub Actions se firma siempre con el mismo certificado debug, guardado como secret `DEBUG_KEYSTORE_BASE64` (keystore PKCS12 en base64; contraseñas `android`, alias `androiddebugkey`). Sin ese secret se usa la clave debug aleatoria del runner y el login con Google no funciona. Su huella SHA-1 es la que hay que registrar en el cliente OAuth Android.

### AAB release firmado para Google Play

Si existen estos secrets en el repositorio, la CI genera también un `app-release.aab` firmado y lo adjunta a la ejecución como artefacto `MenuPlanner-release-aab`:

| Secret | Contenido |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Keystore de subida en base64 (`base64 -w0 upload.jks`) |
| `RELEASE_KEYSTORE_PASSWORD` | Contraseña del keystore |
| `RELEASE_KEY_ALIAS` | Alias de la clave |
| `RELEASE_KEY_PASSWORD` | Contraseña de la clave |

`versionCode` se toma del número de ejecución de GitHub Actions (`GITHUB_RUN_NUMBER`), así que cada build es mayor que el anterior; `versionName` es `1.0.<versionCode>`. En local vale 1.

## Desarrollo

- Módulos: `app`, `core:domain` (Kotlin puro), `core:data` (Room, DataStore, JSON), `core:cloud` y `core:cloud-ui` (Google Drive), `core:ui` y las features `onboarding`, `dishes`, `calendar`, `settings` y `share`. La configuración común de Gradle está en los convention plugins de `build-logic`.
- Todos los tests son de JVM (los de Room, ViewModels y pantallas con Robolectric), así que la CI los ejecuta en cada pull request junto con Android Lint, la cobertura de Kover, ktlint (estilo en `.editorconfig`) y detekt (reglas en `config/detekt/detekt.yml`); ktlint y detekt bloquean si hay infracciones.
- Los esquemas de Room de cada versión están en `core/data/schemas` y los tests de migración los usan; al cambiar la base de datos hay que subir la versión, añadir la migración en `Migrations` y versionar el nuevo esquema que genera el build.
