package com.example.smarttravelplanner.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.UserPreferences
import com.example.smarttravelplanner.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsDialog(
    prefs: UserPreferences,
    onDismiss: () -> Unit,
    onSave: (UserPreferences) -> Unit
) {
    var selectedProvider by remember(prefs) { mutableStateOf(prefs.aiProvider) }
    var selectedCloudModel by remember(prefs) { mutableStateOf(prefs.cloudModel) }
    var enableWebGrounding by remember(prefs) { mutableStateOf(prefs.enableWebGrounding) }
    var cloudApiKey by remember(prefs) { mutableStateOf(prefs.cloudApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var ollamaBaseUrl by remember(prefs) { mutableStateOf(prefs.ollamaBaseUrl) }
    var ollamaModel by remember(prefs) { mutableStateOf(prefs.ollamaModel) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val segmentedTrackColor = if (isDark) SurfaceElevated else SegmentedTrackLight
    val textMuted = if (isDark) OnBackgroundGray else OnBackgroundMedium

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header Title & Subtitle ──────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Engine Settings",
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure itinerary planner & intelligence",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // ── Smooth Segmented Switcher (Cloud vs Local) matching LoginScreen ────
            val isCloudSelected = selectedProvider.equals("CLOUD", ignoreCase = true)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(segmentedTrackColor)
                    .padding(4.dp)
            ) {
                val tabWidth = maxWidth / 2
                val targetOffset = if (isCloudSelected) 0.dp else tabWidth
                val animatedOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "provider_tab_indicator_offset"
                )

                // Smooth sliding indicator pill (yellow / Amber)
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .offset(x = animatedOffset)
                        .then(
                            if (!isDark) Modifier.shadow(2.dp, RoundedCornerShape(20.dp))
                            else Modifier
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(Amber)
                )

                // Tab click targets & labels
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedProvider = "CLOUD" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudQueue,
                                contentDescription = null,
                                tint = if (isCloudSelected) Color(0xFF0F172A) else textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cloud (Gemini)",
                                fontFamily = DmSansFontFamily,
                                fontWeight = if (isCloudSelected) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (isCloudSelected) Color(0xFF0F172A) else textMuted
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedProvider = "LOCAL" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = if (!isCloudSelected) Color(0xFF0F172A) else textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Local (Ollama)",
                                fontFamily = DmSansFontFamily,
                                fontWeight = if (!isCloudSelected) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (!isCloudSelected) Color(0xFF0F172A) else textMuted
                            )
                        }
                    }
                }
            }

            // ── Animated Content Switcher (Cloud vs Local) ────────────────────
            AnimatedContent(
                targetState = isCloudSelected,
                transitionSpec = {
                    if (!targetState) {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 } +
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                        (slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } +
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                    } else {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } +
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                        (slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 } +
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                    }.using(SizeTransform(clip = false))
                },
                label = "AiSettingsContentTransition"
            ) { isCloud ->
                if (isCloud) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Gemini Model",
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = DmSansFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Model 1: gemini-3.1-flash-lite (Default)
                            ModelSelectionCard(
                                title = "Gemini 3.1 Flash-Lite",
                                tag = "Default • Fastest",
                                description = "Ultra-fast response (~1.5s), lightweight, high accuracy on coordinates and budget.",
                                isSelected = selectedCloudModel == "gemini-3.1-flash-lite" || selectedCloudModel.isBlank(),
                                onClick = { selectedCloudModel = "gemini-3.1-flash-lite" }
                            )

                            // Model 2: gemini-3.6-flash
                            ModelSelectionCard(
                                title = "Gemini 3.6 Flash",
                                tag = "Balanced",
                                description = "Stronger reasoning and richer itinerary detail, with only a modest speed trade-off.",
                                isSelected = selectedCloudModel == "gemini-3.6-flash",
                                onClick = { selectedCloudModel = "gemini-3.6-flash" }
                            )

                            // Model 3: gemini-3.8-flash
                            ModelSelectionCard(
                                title = "Gemini 3.8 Flash",
                                tag = "Most Capable",
                                description = "Advanced spatial reasoning and multi-step synthesis for complex budget and travel surge analysis.",
                                isSelected = selectedCloudModel == "gemini-3.8-flash",
                                onClick = { selectedCloudModel = "gemini-3.8-flash" }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Real-Time Web Grounding Switch
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (enableWebGrounding) Amber.copy(alpha = 0.5f) else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.TravelExplore,
                                            contentDescription = null,
                                            tint = if (enableWebGrounding) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Live Web Grounding",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = DmSansFontFamily,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Uses Google Search to check live flight fare spikes, seasonal booking surges, and festival dates.",
                                        fontSize = 11.sp,
                                        fontFamily = DmSansFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                                Switch(
                                    checked = enableWebGrounding,
                                    onCheckedChange = { enableWebGrounding = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF0F172A),
                                        checkedTrackColor = Amber
                                    )
                                )
                            }
                        }

                        // Optional Custom API Key
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Custom Gemini API Key (Optional)",
                                fontWeight = FontWeight.Medium,
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = cloudApiKey,
                                onValueChange = { cloudApiKey = it },
                                placeholder = {
                                    Text("Using build-time key from local.properties", fontSize = 12.sp, fontFamily = DmSansFontFamily)
                                },
                                singleLine = true,
                                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                        Icon(
                                            imageVector = if (isApiKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                            contentDescription = "Toggle visibility",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Text(
                                text = "Leave blank to use preconfigured key. Override with your personal key if needed.",
                                fontSize = 10.sp,
                                fontFamily = DmSansFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    // ── Local Ollama Settings ─────────────────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Ollama Connection",
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = DmSansFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = ollamaBaseUrl,
                                onValueChange = { ollamaBaseUrl = it },
                                label = { Text("Ollama Base URL", fontFamily = DmSansFontFamily) },
                                placeholder = { Text("http://10.0.2.2:11434/", fontFamily = DmSansFontFamily) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = ollamaModel,
                                onValueChange = { ollamaModel = it },
                                label = { Text("Ollama Model", fontFamily = DmSansFontFamily) },
                                placeholder = { Text("qwen3.5:4b", fontFamily = DmSansFontFamily) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Amber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Local AI runs offline on your machine. Ensure Ollama is running and model is loaded before planning.",
                                        fontSize = 11.sp,
                                        fontFamily = DmSansFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Save Button ───────────────────────────────────────────────────
            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        coroutineScope.launch {
                            sheetState.hide()
                            val updated = prefs.copy(
                                aiProvider = selectedProvider,
                                cloudModel = selectedCloudModel.ifBlank { "gemini-3.1-flash-lite" },
                                enableWebGrounding = enableWebGrounding,
                                cloudApiKey = cloudApiKey.trim(),
                                ollamaBaseUrl = ollamaBaseUrl.trim(),
                                ollamaModel = ollamaModel.trim().ifBlank { "qwen3.5:4b" }
                            )
                            onSave(updated)
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC85A0D)
                )
            ) {
                Text(
                    text = "Save Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ModelSelectionCard(
    title: String,
    tag: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Amber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            1.dp,
            if (isSelected) Amber else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 2.dp else 1.5.dp,
                        color = if (isSelected) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Amber)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Amber.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            color = if (isSelected) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
