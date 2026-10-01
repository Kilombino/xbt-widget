package com.kilombino.xbtwidget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Descarga el precio y repinta los widgets. Android no deja refrescar un widget
 * más a menudo que cada 30 min por sí solo; con WorkManager se consigue cada 15 min
 * (el mínimo del sistema), y siempre con red disponible.
 */
class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            XbtRepository.refresh(applicationContext)
            HeaderRepository.refresh(applicationContext)   // opcional: si falla, sigue el precio
        }
        Widgets.renderAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "xbt-periodic"
        private const val NOW = "xbt-now"

        private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(ctx: Context) {
            // Retardo inicial aleatorio: si mucha gente instala la app a la vez (tras un
            // anuncio, por ejemplo), sus consultas quedan repartidas en el cuarto de hora
            // en vez de llegar todas en el mismo minuto.
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay((0L..14L).random(), TimeUnit.MINUTES)
                .setConstraints(net).build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun runNow(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<UpdateWorker>().setConstraints(net).build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, req)
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(PERIODIC)
        }
    }
}
