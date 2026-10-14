package com.example.smarttravelplanner.ui.screens.agentgroups

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.agent.GroupMember
import com.example.smarttravelplanner.domain.util.GroupFormat
import com.example.smarttravelplanner.domain.util.GroupStatus
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class GroupConfirm { CANCEL, DELETE }

/** One group for the agent: trip details, who joined, publish / cancel / delete. */
@Composable
fun AgentGroupDetailScreen(
    onBack: () -> Unit,
    onEdit: (groupId: Long) -> Unit,
    viewModel: AgentGroupDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<GroupConfirm?>(null) }

    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.noticeShown()
        }
    }
    LaunchedEffect(deleted) { if (deleted) onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        val ready = state as? GroupDetailState.Ready
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Group trip", fontFamily = DmSansFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            if (ready != null && !ready.detail.group.isCancelled) {
                IconButton(onClick = { onEdit(ready.detail.group.id) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Amber)
                }
            }
        }

        when (val s = state) {
            GroupDetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Amber)
            }
            GroupDetailState.Missing -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("This group trip no longer exists.", fontFamily = DmSansFontFamily, color = OnBackgroundGray)
            }
            is GroupDetailState.Ready -> {
                val group = s.detail.group
                val members = s.detail.members
                val today = LocalDate.now()
                val status = GroupStatus.of(group, today)
                val canToggle = status == GroupStatus.DRAFT || status == GroupStatus.OPEN || status == GroupStatus.FULL

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // -- Header ------------------------------------------------
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(group.title, fontFamily = PlayfairFontFamily, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground)
                            Text(group.destination, fontFamily = DmSansFontFamily, fontSize = 14.sp, color = OnBackgroundGray)
                        }
                        Spacer(Modifier.width(8.dp))
                        GroupStatusChip(status)
                    }
                    if (group.isCancelled) {
                        Text(
                            "You cancelled this trip. Travellers who joined can still see it, but nobody can join.",
                            fontFamily = DmSansFontFamily, fontSize = 13.sp, color = Color(0xFFF87171),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF87171).copy(alpha = 0.12f)).padding(12.dp)
                        )
                    }

                    // -- Facts -------------------------------------------------
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Fact("Dates", GroupFormat.dateRange(group.startDate, group.endDate) + " \u00B7 " + GroupFormat.duration(group))
                        Fact("Price", GroupFormat.price(group.pricePerPerson))
                        if (group.meetingPoint.isNotBlank()) Fact("Meeting point", group.meetingPoint)
                        s.detail.packageTitle?.let { Fact("Follows package", it) }
                        if (group.description.isNotBlank()) Fact("About", group.description)
                    }

                    // -- Seats -------------------------------------------------
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(16.dp)
                    ) {
                        Text(GroupFormat.seatsTaken(group), fontFamily = DmSansFontFamily, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (group.totalSeats == 0) 0f else group.seatsTaken.toFloat() / group.totalSeats },
                            color = Amber,
                            trackColor = Amber.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (group.isFull) "Every seat is taken." else "${group.seatsLeft} seat${if (group.seatsLeft == 1) "" else "s"} left",
                            fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray
                        )
                    }

                    // -- Visibility --------------------------------------------
                    if (!group.isCancelled) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface).padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Visible to travellers", fontFamily = DmSansFontFamily, fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (group.isPublished) "Solo travellers can find and join this trip" else "Only you can see this draft",
                                    fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                            }
                            Switch(
                                checked = group.isPublished,
                                onCheckedChange = { viewModel.togglePublished(group) },
                                enabled = canToggle,
                                colors = SwitchDefaults.colors(checkedTrackColor = Amber)
                            )
                        }
                    }

                    // -- Members -----------------------------------------------
                    Text("Travellers who joined", fontFamily = PlayfairFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                    if (members.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Groups, contentDescription = null, tint = OnBackgroundGray, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Nobody has joined yet.", fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray)
                        }
                    } else {
                        members.forEachIndexed { index, member -> MemberRow(index + 1, member) }
                    }

                    // -- Danger zone -------------------------------------------
                    Spacer(Modifier.height(4.dp))
                    if (!group.isCancelled && status != GroupStatus.COMPLETED) {
                        OutlinedButton(
                            onClick = { confirm = GroupConfirm.CANCEL },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Cancel this group trip", color = MaterialTheme.colorScheme.error) }
                    }
                    if (members.isEmpty()) {
                        TextButton(onClick = { confirm = GroupConfirm.DELETE }, modifier = Modifier.fillMaxWidth()) {
                            Text("Delete group trip", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                when (confirm) {
                    GroupConfirm.CANCEL -> AlertDialog(
                        onDismissRequest = { confirm = null },
                        title = { Text("Cancel \u201C${group.title}\u201D?") },
                        text = {
                            Text(
                                if (members.isEmpty()) "Nobody can join it any more."
                                else "${members.size} traveller${if (members.size == 1) "" else "s"} joined. They will see that you cancelled the trip, and nobody else can join. You can't undo this."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.cancel(group); confirm = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("Cancel trip") }
                        },
                        dismissButton = { TextButton(onClick = { confirm = null }) { Text("Keep it") } }
                    )
                    GroupConfirm.DELETE -> AlertDialog(
                        onDismissRequest = { confirm = null },
                        title = { Text("Delete \u201C${group.title}\u201D?") },
                        text = { Text("Nobody has joined, so it will simply be removed.") },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.delete(group); confirm = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("Delete") }
                        },
                        dismissButton = { TextButton(onClick = { confirm = null }) { Text("Keep") } }
                    )
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(label.uppercase(Locale.US), fontFamily = DmSansFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp, color = OnBackgroundGray)
        Text(value, fontFamily = DmSansFontFamily, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private val joinedFormat = DateTimeFormatter.ofPattern("d MMM", Locale.US)

@Composable
private fun MemberRow(number: Int, member: GroupMember) {
    val joined = Instant.ofEpochMilli(member.joinedAt).atZone(ZoneId.systemDefault()).toLocalDate().format(joinedFormat)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(Amber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text("$number", fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(member.travellerName.ifBlank { "Traveller" }, fontFamily = DmSansFontFamily, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(member.travellerEmail, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
        }
        Text("Joined $joined", fontFamily = DmSansFontFamily, fontSize = 11.sp, color = OnBackgroundGray)
    }
}
