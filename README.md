# Dónde Mierda Iba 👻

App Android nativa para llevar el registro de en qué serie, película o video quedaste. Sin cuentas, sin nube de terceros, sin rastreo: todo vive en tu propio dispositivo, y si querés pasar tus listas a otro celular, es un archivo y listo.

Desarrollada por **Black Phantom Tech** — Kotlin nativo, sin frameworks multiplataforma, pensada para andar liviana incluso en celulares viejos.

---

## Por qué existe

Entre series que dejás pausadas, películas que arrancás y no terminás, y videos largos de YouTube o Twitch que retomás días después, perdés la cuenta de "dónde mierda ibas". Esta app resuelve exactamente eso: cargás lo que estás viendo, anotás en qué temporada/capítulo/minuto quedaste, y listo. Nada más.

No hay backend, no hay usuarios, no hay analíticas. Es una libreta digital.

## Funcionalidades

**Seguimiento de contenido**
- Series, películas y videos por separado, cada uno con su propia lógica (temporada + capítulo para series, minuto para películas y videos).
- Vista en lista o en cuadrícula, con buscador en tiempo real.
- Edición rápida de nombre, imagen y progreso sin tener que borrar y recargar.
- Vista previa en grande antes de cambiar una imagen ya elegida (no salta directo al selector) — se ve la imagen completa, sin recortar, sea cual sea su proporción.
- Selector de progreso estilo "candado de valija" (`NumberPicker`) para subir o bajar temporada, capítulo o duración de forma intuitiva — la duración se edita en horas + minutos (mucho más cómodo para películas largas que scrollear un solo contador hasta 150).
- Botón de confirmar (✔) arriba, al lado del de anclar, para guardar los cambios sin tener que bajar hasta el final del modal.
- **Calificación con estrellas**: en series se califica capítulo por capítulo (y se muestra el promedio de la temporada elegida), y en películas y videos se califica una sola vez por título.
- **Marcar como visto completo**: en series se marca capítulo por capítulo, y cuando quedan vistos todos los de una temporada la app la da por completa sola (y cuando se completan todas las temporadas conocidas, la serie entera). En películas y videos es un solo botón. Lo que quedó completado se resalta en la lista con un borde verde fino.
- **Dónde ver**: modal translúcido tipo vidrio que muestra en qué plataformas de streaming está disponible una serie o película en Argentina (suscripción, alquiler o compra), consultando la API de [TMDb](https://www.themoviedb.org/documentation/api).

**Integración con TVmaze**
- Al cargar una serie, se busca automáticamente en la [API pública de TVmaze](https://www.tvmaze.com/api) (gratis, sin API key).
- El candado de capítulo no deja poner más de los que esa temporada realmente tiene, según TVmaze (si la temporada todavía está al aire, el límite es "lo que salió hasta ahora", porque ni TVmaze sabe el total final todavía). El candado de temporada tiene el mismo límite: no deja poner una temporada que la serie todavía no tiene.
- Al cargar una serie por primera vez, si ponés una temporada o capítulo que esa serie no tiene (ej: T3·C23 en una que solo llegó a T1·C08), se ajusta automáticamente al máximo real y te avisa qué se corrigió — en vez de guardar un progreso imposible o rechazar la carga.
- Chequeo periódico en segundo plano (`WorkManager`, cada 6 h, solo con conexión) que avisa por notificación local cuando sale una temporada o capítulo nuevo *ya emitido*. Al iniciar la app también se sugiere (una sola vez) excluirla del ahorro de batería del sistema, porque varios fabricantes (Xiaomi, Samsung, Huawei, etc.) retrasan bastante las tareas en segundo plano de apps que no se abrieron hace rato — sin eso, WorkManager puede tardar en avisar aunque el intervalo configurado sea corto.
- Cada serie vinculada muestra chiquito hasta dónde llegó lo emitido, para saber de un vistazo si te quedaste atrás.
- Si al cargar una serie no elegís ninguna imagen, se usa automáticamente el póster oficial que trae TVmaze (si vos elegís una foto propia, esa tiene prioridad siempre).
- Si una serie no se pudo vincular al cargarla (nombre mal escrito, sin internet en ese momento, etc.) y después le corregís el nombre desde el editor, la app vuelve a intentar vincularla con TVmaze automáticamente — y si lo logra, también trae el póster si todavía no tenías uno elegido.
- Las series se ordenan mostrando primero las que tenés atrasadas respecto a lo último emitido, y después las que ya están al día — así lo que necesita tu atención queda arriba.

**Widget de pantalla de inicio — "Viendo ahora"**
- Mostrá las series que marcaste como "viendo ahora" (📌) directo en el home de tu Android, con imagen y progreso.
- Sumá capítulos, temporada o minutos **sin abrir la app**, tocando los botones de cada fila.
- Tocar el nombre de una serie abre la app directo en su editor completo.
- Widget redimensionable, sin polling agresivo (se refresca por evento, no consume batería de fondo).
- Estética "liquid glass": tarjetas en capas con degradado, brillo diagonal y sombra sutil, avatar con aro, botones tipo píldora con iconografía real (no texto suelto), y el mismo código de color rosa/celeste que la lista principal para marcar si estás atrasado o al día — todo dentro de lo que permite `RemoteViews` (sin blur real ni fuentes custom, esas son limitaciones del sistema de widgets de Android, no del diseño).
- Insignia en la esquina de la miniatura: un ✅ celeste cuando estás al día con esa serie, o un ⚠️ rosa cuando salió algo nuevo que todavía no viste — cambia sola apenas se detecta la novedad.

**Backup portable**
- Exportá todas tus listas (series, películas, videos) a un único archivo `.json`.
- Compartilo por WhatsApp, mail, Drive o lo que uses, e importalo en otro dispositivo con un par de toques.
- Si te llega el archivo por WhatsApp, podés tocarlo directamente y la app se abre para importar, sin pasos intermedios.

**Interfaz**
- Diseño oscuro con estética *glassmorphism* (tarjetas translúcidas, bordes sutiles) y botones estilo iOS.
- Fondo animado con iconografía genérica (nada de logos con copyright).
- Splash screen con logo y animación propios.
- Tour guiado de bienvenida para usuarios nuevos: 7 pasos con un efecto "spotlight" (pantalla atenuada, agujero recortado sobre el botón que se está explicando) y globos de texto con un poco de humor. Hecho a mano con `Canvas`, sin ninguna librería externa. Se muestra una sola vez, pero se puede volver a ver cuando quieras desde el menú de los 3 puntitos (⋮) → "Ver el tour de nuevo".

**Seguimiento fino**
- En el selector de temporada y capítulo, los capítulos ya vistos y las temporadas completas se ven con el número en verde.
- Al marcar un capítulo como visto, la duración se fija sola en lo que dura ese capítulo (dato de TVmaze, con la duración de la serie como respaldo).
- Favoritos ❤️: cualquier serie, película o video se puede marcar y sube al tope de la lista y del widget, con un corazón en la tarjeta.

**Avisos y legales**
- Campanita 🔔 en el encabezado: historial de los últimos avisos de capítulos/temporadas nuevas (se guardan en la base local, hasta 100), para no perderse nada si se borra la notificación del sistema. Punto naranja si hay avisos sin leer; tocar uno abre la serie.
- Términos y condiciones propios dentro del menú ⋮, y créditos/atribuciones (TVmaze, TMDb, JustWatch) en "Acerca de la app".
- El widget mantiene un orden fijo: sumar capítulos o temporadas desde ahí no mueve la serie de lugar.
- Ícono de notificación monocromo con el fantasma de BlackPhantom.

## Capturas

_(pendiente — sacar un par de capturas de la app corriendo y linkearlas acá antes de hacer público el repo)_

## Stack técnico

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Views + ViewBinding, Material Components |
| Persistencia | Room (SQLite) |
| Carga de imágenes | Glide |
| Trabajo en segundo plano | WorkManager |
| Widget | `AppWidgetProvider` + `RemoteViewsService` (colección clásica, sin Jetpack Glance) |
| Networking | `HttpURLConnection` + `org.json` (sin Retrofit/OkHttp, a propósito, para no sumar peso) |
| APIs externas | [TVmaze](https://www.tvmaze.com/api), [TMDb](https://www.themoviedb.org/documentation/api) (solo para "Dónde ver", requiere clave propia) |

**Compatibilidad:** `minSdk 19` (Android 4.4+) / `compileSdk 34`. Usa multidex (`androidx.multidex`) porque por debajo de API 21 no viene soportado de forma nativa.

## Cómo correrlo

```bash
git clone <este-repo>
```

1. Abrir la carpeta del proyecto en Android Studio (Hedgehog o más nuevo).
2. Dejar que sincronice Gradle (regenera el wrapper automáticamente si hace falta).
3. Correr en un emulador o dispositivo físico.

No requiere ninguna API key para lo principal: TVmaze es de acceso libre.

**Opcional — "Dónde ver":** esta función consulta TMDb, que pide una clave gratuita propia de cada persona. No hace falta tocar el código ni ningún archivo: se carga desde la app.

1. Creá una cuenta en [themoviedb.org](https://www.themoviedb.org/) y copiá tu "Clave de API" (v3) en *Ajustes → API* (es gratis).
2. En la app: menú ⋮ → **Clave de TMDb**, pegala y tocá *Probar y guardar* (también te lo ofrece el propio modal de "Dónde ver" si todavía no cargaste una).

La clave se valida contra TMDb y queda guardada solo en tu dispositivo; nunca viaja a ningún otro lado ni forma parte del repo. Si no cargás ninguna, el resto de la app funciona igual.

## Estructura del proyecto

```
app/src/main/java/com/blackphantom/dondemierdaiba/
├── data/        # Entidades y DAO de Room, base de datos
├── ui/          # Activities, Fragments y el tour guiado (OnboardingController)
├── dialogs/     # Modales (agregar, editar progreso, elegir imagen)
├── adapter/     # Adapters de RecyclerView
├── network/     # Clientes HTTP livianos (TVmaze)
├── work/        # WorkManager: chequeo periódico de nuevos capítulos
├── widget/      # AppWidgetProvider + RemoteViewsService del widget
└── util/        # Helpers (backup, notificaciones, preferencias, imágenes)
```

## Roadmap

- [ ] Buscador de imágenes por web dentro de la app (desactivado por ahora — la primera implementación con Openverse tenía un límite de 5 búsquedas por hora que lo hacía inutilizable; evaluando alternativas).
- [x] Vincular automáticamente el póster de TVmaze cuando no se elige una imagen propia al cargar una serie.
- [x] Ordenar series por progreso pendiente (las atrasadas primero, las que ya están al día después).
- [x] Limitar el candado de capítulo al máximo real de cada temporada, según TVmaze.
- [x] Selector de horas además de minutos (más cómodo para películas largas).
- [x] Chequeo periódico más frecuente (6 h) + aviso para excluir la app del ahorro de batería, para que las notificaciones de capítulos nuevos lleguen sin tener que abrir la app.
- [x] Notificaciones con el logo de la app (ícono grande a color) en vez de una campanita genérica.
- [x] Calificación con estrellas por capítulo (con promedio por temporada) y por película/video.
- [x] Marcado de "visto completo", con detección automática de temporada y serie completas, resaltado con borde verde en la lista.
- [x] Botón de confirmar rápido junto al de anclar.
- [x] Modal "Dónde ver" (TMDb / JustWatch) con plataformas de streaming disponibles en Argentina.
- [x] Revisión de bugs visuales: el recorte redondeado de varias miniaturas (lista, cuadrícula, vista previa, buscador de imágenes) estaba calculado en píxeles crudos en vez de convertir por densidad de pantalla, así que en equipos de alta densidad se veían las puntas cuadradas de la foto asomando por las esquinas del marco. También faltaba el recorte redondeado directamente en las imágenes elegidas al cargar contenido nuevo.
- [ ] Selector de posters oficiales de películas vía TMDb (requeriría API key propia — TVmaze solo cubre series).
- [ ] Soporte para más de un idioma.
- [ ] Tests instrumentados para DAO y Workers.

## Licencia

Repositorio personal. Si querés reutilizar código o adaptarlo a tu propio proyecto, avisame — no hay licencia formal asignada todavía.

## Créditos

- Datos de series: [TVmaze API](https://www.tvmaze.com/api), bajo licencia [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
- Datos de disponibilidad de streaming ("Dónde ver"): [TMDb API](https://www.themoviedb.org/documentation/api) — este producto usa la API de TMDb pero no está avalado ni certificado por TMDb.
- Iconografía e ilustraciones: creadas desde cero para este proyecto, sin assets de terceros.

> **Nota legal:** la app no aloja ni reproduce contenido protegido; solo guarda un registro personal. Los términos de la app son un texto orientativo y conviene que los revise un abogado antes de publicarla en una tienda. La API de TMDb es de uso no comercial salvo acuerdo con ellos.

## Base de datos

La base va en su versión 7 (Room: tabla `notification_log` en la v6 y columna `isFavorite` en la v7). Todos los saltos desde la v2 en adelante
tienen migración real (`ALTER TABLE`), así que actualizar la app no borra lo
que ya tenías cargado.

---

**Black Phantom Tech** · San Juan, Argentina
