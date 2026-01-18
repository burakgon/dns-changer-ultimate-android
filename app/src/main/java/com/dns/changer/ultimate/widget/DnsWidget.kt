package com.dns.changer.ultimate.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dns.changer.ultimate.MainActivity
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.data.repository.DnsRepository
import com.dns.changer.ultimate.service.DnsConnectionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DnsWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val IS_CONNECTED = booleanPreferencesKey("is_connected")
        val IS_CONNECTING = booleanPreferencesKey("is_connecting")
        val SERVER_NAME = stringPreferencesKey("server_name")
        val SERVER_ID = stringPreferencesKey("server_id")
        val PRIMARY_DNS = stringPreferencesKey("primary_dns")
        val SECONDARY_DNS = stringPreferencesKey("secondary_dns")

        suspend fun updateWidgetState(context: Context) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DnsWidgetEntryPoint::class.java
            )
            val connectionManager = entryPoint.dnsConnectionManager()
            val dnsRepository = entryPoint.dnsRepository()

            val state = connectionManager.connectionState.value
            val selectedServer = dnsRepository.selectedServer.first()

            GlanceAppWidgetManager(context).getGlanceIds(DnsWidget::class.java).forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[IS_CONNECTED] = state is ConnectionState.Connected
                    prefs[IS_CONNECTING] = state is ConnectionState.Connecting || state is ConnectionState.Switching
                    when (state) {
                        is ConnectionState.Connected -> {
                            prefs[SERVER_NAME] = state.server.name
                            prefs[SERVER_ID] = state.server.id
                            prefs[PRIMARY_DNS] = state.server.primaryDns
                            prefs[SECONDARY_DNS] = state.server.secondaryDns
                        }
                        else -> {
                            selectedServer?.let { server ->
                                prefs[SERVER_NAME] = server.name
                                prefs[SERVER_ID] = server.id
                                prefs[PRIMARY_DNS] = server.primaryDns
                                prefs[SECONDARY_DNS] = server.secondaryDns
                            }
                        }
                    }
                }
            }

            DnsWidget().updateAll(context)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DnsWidgetEntryPoint {
        fun dnsConnectionManager(): DnsConnectionManager
        fun dnsRepository(): DnsRepository
        fun dnsPreferences(): DnsPreferences
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Update state before rendering
        updateWidgetState(context)

        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val isConnected = prefs[IS_CONNECTED] ?: false
        val isConnecting = prefs[IS_CONNECTING] ?: false
        val serverName = prefs[SERVER_NAME] ?: "Cloudflare"

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.widgetBackground)
                .clickable(
                    onClick = if (isConnecting) {
                        actionStartActivity<MainActivity>()
                    } else {
                        actionRunCallback<ToggleDnsAction>()
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator - use image with colored background
                Image(
                    provider = ImageProvider(
                        when {
                            isConnecting -> R.drawable.ic_widget_status_connecting
                            isConnected -> R.drawable.ic_widget_status_connected
                            else -> R.drawable.ic_widget_status_disconnected
                        }
                    ),
                    contentDescription = null,
                    modifier = GlanceModifier.size(40.dp)
                )

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Text content
                Column(
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Text(
                        text = when {
                            isConnecting -> "Connecting..."
                            isConnected -> serverName
                            else -> "DNS Off"
                        },
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = when {
                            isConnecting -> "Please wait"
                            isConnected -> "Tap to disconnect"
                            else -> "Tap to connect"
                        },
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

class ToggleDnsAction : ActionCallback {
    companion object {
        const val EXTRA_WIDGET_ACTION = "widget_action"
        const val ACTION_CONNECT = "connect"
        const val ACTION_DISCONNECT = "disconnect"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DnsWidget.DnsWidgetEntryPoint::class.java
        )
        val connectionManager = entryPoint.dnsConnectionManager()

        val currentState = connectionManager.connectionState.value

        // Open app with action - let the app handle connect/disconnect to show overlays
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            when (currentState) {
                is ConnectionState.Connected -> {
                    putExtra(EXTRA_WIDGET_ACTION, ACTION_DISCONNECT)
                }
                is ConnectionState.Disconnected, is ConnectionState.Error -> {
                    putExtra(EXTRA_WIDGET_ACTION, ACTION_CONNECT)
                }
                else -> {
                    // In transition - just open app
                }
            }
        }
        context.startActivity(intent)
    }
}
