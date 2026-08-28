package com.example.smarttravelplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import androidx.compose.material3.MaterialTheme

private data class NavItem(
    val route:        String,
    val label:        String,
    val filledIcon:   ImageVector,
    val outlineIcon:  ImageVector
)

private val navItems = listOf(
    NavItem("home",    "Home",     Icons.Filled.Home,    Icons.Outlined.Home),
    NavItem("plan",    "Plan",     Icons.Filled.Map,     Icons.Outlined.Map),
    NavItem("mytrips", "My Trips", Icons.Filled.Luggage, Icons.Outlined.Luggage),
    NavItem("explore", "Explore",  Icons.Filled.Explore, Icons.Outlined.Explore),
)

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Subtle top divider
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavBarItem(
                        modifier = Modifier.weight(1f),
                        item     = item,
                        selected = selected,
                        onClick  = { onNavigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    modifier: Modifier = Modifier,
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container: circular amber pill when active
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.background(Amber.copy(alpha = 0.15f))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector  = if (selected) item.filledIcon else item.outlineIcon,
                contentDescription = item.label,
                tint         = if (selected) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier     = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Label with explicit single line and center alignment
        Text(
            text       = item.label,
            fontSize   = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color      = if (selected) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = DmSansFontFamily,
            textAlign  = TextAlign.Center,
            maxLines   = 1
        )
    }
}