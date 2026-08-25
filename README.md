# Dónde Mierda Iba

App Android nativa (Kotlin) para llevar el registro de en qué serie, película o
video quedaste. Tema oscuro con **glassmorphism**, botones estilo Apple (pill,
degradados suaves) y un fondo animado con iconitos genéricos (play, claqueta,
TV, rollo de película) flotando — todo dibujado a mano, sin ningún logo real
ni contenido con copyright.

## Cómo abrirlo

1. Abrí **Android Studio** (Hedgehog o más nuevo recomendado).
2. `File > Open` y seleccioná la carpeta `DondeMierdaIba/`.
3. Dejá que Android Studio sincronice Gradle. Como el proyecto no trae el
   `gradle-wrapper.jar` binario (no se puede generar sin conexión a
   `services.gradle.org` desde este entorno), Android Studio te va a
   ofrecer regenerarlo automáticamente al abrir el proyecto — aceptá esa
   sugerencia, o corré `gradle wrapper` una vez si tenés Gradle instalado
   en tu PC. Después de eso `./gradlew` funciona normal.
4. Conectá un dispositivo/emulador y le das Run ▶.

## Compatibilidad

Android 4.5 en adelante.Usé como piso **API 19 (Android 4.4 KitKat)**,
que es la compatibilidad real más baja razonable hoy en día (las librerías
de Google ya no soportan nada anterior). `minSdk` está en `19` en
`app/build.gradle` 

## Qué incluye

- **Splash animado**: logo (persona rascándose la cabeza con un globo de
  pensamiento con un cronómetro adentro) + nombre de la app apareciendo con
  fade/scale, y abajo, chiquito y sutil, "By BlackPhantom" con tu logo del
  fantasma.
- **Fondo dinámico** en toda la app: iconitos genéricos de play/claqueta/TV/
  rollo de película flotando de abajo hacia arriba, muy livianos (10
  bitmaps chicos reciclados, sin GIFs ni video de fondo) para no cargar
  dispositivos viejos.
- **3 secciones separadas**: Series / Películas / Videos, con tabs.
- **Agregar contenido** (bottom sheet modal):
  - Serie: nombre, imagen (galería o URL web), temporada, capítulo y
    minuto (si se deja vacío, arranca en 0).
  - Película: nombre, imagen (galería o URL web) y minuto.
  - Video: nombre, minuto, y elegís la plataforma entre 6 chips
    (YouTube / Twitch / Kick / TikTok / Facebook / X) — cada una con su
    color, generados por mí, **sin usar ningún logo real**.
- **Lista o cuadrícula**, con botón para alternar (se guarda la preferencia).
- **Buscador** en tiempo real dentro de cada sección.
- **Editar progreso**: tocás cualquier ítem guardado y se abre un modal con
  selectores tipo "candado de valija" (NumberPicker con scroll) para subir
  o bajar temporada / capítulo / minuto de forma intuitiva, más un botón
  para eliminar el ítem.
- Todo se guarda localmente con **Room** (SQLite), no depende de internet
  salvo que elijas cargar una imagen desde una URL web.

## Notas de diseño / cosas para retocar vos

- Los 6 colores de plataforma y los degradados están en
  `res/values/colors.xml` — cambiálos ahí si querés otra paleta.
- El logo (`res/drawable/ic_logo.xml`) es un vector armado con formas
  simples; si querés algo más elaborado, te recomiendo abrirlo en Android
  Studio con el editor de vectores o reemplazarlo por un SVG propio con el
  mismo nombre de archivo.
- El ícono de la app (launcher) ya tiene versión adaptativa (Android 8+) y
  fallback en PNG para versiones viejas, generado a partir del mismo
  concepto — podés regenerarlo con mejor calidad usando
  `Image Asset Studio` de Android Studio (click derecho en `res` >
  `New > Image Asset`) tomando como base `ic_logo.xml`.
- Glide se usa para cargar imágenes locales/web de forma liviana y con
  caché automático.

Cualquier ajuste de paleta, tipografía o comportamiento, avisame.

— By BlackPhantom
