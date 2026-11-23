package com.devwithzachary.mineserve.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.auth.AuditLogEntry
import com.devwithzachary.mineserve.auth.PanelUser
import com.devwithzachary.mineserve.auth.UserRole
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.ui.theme.DiamondLight
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950
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
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<PanelUser?>(null) }
    var deletingUser by remember { mutableStateOf<PanelUser?>(null) }
    var showAuditLog by remember { mutableStateOf(false) }

    val serverNameById = remember(servers) { servers.associate { it.id to it.name } }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Panel Users & Roles (${users.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add User", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Text(
                text = "Create separate Web Admin logins with role-based permissions (Owner, Admin, Moderator, Viewer) and optional per-server access limits.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (user in users) {
                    val roleColor = when (user.role) {
                        UserRole.OWNER -> EmeraldLight
                        UserRole.ADMIN -> DiamondLight
                        UserRole.MODERATOR -> GoldYellow
                        UserRole.VIEWER -> Slate400
                    }
                    val scopeText = if (user.role == UserRole.OWNER || !user.restrictServers) {
                        "All servers"
                    } else {
                        val names = user.allowedServerIds.mapNotNull { serverNameById[it] }
                        if (names.isEmpty()) "No servers assigned" else "Scoped: ${names.joinToString(", ")}"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate900,
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = user.username,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = roleColor.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, roleColor.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = user.role.displayName.uppercase(),
                                            color = roleColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (!user.enabled) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = RedstoneLight.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = "DISABLED",
                                                color = RedstoneLight,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = scopeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { editingUser = user },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit user",
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { deletingUser = user },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete user",
                                        tint = RedstoneLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = ObsidianCardBorder)

            OutlinedButton(
                onClick = {
                    showAuditLog = !showAuditLog
                    if (showAuditLog) onRefresh()
                },
                border = BorderStroke(1.dp, Slate700),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showAuditLog) "Hide Recent Audit Log" else "View Recent Audit Log (${auditEntries.size})",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            if (showAuditLog) {
                val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
                if (auditEntries.isEmpty()) {
                    Text(
                        text = "No audit events recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (entry in auditEntries.take(15)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate950,
                                border = BorderStroke(1.dp, Slate800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${entry.action} · ${entry.username}",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = fmt.format(Date(entry.timestamp)),
                                            color = Slate400,
                                            fontSize = 10.sp
                                        )
                                    }
                                    val detailLine = listOf(entry.target, entry.details, entry.clientIp)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" · ")
                                    if (detailLine.isNotBlank()) {
                                        Text(
                                            text = detailLine,
                                            color = Slate400,
                                            fontSize = 10.5.sp
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
        AlertDialog(
            onDismissRequest = { deletingUser = null },
            containerColor = ObsidianCard,
            title = { Text("Delete '${target.username}'?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This user will immediately lose access to Web Remote Admin and any active sessions will be revoked.",
                    color = Slate400,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
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
                    colors = ButtonDefaults.buttonColors(containerColor = RedstoneLight)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingUser = null }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Text(
                text = if (isEditing) "Edit User: ${existingUser!!.username}" else "Add Panel User",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isEditing) "New Password (optional)" else "Password (min 6 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Role", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldDark.copy(alpha = 0.45f),
                                selectedLabelColor = EmeraldLight,
                                labelColor = Slate400
                            )
                        )
                    }
                }
                Text(
                    text = roleDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 11.sp
                )

                if (selectedRole != UserRole.OWNER) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Restrict to specific servers",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = restrictServers,
                            onCheckedChange = { restrictServers = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldPrimary,
                                checkedTrackColor = Slate800,
                                checkedBorderColor = EmeraldPrimary.copy(alpha = 0.6f),
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate900,
                                uncheckedBorderColor = Slate700
                            )
                        )
                    }

                    if (restrictServers) {
                        if (servers.isEmpty()) {
                            Text("No servers created yet.", color = Slate400, fontSize = 12.sp)
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
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                        )
                                        Text(
                                            text = srv.name,
                                            color = Color.White,
                                            fontSize = 13.sp
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
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldPrimary,
                                checkedTrackColor = Slate800,
                                checkedBorderColor = EmeraldPrimary.copy(alpha = 0.6f),
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate900,
                                uncheckedBorderColor = Slate700
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        username.trim(),
                        password,
                        selectedRole,
                        restrictServers,
                        selectedServerIds.toList(),
                        enabled
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(if (isEditing) "Save" else "Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}
