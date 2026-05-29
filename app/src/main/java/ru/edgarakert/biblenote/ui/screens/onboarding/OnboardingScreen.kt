package ru.edgarakert.biblenote.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.CardSurface
import ru.edgarakert.biblenote.ui.theme.Ink
import ru.edgarakert.biblenote.ui.theme.Parchment
import ru.edgarakert.biblenote.ui.theme.WarmGray

private fun buildDemoAnnotatedString(
    highlight: Boolean,
    prefix: String,
    ref: String,
    suffix: String,
    highlightColor: Color,
    defaultColor: Color
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = defaultColor)) { append(prefix) }
    withStyle(
        SpanStyle(
            color = if (highlight) highlightColor else defaultColor,
            textDecoration = if (highlight) TextDecoration.Underline else TextDecoration.None
        )
    ) { append(ref) }
    withStyle(SpanStyle(color = defaultColor)) { append(suffix) }
}

@Composable
fun OnboardingScreen(
    onNavigateToTranslations: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> OnboardingSlide1()
                    1 -> OnboardingSlide2()
                    else -> OnboardingSlide3()
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 52.dp, top = 16.dp)
            ) {
                PageDotsView(count = 3, current = pagerState.currentPage)
                Spacer(modifier = Modifier.height(24.dp))
                if (pagerState.currentPage < 2) {
                    OnboardingButton(
                        text = stringResource(R.string.onboarding_next),
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    )
                } else {
                    OnboardingButton(
                        text = stringResource(R.string.onboarding_choose_translations),
                        onClick = onNavigateToTranslations
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDotsView(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val isActive = i == current
            val width: Dp = if (isActive) 20.dp else 7.dp
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(7.dp)
                    .width(width)
                    .clip(RoundedCornerShape(50))
                    .background(if (isActive) Amber else Amber.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
private fun OnboardingButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Amber,
            contentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun OnboardingSlide1() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(50); appeared = true }

    SlideLayout(
        icon = Icons.Outlined.Book,
        appeared = appeared,
        iconBottomPadding = 40.dp
    ) {
        Text(
            text = stringResource(R.string.onboarding_slide1_title),
            fontFamily = FontFamily.Serif,
            fontSize = 30.sp,
            color = Ink
        )
        Text(
            text = stringResource(R.string.onboarding_slide1_subtitle),
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = Amber,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stringResource(R.string.onboarding_slide1_body),
            fontSize = 14.sp,
            color = WarmGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
private fun OnboardingSlide2() {
    var appeared by remember { mutableStateOf(false) }
    var highlightVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        appeared = true
        delay(700)
        highlightVisible = true
    }

    SlideLayout(
        icon = Icons.Outlined.Search,
        appeared = appeared,
        iconBottomPadding = 32.dp
    ) {
        Text(
            text = stringResource(R.string.onboarding_slide2_title),
            fontFamily = FontFamily.Serif,
            fontSize = 30.sp,
            color = Ink
        )
        Text(
            text = stringResource(R.string.onboarding_slide2_body),
            fontSize = 14.sp,
            color = WarmGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
        val prefix = stringResource(R.string.onboarding_slide2_demo_prefix)
        val ref = stringResource(R.string.onboarding_slide2_demo_ref)
        val suffix = stringResource(R.string.onboarding_slide2_demo_suffix)
        val demoText = remember(highlightVisible, prefix, ref, suffix) {
            buildDemoAnnotatedString(highlightVisible, prefix, ref, suffix, Amber, Ink)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_slide2_demo_label).uppercase(),
                fontSize = 11.sp,
                color = WarmGray,
                letterSpacing = 0.6.sp
            )
            Text(
                text = demoText,
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun OnboardingSlide3() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(50); appeared = true }

    SlideLayout(
        icon = Icons.Outlined.Download,
        appeared = appeared,
        iconBottomPadding = 40.dp
    ) {
        Text(
            text = stringResource(R.string.onboarding_slide3_title),
            fontFamily = FontFamily.Serif,
            fontSize = 30.sp,
            color = Ink
        )
        Text(
            text = stringResource(R.string.onboarding_slide3_body),
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = WarmGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

@Composable
private fun SlideLayout(
    icon: ImageVector,
    appeared: Boolean,
    iconBottomPadding: Dp = 40.dp,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 44.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        AnimatedVisibility(
            visible = appeared,
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = spring(dampingRatio = 0.75f)
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier
                    .size(88.dp)
                    .padding(bottom = iconBottomPadding)
            )
        }
        AnimatedVisibility(
            visible = appeared,
            enter = fadeIn()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                content()
            }
        }
        Spacer(modifier = Modifier.weight(2f))
    }
}
