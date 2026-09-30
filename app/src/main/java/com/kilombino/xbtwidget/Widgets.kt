package com.kilombino.xbtwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews

object Widgets {
    const val ACTION_REFRESH = "com.kilombino.xbtwidget.REFRESH"

    // Por debajo de este ancho/alto solo caben precio y variación.
    private const val FULL_MIN_WIDTH_DP = 180
    private const val FULL_MIN_HEIGHT_DP = 100

    fun renderAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, XbtWidgetProvider::class.java))
        for (id in ids) render(ctx, mgr, id)
    }

    fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val q = XbtRepository.cached(ctx)
        val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: el lanzador elige el diseño según el tamaño real del widget.
            RemoteViews(mapOf(
                SizeF(110f, 40f) to build(ctx, q, full = false),
                SizeF(FULL_MIN_WIDTH_DP.toFloat(), FULL_MIN_HEIGHT_DP.toFloat()) to build(ctx, q, full = true),
            ))
        } else {
            val o = mgr.getAppWidgetOptions(id)
            val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            build(ctx, q, full = w >= FULL_MIN_WIDTH_DP && h >= FULL_MIN_HEIGHT_DP)
        }
        mgr.updateAppWidget(id, views)
    }

    private fun build(ctx: Context, q: XbtQuote?, full: Boolean): RemoteViews {
        val v = RemoteViews(ctx.packageName, if (full) R.layout.widget_full else R.layout.widget_small)

        // Tocar el widget abre la app; el botón circular fuerza una actualización.
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        v.setOnClickPendingIntent(R.id.root, open)
        val refresh = PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, XbtWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        v.setOnClickPendingIntent(R.id.refresh, refresh)

        if (q == null) {
            v.setTextViewText(R.id.price, "–")
            v.setTextViewText(R.id.change, ctx.getString(R.string.loading))
            v.setTextColor(R.id.change, ctx.getColor(R.color.muted))
            if (full) {
                v.setTextViewText(R.id.ratio, "–")
                v.setTextViewText(R.id.range, "–")
                v.setTextViewText(R.id.block, "–")
                v.setTextViewText(R.id.updated, "xbt.live")
            }
            return v
        }

        v.setTextViewText(R.id.price, Format.usd(q.price))
        v.setTextViewText(R.id.change, Format.change(q.changePct) + if (q.changePct != null) " 24h" else "")
        v.setTextColor(R.id.change, ctx.getColor(
            when {
                q.changePct == null -> R.color.muted
                q.changePct >= 0 -> R.color.up
                else -> R.color.down
            }))
        if (full) {
            v.setTextViewText(R.id.ratio, Format.ratio(ctx, q))
            v.setTextViewText(R.id.range, Format.range(q))
            v.setTextViewText(R.id.block, Format.block(q.height))
            v.setTextViewText(R.id.updated, "xbt.live · " + Format.updated(ctx, q))
        }
        return v
    }
}
