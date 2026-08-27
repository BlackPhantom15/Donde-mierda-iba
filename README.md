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

- **Aviso de nuevos capítulos/temporadas**: al agregar una serie, la app la
  busca automáticamente en **TVmaze** (API pública y gratuita, sin API key,
  sin datos con copyright — solo título/temporada/capítulo/fecha, que son
  hechos objetivos). Si la encuentra, queda vinculada y cada 12hs (con
  conexión a internet) se chequea si salió algo nuevo *ya emitido*; si es
  así, dispara una notificación local. También hay un botón de refresco (🔄)
  en el header para forzar el chequeo al toque, útil para probarlo sin
  esperar. Si no se encuentra la serie en TVmaze (nombre raro, sin
  internet, etc.), igual se guarda, solo que sin avisos automáticos.

- **Editar la imagen después de guardar**: en el modal de "actualizar progreso"
  ahora también se puede tocar la foto (lápiz en la esquina) para cambiarla,
  en series, películas y videos. Para videos, además aparecen los chips de
  plataforma para poder cambiarla directamente (esto reemplaza la imagen por
  el badge de la nueva plataforma).
- **Buscador de fotos tipo WhatsApp**: en vez de tener que pegar un link a
  mano, tocar "Buscar foto en la web" abre un buscador con grilla de
  resultados (como al cambiar la foto de un grupo de WhatsApp): escribís,
  aparecen miniaturas, tocás una y queda elegida. Usa **Openverse**
  (`api.openverse.org`), el buscador de imágenes de dominio público /
  Creative Commons — gratis, sin API key, sin problemas de copyright.
  Pegar un link manualmente sigue disponible como opción secundaria chiquita
  por si no encontrás lo que buscabas.

- **Buscador de fotos tipo WhatsApp**: al elegir imagen hay 3 opciones con
  igual jerarquía: **galería**, **buscar en la web** (grilla de resultados,
  escribís, tocás una miniatura y queda elegida) y **pegar un link (URL)** a
  mano. El buscador usa **Wikimedia Commons** (`commons.wikimedia.org`, el
  mismo banco de imágenes libres que usa Wikipedia) — gratis, sin API key,
  con un límite de uso mucho más generoso que otras alternativas "sin
  registro" como Openverse (que solo daba 5 búsquedas por hora y por eso a
  veces no encontraba nada).
- **"Hasta dónde va" cada serie vinculada a TVmaze**: debajo del progreso
  propio (T2 · C05 · 34 min), las series vinculadas muestran chiquito lo
  último que salió: "Último: T4 · C10" en rosa si te falta ponerte al día,
  o "✅ Estás al día" en celeste si ya estás a la altura de lo emitido.
- **Compartir e importar tus listas sin cuentas ni login**: el botón de
  los 3 puntitos (⋮) en el header abre un menú con dos opciones:
  - **Compartir mis listas**: genera un archivo `.json` con todo lo
    guardado (series, películas y videos) y abre el selector nativo de
    Android para mandarlo por WhatsApp, mail, Drive, lo que sea.
  - **Importar desde un archivo**: elegís el archivo recibido y se agrega
    todo a tu lista actual (te muestra antes cuántos elementos va a traer,
    separados por tipo, para que confirmes).
  - Además, si te llega el archivo por WhatsApp y lo tocás directamente
    desde ahí, aparece "Dónde Mierda Iba" como opción para abrirlo e
    importa automáticamente — no hace falta entrar a la app primero.
  - Nota: importar siempre **agrega** contenido nuevo, nunca pisa ni
    intenta "fusionar" por nombre — si algo ya lo tenías, va a quedar
    duplicado y lo borrás a mano desde el modal de editar progreso. Se
    hizo así a propósito para no arriesgar perder datos por una mala
    coincidencia de nombres.

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

## ⚠️ Importante sobre esta actualización

Se agregaron columnas nuevas a la base de datos local (para el vínculo con
TVmaze) y la migración está configurada como **destructiva**
(`fallbackToDestructiveMigration()`), lo que significa que **si ya tenías
series/películas/videos cargados en el dispositivo con la versión
anterior, se van a borrar al actualizar** (se recrea la tabla desde cero).
Si eso te importa, avisame y te armo una migración real que preserve los
datos existentes; no la hice por defecto para no complicar el código en
esta etapa temprana del proyecto.

— By BlackPhantom
