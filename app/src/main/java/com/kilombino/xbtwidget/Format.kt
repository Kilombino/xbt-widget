package com.kilombino.xbtwidget

import android.content.Context
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Format {
    // Lectura con más de 45 minutos: se marca como antigua.
    private const val STALE_MS = 45 * 60_000L

    private fun loc(): Locale = Locale.getDefault()

    fun usd(v: Double?): String = if (v == null) "–" else "$" + String.format(loc(), "%,.2f", v)

    fun change(v: Double?): String =
        if (v == null) "" else (if (v >= 0) "▲ " else "▼ ") + String.format(loc(), "%.2f%%", abs(v))

    fun range(q: XbtQuote): String =
        if (q.low24 == null || q.high24 == null) "–" else "${usd(q.low24)} – ${usd(q.high24)}"

    fun block(h: Long?): String = if (h == null) "–" else "#" + String.format(loc(), "%,d", h)

    /** Relación con la cadena mayoritaria, en Poolsats o en Poolcoins. */
    fun ratio(ctx: Context, q: XbtQuote): String {
        val r = q.xbtPerPoolcoin ?: return "–"
        return if (XbtRepository.unit(ctx) == XbtRepository.UNIT_POOLCOINS) {
            String.format(loc(), "%.5f", r) + " " + ctx.getString(R.string.poolcoins)
        } else {
            String.format(loc(), "%,d", (r * 1e8).roundToLong()) + " " + ctx.getString(R.string.poolsats)
        }
    }

    fun isStale(q: XbtQuote): Boolean = System.currentTimeMillis() - q.fetchedMs > STALE_MS

    fun updated(ctx: Context, q: XbtQuote): String {
        val t = DateFormat.getTimeInstance(DateFormat.SHORT, loc()).format(Date(q.fetchedMs))
        return if (isStale(q)) ctx.getString(R.string.updated_stale, t) else ctx.getString(R.string.updated_at, t)
    }
}
