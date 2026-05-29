package ru.edgarakert.biblenote.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.CardSurface
import ru.edgarakert.biblenote.ui.theme.Hairline
import ru.edgarakert.biblenote.ui.theme.Ink
import ru.edgarakert.biblenote.ui.theme.Parchment
import ru.edgarakert.biblenote.ui.theme.WarmGray
import ru.edgarakert.biblenote.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateToTheme: () -> Unit,
    onNavigateToTranslations: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val defaultTranslationName = uiState.translationGroups
        .flatMap { it.translations }
        .find { it.id == uiState.defaultTranslation }
        ?.nameResId
        ?.let { stringResource(it) }
        ?: uiState.defaultTranslation.uppercase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.editor_back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Parchment,
                    titleContentColor = Ink,
                    navigationIconContentColor = Ink
                )
            )
        },
        containerColor = Parchment
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SectionHeader(stringResource(R.string.settings_language_header))
            SettingsCard {
                SettingsRow(
                    title = stringResource(R.string.settings_language_row),
                    subtitle = stringResource(R.string.settings_language_hint),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = WarmGray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS))
                    }
                )
            }

            SectionHeader(
                text = stringResource(R.string.settings_bible_header),
                modifier = Modifier.padding(top = 20.dp)
            )
            SettingsCard {
                SettingsRow(
                    title = stringResource(R.string.settings_bible_theme_row),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = WarmGray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    onClick = onNavigateToTheme
                )
                HorizontalDivider(
                    color = Hairline,
                    modifier = Modifier.padding(start = 16.dp)
                )
                SettingsRow(
                    title = stringResource(R.string.settings_translations_row),
                    subtitle = defaultTranslationName,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = WarmGray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    onClick = onNavigateToTranslations
                )
            }

            SectionHeader(
                text = stringResource(R.string.settings_app_header),
                modifier = Modifier.padding(top = 20.dp)
            )
            SettingsCard {
                SettingsRow(
                    title = stringResource(R.string.settings_about_row),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = WarmGray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Amber,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        fontFamily = FontFamily.SansSerif,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
    ) {
        content()
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = Ink
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = WarmGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        trailingIcon?.invoke()
    }
}
