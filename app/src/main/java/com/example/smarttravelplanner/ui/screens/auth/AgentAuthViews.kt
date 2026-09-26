package com.example.smarttravelplanner.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.data.local.seed.DemoAgentSeeder
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/** Colours shared by the agent views; mirrors the traveller form's palette. */
internal data class AgentFormColors(
    val textPrimary: Color,
    val textMuted: Color,
    val labelColor: Color,
    val fieldBackground: Color,
    val fieldBorder: Color,
    val fieldPlaceholder: Color,
    val fieldIcon: Color,
    val submitBtnBg: Color,
    val submitBtnDisabledBg: Color,
    val submitTextEnabled: Color,
    val submitTextDisabled: Color,
    val accent: Color
)

// ─────────────────────────────────────────────────────────────────────────────
//  Agent Sign In
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun AgentSignInView(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    colors: AgentFormColors,
    onSignIn: () -> Unit,
    onUseDemoAccount: (email: String, password: String) -> Unit,
    onCreateAgentAccount: () -> Unit,
    onBack: () -> Unit
) {
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !isLoading

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AgentHeader(
            title = "Agent Sign In",
            subtitle = "For travel agents reviewing travellers' itineraries.",
            colors = colors
        )

        AuthFormField(
            label = "AGENT EMAIL",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "agent@agency.com",
            leadingIcon = Icons.Outlined.Email,
            labelColor = colors.labelColor,
            fieldBackground = colors.fieldBackground,
            fieldBorder = colors.fieldBorder,
            fieldPlaceholder = colors.fieldPlaceholder,
            fieldIcon = colors.fieldIcon,
            textPrimary = colors.textPrimary,
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(14.dp))
        AuthFormField(
            label = "PASSWORD",
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "••••••••",
            leadingIcon = Icons.Outlined.Lock,
            labelColor = colors.labelColor,
            fieldBackground = colors.fieldBackground,
            fieldBorder = colors.fieldBorder,
            fieldPlaceholder = colors.fieldPlaceholder,
            fieldIcon = colors.fieldIcon,
            textPrimary = colors.textPrimary,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = { if (canSubmit) onSignIn() }
        )

        Spacer(modifier = Modifier.height(20.dp))
        AgentSubmitButton(
            text = "Sign In as Agent →",
            enabled = canSubmit,
            isLoading = isLoading,
            colors = colors,
            onClick = onSignIn
        )

        // ── Demo accounts (tap to fill) ──────────────────────────────────────
        Spacer(modifier = Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.fieldBackground)
                .padding(12.dp)
        ) {
            Text(
                text = "DEMO AGENTS · password ${DemoAgentSeeder.DEMO_PASSWORD}",
                fontFamily = DmSansFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.labelColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            DemoAgentSeeder.DEMO_AGENTS.forEach { agent ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onUseDemoAccount(agent.email, DemoAgentSeeder.DEMO_PASSWORD) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = agent.agencyName,
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = agent.email,
                        fontFamily = DmSansFontFamily,
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "New agent? Create an agent account",
            fontFamily = DmSansFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accent,
            modifier = Modifier
                .clickable(onClick = onCreateAgentAccount)
                .padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        BackLink(text = "← Back to traveller sign in", colors = colors, onClick = onBack)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Agent Sign Up
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun AgentSignUpView(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    agencyName: String,
    onAgencyNameChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    colors: AgentFormColors,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val canSubmit = fullName.isNotBlank() && agencyName.isNotBlank() &&
        email.isNotBlank() && password.isNotBlank() && !isLoading

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AgentHeader(
            title = "Create Agent Account",
            subtitle = "Travellers will see your agency when choosing who reviews their trip.",
            colors = colors
        )

        AuthFormField(
            label = "AGENCY NAME",
            value = agencyName,
            onValueChange = onAgencyNameChange,
            placeholder = "e.g. Wanderlust Journeys",
            leadingIcon = Icons.Outlined.Business,
            labelColor = colors.labelColor,
            fieldBackground = colors.fieldBackground,
            fieldBorder = colors.fieldBorder,
            fieldPlaceholder = colors.fieldPlaceholder,
            fieldIcon = colors.fieldIcon,
            textPrimary = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(14.dp))
        AuthFormField(
            label = "CITY",
            value = city,
            onValueChange = onCityChange,
            placeholder = "e.g. Jaipur",
            leadingIcon = Icons.Outlined.LocationCity,
            labelColor = colors.labelColor,
            fieldBackground = colors.fieldBackground,
            fieldBorder = colors.fieldBorder,
            fieldPlaceholder = colors.fieldPlaceholder,
            fieldIcon = colors.fieldIcon,
            textPrimary = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(14.dp))
        AuthFormField(
            label = "YOUR FULL NAME",
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = "e.g. Priya Sharma",
            leadingIcon = Icons.Outlined.Person,
            labelColor = colors.labelColor,
            fieldBackground = colors.fieldBackground,
            fieldBorder = colors.fieldBorder,
            fieldPlaceholder = colors.fieldPlaceholder,
            fieldIcon = colors.fieldIcon,
            textPrimary = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(14.dp))
        AuthFormField(
            label = "WORK EMAIL",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "agent@agency.com",
            leadingIcon = Icons.Outlined.Email,
            labelColor = colors.labelColor,
            fieldBackground = colors.fieldBackground,
            fieldBorder = colors.fieldBorder,
            fieldPlaceholder = colors.fieldPlaceholder,
            fieldIcon = colors.fieldIcon,
            textPrimary = colors.textPrimary,
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(14.dp))
        AuthFormField(
            label = "PASSWORD",
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "At least 6 characters",
            leadingIcon = Icons.Outlined.Lock,
            labelColor = colors.labelColor,
            fieldBackground = colors.fieldBackground,
            fieldBorder = colors.fieldBorder,
            fieldPlaceholder = colors.fieldPlaceholder,
            fieldIcon = colors.fieldIcon,
            textPrimary = colors.textPrimary,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = { if (canSubmit) onSubmit() }
        )

        Spacer(modifier = Modifier.height(20.dp))
        AgentSubmitButton(
            text = "Send Verification Code →",
            enabled = canSubmit,
            isLoading = isLoading,
            colors = colors,
            onClick = onSubmit
        )
        Spacer(modifier = Modifier.height(16.dp))
        BackLink(text = "← Back to agent sign in", colors = colors, onClick = onBack)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared pieces
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AgentHeader(title: String, subtitle: String, colors: AgentFormColors) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Amber.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.SupportAgent,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "TRAVEL AGENT",
            fontFamily = DmSansFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Amber
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = title,
        fontFamily = PlayfairFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = subtitle,
        fontFamily = DmSansFontFamily,
        fontSize = 13.sp,
        color = colors.textMuted,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun AgentSubmitButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    colors: AgentFormColors,
    onClick: () -> Unit
) {
    val textColor = if (enabled) colors.submitTextEnabled else colors.submitTextDisabled
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.submitBtnBg,
            contentColor = textColor,
            disabledContainerColor = colors.submitBtnDisabledBg,
            disabledContentColor = textColor
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = textColor
            )
        }
    }
}

@Composable
private fun BackLink(text: String, colors: AgentFormColors, onClick: () -> Unit) {
    Text(
        text = text,
        fontFamily = DmSansFontFamily,
        fontSize = 13.sp,
        color = colors.textMuted,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    )
}
