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

/** Canais e montagem das notificações: alertas de preço do usuário e avisos da KriptoBR. */
object Avisos {
    const val CANAL_ALERTAS = "kriptobr_precos"
    const val CANAL_MARCA = "kriptobr_avisos"

    fun criarCanais(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ALERTAS, ctx.getString(R.string.canal_precos), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = ctx.getString(R.string.canal_precos_desc) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_MARCA, ctx.getString(R.string.canal_marca), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = ctx.getString(R.string.canal_marca_desc) }
        )
    }

    @SuppressLint("MissingPermission")   // o runCatching abaixo cobre a permissão negada
    fun mostrar(ctx: Context, canal: String, titulo: String, corpo: String, link: String? = null, id: Int? = null) {
        criarCanais(ctx)
        val destino = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!link.isNullOrBlank()) data = Uri.parse(link)
        }
        val codigo = id ?: System.currentTimeMillis().toInt()
        val pi = PendingIntent.getActivity(
            ctx, codigo, destino,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, canal)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(0xFF00C69F.toInt())
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(codigo, n) }
    }
}
