package net.irazad.ghostwire.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.irazad.ghostwire.R
import net.irazad.ghostwire.data.models.VpnStatus
import net.irazad.ghostwire.ui.theme.ElectricCyan
import net.irazad.ghostwire.ui.theme.DeepViolet
import net.irazad.ghostwire.ui.theme.StatusAmber
import net.irazad.ghostwire.ui.theme.StatusGreen

@Composable
fun BiometricConnectRing(
    status: VpnStatus,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BiometricRing")

    // Rotation angle for outer ticks
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (status == VpnStatus.CONNECTING) 3000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    // Pulse scale for ambient breathing glow
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val ringColor = when (status) {
        VpnStatus.DISCONNECTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        VpnStatus.CONNECTING -> StatusAmber
        VpnStatus.CONNECTED -> StatusGreen
        VpnStatus.RECONNECTING -> StatusAmber
    }

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    val statusStringRes = when (status) {
        VpnStatus.DISCONNECTED -> R.string.status_disconnected
        VpnStatus.CONNECTING -> R.string.status_connecting
        VpnStatus.CONNECTED -> R.string.status_connected
        VpnStatus.RECONNECTING -> R.string.status_reconnecting
    }
    val actionStringRes = if (status == VpnStatus.CONNECTED) {
        R.string.tap_to_disconnect
    } else {
        R.string.tap_to_connect
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Canvas Outer Glow & Cyber Ticks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 16.dp.toPx()

            // Outer Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ringColor.copy(alpha = pulseAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius + 24.dp.toPx()
                ),
                radius = radius + 24.dp.toPx(),
                center = center
            )

            // Middle Cyber Ring
            drawCircle(
                color = ringColor.copy(alpha = 0.6f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Outer Orbit Arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ElectricCyan, DeepViolet, ElectricCyan),
                    center = center
                ),
                startAngle = rotationAngle,
                sweepAngle = 220f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx()),
                topLeft = Offset(center.x - radius - 8.dp.toPx(), center.y - radius - 8.dp.toPx()),
                size = androidx.compose.ui.geometry.Size((radius + 8.dp.toPx()) * 2, (radius + 8.dp.toPx()) * 2)
            )

            // Inner Core Circle
            drawCircle(
                color = surfaceVariantColor,
                radius = radius - 16.dp.toPx(),
                center = center
            )
            drawCircle(
                color = ringColor.copy(alpha = 0.25f),
                radius = radius - 16.dp.toPx(),
                center = center
            )
        }

        // Center Content: Icon + Status Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_ghostwire_shield),
                contentDescription = null,
                tint = ringColor,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(statusStringRes),
                color = ringColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(actionStringRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
