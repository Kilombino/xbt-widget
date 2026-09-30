package com.kilombino.xbtwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.version).text = getString(R.string.version, BuildConfigInfo.VERSION)

        val units = findViewById<RadioGroup>(R.id.units)
        units.check(if (XbtRepository.unit(this) == XbtRepository.UNIT_POOLCOINS) R.id.unit_poolcoins else R.id.unit_poolsats)
        units.setOnCheckedChangeListener { _, checked ->
            XbtRepository.setUnit(this,
                if (checked == R.id.unit_poolcoins) XbtRepository.UNIT_POOLCOINS else XbtRepository.UNIT_POOLSATS)
            show()
            Widgets.renderAll(this)
        }

        findViewById<Button>(R.id.btn_refresh).setOnClickListener { refresh() }
        findViewById<Button>(R.id.btn_site).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(XbtRepository.SITE)))
        }

        // "Añadir a la pantalla de inicio" solo si el lanzador lo permite.
        val mgr = getSystemService(AppWidgetManager::class.java)
        val pin = findViewById<Button>(R.id.btn_pin)
        if (mgr != null && mgr.isRequestPinAppWidgetSupported) {
            pin.setOnClickListener {
                mgr.requestPinAppWidget(ComponentName(this, XbtWidgetProvider::class.java), null, null)
            }
        } else {
            pin.visibility = View.GONE
            findViewById<TextView>(R.id.pin_hint).visibility = View.VISIBLE
        }

        UpdateWorker.schedule(this)
        show()
        refresh()
    }

    private fun refresh() {
        Thread {
            val q = XbtRepository.refresh(applicationContext)
            main.post {
                show()
                Widgets.renderAll(this)
                if (q == null) Toast.makeText(this, R.string.no_data, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun show() {
        val q = XbtRepository.cached(this)
        val price = findViewById<TextView>(R.id.price)
        val change = findViewById<TextView>(R.id.change)
        if (q == null) {
            price.text = "–"
            change.text = getString(R.string.loading)
            return
        }
        price.text = Format.usd(q.price)
        change.text = Format.change(q.changePct) + if (q.changePct != null) " 24h" else ""
        change.setTextColor(getColor(when {
            q.changePct == null -> R.color.muted
            q.changePct >= 0 -> R.color.up
            else -> R.color.down
        }))
        findViewById<TextView>(R.id.ratio).text = Format.ratio(this, q)
        findViewById<TextView>(R.id.range).text = Format.range(q)
        findViewById<TextView>(R.id.block).text = Format.block(q.height)
        findViewById<TextView>(R.id.updated).text = "xbt.live · " + Format.updated(this, q)
    }
}
