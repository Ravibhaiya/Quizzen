package com.ravibhaiya.quizzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ravibhaiya.quizzen.ui.theme.primaryGradient
import com.ravibhaiya.quizzen.ui.theme.quizzen

/** Gradient (primary -> tone30) organic shape with a centered white letter. */
@Composable
fun BlobLetter(
    letter: String,
    shape: Shape,
    size: Dp,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
) {
    val gradient = primaryGradient(MaterialTheme.colorScheme.primary, MaterialTheme.quizzen.tone30)
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (elevated) {
                    Modifier.shadow(
                        6.dp, shape,
                        ambientColor = MaterialTheme.quizzen.tone30,
                        spotColor = MaterialTheme.quizzen.tone30,
                    )
                } else {
                    Modifier
                },
            )
            .background(gradient, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = letter, style = textStyle, color = Color.White)
    }
}

/** Gradient blob holding a vector icon (used by the Timer card). */
@Composable
fun BlobIcon(
    icon: ImageVector,
    shape: Shape,
    size: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val brush: Brush = primaryGradient(MaterialTheme.colorScheme.primary, MaterialTheme.quizzen.tone30)
    Box(
        modifier = modifier
            .size(size)
            .background(brush, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}
