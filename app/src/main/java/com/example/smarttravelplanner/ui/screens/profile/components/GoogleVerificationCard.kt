package com.example.smarttravelplanner.ui.screens.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.ui.theme.CompletedGreen
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.ProductSansFontFamily

@Composable
fun GoogleVerificationCard(
    isGoogleVerified: Boolean,
    onVerifyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Other Verification Option",
            fontSize = 11.sp,
            fontFamily = DmSansFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(
                    width = 1.dp,
                    color = if (isGoogleVerified) CompletedGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(enabled = !isGoogleVerified, onClick = onVerifyClick)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(22.dp)
                )

                Text(
                    text = if (isGoogleVerified) {
                        "Google Account Connected ✓"
                    } else "Verify with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = ProductSansFontFamily,
                    color = if (isGoogleVerified) CompletedGreen else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
