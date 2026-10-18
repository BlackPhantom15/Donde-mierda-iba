# Historial de versiones

Cada versión de **Dónde Mierda Iba** está guardada en este repositorio como un commit con su propio *tag*. Para ver o bajar una versión puntual, entrá a **Tags** (o a *Releases*) y elegí la que quieras, o usá los links de la tabla.

> La app se fue armando de a poco, versión por versión: arrancó como una libreta simple y terminó con avisos, widget, calificaciones y todo el diseño neumórfico. Acá queda el camino completo.

| Versión | Fecha | Qué trajo |
|---|---|---|
| [v0.1](../../tree/v0.1) | 25/08/2026 | Primera versión: listas de series, películas y videos, buscador, modal para agregar, selector tipo "candado" para editar el progreso, splash y fondo animado. |
| [v0.2](../../tree/v0.2) | 27/08/2026 | Avisos de capítulos y temporadas nuevas (TVmaze + WorkManager) y vinculación automática con TVmaze al cargar una serie. |
| [v0.3](../../tree/v0.3) | 27/08/2026 | Arreglo del límite de métodos de Android (multidex) y edición de la imagen desde "modificar", con galería y link como opciones. |
| [v0.4](../../tree/v0.4) | 27/08/2026 | Cada serie vinculada muestra su última temporada y último capítulo según TVmaze. |
| [v0.5](../../tree/v0.5) | 27/08/2026 | Exportar e importar las listas en un archivo JSON para compartir o respaldar. |
| [v0.6](../../tree/v0.6) | 29/08/2026 | Widget de pantalla de inicio, carátula automática desde TVmaze y orden inteligente (las series con capítulos pendientes primero). |
| [v0.7](../../tree/v0.7) | 29/08/2026 | Arreglo del widget que se quedaba en "Cargando…". |
| [v0.8](../../tree/v0.8) | 29/08/2026 | Primer rediseño "liquid glass" del widget. |
| [v0.9](../../tree/v0.9) | 29/08/2026 | Ajustes del estilo liquid glass: alineación de textos e íconos en los botones. |
| [v0.10](../../tree/v0.10) | 01/09/2026 | Al renombrar una serie se vuelve a buscar en TVmaze y la imagen tiene vista previa. |
| [v1.0](../../tree/v1.0) | 01/09/2026 | Temporadas y capítulos reales según TVmaze, duración en horas y minutos, y búsqueda de novedades automática. |
| [v1.1](../../tree/v1.1) | 03/09/2026 | Mejoras del widget (indicador de "estoy al día"), vista previa de imágenes sin cortes y tour guiado de bienvenida. |
| v1.2 | 04/09/2026 | Intento de estilo *skeuomorphism*. Esta versión quedó corrupta y **no está incluida** en el repositorio. |
| [v1.3](../../tree/v1.3) | 06/09/2026 | Rediseño neumórfico completo con la nueva paleta naranja y negra. |
| [v1.4](../../tree/v1.4) | 06/09/2026 | Nuevo logo: imagen de inicio e íconos de la app. |
| [v1.5](../../tree/v1.5) | 07/09/2026 | Ajustes de los logos y efecto de luz naranja en las líneas, sobre el fondo neumórfico. |
| [v1.6](../../tree/v1.6) | 07/09/2026 | Ronda de arreglos generales. |
| [v1.7](../../tree/v1.7) | 29/09/2026 | Calificación con estrellas, marcar como visto con halo verde, botón de confirmar rápido, modal "Dónde ver" (TMDb) y arreglos visuales. |
| [v1.8](../../tree/v1.8) | 14/10/2026 | Campanita con historial de avisos, términos y condiciones propios, orden fijo en el widget, ícono de notificación con el fantasma y clave de TMDb propia cargada desde la app. |
| [v1.9](../../tree/v1.9) | 18/10/2026 | Favoritos ❤️ que suben al tope de la lista y del widget. Selector de temporada y capítulo con números en verde (capítulos vistos y temporadas completas). Al avanzar de capítulo, los anteriores se marcan solos como vistos; al pasar de temporada, la anterior queda completa y salta a la siguiente. "Marcar serie vista" tilda todas las temporadas y capítulos de una sola vez. El contador de horas y minutos vuelve a 0 al marcar visto. El backup ahora guarda también calificaciones, vistos y favoritos. |
| [v2.0](../../tree/v2.0) | 18/10/2026 | Rediseño total en modo oscuro: barra inferior flotante con íconos, encabezado nuevo, grilla con pósters a pantalla completa y tarjetas teñidas por tipo. Las series arrancan en T1·C1. Modal animado de "Serie completada" al girar de más en el último capítulo. El tour sale primero en la primera apertura y se puede omitir. |

## Cómo probar una versión vieja

1. En GitHub, elegí el tag en el selector de ramas y tocá **Code → Download ZIP**.
2. Abrí la carpeta con Android Studio y dejá que sincronice Gradle.
3. Compilala y correla en un emulador o en un celular.

Algunas versiones viejas pueden necesitar versiones de Gradle o del SDK de esa época. Si alguna no compila a la primera, probá con la versión de Android Studio y de SDK que indica su `build.gradle`.

## Nota sobre el repositorio

Las versiones se importaron desde carpetas separadas y por eso cada commit lleva la fecha original de la carpeta. Nunca se subieron claves ni archivos locales (`local.properties`). La versión más reciente es la que ves en `main`.

---

**Black Phantom Tech** · San Juan, Argentina
