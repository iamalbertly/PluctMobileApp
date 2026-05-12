package app.pluct.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pluct.R

/**
 * Pluct-UI-Component-02Branding-01LogoMark
 * In-app brand mark aligned with launcher foreground (Customer: one recognizable icon everywhere).
 */
@Composable
fun PluctUIComponent02Branding01LogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Image(
        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .semantics {
                testTag = "pluct_brand_logo_mark"
                contentDescription = "Pluct"
            },
        contentScale = ContentScale.Fit
    )
}
