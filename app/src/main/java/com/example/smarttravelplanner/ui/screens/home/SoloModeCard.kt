package com.example.smarttravelplanner.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.util.SoloModeState
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray

/**
 * Home card with the Solo-mode switch. When the traveller's trips look solo and they
 * haven't chosen yet, it also suggests turning it on ("Turn on" / "Not now").
 */
@Composable
fun SoloModeCard(
    state: SoloModeState,
    onSoloModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (state.enabled) Amber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Amber.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Hiking, contentDescription = null, tint = Amber)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Solo mode",
                    fontFamily = DmSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        state.enabled -> "Suggestions are tuned for travelling alone"
                        state.detectedSolo -> "You usually travel solo"
                        else -> "Tune suggestions for travelling alone"
                    },
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = OnBackgroundGray
                )
            }
            Switch(
                checked = state.enabled,
                onCheckedChange = onSoloModeChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Amber, checkedThumbColor = Color.White)
            )
        }
        if (state.showNudge) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Most of your recent trips are just you. Turn on Solo mode to see group trips you can join and solo-friendly plans.",
                fontFamily = DmSansFontFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSoloModeChange(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color(0xFF1A0A00))
                ) { Text("Turn on", fontWeight = FontWeight.Bold) }
                TextButton(onClick = { onSoloModeChange(false) }) {
                    Text("Not now", color = OnBackgroundGray)
                }
            }
        }
    }
}
