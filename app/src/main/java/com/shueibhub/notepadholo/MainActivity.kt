package com.shueibhub.notepadholo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val vm: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            val accent by animateColorAsState(
                targetValue = AccentOption.entries[vm.accentIndex].color,
                animationSpec = tween(400),
                label = "accent",
            )
            NotepadTheme(accent) {
                AppBackground {
                    AnimatedContent(
                        targetState = vm.screen,
                        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
                        label = "screen",
                    ) { screen ->
                        when (screen) {
                            Screen.Home -> HomeScreen(vm)
                            Screen.Settings -> SettingsScreen(vm)
                            is Screen.Editor -> EditorScreen(vm, screen.id)
                        }
                    }
                }
            }
        }
    }
}
