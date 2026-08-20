package com.kriptobr.mercado

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recebe as notificações enviadas pelo painel do Firebase.
 * Só entra em ação quando existir google-services.json no módulo app —
 * sem o arquivo, o Firebase nunca inicia e esta classe fica dormindo.
 *
 * Para o cupom exclusivo de quem tem o app: no Firebase Console, em Messaging,
 * dá para mirar "usuários que instalaram há mais de X dias" sem escrever
 * nenhuma linha de servidor. O campo "link" vai no bloco de dados da campanha.
 */
class ServicoPush : FirebaseMessagingService() {

    override fun onMessageReceived(msg: RemoteMessage) {
        val titulo = msg.notification?.title ?: msg.data["titulo"]
        val corpo = msg.notification?.body ?: msg.data["corpo"]
        val link = msg.data["link"]
        if (titulo != null || corpo != null) Avisos.mostrar(this, titulo, corpo, link)
    }

    override fun onNewToken(token: String) {
        // Nada a fazer: as campanhas por público do Firebase não precisam do token
        // guardado num servidor nosso. Se um dia houver envio individual, é aqui
        // que o token seria registrado.
    }
}
