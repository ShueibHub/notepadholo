package com.shueibhub.notepadholo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: NotesViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val accent = MaterialTheme.colorScheme.primary
    var confirmDelete by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val tell: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    BackHandler { vm.screen = Screen.Home }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { AppSnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { vm.screen = Screen.Home },
                    modifier = Modifier.size(48.dp).glass(CircleShape),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = accent)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── الألوان ──
            SectionCard(stringResource(R.string.colors)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AccentOption.entries.forEachIndexed { i, opt ->
                            ColorDot(
                                color = opt.color,
                                name = stringResource(opt.label),
                                selected = vm.accentIndex == i,
                                onClick = { vm.setAccent(i) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(AccentOption.entries[vm.accentIndex].label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ──    يرحم والديكم لا تغيروا البيانات حتى المبتدئين ما يلخبطوا   ──
            SectionCard(stringResource(R.string.data)) {
                SettingRow(
                    icon = AppIcons.Copy,
                    title = stringResource(R.string.copy_data),
                    subtitle = stringResource(R.string.copy_data_sub),
                    tint = accent,
                ) {
                    clipboard.setText(AnnotatedString(vm.exportJson()))
                    tell(context.getString(R.string.copied))
                }
                SettingRow(
                    icon = AppIcons.Paste,
                    title = stringResource(R.string.paste_data),
                    subtitle = stringResource(R.string.paste_data_sub),
                    tint = accent,
                ) {
                    val text = clipboard.getText()?.text.orEmpty()
                    val n = vm.importJson(text)
                    tell(
                        if (n >= 0) context.getString(R.string.imported, n)
                        else context.getString(R.string.import_failed)
                    )
                }
                SettingRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.delete_all),
                    subtitle = stringResource(R.string.delete_all_sub),
                    tint = MaterialTheme.colorScheme.error,
                    destructive = true,
                ) { confirmDelete = true }
            }

            Spacer(Modifier.height(16.dp))

            SectionCard(null) {
                SettingRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.about),
                    subtitle = stringResource(R.string.about_sub),
                    tint = Cyan,
                ) { showAbout = true }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_all),
            text = stringResource(R.string.delete_all_text),
            confirm = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                vm.deleteAll()
                tell(context.getString(R.string.all_deleted))
            },
            onDismiss = { confirmDelete = false },
        )
    }

    if (showAbout) {
        val version = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                .getOrNull() ?: "2.0"
        }
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = Color(0xFF1D2028),
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.app_name), color = Cyan, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.version, version), color = accent, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.about_text), color = Color(0xFFD5D8E0))
                    Spacer(Modifier.height(14.dp))
                    Text("ShueibTech", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}

@Composable
private fun SectionCard(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().glass().padding(vertical = 8.dp)) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        content()
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorDot(color: Color, name: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.15f),
                shape = CircleShape,
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = color.onAccent())
    }
}
