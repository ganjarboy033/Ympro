package io.ymusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ymusic.settings.AppSettings
import io.ymusic.settings.flow
import io.ymusic.settings.put
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.sound_ok
import io.ymusic.ui.resources.sound_reset
import io.ymusic.ui.resources.theme_studio_background
import io.ymusic.ui.resources.theme_studio_background_default
import io.ymusic.ui.resources.theme_studio_code
import io.ymusic.ui.resources.theme_studio_code_hint
import io.ymusic.ui.resources.theme_studio_corners
import io.ymusic.ui.resources.theme_studio_font_size
import io.ymusic.ui.resources.theme_studio_presets
import io.ymusic.ui.resources.theme_studio_title
import io.ymusic.ui.resources.theme_preset_forest
import io.ymusic.ui.resources.theme_preset_midnight
import io.ymusic.ui.resources.theme_preset_ocean
import io.ymusic.ui.resources.theme_preset_paper
import io.ymusic.ui.resources.theme_preset_rose
import io.ymusic.ui.resources.theme_preset_sunset
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** A named look: the theme mode, accent and ground tint it sets at once (hex `RRGGBB`; blank = default). */
private class ThemePreset(
    val name: StringResource,
    val mode: String,
    val accent: String,
    val surface: String,
)

private val ThemePresets = listOf(
    ThemePreset(Res.string.theme_preset_midnight, "dark", "5C6BC0", "151A2E"),
    ThemePreset(Res.string.theme_preset_ocean, "dark", "0277BD", "0E1E2B"),
    ThemePreset(Res.string.theme_preset_forest, "dark", "2E7D32", "122018"),
    ThemePreset(Res.string.theme_preset_rose, "dark", "C2185B", "26141C"),
    ThemePreset(Res.string.theme_preset_sunset, "amoled", "E8732A", ""),
    ThemePreset(Res.string.theme_preset_paper, "light", "8D6E63", "FAF6EF"),
)

private val SurfaceChoices = listOf("", "151A2E", "0E1E2B", "122018", "26141C", "1E1E1E", "FAF6EF", "EAF2FB")

private const val CODE_PREFIX = "ymtheme1"

/** `ymtheme1|mode|accent|surface|fontPct|cornerPct` — shareable, pasted back into the code field to apply. */
internal fun themeCodeOf(mode: String, accent: String, surface: String, fontPct: Int, cornerPct: Int): String =
    listOf(CODE_PREFIX, mode, accent, surface, fontPct.toString(), cornerPct.toString()).joinToString("|")

private val HexRegex = Regex("^[0-9A-Fa-f]{6}$")

/** Null when [code] is not a theme code; every field is validated, never trusted. */
internal fun parseThemeCode(code: String): List<String>? {
    val parts = code.trim().split('|')
    if (parts.size != 6 || parts[0] != CODE_PREFIX) return null
    if (parts[1] !in setOf("", "light", "dark", "amoled")) return null
    if (parts[2].isNotEmpty() && !HexRegex.matches(parts[2])) return null
    if (parts[3].isNotEmpty() && !HexRegex.matches(parts[3])) return null
    if (parts[4].toIntOrNull() == null || parts[5].toIntOrNull() == null) return null
    return parts
}

/** Settings → Appearance row opening the theme customizer. */
@Composable
internal fun ThemeStudioRow(graph: AppGraph) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(horizontal = DesignSpacing.SettingsRowHorizontal, vertical = DesignSpacing.SettingsRowVertical),
        horizontalArrangement = Arrangement.spacedBy(DesignSpacing.Space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.theme_studio_title),
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.Ink),
            modifier = Modifier.weight(1f),
        )
    }
    if (open) ThemeStudioDialog(graph) { open = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeStudioDialog(graph: AppGraph, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val mode by remember(graph) { graph.settings.flow(AppSettings.theme) }.collectAsState()
    val accent by remember(graph) { graph.settings.flow(AppSettings.themeAccent) }.collectAsState()
    val surface by remember(graph) { graph.settings.flow(AppSettings.themeSurface) }.collectAsState()
    val fontPct by remember(graph) { graph.settings.flow(AppSettings.themeFontScalePct) }.collectAsState()
    val cornerPct by remember(graph) { graph.settings.flow(AppSettings.themeCornerPct) }.collectAsState()
    var codeText by remember { mutableStateOf("") }
    val currentCode = themeCodeOf(mode, accent, surface, fontPct, cornerPct)

    fun apply(mode: String, accent: String, surface: String, fontPct: Int, cornerPct: Int) {
        scope.launch {
            graph.settings.put(AppSettings.theme, mode)
            graph.settings.put(AppSettings.themeAccent, accent)
            graph.settings.put(AppSettings.themeSurface, surface)
            graph.settings.put(AppSettings.themeFontScalePct, fontPct.coerceIn(85, 130))
            graph.settings.put(AppSettings.themeCornerPct, cornerPct.coerceIn(0, 200))
        }
    }

    ElevatedBarDialog(
        title = stringResource(Res.string.theme_studio_title),
        onDismiss = onDismiss,
        canScrollBackward = false,
        canScrollForward = false,
        buttons = {
            DialogSecondaryButton(stringResource(Res.string.sound_reset)) { apply("dark", "", "", 100, 100) }
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sound_ok), color = DesignColors.Accent) }
        },
    ) {
        Column(
            Modifier.padding(horizontal = DesignSpacing.DialogRowHorizontal, vertical = DesignSpacing.Space16),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.Space16),
        ) {
            Label(stringResource(Res.string.theme_studio_presets))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesignSpacing.Space8),
                verticalArrangement = Arrangement.spacedBy(DesignSpacing.Space8),
            ) {
                ThemePresets.forEach { preset ->
                    val dot = accentColorOf(preset.accent)
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, DesignColors.OutlineSoft, RoundedCornerShape(16.dp))
                            .clickable { apply(preset.mode, preset.accent, preset.surface, fontPct, cornerPct) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(DesignSpacing.Space8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(dot))
                        Text(stringResource(preset.name), style = TextStyle(fontSize = 13.sp, color = DesignColors.Ink))
                    }
                }
            }

            Label(stringResource(Res.string.theme_studio_background))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesignSpacing.Space8),
                verticalArrangement = Arrangement.spacedBy(DesignSpacing.Space8),
            ) {
                SurfaceChoices.forEach { hex ->
                    val swatch = groundTintOf(hex, DesignColors.isDark) ?: DesignColors.Surface
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (hex == surface) DesignColors.Ink else DesignColors.OutlineSoft, CircleShape)
                            .background(swatch)
                            .clickable { scope.launch { graph.settings.put(AppSettings.themeSurface, hex) } },
                    )
                }
            }
            if (surface.isEmpty()) {
                Text(
                    stringResource(Res.string.theme_studio_background_default),
                    style = TextStyle(fontSize = 12.sp, color = DesignColors.InkSecondary),
                )
            }

            Label("${stringResource(Res.string.theme_studio_font_size)}: $fontPct%")
            Slider(
                value = fontPct.toFloat(),
                onValueChange = { v -> scope.launch { graph.settings.put(AppSettings.themeFontScalePct, ((v / 5f).roundToInt() * 5)) } },
                valueRange = 85f..130f,
            )

            Label("${stringResource(Res.string.theme_studio_corners)}: $cornerPct%")
            Slider(
                value = cornerPct.toFloat(),
                onValueChange = { v -> scope.launch { graph.settings.put(AppSettings.themeCornerPct, ((v / 10f).roundToInt() * 10)) } },
                valueRange = 0f..200f,
            )

            Label(stringResource(Res.string.theme_studio_code))
            Text(
                currentCode,
                style = TextStyle(fontSize = 12.sp, color = DesignColors.InkSecondary, fontFamily = FontFamily.Monospace),
            )
            BasicTextField(
                value = codeText,
                onValueChange = { raw ->
                    codeText = raw
                    parseThemeCode(raw)?.let { p ->
                        apply(p[1], p[2], p[3], p[4].toInt(), p[5].toInt())
                        codeText = ""
                    }
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = DesignColors.Ink, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(DesignColors.Accent),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignColors.FieldPill)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        if (codeText.isEmpty()) {
                            Text(
                                stringResource(Res.string.theme_studio_code_hint),
                                style = TextStyle(fontSize = 13.sp, color = DesignColors.InkFaint),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.Ink))
}
