package dev.supersam.devassist.api

/** Public entry point for developers to register custom components. */
object DebugOverlay {
    fun addInfoProvider(provider: DebugInfoProvider) {
        DebugOverlayRegistry.addProvider(provider)
    }

    fun addAction(action: DebugAction) {
        DebugOverlayRegistry.addAction(action)
    }
}