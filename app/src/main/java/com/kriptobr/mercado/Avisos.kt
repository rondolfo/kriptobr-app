package com.kriptobr.mercado

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Canal e montagem das notificações da KriptoBR (cupons, alertas de preço, novidades). */
object Avisos {
    const val CANAL = "kriptobr_avisos"

    fun criarCanal(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL,
            ctx.getString(R.string.canal_nome),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = ctx.getString(R.string.canal_desc)
            enableLights(true)
            lightColor = ctx.getColor(R.color.mint)
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    @SuppressLint("MissingPermission")   // o runCatching abaixo cobre o caso de permissão negada
    fun mostrar(ctx: Context, titulo: String?, corpo: String?, link: String?) {
        criarCanal(ctx)
        val destino = Intent(ctx, TelaPrincipal::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!link.isNullOrBlank()) data = Uri.parse(link)
        }
        val pi = PendingIntent.getActivity(
            ctx, System.currentTimeMillis().toInt(), destino,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(ctx.getColor(R.color.mint))
            .setContentTitle(titulo ?: ctx.getString(R.string.app_name))
            .setContentText(corpo ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo ?: ""))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching {
            NotificationManagerCompat.from(ctx).notify(System.currentTimeMillis().toInt(), n)
        }
    }
}
