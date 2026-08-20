package com.kriptobr.mercado

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recebe as campanhas enviadas pelo painel do Firebase (cupons, novidades).
 * Só entra em ação quando existir google-services.json no módulo app.
 */
class ServicoPush : FirebaseMessagingService() {
    override fun onMessageReceived(msg: RemoteMessage) {
        val titulo = msg.notification?.title ?: msg.data["titulo"] ?: return
        val corpo = msg.notification?.body ?: msg.data["corpo"] ?: ""
        Avisos.mostrar(this, Avisos.CANAL_MARCA, titulo, corpo, msg.data["link"])
    }

    override fun onNewToken(token: String) {
        // As campanhas por público do Firebase não precisam do token num servidor nosso.
    }
}
