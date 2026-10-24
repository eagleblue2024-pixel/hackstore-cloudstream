package com.hackstore

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class HackstorePlugin : Plugin() {
    override fun load(context: Context) {
        // Registrar el proveedor principal de Hackstore
        registerMainAPI(HackstoreProvider())
    }
}
