package com.dns.changer.ultimate.widget

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Helper object to trigger widget updates from anywhere in the app
 */
object WidgetUpdater {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun update(context: Context) {
        scope.launch {
            try {
                DnsWidget.updateWidgetState(context)
            } catch (e: Exception) {
                android.util.Log.e("WidgetUpdater", "Error updating widget", e)
            }
        }
    }
}
