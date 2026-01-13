package com.dns.changer.ultimate.ui.screens.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.ui.theme.DnsShapes

@Composable
fun AddCustomDnsDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, primaryDns: String, secondaryDns: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var primaryDns by remember { mutableStateOf("") }
    var secondaryDns by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var primaryError by remember { mutableStateOf(false) }

    val isValidIp: (String) -> Boolean = { ip ->
        ip.isEmpty() || ip.matches(Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 400.dp),
        shape = DnsShapes.Dialog,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        icon = {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.add_custom_dns),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Name") },
                    placeholder = { Text("My Custom DNS") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Name is required") }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = DnsShapes.SmallButton
                )

                OutlinedTextField(
                    value = primaryDns,
                    onValueChange = {
                        primaryDns = it
                        primaryError = !isValidIp(it) || it.isBlank()
                    },
                    label = { Text(stringResource(R.string.dns_primary)) },
                    placeholder = { Text("1.1.1.1") },
                    isError = primaryError,
                    supportingText = if (primaryError) {
                        { Text("Enter a valid IP address") }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = DnsShapes.SmallButton
                )

                OutlinedTextField(
                    value = secondaryDns,
                    onValueChange = { secondaryDns = it },
                    label = { Text(stringResource(R.string.dns_secondary)) },
                    placeholder = { Text("1.0.0.1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = DnsShapes.SmallButton
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = name.isBlank()
                    primaryError = primaryDns.isBlank() || !isValidIp(primaryDns)

                    if (!nameError && !primaryError) {
                        onConfirm(
                            name.trim(),
                            primaryDns.trim(),
                            secondaryDns.trim().ifBlank { primaryDns.trim() }
                        )
                    }
                },
                shape = DnsShapes.SmallButton
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = DnsShapes.SmallButton
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
