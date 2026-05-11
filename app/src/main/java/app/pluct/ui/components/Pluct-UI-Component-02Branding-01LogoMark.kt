package app.pluct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.ui.theme.PluctBrandCyan
import app.pluct.ui.theme.PluctBrandPurple

/**
 * Pluct-UI-Component-02Branding-01LogoMark
 * In-app brand mark (gradient P) — avoids shipping a raster dependency; swap for PNG later if needed.
 */
@Composable
fun PluctUIComponent02Branding01LogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val gradient = Brush.linearGradient(
        colors = listOf(PluctBrandPurple, PluctBrandCyan)
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(gradient)
            .semantics {
                testTag = "pluct_brand_logo_mark"
                contentDescription = "Pluct"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "P",
            color = Color.White,
            fontSize = (size.value * 0.45f).sp,
            fontWeight = FontWeight.Black
        )
    }
}
