package com.kilombino.xbtwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

class XbtWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) Widgets.render(ctx, mgr, id)   // pinta ya con lo guardado
        UpdateWorker.schedule(ctx)
        UpdateWorker.runNow(ctx)
    }

    override fun onEnabled(ctx: Context) {
        UpdateWorker.schedule(ctx)
    }

    override fun onDisabled(ctx: Context) {
        UpdateWorker.cancel(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, opts: Bundle) {
        Widgets.render(ctx, mgr, id)                   // se ha redimensionado
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == Widgets.ACTION_REFRESH) UpdateWorker.runNow(ctx)
    }
}
