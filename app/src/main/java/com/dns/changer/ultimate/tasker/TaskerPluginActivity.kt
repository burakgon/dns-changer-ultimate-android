package com.dns.changer.ultimate.tasker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.ui.theme.DnsChangerTheme

/**
 * Tasker Plugin Configuration Activity
 *
 * This activity implements the Locale Plugin API to allow DNS Changer
 * to appear as a native plugin in Tasker's action list.
 *
 * Intent Filter: com.twofortyfouram.locale.intent.action.EDIT_SETTING
 */
class TaskerPluginActivity : ComponentActivity() {

    companion object {
        // Locale Plugin API constants
        const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
        const val EXTRA_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"

        // Bundle keys for our configuration
        const val BUNDLE_ACTION = "action"
        const val BUNDLE_SERVER_ID = "server_id"
        const val BUNDLE_PRIMARY_DNS = "primary_dns"
        const val BUNDLE_SECONDARY_DNS = "secondary_dns"
        const val BUNDLE_SERVER_NAME = "server_name"
        const val BUNDLE_IS_DOH = "is_doh"
        const val BUNDLE_DOH_URL = "doh_url"

        // Action types
        const val ACTION_CONNECT = "connect"
        const val ACTION_DISCONNECT = "disconnect"
        const val ACTION_SWITCH = "switch"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse existing configuration if editing
        val existingBundle = intent.getBundleExtra(EXTRA_BUNDLE)
        val existingAction = existingBundle?.getString(BUNDLE_ACTION)
        val existingServerId = existingBundle?.getString(BUNDLE_SERVER_ID)

        setContent {
            DnsChangerTheme {
                TaskerPluginScreen(
                    initialAction = existingAction,
                    initialServerId = existingServerId,
                    onSave = { action, serverId, serverName ->
                        saveAndFinish(action, serverId, serverName)
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    private fun saveAndFinish(action: String, serverId: String?, serverName: String?) {
        val resultBundle = Bundle().apply {
            putString(BUNDLE_ACTION, action)
            if (serverId != null) {
                putString(BUNDLE_SERVER_ID, serverId)
            }
        }

        // Create blurb (human-readable description shown in Tasker)
        val blurb = when (action) {
            ACTION_CONNECT -> "Connect to ${serverName ?: "DNS"}"
            ACTION_DISCONNECT -> "Disconnect from DNS"
            ACTION_SWITCH -> "Switch to ${serverName ?: "DNS"}"
            else -> "DNS Action"
        }

        val resultIntent = Intent().apply {
            putExtra(EXTRA_BUNDLE, resultBundle)
            putExtra(EXTRA_BLURB, blurb)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskerPluginScreen(
    initialAction: String?,
    initialServerId: String?,
    onSave: (action: String, serverId: String?, serverName: String?) -> Unit,
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableStateOf(if (initialAction != null) 1 else 0) }
    var selectedAction by remember { mutableStateOf(initialAction ?: "") }
    var selectedServerId by remember { mutableStateOf(initialServerId) }
    var selectedServerName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DNS Changer - Tasker") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentStep) {
                0 -> ActionSelectionStep(
                    onActionSelected = { action ->
                        selectedAction = action
                        if (action == TaskerPluginActivity.ACTION_DISCONNECT) {
                            // Disconnect doesn't need server selection
                            onSave(action, null, null)
                        } else {
                            currentStep = 1
                        }
                    }
                )
                1 -> ServerSelectionStep(
                    onServerSelected = { serverId, serverName ->
                        selectedServerId = serverId
                        selectedServerName = serverName
                        onSave(selectedAction, serverId, serverName)
                    },
                    onBack = { currentStep = 0 }
                )
            }
        }
    }
}

@Composable
private fun ActionSelectionStep(
    onActionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Action",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose what DNS Changer should do when triggered",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        ActionCard(
            icon = Icons.Default.PowerSettingsNew,
            title = "Connect",
            description = "Connect to a DNS server",
            onClick = { onActionSelected(TaskerPluginActivity.ACTION_CONNECT) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActionCard(
            icon = Icons.Default.LinkOff,
            title = "Disconnect",
            description = "Disconnect from current DNS and use system default",
            onClick = { onActionSelected(TaskerPluginActivity.ACTION_DISCONNECT) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActionCard(
            icon = Icons.Default.Sync,
            title = "Switch",
            description = "Switch to a different DNS server while connected",
            onClick = { onActionSelected(TaskerPluginActivity.ACTION_SWITCH) }
        )
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ServerSelectionStep(
    onServerSelected: (serverId: String, serverName: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<DnsCategory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onBack)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Select DNS Server",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Category filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DnsCategory.entries.filter { it != DnsCategory.CUSTOM }.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Server list
        val servers = if (selectedCategory != null) {
            PresetDnsServers.getByCategory(selectedCategory!!)
        } else {
            PresetDnsServers.all
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(servers) { server ->
                ServerCard(
                    name = server.name,
                    description = server.description,
                    primaryDns = server.primaryDns,
                    onClick = { onServerSelected(server.id, server.name) }
                )
            }
        }
    }
}

@Composable
private fun ServerCard(
    name: String,
    description: String,
    primaryDns: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = primaryDns,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Icon(
                Icons.Default.Check,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
