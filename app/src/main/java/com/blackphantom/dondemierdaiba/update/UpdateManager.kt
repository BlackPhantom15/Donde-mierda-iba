package com.blackphantom.dondemierdaiba.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Actualizador propio: consulta la última release pública de GitHub, baja el
 * APK adjunto con el DownloadManager del sistema y lo deja listo para que
 * Android lo instale. Sin librerías extra, sin servidor y sin cuentas.
 *
 * Límite de Android: la instalación siempre la confirma el usuario en la
 * pantalla del instalador del sistema; una app común no puede saltearla.
 */
object UpdateManager {

    const val OWNER = "BlackPhantom15"
    const val REPO = "Donde-mierda-iba"
    private const val LATEST_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    private const val TIMEOUT_MS = 10000
    private const val UPDATES_SUBDIR = "updates"

    data class Release(
        val tag: String,
        val notes: String,
        val apkUrl: String,
        val apkName: String,
        val apkSize: Long,
        val sha256: String?,
        val pageUrl: String
    )

    sealed class CheckResult {
        data class Available(val release: Release) : CheckResult()
        object UpToDate : CheckResult()
        object Error : CheckResult()
    }

    data class DownloadState(val status: Int, val done: Long, val total: Long)

    // ---------- Versiones ----------

    fun currentVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    } catch (e: Exception) {
        ""
    }

    /** "v2.1" -> [2, 1]. Ignora cualquier texto que no sean números. */
    private fun parts(version: String): List<Int> =
        Regex("\\d+").findAll(version).mapNotNull { it.value.toIntOrNull() }.toList()

    fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote)
        val l = parts(local)
        if (r.isEmpty()) return false
        val size = maxOf(r.size, l.size)
        for (i in 0 until size) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    // ---------- Consulta a GitHub ----------

    suspend fun check(context: Context): CheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = URL(LATEST_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "DondeMierdaIba-Android")
            try {
                when (conn.responseCode) {
                    200 -> Unit
                    404 -> return@withContext CheckResult.UpToDate // todavía no hay releases
                    else -> return@withContext CheckResult.Error
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val release = parseRelease(body) ?: return@withContext CheckResult.UpToDate
                if (isNewer(release.tag, currentVersionName(context))) {
                    CheckResult.Available(release)
                } else {
                    CheckResult.UpToDate
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            CheckResult.Error
        }
    }

    private fun parseRelease(json: String): Release? {
        val obj = JSONObject(json)
        val tag = obj.optString("tag_name").takeIf { it.isNotBlank() } ?: return null
        val notes = obj.optString("body").trim()
        val assets = obj.optJSONArray("assets") ?: return null

        var chosen: JSONObject? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                // Si hay varios APK, se prefiere el que no diga "debug".
                if (chosen == null || (chosen.optString("name").contains("debug", true) && !name.contains("debug", true))) {
                    chosen = a
                }
            }
        }
        val asset = chosen ?: return null
        val url = asset.optString("browser_download_url")
        if (!isTrustedUrl(url)) return null

        // Hash opcional para verificar la descarga: lo da GitHub en "digest"
        // ("sha256:...") o se puede escribir en el texto del release.
        val digest = asset.optString("digest").removePrefix("sha256:").trim().lowercase()
        val fromDigest = digest.takeIf { Regex("^[a-f0-9]{64}$").matches(it) }
        val fromNotes = Regex("(?i)sha-?256\\W{0,6}([a-f0-9]{64})").find(notes)?.groupValues?.get(1)?.lowercase()

        return Release(
            tag = tag,
            notes = notes,
            apkUrl = url,
            apkName = asset.optString("name"),
            apkSize = asset.optLong("size", 0L),
            sha256 = fromDigest ?: fromNotes,
            pageUrl = obj.optString("html_url")
        )
    }

    /** Solo se baja por https desde GitHub. */
    private fun isTrustedUrl(url: String): Boolean {
        val host = try { Uri.parse(url).host.orEmpty() } catch (e: Exception) { "" }
        return url.startsWith("https://") &&
            (host == "github.com" || host.endsWith(".github.com") || host.endsWith(".githubusercontent.com"))
    }

    // ---------- Archivos ----------

    private fun updatesDir(context: Context): File? {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
        return File(base, UPDATES_SUBDIR)
    }

    private fun safeName(release: Release): String =
        release.apkName.replace(Regex("[^A-Za-z0-9._-]"), "_")

    fun apkFile(context: Context, release: Release): File? =
        updatesDir(context)?.let { File(it, safeName(release)) }

    /** True si ya hay un APK completo y válido de esa release (así no se baja dos veces). */
    suspend fun isAlreadyDownloaded(context: Context, release: Release): Boolean {
        val file = apkFile(context, release) ?: return false
        if (!file.exists()) return false
        if (release.apkSize > 0 && file.length() != release.apkSize) return false
        return verify(context, file, release)
    }

    /** Borra los APK viejos de actualizaciones que ya están instaladas. */
    suspend fun cleanupOldApks(context: Context) = withContext(Dispatchers.IO) {
        val dir = updatesDir(context) ?: return@withContext
        val current = currentVersionName(context)
        dir.listFiles()?.forEach { f ->
            val info = context.packageManager.getPackageArchiveInfo(f.path, 0)
            val v = info?.versionName
            if (v != null && !isNewer(v, current)) f.delete()
        }
    }

    // ---------- Descarga ----------

    /** Devuelve el id de descarga del sistema, o null si no se pudo empezar. */
    fun startDownload(context: Context, release: Release): Long? {
        return try {
            val dir = updatesDir(context) ?: return null
            dir.mkdirs()
            File(dir, safeName(release)).delete()
            val request = DownloadManager.Request(Uri.parse(release.apkUrl))
                .setTitle(release.apkName)
                .setDescription("Dónde Mierda Iba ${release.tag}")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(
                    context, Environment.DIRECTORY_DOWNLOADS, "$UPDATES_SUBDIR/${safeName(release)}"
                )
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        } catch (e: Exception) {
            null
        }
    }

    fun cancelDownload(context: Context, id: Long) {
        try {
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(id)
        } catch (e: Exception) {
        }
    }

    /** status = -1 si la descarga ya no existe (la canceló el usuario desde el sistema). */
    fun queryDownload(context: Context, id: Long): DownloadState {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (c.moveToFirst()) {
                return DownloadState(
                    status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                    done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                )
            }
        }
        return DownloadState(-1, 0, 0)
    }

    // ---------- Verificación ----------

    /**
     * 1) Si la release trae SHA-256, tiene que coincidir.
     * 2) El archivo tiene que ser un APK de esta misma app (mismo paquete).
     * La firma la vuelve a comprobar Android al instalar.
     */
    suspend fun verify(context: Context, file: File, release: Release): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext false
            val expected = release.sha256
            if (expected != null) {
                val md = MessageDigest.getInstance("SHA-256")
                file.inputStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        md.update(buf, 0, n)
                    }
                }
                val actual = md.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(expected, ignoreCase = true)) return@withContext false
            }
            val info = context.packageManager.getPackageArchiveInfo(file.path, 0)
            info != null && info.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    // ---------- Instalación ----------

    /** Desde Android 8 hay que habilitar "instalar apps desconocidas" para esta app (una sola vez). */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            } catch (e2: Exception) {
            }
        }
    }

    /** Abre el instalador del sistema. Devuelve false si no pudo. */
    fun install(context: Context, file: File): Boolean {
        return try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
