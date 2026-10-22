package net.irazad.ghostwire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.irazad.ghostwire.ui.theme.ElectricCyan
import net.irazad.ghostwire.ui.theme.DeepViolet

@Composable
fun LiveThroughputChart(
    uploadBps: Long,
    downloadBps: Long,
    modifier: Modifier = Modifier
) {
    // Generate simulated dynamic waveforms based on current rates
    val downKbps = (downloadBps / 1024).coerceAtLeast(0)
    val upKbps = (uploadBps / 1024).coerceAtLeast(0)

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ElectricCyan, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DOWN: ${downKbps} KB/s",
                        color = ElectricCyan,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(DeepViolet, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UP: ${upKbps} KB/s",
                        color = DeepViolet,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Waveform Graphic Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val width = size.width
                val height = size.height

                // Draw background horizontal grid lines
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, height * 0.25f),
                    end = Offset(width, height * 0.25f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, height * 0.75f),
                    end = Offset(width, height * 0.75f),
                    strokeWidth = 1.dp.toPx()
                )

                // Downlink Waveform Path
                val downPath = Path().apply {
                    moveTo(0f, height * 0.8f)
                    cubicTo(
                        width * 0.25f, height * 0.3f,
                        width * 0.5f, height * 0.7f,
                        width * 0.75f, height * 0.2f
                    )
                    lineTo(width, height * 0.4f)
                }

                drawPath(
                    path = downPath,
                    color = ElectricCyan,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Uplink Waveform Path
                val upPath = Path().apply {
                    moveTo(0f, height * 0.9f)
                    cubicTo(
                        width * 0.25f, height * 0.6f,
                        width * 0.5f, height * 0.85f,
                        width * 0.75f, height * 0.5f
                    )
                    lineTo(width, height * 0.65f)
                }

                drawPath(
                    path = upPath,
                    color = DeepViolet,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
