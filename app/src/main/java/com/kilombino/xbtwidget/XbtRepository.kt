package com.kilombino.xbtwidget

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Una lectura del endpoint público de xbt.live. */
data class XbtQuote(
    val price: Double?,
    val changePct: Double?,
    val high24: Double?,
    val low24: Double?,
    /** Cuántos Poolcoins (Spamchain, la cadena SHA256d) vale 1 XBT. */
    val xbtPerPoolcoin: Double?,
    val height: Long?,
    val updatedMs: Long,
    val fetchedMs: Long,
)

object XbtRepository {
    const val API = "https://xbt.live/api/widget"
    const val SITE = "https://xbt.live"

    // xbt.live pide no consultar más de una vez por minuto. Se respeta también
    // cuando se pulsa "actualizar" varias veces seguidas.
    private const val MIN_INTERVAL_MS = 60_000L

    private const val PREFS = "xbt"
    private const val KEY_JSON = "last_json"
    private const val KEY_FETCHED = "fetched_at"
    private const val KEY_UNIT = "unit"

    const val UNIT_POOLSATS = "poolsats"
    const val UNIT_POOLCOINS = "poolcoins"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun unit(ctx: Context): String = prefs(ctx).getString(KEY_UNIT, UNIT_POOLSATS) ?: UNIT_POOLSATS
    fun setUnit(ctx: Context, unit: String) = prefs(ctx).edit().putString(KEY_UNIT, unit).apply()

    fun cached(ctx: Context): XbtQuote? {
        val p = prefs(ctx)
        val json = p.getString(KEY_JSON, null) ?: return null
        return runCatching { parse(json, p.getLong(KEY_FETCHED, 0L)) }.getOrNull()
    }

    /**
     * Descarga una lectura nueva si ha pasado al menos un minuto desde la anterior.
     * Si falla la red, devuelve la última guardada: un widget con un dato de hace un
     * rato (marcado como antiguo) es más útil que uno en blanco.
     */
    fun refresh(ctx: Context): XbtQuote? {
        val p = prefs(ctx)
        val now = System.currentTimeMillis()
        if (now - p.getLong(KEY_FETCHED, 0L) < MIN_INTERVAL_MS) return cached(ctx)
        return try {
            val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "XBTWidget-Kilombino/" + BuildConfigInfo.VERSION)
                setRequestProperty("Accept", "application/json")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val q = parse(body, now)
            if (q.price == null) return cached(ctx)
            p.edit().putString(KEY_JSON, body).putLong(KEY_FETCHED, now).apply()
            q
        } catch (e: Exception) {
            cached(ctx)
        }
    }

    private fun JSONObject.optNum(k: String): Double? =
        if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null

    private fun parse(json: String, fetchedMs: Long): XbtQuote {
        val o = JSONObject(json)
        return XbtQuote(
            price = o.optNum("price"),
            changePct = o.optNum("changePct"),
            high24 = o.optNum("high24"),
            low24 = o.optNum("low24"),
            xbtPerPoolcoin = o.optNum("xbtPerBtc"),
            height = o.optNum("height")?.toLong(),
            updatedMs = o.optLong("updated", fetchedMs),
            fetchedMs = fetchedMs,
        )
    }
}

/** Evita depender de BuildConfig (desactivado por defecto en AGP 8). */
object BuildConfigInfo {
    const val VERSION = "1.1.0"
}
