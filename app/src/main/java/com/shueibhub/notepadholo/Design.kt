package com.shueibhub.notepadholo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Cyan = Color(0xFF29ABE2)
private val BgTop = Color(0xFF0A0B0D)
private val BgMid = Color(0xFF14161B)
private val BgBottom = Color(0xFF23262F)

fun Color.onAccent(): Color = if (luminance() > 0.55f) Color(0xFF0A0B0D) else Color.White

val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
)

@Composable
fun NotepadTheme(accent: Color, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = accent.onAccent(),
        secondary = Cyan,
        background = BgTop,
        onBackground = Color(0xFFECEDF2),
        surface = Color(0xFF15171C),
        onSurface = Color(0xFFECEDF2),
        surfaceVariant = Color(0xFF1E2027),
        onSurfaceVariant = Color(0xFFA0A5B2),
        outline = Color(0xFF3A3D48),
        error = Color(0xFFFF6B7A),
    )
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}

/** نفس الخلفية الأصلية (داكن → رمادي) مع وهج خفيف بلون التمييز */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgMid, BgBottom)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width,
                    )
                )
            }
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}

/** بطاقة زجاجية بحدود متدرجة */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(22.dp)): Modifier {
    val accent = MaterialTheme.colorScheme.primary
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0xFF1D2028), Color(0xFF14161B))))
        .border(
            1.dp,
            Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f))),
            shape,
        )
}

@Composable
fun AppSnackbarHost(state: SnackbarHostState) {
    SnackbarHost(state) { data ->
        Snackbar(
            data,
            shape = RoundedCornerShape(16.dp),
            containerColor = Color(0xFF2A2D37),
            contentColor = Color.White,
        )
    }
}

@Composable
fun fieldColors(): TextFieldColors {
    val accent = MaterialTheme.colorScheme.primary
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent,
        unfocusedBorderColor = Color(0xFF3A3D48),
        focusedContainerColor = Color(0xFF15171C),
        unfocusedContainerColor = Color(0xFF15171C),
        cursorColor = accent,
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        focusedLeadingIconColor = accent,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1D2028),
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, color = Cyan, fontWeight = FontWeight.Bold) },
        text = { Text(text, color = Color(0xFFD5D8E0)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ── أيقونات إضافية (بدون مكتبة material-icons-extended الثقيلة) ──
private fun vector(name: String, path: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        .build()

object AppIcons {
    val Copy: ImageVector by lazy {
        vector("Copy", "M16,1L4,1c-1.1,0 -2,0.9 -2,2v14h2L4,3h12L16,1zM19,5L8,5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2L21,7c0,-1.1 -0.9,-2 -2,-2zM19,21L8,21L8,7h11v14z")
    }
    val Download: ImageVector by lazy {
        vector("Download", "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z")
    }
    val Paste: ImageVector by lazy {
        vector("Paste", "M19,2h-4.18C14.4,0.84 13.3,0 12,0c-1.3,0 -2.4,0.84 -2.82,2L5,2c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2L21,4c0,-1.1 -0.9,-2 -2,-2zM12,2c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM19,20L5,20L5,4h2v3h10L17,4h2v16z")
    }
    val Pin: ImageVector by lazy {
        vector("Pin", "M16,9V4h1c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3s0.45,1 1,1h1v5c0,1.66 -1.34,3 -3,3v2h5.97v7l1,1 1,-1v-7H19v-2c-1.66,0 -3,-1.34 -3,-3z")
    }
}
