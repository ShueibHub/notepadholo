package com.shueibhub.notepadholo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun EditorScreen(vm: NotesViewModel, noteId: String?) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val accent = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    val original = remember { noteId?.let { nid -> vm.notes.firstOrNull { it.id == nid } } }
    val id = rememberSaveable { original?.id ?: UUID.randomUUID().toString() }
    var title by rememberSaveable { mutableStateOf(original?.title.orEmpty()) }
    var content by rememberSaveable { mutableStateOf(original?.content.orEmpty()) }
    var showName by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }

    val fullText = when {
        title.isBlank() -> content
        content.isBlank() -> title
        else -> title + "\n\n" + content
    }

    fun commit() {
        if (original == null || title != original.title || content != original.content) {
            val pinned = vm.notes.firstOrNull { it.id == id }?.pinned ?: false
            vm.upsert(Note(id = id, title = title, content = content, updatedAt = System.currentTimeMillis(), pinned = pinned))
        }
    }

    fun exit() {
        commit()
        vm.screen = Screen.Home
    }

    BackHandler { exit() }

    // حفظ تلقائي عند مغادرة التطبيق
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) commit()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (original == null) runCatching { titleFocus.requestFocus() }
    }

    val titleStyle = TextStyle(
        color = onBg, fontSize = 26.sp, lineHeight = 34.sp,
        fontWeight = FontWeight.Bold, textDirection = TextDirection.Content,
    )
    val bodyStyle = TextStyle(
        color = onBg.copy(alpha = 0.92f), fontSize = 17.sp, lineHeight = 27.sp,
        textDirection = TextDirection.Content,
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { AppSnackbarHost(snackbar) },
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { exit() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(fullText))
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.copied)) }
                }) {
                    Icon(AppIcons.Copy, stringResource(R.string.copy), tint = accent)
                }
                IconButton(onClick = { showName = true }) {
                    Icon(AppIcons.Download, stringResource(R.string.save_txt), tint = accent)
                }
                if (original != null) {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
                FilledIconButton(
                    onClick = { exit() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accent,
                        contentColor = accent.onAccent(),
                    ),
                ) {
                    Icon(Icons.Default.Check, stringResource(R.string.save_exit))
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(horizontal = 24.dp),
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = titleStyle,
                cursorBrush = SolidColor(accent),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { contentFocus.requestFocus() }),
                decorationBox = { inner ->
                    androidx.compose.foundation.layout.Box {
                        if (title.isEmpty()) {
                            Text(stringResource(R.string.title_hint), style = titleStyle.copy(color = hintColor))
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.Box(
                Modifier.width(44.dp).height(3.dp).clip(CircleShape).background(accent)
            )
            Spacer(Modifier.height(16.dp))

            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = bodyStyle,
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { inner ->
                    androidx.compose.foundation.layout.Box {
                        if (content.isEmpty()) {
                            Text(stringResource(R.string.content_hint), style = bodyStyle.copy(color = hintColor))
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f).focusRequester(contentFocus),
            )

            val words = remember(content) { content.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.words_chars, words, content.length),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.glass(RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }

    if (showName) {
        var name by remember { mutableStateOf(title.trim()) }
        AlertDialog(
            onDismissRequest = { showName = false },
            containerColor = Color(0xFF1D2028),
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.app_name), color = Cyan, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.name_txt), color = Color(0xFFD5D8E0))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text(stringResource(R.string.name_hint)) },
                        colors = fieldColors(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val where = saveTxt(context, name, fullText)
                    showName = false
                    scope.launch {
                        snackbar.showSnackbar(
                            if (where != null) context.getString(R.string.txt_saved, where)
                            else context.getString(R.string.txt_failed)
                        )
                    }
                }) { Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showName = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_note_title),
            text = stringResource(R.string.delete_note_text),
            confirm = stringResource(R.string.delete),
            onConfirm = {
                showDelete = false
                vm.delete(id)
                vm.screen = Screen.Home
            },
            onDismiss = { showDelete = false },
        )
    }
}
