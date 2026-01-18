package com.dns.changer.ultimate.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class DnsWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = DnsWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Handle custom update broadcasts
        if (intent.action == ACTION_UPDATE_WIDGET) {
            // Widget will update via GlanceAppWidget mechanism
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.dns.changer.ultimate.ACTION_UPDATE_WIDGET"
    }
}
