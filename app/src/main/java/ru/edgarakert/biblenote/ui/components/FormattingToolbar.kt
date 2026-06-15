package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FormattingToolbar(
    isBold: Boolean,
    isItalic: Boolean,
    isLarge: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onLarge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)

    Column(modifier = modifier) {
        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBold,
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "B",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isBold) activeColor else inactiveColor,
                        textAlign = TextAlign.Center
                    )
                )
            }
            TextButton(
                onClick = onItalic,
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "I",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        color = if (isItalic) activeColor else inactiveColor,
                        textAlign = TextAlign.Center
                    )
                )
            }
            TextButton(
                onClick = onLarge,
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Aa",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isLarge) 15.sp else 13.sp,
                        color = if (isLarge) activeColor else inactiveColor,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}
