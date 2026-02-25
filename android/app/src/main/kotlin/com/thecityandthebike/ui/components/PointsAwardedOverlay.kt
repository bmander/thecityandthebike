package com.thecityandthebike.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.thecityandthebike.data.model.ScoreRules
import com.thecityandthebike.data.model.dto.ScoringBreakdown
import kotlinx.coroutines.delay

@Composable
fun PointsAwardedOverlay(
    breakdown: List<ScoringBreakdown>,
    onDismiss: () -> Unit,
    onShowScoreRules: (() -> Unit)? = null
) {
    val total = breakdown.sumOf { it.points }
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val dismissRequested = remember { mutableStateOf(false) }

    LaunchedEffect(breakdown) {
        // Animate in
        alpha.animateTo(1f, tween(200))
        scale.animateTo(1.2f, tween(300))
        scale.animateTo(1f, tween(150))

        // Hold
        delay(10_000)
        dismissRequested.value = true
    }

    LaunchedEffect(dismissRequested.value) {
        if (dismissRequested.value) {
            // Animate out
            alpha.animateTo(0f, tween(300))
            onDismiss()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(horizontal = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                breakdown.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "+${item.points}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = "+$total",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp)
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.85f), CircleShape)
                    .border(1.dp, Color.DarkGray, CircleShape)
                    .clickable { dismissRequested.value = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u00D7",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (onShowScoreRules != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-20).dp, y = (-16).dp)
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                        .border(1.dp, Color.DarkGray, CircleShape)
                        .clickable {
                            onDismiss()
                            onShowScoreRules()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "?",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF888888)
@Composable
private fun PointsAwardedOverlayPreview() {
    val breakdown = ScoreRules.photoRules
        .filter { it.id != "first_bike_for_user" }
        .map { ScoringBreakdown(eventType = it.id, label = it.label, points = it.points) }
    PointsAwardedOverlay(
        breakdown = breakdown,
        onDismiss = {},
        onShowScoreRules = {}
    )
}
