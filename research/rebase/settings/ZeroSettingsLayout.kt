// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zerokeyboard.ime.VoiceSetupActivity
import helium314.keyboard.latin.R
import helium314.keyboard.settings.preferences.Preference

private class SettingsCategory(val route: String, val title: Int, val icon: Int)

private val categories = listOf(
    SettingsCategory(SettingsDestination.Languages, R.string.language_and_layouts_title, R.drawable.ic_settings_languages),
    SettingsCategory(SettingsDestination.Preferences, R.string.settings_screen_preferences, R.drawable.ic_settings_preferences),
    SettingsCategory(SettingsDestination.Appearance, R.string.settings_screen_appearance, R.drawable.ic_settings_appearance),
    SettingsCategory(SettingsDestination.Toolbar, R.string.settings_screen_toolbar, R.drawable.ic_settings_toolbar),
    SettingsCategory(SettingsDestination.TextCorrection, R.string.settings_screen_correction, R.drawable.ic_settings_correction),
    SettingsCategory(SettingsDestination.Layouts, R.string.settings_screen_secondary_layouts, R.drawable.ic_ime_switcher),
    SettingsCategory(SettingsDestination.Dictionaries, R.string.dictionary_settings_category, R.drawable.ic_dictionary),
    SettingsCategory(SettingsDestination.Advanced, R.string.settings_screen_advanced, R.drawable.ic_settings_advanced),
    SettingsCategory(SettingsDestination.About, R.string.settings_screen_about, R.drawable.ic_settings_about),
)

@Composable
internal fun ZeroSettingsSidebar(activeRoute: String?, onCategory: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val activeCategory = when {
        activeRoute == null -> null
        activeRoute.startsWith(SettingsDestination.Subtype) -> SettingsDestination.Languages
        activeRoute.startsWith(SettingsDestination.Colors) ||
            activeRoute.startsWith(SettingsDestination.ColorsNight) -> SettingsDestination.Appearance
        activeRoute == SettingsDestination.PersonalDictionaries ||
            activeRoute.startsWith(SettingsDestination.PersonalDictionary) -> SettingsDestination.Dictionaries
        activeRoute == SettingsDestination.Debug -> SettingsDestination.Advanced
        else -> activeRoute
    }

    Surface(modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, top = 18.dp, end = 12.dp, bottom = 12.dp)
        ) {
            for (category in categories) {
                val active = category.route == activeCategory
                val row = Modifier.semantics { selected = active }.padding(vertical = 2.dp)
                Preference(
                    name = stringResource(category.title),
                    icon = category.icon,
                    onClick = { onCategory(category.route) },
                    modifier = if (active) row.background(
                        MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)
                    ) else row,
                )
            }
            Preference(
                name = stringResource(R.string.zero_voice_and_glide),
                icon = R.drawable.ic_settings_gesture,
                onClick = { context.startActivity(Intent(context, VoiceSetupActivity::class.java)) },
            )
        }
    }
}

@Composable
internal fun ZeroTypingPreview() {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        label = { Text(stringResource(R.string.zero_try_keyboard)) },
        minLines = 3,
        maxLines = 5,
    )
}
