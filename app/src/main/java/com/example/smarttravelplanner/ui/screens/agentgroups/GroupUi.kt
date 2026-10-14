package com.example.smarttravelplanner.ui.screens.agentgroups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.util.GroupStatus
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray

/** Colour used for a group's status chip. */
fun groupStatusColor(status: GroupStatus): Color = when (status) {
    GroupStatus.OPEN -> Color(0xFF4ADE80)
    GroupStatus.FULL -> Amber
    GroupStatus.ONGOING -> Color(0xFF60A5FA)
    GroupStatus.CANCELLED -> Color(0xFFF87171)
    GroupStatus.DRAFT, GroupStatus.COMPLETED -> OnBackgroundGray
}

/** Small rounded label: Draft / Open / Full / On the trip / Completed / Cancelled. */
@Composable
fun GroupStatusChip(status: GroupStatus) {
    val color = groupStatusColor(status)
    Text(
        status.label,
        fontFamily = DmSansFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/** Packages | Group trips switch shown at the top of the agent's Packages tab. */
@Composable
fun PackagesModeSwitch(
    groupsSelected: Boolean,
    onSelect: (groups: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("Packages" to false, "Group trips" to true).forEach { (label, isGroups) ->
            val selected = groupsSelected == isGroups
            Text(
                label,
                fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) Amber else Color.Transparent)
                    .clickable { onSelect(isGroups) }
                    .padding(vertical = 10.dp)
            )
        }
    }
}
