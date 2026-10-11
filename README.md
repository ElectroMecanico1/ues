# UES Lectura — aplicación nativa para Android

Proyecto Android **nativo en Java**, sin WebView y sin necesidad de Internet. Usa componentes de Android (Activity, TextView, ScrollView, SharedPreferences, TextToSpeech y el selector de archivos del sistema). **No es la PWA ni un HTML convertido a aplicación.**

## Contenido

- **450** preguntas de banco; **100** del simulacro; **100** de la prueba histórica UES 2016 (en total 650 tarjetas, algunas con contenido repetido entre colecciones).
- Organización por **Ciencias Sociales, Biología, Física, Química, Matemática, Lenguaje y Literatura**.
- Una pregunta y su respuesta correcta por pantalla; botones Anterior/Siguiente y gesto de desplazamiento horizontal.
- **122 preguntas de Matemática con desarrollo paso a paso** tal como están en el material previo.
- Guarda automáticamente la posición y las preguntas vistas **por colección y materia**.
- Botón **Continuá donde te quedaste**, favoritos, búsqueda, aumento de letra y lectura en voz alta mediante voz instalada en Android.
- **Exportar/Importar progreso** como JSON compatible con las copias de seguridad de la versión HTML, para cambiar desde PC o celular.
- **Uso sin conexión**, sin cuentas, sin anuncios, sin permisos de Internet ni envío de información externa.
- App con ícono y entrada normal en el cajón de aplicaciones.

## IMPORTANTE: ¿dónde está el APK?

Este ZIP contiene el **proyecto Android listo para compilar**, no un APK compilado. El entorno de generación no dispone de Android SDK/Gradle ni de acceso a repositorios de compilación, por lo que no fue posible crear ni probar un APK real aquí. La app necesita una compilación para instalarse.

### Opción 1: Generar APK desde Android Studio en Windows

1. Descargá y descomprimí este ZIP.
2. Instalá **Android Studio** (versión actual) en una PC con Internet.
3. Seleccioná **Open** y elegí la carpeta `UES_Lectura_Android_Nativo` (donde está `settings.gradle`).
4. Esperá a que descargue el SDK, Gradle y los componentes y finalice **Gradle Sync**. Usa Java 17, Gradle 8.7, AGP 8.5.2 y Android SDK 34.
5. En **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Si el menú es diferente: **Build → Generate App Bundles or APKs → Generate APKs** (varía entre versiones).
6. El APK de prueba estará en `app/build/outputs/apk/debug/app-debug.apk`.
7. Pasá ese archivo al teléfono por USB, Bluetooth o archivo compartido y abrilo para instalar. Android puede pedirte que habilités la instalación desde la app que abra ese archivo.

**Este APK de depuración es instalable**, pero debe compilarse y firmarse con la clave de depuración de Android Studio; para distribuir una versión estable, generá un APK de lanzamiento firmado con tu propia clave y conservá ese archivo de firma.

### Opción 2: GitHub Actions, sin instalar Android Studio

La carpeta `.github/workflows/construir-apk.yml` define una compilación automática.

1. Creá un repositorio en GitHub y subí **todos los archivos y carpetas** de este proyecto, incluida `.github/workflows`.
2. Entrá a la pestaña **Actions** y buscá **Crear APK Android**.
3. Si el flujo no se inició con el push, tocá **Run workflow**.
4. Cuando termine, abrí la ejecución completada y descargá el artefacto **UES-Lectura-Android-APK**.
5. Descomprimí el artefacto para obtener `app-debug.apk`, instalable en Android.

> La compilación en GitHub no fue ejecutada aquí. Si hay errores en la ejecución, compartí el registro para corregirlos. No subas al repositorio tus archivos JSON de avance, porque contienen tus preferencias de estudio.

## Migrar el progreso de UES Lectura HTML

1. Abrí la versión HTML en el navegador donde estudiabas y tocá **Guardar copia**. Esto crea `*.json`.
2. Transferí ese JSON al celular.
3. En la app Android, usá **Importar progreso de HTML** y elegí el JSON.
4. La app recuperará posiciones, marcadores y tamaño de letra. Confirmá la importación antes de reemplazar cualquier avance nativo.

Las preguntas y respuestas están incorporadas sin cambio desde los materiales que se utilizaron en la app anterior. **Son material de preparación; no constituyen un examen oficial actual de la UES ni una clave oficial de 2026.**

## Datos técnicos

- Paquete: `sv.ues.lectura`
- Java 17, Android SDK 34, minSdk 24, targetSdk 34.
- Datos: `app/src/main/assets/preguntas.json` (650 registros).
- Código: `app/src/main/java/sv/ues/lectura/`.
- Respaldos: formato compatible con `UES Lectura` versión 2.
- Sin permisos de Internet, almacenamiento externo ni acceso a cámara.
