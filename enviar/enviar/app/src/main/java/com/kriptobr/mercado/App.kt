package com.kriptobr.mercado

import android.app.Application
import com.kriptobr.mercado.dados.Idioma

/**
 * Roda antes de tudo no processo — inclusive quando quem acorda o app é o
 * WorkManager ou o widget, sem ninguém abrir a tela. É o lugar certo para fixar
 * o idioma escolhido, senão o alerta em segundo plano sairia no idioma do
 * aparelho enquanto a tela está em outro.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Idioma.aplicarNoProcesso(this)
    }
}
