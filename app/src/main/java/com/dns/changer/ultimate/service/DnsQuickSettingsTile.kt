package com.dns.changer.ultimate.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.dns.changer.ultimate.MainActivity
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.repository.DnsRepository
import com.dns.changer.ultimate.widget.ToggleDnsAction
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DnsQuickSettingsTile : TileService() {

    companion object {
        // Keep for backwards compatibility with MainActivity
        const val EXTRA_SHOW_TILE_PAYWALL = "show_tile_paywall"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface QuickSettingsTileEntryPoint {
        fun dnsConnectionManager(): DnsConnectionManager
        fun dnsRepository(): DnsRepository
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val entryPoint: QuickSettingsTileEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            QuickSettingsTileEntryPoint::class.java
        )
    }

    private val connectionManager: DnsConnectionManager by lazy {
        entryPoint.dnsConnectionManager()
    }

    private val dnsRepository: DnsRepository by lazy {
        entryPoint.dnsRepository()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        scope.launch {
            val currentState = connectionManager.connectionState.value

            when (currentState) {
                is ConnectionState.Connected -> {
                    // Open app with disconnect action (same flow as widget/button - shows ad)
                    openAppWithAction(ToggleDnsAction.ACTION_DISCONNECT)
                }
                is ConnectionState.Disconnected -> {
                    // Check if server is selected
                    val server = dnsRepository.selectedServer.first()
                    if (server == null) {
                        // No server selected - open app normally
                        openApp()
                        return@launch
                    }
                    // Open app with connect action (same flow as widget/button - shows ad)
                    openAppWithAction(ToggleDnsAction.ACTION_CONNECT)
                }
                is ConnectionState.Connecting,
                is ConnectionState.Disconnecting,
                is ConnectionState.Switching -> {
                    // In transition state - just open app
                    openApp()
                }
                is ConnectionState.Error -> {
                    // On error, open app
                    openApp()
                }
            }
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        launchActivity(intent)
    }

    private fun openAppWithAction(action: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ToggleDnsAction.EXTRA_WIDGET_ACTION, action)
        }
        launchActivity(intent)
    }

    private fun launchActivity(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+ requires PendingIntent
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val state = connectionManager.connectionState.value

        when (state) {
            is ConnectionState.Connected -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.app_name)
                tile.subtitle = state.server.name
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_tile)
            }
            is ConnectionState.Disconnected -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.app_name)
                tile.subtitle = getString(R.string.tile_disconnected)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_tile)
            }
            is ConnectionState.Connecting -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.app_name)
                tile.subtitle = getString(R.string.tile_connecting)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_tile)
            }
            is ConnectionState.Disconnecting -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.app_name)
                tile.subtitle = getString(R.string.tile_disconnecting)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_tile)
            }
            is ConnectionState.Switching -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.app_name)
                tile.subtitle = getString(R.string.tile_switching)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_tile)
            }
            is ConnectionState.Error -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.app_name)
                tile.subtitle = getString(R.string.tile_error)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_tile)
            }
        }

        tile.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
