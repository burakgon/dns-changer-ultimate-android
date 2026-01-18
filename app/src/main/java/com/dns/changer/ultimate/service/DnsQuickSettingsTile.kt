package com.dns.changer.ultimate.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.dns.changer.ultimate.MainActivity
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.data.repository.DnsRepository
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
        const val EXTRA_SHOW_TILE_PAYWALL = "show_tile_paywall"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface QuickSettingsTileEntryPoint {
        fun dnsConnectionManager(): DnsConnectionManager
        fun dnsRepository(): DnsRepository
        fun dnsPreferences(): DnsPreferences
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

    private val dnsPreferences: DnsPreferences by lazy {
        entryPoint.dnsPreferences()
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
            // Check if user is premium - Quick Settings tile is a premium feature
            val isPremium = dnsPreferences.isPremium.first()
            if (!isPremium) {
                // Not premium - open app and show paywall
                openAppWithPaywall()
                return@launch
            }

            val currentState = connectionManager.connectionState.value

            when (currentState) {
                is ConnectionState.Connected -> {
                    // Disconnect
                    connectionManager.disconnect()
                }
                is ConnectionState.Disconnected -> {
                    // Connect - need VPN permission check
                    val permissionIntent = connectionManager.checkVpnPermission()
                    if (permissionIntent != null) {
                        // Need VPN permission - open app
                        openApp()
                        return@launch
                    }

                    // Get selected server
                    val server = dnsRepository.selectedServer.first()
                    if (server != null) {
                        connectionManager.connect(server)
                    } else {
                        // No server selected - open app
                        openApp()
                    }
                }
                is ConnectionState.Connecting,
                is ConnectionState.Disconnecting,
                is ConnectionState.Switching -> {
                    // In transition state - do nothing
                }
                is ConnectionState.Error -> {
                    // On error, open app
                    openApp()
                }
            }

            updateTileState()
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        launchActivity(intent)
    }

    private fun openAppWithPaywall() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_SHOW_TILE_PAYWALL, true)
        }
        launchActivity(intent)
    }

    private fun launchActivity(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(intent)
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
