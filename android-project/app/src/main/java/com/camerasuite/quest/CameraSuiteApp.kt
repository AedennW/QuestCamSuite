package com.camerasuite.quest

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class CameraSuiteApp : Application() {

    /** App-wide scope for capture pipelines that outlive a given screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        const val NOTIFY_CHANNEL = "captures"

        private val _events = MutableSharedFlow<String>(
            extraBufferCapacity = 64,
            onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
        )
        val events: SharedFlow<String> = _events

        /** Fire-and-forget snackbar/toast text from any layer. */
        fun emit(message: String) {
            _events.tryEmit(message)
        }
    }
}
