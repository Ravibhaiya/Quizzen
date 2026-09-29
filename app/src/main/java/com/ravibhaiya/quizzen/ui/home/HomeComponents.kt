package com.ravibhaiya.quizzen.ui.home

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.ui.components.BlobLetter
import com.ravibhaiya.quizzen.ui.components.FitText
import com.ravibhaiya.quizzen.ui.components.NeutralShadowColor
import com.ravibhaiya.quizzen.ui.components.QuizzenIcons
import com.ravibhaiya.quizzen.ui.components.cssShadow
import com.ravibhaiya.quizzen.ui.components.pressScale
import com.ravibhaiya.quizzen.ui.theme.BlobShape
import com.ravibhaiya.quizzen.ui.theme.QuizzenShapes
import com.ravibhaiya.quizzen.ui.theme.heroGradient
import com.ravibhaiya.quizzen.ui.theme.quizzen

/** Large gradient card at the top of each tab (radius 38, min height 150). */
@Composable
fun HeroCard(
    letter: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone30 = MaterialTheme.quizzen.tone30
    val shape = RoundedCornerShape(38.dp)
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source)
            .cssShadow(tone30.copy(alpha = 0.65f), offsetY = 18.dp, blur = 32.dp, spread = (-14).dp, shape = shape)
            .clip(shape)
            .background(heroGradient(MaterialTheme.colorScheme.primary, tone30))
            .drawBehind {
                // Decorative translucent circle: 180px, right:-50, bottom:-60
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = 90.dp.toPx(),
                    center = Offset(size.width + 50.dp.toPx() - 90.dp.toPx(), size.height + 60.dp.toPx() - 90.dp.toPx()),
                )
            }
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick,
            )
            .heightIn(min = 150.dp)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .background(Color.White.copy(alpha = 0.18f), QuizzenShapes.Hero),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.8.sp),
                color = Color.White,
            )
        }
        FitText(
            text = AnnotatedString(title),
            style = MaterialTheme.typography.headlineLarge.copy(color = Color.White),
            maxFontSize = 24.8.sp,
            minFontSize = 14.sp,
            step = 0.5.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(QuizzenIcons.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/** Square-ish feature tile: blob letter icon top-left, chevron chip top-right, title bottom-left. */
@Composable
fun FeatureTile(
    letter: String,
    title: String,
    blobShape: BlobShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(1f / 0.98f)
            .pressScale(source)
            .cssShadow(NeutralShadowColor.copy(alpha = 0.05f), offsetY = 2.dp, blur = 6.dp, spread = 0.dp, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = stringResource(R.string.cd_open_feature, title),
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BlobLetter(
                letter = letter,
                shape = blobShape,
                size = 52.dp,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(34.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                QuizzenIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
