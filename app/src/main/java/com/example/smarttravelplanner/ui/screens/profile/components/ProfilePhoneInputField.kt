package com.example.smarttravelplanner.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.CompletedGreen
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily

@Composable
fun ProfilePhoneInputField(
    phone: String,
    isVerified: Boolean,
    onPhoneChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Phone Number",
            fontSize = 11.sp,
            fontFamily = DmSansFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+91",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )

            VerticalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .height(18.dp)
                    .padding(horizontal = 10.dp)
            )

            BasicTextField(
                value = phone,
                onValueChange = onPhoneChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(Amber),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (phone.isEmpty()) {
                        Text(
                            text = "Enter 10-digit number",
                            fontSize = 14.sp,
                            fontFamily = DmSansFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )

            if (isVerified) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = CompletedGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
