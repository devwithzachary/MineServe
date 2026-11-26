package com.devwithzachary.mineserve.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.auth.AuditLogCsvExporter
import com.devwithzachary.mineserve.auth.AuditLogEntry
import com.devwithzachary.mineserve.auth.PanelUser
import com.devwithzachary.mineserve.auth.UserRole
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.MinecraftServer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanelUsersCard(
    users: List<PanelUser>,
    auditEntries: List<AuditLogEntry>,
    servers: List<MinecraftServer>,
    onCreateUser: (username: String, password: String, role: UserRole, restrictServers: Boolean, allowedIds: List<String>) -> Result<PanelUser>,
    onUpdateUser: (userId: String, role: UserRole, restrictServers: Boolean, allowedIds: List<String>, enabled: Boolean, newPassword: String?) -> Result<PanelUser>,
    onDeleteUser: (userId: String) -> Result<Unit>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<PanelUser?>(null) }
    var deletingUser by remember { mutableStateOf<PanelUser?>(null) }
    var showAuditLog by remember { mutableStateOf(false) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val csv = AuditLogCsvExporter.toCsv(auditEntries)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csv.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Unable to open output stream")
        }.fold(
            onSuccess = {
                Toast.makeText(context, "Exported ${auditEntries.size} audit entries to CSV", Toast.LENGTH_SHORT).show()
            },
            onFailure = { err ->
                Toast.makeText(context, err.message ?: "Failed to export CSV", Toast.LENGTH_LONG).show()
            }
        )
    }

    val serverNameById = remember(servers) { servers.associate { it.id to it.name } }

    MineServeCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Panel Users & Roles (${users.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalButton(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = ShapeDefaults.Small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add User", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                text = "Create separate Web Admin logins with role-based permissions (Owner, Admin, Moderator, Viewer) and optional per-server access limits.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MineServeTableContainer {
                users.forEachIndexed { index, user ->
                    val (roleContainer, roleContent) = when (user.role) {
                        UserRole.OWNER -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        UserRole.ADMIN -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                        UserRole.MODERATOR -> extColors.warningContainer to extColors.onWarningContainer
                        UserRole.VIEWER -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val scopeText = if (user.role == UserRole.OWNER || !user.restrictServers) {
                        "All servers"
                    } else {
                        val names = user.allowedServerIds.mapNotNull { serverNameById[it] }
                        if (names.isEmpty()) "No servers assigned" else "Scoped: ${names.joinToString(", ")}"
                    }

                    MineServeTableRow(showDivider = index < users.lastIndex) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = user.username,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = ShapeDefaults.Small,
                                    color = roleContainer
                                ) {
                                    Text(
                                        text = user.role.displayName.uppercase(),
                                        color = roleContent,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (!user.enabled) {
                                    Surface(
                                        shape = ShapeDefaults.Small,
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = "DISABLED",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = scopeText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { editingUser = user },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit user",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { deletingUser = user },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete user",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            FilledTonalButton(
                onClick = {
                    showAuditLog = !showAuditLog
                    if (showAuditLog) onRefresh()
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = ShapeDefaults.Small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showAuditLog) "Hide Recent Audit Log" else "View Recent Audit Log (${auditEntries.size})",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (showAuditLog) {
                val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
                if (auditEntries.isEmpty()) {
                    Text(
                        text = "No audit events recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val entries = auditEntries.take(15)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Showing latest ${entries.size} of ${auditEntries.size} events",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = {
                                csvExportLauncher.launch("mineserve-audit-log-${System.currentTimeMillis()}.csv")
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = ShapeDefaults.Small
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    MineServeTableContainer {
                        entries.forEachIndexed { index, entry ->
                            MineServeTableRow(showDivider = index < entries.lastIndex) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${entry.action} - ${entry.username}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        MineServeTableMonoText(
                                            text = fmt.format(Date(entry.timestamp)),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val detailLine = listOf(entry.target, entry.details, entry.clientIp)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" - ")
                                    if (detailLine.isNotBlank()) {
                                        Text(
                                            text = detailLine,
                                            fontFamily = monoFont,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        UserEditorDialog(
            existingUser = null,
            servers = servers,
            onDismiss = { showCreateDialog = false },
            onSave = { username, password, role, restrict, allowed, _ ->
                onCreateUser(username, password, role, restrict, allowed).fold(
                    onSuccess = {
                        showCreateDialog = false
                        onRefresh()
                        Toast.makeText(context, "Created user $username", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, err.message ?: "Failed to create user", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    editingUser?.let { target ->
        UserEditorDialog(
            existingUser = target,
            servers = servers,
            onDismiss = { editingUser = null },
            onSave = { _, password, role, restrict, allowed, enabled ->
                onUpdateUser(
                    target.id,
                    role,
                    restrict,
                    allowed,
                    enabled,
                    password.takeIf { it.isNotEmpty() }
                ).fold(
                    onSuccess = {
                        editingUser = null
                        onRefresh()
                        Toast.makeText(context, "Updated user ${target.username}", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, err.message ?: "Failed to update user", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    deletingUser?.let { target ->
        MineServeDialog(
            onDismissRequest = { deletingUser = null },
            title = "Delete '${target.username}'?",
            confirmLabel = "Delete",
            onConfirm = {
                onDeleteUser(target.id).fold(
                    onSuccess = {
                        deletingUser = null
                        onRefresh()
                        Toast.makeText(context, "Deleted ${target.username}", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, err.message ?: "Cannot delete user", Toast.LENGTH_LONG).show()
                    }
                )
            },
            icon = Icons.Rounded.Delete,
            isDestructive = true
        ) {
            Text(
                text = "This user will immediately lose access to Web Remote Admin and any active sessions will be revoked.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserEditorDialog(
    existingUser: PanelUser?,
    servers: List<MinecraftServer>,
    onDismiss: () -> Unit,
    onSave: (username: String, password: String, role: UserRole, restrictServers: Boolean, allowedServerIds: List<String>, enabled: Boolean) -> Unit
) {
    val isEditing = existingUser != null
    var username by remember { mutableStateOf(existingUser?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(existingUser?.role ?: UserRole.MODERATOR) }
    var restrictServers by remember { mutableStateOf(existingUser?.restrictServers ?: false) }
    var selectedServerIds by remember { mutableStateOf(existingUser?.allowedServerIds?.toSet() ?: emptySet()) }
    var enabled by remember { mutableStateOf(existingUser?.enabled ?: true) }

    val roleDescription = when (selectedRole) {
        UserRole.OWNER -> "Full host control, user management, Cloud/Diagnostics, and all servers."
        UserRole.ADMIN -> "Full server management (settings, files, plugins, backups, world, network) and audit log."
        UserRole.MODERATOR -> "Start/stop/restart, console commands, players, and create/download backups."
        UserRole.VIEWER -> "Read-only console, live performance metrics, and Live Map."
    }

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "Edit User: ${existingUser!!.username}" else "Add Panel User",
        confirmLabel = if (isEditing) "Save" else "Create",
        onConfirm = {
            onSave(
                username.trim(),
                password,
                selectedRole,
                restrictServers,
                selectedServerIds.toList(),
                enabled
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isEditing) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    placeholder = { Text("alex_mod") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isEditing) "New Password (optional)" else "Password (min 6 chars)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Role",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UserRole.entries.forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        label = { Text(role.displayName, style = MaterialTheme.typography.labelSmall) },
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
            Text(
                text = roleDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedRole != UserRole.OWNER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Restrict to specific servers",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = restrictServers,
                        onCheckedChange = { restrictServers = it }
                    )
                }

                if (restrictServers) {
                    if (servers.isEmpty()) {
                        Text(
                            text = "No servers created yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (srv in servers) {
                                val checked = selectedServerIds.contains(srv.id)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            selectedServerIds = if (isChecked) {
                                                selectedServerIds + srv.id
                                            } else {
                                                selectedServerIds - srv.id
                                            }
                                        }
                                    )
                                    Text(
                                        text = srv.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account Enabled",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
            }
        }
    }
}
