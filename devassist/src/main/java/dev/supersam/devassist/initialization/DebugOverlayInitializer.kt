package dev.supersam.devassist.initialization

import android.content.Context
import androidx.startup.Initializer
import dev.supersam.devassist.core.DebugOverlayController
import dev.supersam.devassist.features.DefaultActions
import dev.supersam.devassist.features.DefaultInfoProviders

class DebugOverlayInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // Initialize the controller which sets up lifecycle callbacks
        DebugOverlayController.initialize(context.applicationContext)

        // Register default providers and actions
        DefaultInfoProviders.registerDefaults(context.applicationContext)
        DefaultActions.registerDefaults()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            CrashLogManager.logCrash(
                context.applicationContext,
                throwable
            )
            defaultHandler?.uncaughtException(thread, throwable)
        }

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // Host app initializers can depend on *this* one
        return emptyList()
    }
}