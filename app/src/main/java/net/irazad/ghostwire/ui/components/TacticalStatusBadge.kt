package net.irazad.ghostwire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.irazad.ghostwire.ui.theme.ElectricCyan
import net.irazad.ghostwire.ui.theme.StatusGreen

@Composable
fun TacticalStatusBadge(
    title: String,
    accentColor: Color = StatusGreen,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .clip(pillShape)
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.4f), pillShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(accentColor, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = accentColor,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
