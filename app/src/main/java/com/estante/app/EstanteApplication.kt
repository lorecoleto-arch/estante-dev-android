package com.estante.app

import android.app.Application

class EstanteApplication : Application() {

    /** Criado uma única vez e compartilhado por todo o app. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
