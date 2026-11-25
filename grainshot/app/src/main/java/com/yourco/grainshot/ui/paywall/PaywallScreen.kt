package com.yourco.grainshot.ui.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yourco.grainshot.ui.theme.*

@Composable
fun PaywallScreen(
    price:       String,
    pending:     Boolean,
    error:       String?,
    onSubscribe: () -> Unit,
    onDismiss:   () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(ColorScrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(300.dp).background(ColorSurface)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("10 SHOTS USED", color = ColorOnBackground, fontFamily = ShareTechMono,
                fontSize = 20.sp, letterSpacing = 0.05.em, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("UNLOCK UNLIMITED CAPTURES", color = ColorOnSurfaceDim,
                fontFamily = ShareTechMono, fontSize = 11.sp,
                letterSpacing = 0.05.em, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text(price.uppercase(), color = ColorOnBackground, fontFamily = ShareTechMono,
                fontSize = 14.sp, letterSpacing = 0.05.em, textAlign = TextAlign.Center)
            Text("ONE TIME — FOREVER", color = ColorOnSurfaceDim, fontFamily = ShareTechMono,
                fontSize = 11.sp, letterSpacing = 0.05.em, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(48.dp)
                    .background(if (pending) ColorAccentDim else ColorAccent)
                    .clickable(enabled = !pending, onClick = onSubscribe),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (pending) "..." else "BUY ONCE — $price",
                    color = Color.White, fontFamily = ShareTechMono,
                    fontSize = 14.sp, letterSpacing = 0.08.em)
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error.uppercase(), color = ColorError, fontFamily = ShareTechMono,
                    fontSize = 10.sp, letterSpacing = 0.05.em, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
            Text("NOT NOW", color = ColorOnSurfaceDim, fontFamily = ShareTechMono,
                fontSize = 10.sp, letterSpacing = 0.05.em,
                modifier = Modifier.clickable(onClick = onDismiss),
                textAlign = TextAlign.Center)
        }
    }
}
