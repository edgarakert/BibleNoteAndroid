package ru.edgarakert.biblenote.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.CardSurface
import ru.edgarakert.biblenote.ui.theme.Hairline
import ru.edgarakert.biblenote.ui.theme.Ink
import ru.edgarakert.biblenote.ui.theme.Parchment
import ru.edgarakert.biblenote.ui.theme.WarmGray
import ru.edgarakert.biblenote.ui.viewmodels.SettingsViewModel

@Composable
fun TranslationSelectionScreen(
    onComplete: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.translationGroups
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf(setOf("synodal")) }
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        appeared = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = spring(dampingRatio = 0.8f)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp, bottom = 28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .size(48.dp)
                    )
                    Text(
                        text = stringResource(R.string.translations_select_title),
                        fontFamily = FontFamily.Serif,
                        fontSize = 26.sp,
                        color = Ink
                    )
                    Text(
                        text = stringResource(R.string.translations_select_subtitle),
                        fontSize = 13.sp,
                        color = WarmGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(horizontal = 40.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                groups.forEachIndexed { gi, group ->
                    item {
                        Text(
                            text = stringResource(group.titleResId).uppercase(),
                            fontSize = 11.sp,
                            color = WarmGray,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .padding(top = if (gi > 0) 12.dp else 0.dp)
                        )
                    }
                    itemsIndexed(group.translations) { ti, info ->
                        val isSelected = info.id in selected
                        TranslationCard(
                            name = stringResource(info.nameResId),
                            subtitle = stringResource(info.subtitleResId),
                            isSelected = isSelected,
                            onTap = {
                                selected = if (isSelected) {
                                    if (selected.size <= 1) selected else selected - info.id
                                } else {
                                    selected + info.id
                                }
                            }
                        )
                        if (ti < group.translations.lastIndex) {
                            Spacer(modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn()
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.saveOnboardingSelections(selected)
                            onComplete()
                        }
                    },
                    enabled = selected.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = Color.White,
                        disabledContainerColor = Amber.copy(alpha = 0.35f),
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                        .padding(bottom = 40.dp)
                ) {
                    Text(
                        text = stringResource(R.string.translations_select_continue),
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationCard(
    name: String,
    subtitle: String,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .then(
                if (isSelected) Modifier.border(
                    1.5.dp,
                    Amber.copy(alpha = 0.45f),
                    RoundedCornerShape(12.dp)
                )
                else Modifier
            )
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = Ink
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = WarmGray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Amber else Hairline,
                    shape = CircleShape
                )
                .then(
                    if (isSelected) Modifier.background(Amber.copy(alpha = 0.15f)) else Modifier
                )
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
