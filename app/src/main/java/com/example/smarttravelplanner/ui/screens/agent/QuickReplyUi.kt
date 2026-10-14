package com.example.smarttravelplanner.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.QuickReplyRepository
import com.example.smarttravelplanner.domain.util.QuickReplies
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Templates and "save as template", handed down to the review text fields. */
data class QuickReplyState(
    val replies: List<String> = emptyList(),
    val onSave: (String) -> Unit = {}
)

/** Lets the review screen's text fields read the agent's templates without passing them through every composable. */
val LocalQuickReplies = staticCompositionLocalOf { QuickReplyState() }

/** Manages templates from the Profile tab. */
@HiltViewModel
class QuickRepliesViewModel @Inject constructor(
    private val repository: QuickReplyRepository
) : ViewModel() {
    val replies: StateFlow<List<String>> = repository.observeQuickReplies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(text: String) {
        viewModelScope.launch { repository.addQuickReply(text) }
    }

    fun remove(text: String) {
        viewModelScope.launch { repository.removeQuickReply(text) }
    }

    fun reset() {
        viewModelScope.launch { repository.resetQuickReplies() }
    }
}

/**
 * One-tap chips under a review text box. Tapping a chip adds that text to the box;
 * "Save" stores what the agent typed as a new template.
 */
@Composable
internal fun QuickReplyChips(
    currentText: String,
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = LocalQuickReplies.current
    val typed = QuickReplies.clean(currentText)
    val canSave = typed != null && state.replies.none { it.equals(typed, ignoreCase = true) }
    if (state.replies.isEmpty() && !canSave) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canSave) {
            ChipText(
                text = "Save as template",
                leading = { Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, tint = Amber, modifier = Modifier.size(14.dp)) },
                highlighted = true,
                onClick = { state.onSave(typed!!) }
            )
        }
        state.replies.forEach { reply ->
            ChipText(text = reply, onClick = { onInsert(QuickReplies.insertInto(currentText, reply)) })
        }
    }
}

@Composable
private fun ChipText(
    text: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) Amber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.size(4.dp))
        }
        Text(
            text = text,
            fontFamily = DmSansFontFamily,
            fontSize = 12.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (highlighted) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp)
        )
    }
}

/** Profile-tab card to view, add, delete and reset templates. */
@Composable
internal fun QuickRepliesCard(
    replies: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onReset: () -> Unit
) {
    var newText by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "QUICK REPLIES \u00B7 ${replies.size}/${QuickReplies.MAX_COUNT}",
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = OnBackgroundGray,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onReset) {
                Text("Reset", fontFamily = DmSansFontFamily, fontSize = 12.sp, color = Amber)
            }
        }
        Text(
            "Tap one under any review comment to add it in one go.",
            fontFamily = DmSansFontFamily,
            fontSize = 12.sp,
            color = OnBackgroundGray
        )
        Spacer(Modifier.height(6.dp))
        if (replies.isEmpty()) {
            Text(
                "No templates. Add one below or tap Reset for the defaults.",
                fontFamily = DmSansFontFamily,
                fontSize = 13.sp,
                color = OnBackgroundGray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        replies.forEachIndexed { index, reply ->
            if (index > 0) HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    reply,
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(reply) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Delete template", tint = OnBackgroundGray)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it.take(QuickReplies.MAX_LENGTH) },
                placeholder = { Text("New template", fontSize = 13.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Amber, cursorColor = Amber),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    onAdd(newText)
                    newText = ""
                },
                enabled = QuickReplies.clean(newText) != null && replies.size < QuickReplies.MAX_COUNT
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add template", tint = Amber)
            }
        }
    }
}
