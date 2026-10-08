package com.example.smarttravelplanner.ui.screens.tripedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Attractions
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Category Metadata (Icons & Vibrant Themed Accents)
// ─────────────────────────────────────────────────────────────────────────────
private data class CategoryMeta(
    val category: ActivityCategory,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color
)

private val CATEGORY_CONFIG = listOf(
    CategoryMeta(ActivityCategory.ATTRACTION, "Attraction", Icons.Outlined.Attractions, Amber),
    CategoryMeta(ActivityCategory.FOOD, "Dining", Icons.Outlined.Restaurant, Color(0xFFF97316)),
    CategoryMeta(ActivityCategory.TRANSPORT, "Transit", Icons.Outlined.DirectionsBus, Color(0xFF0EA5E9)),
    CategoryMeta(ActivityCategory.ACCOMMODATION, "Stay", Icons.Outlined.Hotel, Color(0xFF6366F1)),
    CategoryMeta(ActivityCategory.NATURE, "Nature", Icons.Outlined.Forest, Color(0xFF10B981)),
    CategoryMeta(ActivityCategory.CULTURE, "Culture", Icons.Outlined.Museum, Color(0xFF8B5CF6)),
    CategoryMeta(ActivityCategory.SHOPPING, "Shopping", Icons.Outlined.ShoppingBag, Color(0xFFEC4899)),
    CategoryMeta(ActivityCategory.OTHER, "Other", Icons.Outlined.Place, Color(0xFF64748B))
)

// ─────────────────────────────────────────────────────────────────────────────
//  Quick Time Slot Presets
// ─────────────────────────────────────────────────────────────────────────────
private data class TimeSlotPreset(val label: String, val start: String, val end: String)

private val TIME_PRESETS = listOf(
    TimeSlotPreset("🌅 Morning", "09:00", "11:30"),
    TimeSlotPreset("☀️ Midday", "12:00", "14:00"),
    TimeSlotPreset("☕ Afternoon", "14:30", "17:00"),
    TimeSlotPreset("🌇 Sunset", "17:30", "19:30"),
    TimeSlotPreset("🌙 Night", "20:00", "22:30")
)

private val AI_IDEA_PRESETS = listOf(
    "🌅 Scenic Viewpoint",
    "🍽️ Local Food & Cafe",
    "🏛️ Historic Landmark",
    "💰 Budget-Friendly Spot",
    "🌧️ Indoor Alternative",
    "⚡ Thrilling Adventure"
)

/**
 * Rebuilt Edit & Add Activity Sheet:
 * - Fluid Material 3 bottom sheet with drag handle.
 * - Interactive Time Picker dialog & 1-tap slot presets (no manual typing colons on numeric keyboards).
 * - Full visual category grid with themed icons and accent colors.
 * - Clean cost entry with currency prefix and quick-add chips.
 * - AI Assistant with instant idea chips and safe asynchronous execution.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityFormSheet(
    dayNumber: Int,
    existing: Activity?,
    onSave: (Activity) -> Unit,
    onDismiss: () -> Unit,
    onRegenerateWithAi: ((baseActivity: Activity, instructions: String, onComplete: (Activity) -> Unit) -> Unit)? = null,
    isRegenerating: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Form states
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var startTime by remember { mutableStateOf(existing?.startTime.orEmpty()) }
    var endTime by remember { mutableStateOf(existing?.endTime.orEmpty()) }
    var location by remember { mutableStateOf(existing?.location.orEmpty()) }
    var costText by remember {
        mutableStateOf(existing?.estimatedCost?.toLong()?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: ActivityCategory.ATTRACTION) }

    // Preserve GPS coords from existing activity or AI regeneration
    var currentLat by remember { mutableStateOf(existing?.lat) }
    var currentLng by remember { mutableStateOf(existing?.lng) }
    var currentPlaceId by remember { mutableStateOf(existing?.placeId.orEmpty()) }

    // Errors & feedback
    var titleError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var aiStatusMessage by remember { mutableStateOf<String?>(null) }
    var isLocalAiBusy by remember { mutableStateOf(false) }

    // Time picker dialog state: null = none, true = start, false = end
    var timePickerTargetIsStart by remember { mutableStateOf<Boolean?>(null) }

    // Synchronize local busy state with external regeneration state
    LaunchedEffect(isRegenerating) {
        if (!isRegenerating && isLocalAiBusy) {
            isLocalAiBusy = false
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Amber,
        focusedLabelColor = Amber,
        cursorColor = Amber,
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Top Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Amber.copy(alpha = 0.18f),
                            border = BorderStroke(0.8.dp, Amber.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "DAY $dayNumber",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Amber,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = if (existing == null) "Add Activity" else "Edit Activity",
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (existing == null) "Plan a new stop for this day" else "Update stop details and timing",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.5.sp,
                        color = OnBackgroundGray
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Title Input ─────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(80)
                        if (title.isNotBlank()) titleError = null
                    },
                    label = { Text("Activity Title *") },
                    placeholder = { Text("e.g. Sunset at Nahargarh Fort") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = if (titleError != null) MaterialTheme.colorScheme.error else Amber,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    isError = titleError != null,
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (titleError != null) {
                    Text(
                        text = titleError!!,
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            // ── Time Selection (Interactive Pickers + Presets) ──────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Activity Timing",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Time Card
                    TimePickerButton(
                        label = "Start",
                        time24 = startTime,
                        onClick = { timePickerTargetIsStart = true },
                        onClear = { startTime = ""; timeError = null },
                        modifier = Modifier.weight(1f)
                    )
                    // End Time Card
                    TimePickerButton(
                        label = "End",
                        time24 = endTime,
                        onClick = { timePickerTargetIsStart = false },
                        onClear = { endTime = ""; timeError = null },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (timeError != null) {
                    Text(
                        text = timeError!!,
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Quick Slot Presets
                Text(
                    text = "Quick slot presets:",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.5.sp,
                    color = OnBackgroundGray
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TIME_PRESETS.forEach { preset ->
                        val isSelected = startTime == preset.start && endTime == preset.end
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Amber.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, if (isSelected) Amber else Color.Transparent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    startTime = preset.start
                                    endTime = preset.end
                                    timeError = null
                                }
                        ) {
                            Text(
                                text = "${preset.label} (${preset.start}-${preset.end})",
                                fontFamily = DmSansFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Amber else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // ── Location ────────────────────────────────────────────────────
            OutlinedTextField(
                value = location,
                onValueChange = { location = it.take(120) },
                label = { Text("Location / Landmark") },
                placeholder = { Text("e.g. City Palace Complex, Pink City") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Category Selector ───────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CATEGORY_CONFIG.forEach { meta ->
                        val selected = meta.category == category
                        val bgColor by animateColorAsState(
                            targetValue = if (selected) meta.accentColor.copy(alpha = if (isDark) 0.25f else 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            label = "cat_bg"
                        )
                        val borderColor = if (selected) meta.accentColor else Color.Transparent

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            border = BorderStroke(1.2.dp, borderColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { category = meta.category }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = meta.icon,
                                    contentDescription = null,
                                    tint = if (selected) meta.accentColor else OnBackgroundGray,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = meta.label,
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) meta.accentColor else MaterialTheme.colorScheme.onSurface
                                )
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = meta.accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Estimated Cost ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter(Char::isDigit).take(7) },
                    label = { Text("Estimated Cost (₹)") },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.CurrencyRupee,
                            contentDescription = null,
                            tint = Amber,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Cost Presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Free" to 0, "+₹200" to 200, "+₹500" to 500, "+₹1k" to 1000).forEach { (label, amount) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val current = costText.toLongOrNull() ?: 0L
                                    costText = if (amount == 0) "0" else (current + amount).toString()
                                }
                        ) {
                            Text(
                                text = label,
                                fontFamily = DmSansFontFamily,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Description / Notes ─────────────────────────────────────────
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(500) },
                label = { Text("Description / Notes") },
                placeholder = { Text("e.g. Highlights, ticket requirements, or photography tips...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Notes,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(18.dp)
                    )
                },
                minLines = 2,
                maxLines = 4,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // ── AI Assistant & Inspiration Section ──────────────────────────
            if (onRegenerateWithAi != null) {
                var showAiPrompt by remember { mutableStateOf(false) }
                var aiInstruction by remember { mutableStateOf("") }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, Amber.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Amber.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = Amber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "AI Assist & Suggestions",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(
                                onClick = { showAiPrompt = !showAiPrompt },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (showAiPrompt) "Hide options" else "Customize prompt",
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 11.5.sp,
                                    color = Amber
                                )
                            }
                        }

                        Text(
                            text = "Ask Gemini AI to refine this stop, enrich details, or suggest alternative ideas.",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            color = OnBackgroundGray
                        )

                        // Quick AI Idea Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AI_IDEA_PRESETS.take(4).forEach { idea ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    border = BorderStroke(0.6.dp, Amber.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            aiInstruction = idea
                                            showAiPrompt = true
                                        }
                                ) {
                                    Text(
                                        text = idea,
                                        fontFamily = DmSansFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (showAiPrompt) {
                            OutlinedTextField(
                                value = aiInstruction,
                                onValueChange = { aiInstruction = it.take(150) },
                                label = { Text("Special AI instruction") },
                                placeholder = { Text("e.g. Suggest a rooftop cafe, or make it indoor") },
                                colors = fieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        AnimatedVisibility(visible = aiStatusMessage != null) {
                            Text(
                                text = aiStatusMessage.orEmpty(),
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                color = Amber,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val cost = costText.toDoubleOrNull() ?: 0.0
                                val base = (existing ?: Activity(itineraryId = 0, title = "")).copy(
                                    title = title.trim(),
                                    startTime = startTime.trim(),
                                    endTime = endTime.trim(),
                                    location = location.trim(),
                                    estimatedCost = cost,
                                    description = description.trim(),
                                    category = category
                                )
                                isLocalAiBusy = true
                                aiStatusMessage = "Asking AI for suggestions..."

                                onRegenerateWithAi(base, aiInstruction) { newAct ->
                                    isLocalAiBusy = false
                                    title = newAct.title
                                    if (newAct.startTime.isNotBlank()) startTime = newAct.startTime
                                    if (newAct.endTime.isNotBlank()) endTime = newAct.endTime
                                    location = newAct.location
                                    if (newAct.estimatedCost > 0) costText = newAct.estimatedCost.toInt().toString()
                                    description = newAct.description
                                    category = newAct.category
                                    if (newAct.lat != null) currentLat = newAct.lat
                                    if (newAct.lng != null) currentLng = newAct.lng
                                    if (newAct.placeId.isNotBlank()) currentPlaceId = newAct.placeId
                                    aiStatusMessage = "✨ Stop updated with AI suggestions!"
                                }
                            },
                            enabled = !isLocalAiBusy && !isRegenerating,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Amber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLocalAiBusy || isRegenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    color = Amber,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Generating with AI...",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Amber,
                                    fontSize = 12.5.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = Amber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Regenerate Stop Details with AI",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Amber,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Primary Action Buttons ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = "Please give the activity a title"
                            return@Button
                        }
                        if (startTime.isNotBlank() && endTime.isNotBlank()) {
                            val startMin = timeToMinutes(startTime)
                            val endMin = timeToMinutes(endTime)
                            if (startMin != null && endMin != null && endMin < startMin) {
                                timeError = "End time should be after start time"
                                return@Button
                            }
                        }

                        val cost = costText.toDoubleOrNull() ?: 0.0
                        val placeChanged = existing != null && existing.location.trim() != location.trim()
                        val base = existing ?: Activity(itineraryId = 0, title = "")

                        onSave(
                            base.copy(
                                title = title.trim(),
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                location = location.trim(),
                                estimatedCost = cost,
                                description = description.trim(),
                                category = category,
                                lat = if (placeChanged && currentLat == existing?.lat) null else currentLat,
                                lng = if (placeChanged && currentLng == existing?.lng) null else currentLng,
                                placeId = if (placeChanged && currentPlaceId == existing?.placeId) "" else currentPlaceId
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = Color(0xFF1A1A1A)
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                ) {
                    Text(
                        text = if (existing == null) "Add Activity" else "Save Changes",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }
            }
        }
    }

    // ── Time Picker Dialog ──────────────────────────────────────────────────
    val targetIsStart = timePickerTargetIsStart
    if (targetIsStart != null) {
        val currentTime = if (targetIsStart) startTime else endTime
        TimePickerDialog(
            initialTime24 = currentTime,
            title = if (targetIsStart) "Select Start Time" else "Select End Time",
            onDismiss = { timePickerTargetIsStart = null },
            onConfirm = { selected24 ->
                if (targetIsStart) {
                    startTime = selected24
                    // Auto-suggest 1.5h later for end time if end time is unset
                    if (endTime.isBlank()) {
                        val startMin = timeToMinutes(selected24)
                        if (startMin != null) {
                            val defaultEnd = (startMin + 90) % (24 * 60)
                            endTime = minutesToTime(defaultEnd)
                        }
                    }
                } else {
                    endTime = selected24
                }
                timeError = null
                timePickerTargetIsStart = null
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Time Picker Button Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TimePickerButton(
    label: String,
    time24: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasTime = time24.isNotBlank()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, if (hasTime) Amber.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.sp,
                    color = OnBackgroundGray
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = if (hasTime) Amber else OnBackgroundGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (hasTime) formatDisplayTime(time24) else "Select",
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = if (hasTime) FontWeight.Bold else FontWeight.Normal,
                        color = if (hasTime) MaterialTheme.colorScheme.onSurface else OnBackgroundGray
                    )
                }
            }

            if (hasTime) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear time",
                        modifier = Modifier.size(14.dp),
                        tint = OnBackgroundGray
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Intuitive Material 3 Time Picker Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TimePickerDialog(
    initialTime24: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialMinutes = timeToMinutes(initialTime24) ?: (9 * 60)
    var hour12 by remember {
        val h = (initialMinutes / 60) % 24
        mutableIntStateOf(if (h == 0) 12 else if (h > 12) h - 12 else h)
    }
    var minute by remember { mutableIntStateOf(initialMinutes % 60) }
    var isPm by remember { mutableStateOf((initialMinutes / 60) >= 12) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Amber.copy(alpha = 0.3f)),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Time Display Box + AM/PM Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Amber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Amber.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = String.format(Locale.ENGLISH, "%02d:%02d", hour12, minute),
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Amber,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isPm) Amber else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isPm = false }
                        ) {
                            Text(
                                text = "AM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (!isPm) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPm) Amber else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isPm = true }
                        ) {
                            Text(
                                text = "PM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isPm) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Hours Selector (1 to 12)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Hour", fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..12).forEach { h ->
                            val sel = h == hour12
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (sel) Amber else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { hour12 = h }
                            ) {
                                Text(
                                    text = h.toString(),
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (sel) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                // Minutes Selector (00, 15, 30, 45)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Minutes", fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0, 15, 30, 45).forEach { m ->
                            val sel = m == minute
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (sel) Amber else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { minute = m }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.ENGLISH, ":%02d", m),
                                        fontFamily = DmSansFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (sel) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = OnBackgroundGray, fontFamily = DmSansFontFamily)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val h24 = when {
                                !isPm && hour12 == 12 -> 0
                                isPm && hour12 < 12 -> hour12 + 12
                                else -> hour12
                            }
                            val formatted24 = String.format(Locale.ENGLISH, "%02d:%02d", h24, minute)
                            onConfirm(formatted24)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Amber)
                    ) {
                        Text("Done", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Time Conversion Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun formatDisplayTime(time24: String): String {
    val min = timeToMinutes(time24) ?: return time24
    val h = (min / 60) % 24
    val m = min % 60
    val isPm = h >= 12
    val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
    return String.format(Locale.ENGLISH, "%d:%02d %s", h12, m, if (isPm) "PM" else "AM")
}

private fun timeToMinutes(time: String): Int? {
    val clean = time.trim()
    if (clean.isBlank()) return null
    val parts = clean.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

private fun minutesToTime(minutes: Int): String {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    return String.format(Locale.ENGLISH, "%02d:%02d", h, m)
}
