package com.kilombino.xbtwidget

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

/** Datos del cabecero de mempool.kilombino.com, ya calculados en el servidor. */
data class HeaderData(
    val thsXbtDay: Double?,
    val thsUsdDay: Double?,
    val rentPoolsatsPerThDay: Double?,
    val rentUsdPerThDay: Double?,
    val kwhPerXbt: Double?,
    val miner: String?,
    val yshValue: Double?,
    val yshUnit: String?,
    val chainSizeGB: Double?,
)

/**
 * Cliente del endpoint del widget en mempool.kilombino.com. Las protecciones para que
 * muchas instalaciones no carguen el servidor van a los dos lados; las del móvil son:
 *  - Obedece `pollMinutes`: el servidor dicta el intervalo mínimo (nunca menos de 15).
 *  - Ante un 429 respeta `Retry-After` (mínimo 10 min).
 *  - Si falla, espera cada vez el doble (15 → 30 → 60 min… hasta 6 h) en vez de
 *    reintentar a la vez que todos los demás.
 *  - Es opcional: si el servidor no responde, el widget sigue con el precio de xbt.live.
 */
object HeaderRepository {
    const val API = "https://mempool.kilombino.com/api/v1/blake2b/widget"

    private const val PREFS = "xbt_header"
    private const val KEY_JSON = "json"
    private const val KEY_NEXT = "next_allowed"
    private const val KEY_BACKOFF = "backoff_min"
    private const val MIN_POLL_MIN = 15L
    private const val MAX_BACKOFF_MIN = 6 * 60L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cached(ctx: Context): HeaderData? =
        prefs(ctx).getString(KEY_JSON, null)?.let { runCatching { parse(it) }.getOrNull() }

    fun refresh(ctx: Context) {
        val p = prefs(ctx)
        val now = System.currentTimeMillis()
        if (now < p.getLong(KEY_NEXT, 0L)) return
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "XBTWidget-Kilombino/" + BuildConfigInfo.VERSION)
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            if (code == 429) {
                val retry = conn.getHeaderField("Retry-After")?.toLongOrNull() ?: 0L
                val wait = maxOf(retry * 1000, 10 * 60_000L)
                p.edit().putLong(KEY_NEXT, now + wait).apply()
                return
            }
            if (code != 200) throw IllegalStateException("HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val poll = maxOf(JSONObject(body).optLong("pollMinutes", MIN_POLL_MIN), MIN_POLL_MIN)
            parse(body) // valida antes de guardar
            p.edit()
                .putString(KEY_JSON, body)
                // Un poco menos que el intervalo, para no saltarse por segundos el turno
                // del WorkManager (que tampoco es exacto).
                .putLong(KEY_NEXT, now + poll * 60_000L - 60_000L)
                .putLong(KEY_BACKOFF, MIN_POLL_MIN)
                .apply()
        } catch (e: Exception) {
            val backoff = min(p.getLong(KEY_BACKOFF, MIN_POLL_MIN) * 2, MAX_BACKOFF_MIN)
            p.edit().putLong(KEY_NEXT, now + backoff * 60_000L).putLong(KEY_BACKOFF, backoff).apply()
        } finally {
            conn?.disconnect()
        }
    }

    private fun JSONObject.num(k: String): Double? =
        if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null

    private fun JSONObject.str(k: String): String? = if (has(k) && !isNull(k)) optString(k) else null

    private fun parse(json: String): HeaderData {
        val o = JSONObject(json)
        return HeaderData(
            thsXbtDay = o.num("thsBtcDay"),
            thsUsdDay = o.num("thsUsdDay"),
            rentPoolsatsPerThDay = o.num("rentPoolsatsPerThDay"),
            rentUsdPerThDay = o.num("rentUsdPerThDay"),
            kwhPerXbt = o.num("kwhPerBtc"),
            miner = o.str("miner"),
            yshValue = o.num("yshValue"),
            yshUnit = o.str("yshUnit"),
            chainSizeGB = o.num("chainSizeGB"),
        )
    }
}
