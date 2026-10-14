package com.example.smarttravelplanner.ui.screens.groups

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.smarttravelplanner.domain.util.GroupFormat
import com.example.smarttravelplanner.domain.util.GroupRules
import com.example.smarttravelplanner.domain.util.JoinCheck
import com.example.smarttravelplanner.ui.screens.agentshare.RatingLabel
import com.example.smarttravelplanner.ui.screens.agentshare.displayName
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.time.LocalDate
import java.util.Locale

private enum class GroupAsk { JOIN, LEAVE }

/** A group trip for travellers, with Join / Leave. */
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var ask by remember { mutableStateOf<GroupAsk?>(null) }

    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.noticeShown()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Group trip", fontFamily = DmSansFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
        }

        when (val s = state) {
            TravellerGroupState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Amber)
            }
            TravellerGroupState.Missing -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("This group trip isn't available.", fontFamily = DmSansFontFamily, color = OnBackgroundGray)
            }
            is TravellerGroupState.Ready -> {
                val listing = s.listing
                val group = listing.group
                val today = LocalDate.now()
                val check = GroupRules.canJoin(group, listing.joinedByMe, today)
                val canLeave = listing.joinedByMe && GroupRules.canLeave(group, today)

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(group.title, fontFamily = PlayfairFontFamily, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text(group.destination, fontFamily = DmSansFontFamily, fontSize = 14.sp, color = OnBackgroundGray)

                    if (group.isCancelled) {
                        Text(
                            "The agent cancelled this trip.",
                            fontFamily = DmSansFontFamily, fontSize = 13.sp, color = Color(0xFFF87171),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF87171).copy(alpha = 0.12f)).padding(12.dp)
                        )
                    }

                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Fact("Dates", GroupFormat.dateRange(group.startDate, group.endDate) + " \u00B7 " + GroupFormat.duration(group))
                        Fact("Price", GroupFormat.price(group.pricePerPerson))
                        if (group.meetingPoint.isNotBlank()) Fact("Meeting point", group.meetingPoint)
                        if (group.description.isNotBlank()) Fact("About this group", group.description)
                    }

                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(16.dp)
                    ) {
                        Text("Run by", fontFamily = DmSansFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, color = OnBackgroundGray)
                        Text(listing.agent?.displayName ?: "Travel agent", fontFamily = DmSansFontFamily, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        listing.agent?.city?.takeIf { it.isNotBlank() }?.let {
                            Text(it, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                        }
                        RatingLabel(listing.rating)
                    }

                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(16.dp)
                    ) {
                        Text(GroupRules.seatsLabel(group), fontFamily = DmSansFontFamily, fontSize = 14.sp,
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
                            "${group.seatsTaken} of ${group.totalSeats} travellers have joined",
                            fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // -- Join / leave bar ---------------------------------------------
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when {
                        listing.joinedByMe -> {
                            Text("\u2713 You're in this trip", fontFamily = DmSansFontFamily, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                            if (canLeave) {
                                OutlinedButton(
                                    onClick = { ask = GroupAsk.LEAVE },
                                    enabled = !busy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) { Text("Leave this group", color = MaterialTheme.colorScheme.error) }
                            } else {
                                Text("It's too late to leave: the trip starts tomorrow or has started.",
                                    fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                            }
                        }
                        else -> {
                            Button(
                                onClick = { ask = GroupAsk.JOIN },
                                enabled = check == JoinCheck.OK && !busy,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFF1A1A1A), strokeWidth = 2.dp)
                                else Text(
                                    if (check == JoinCheck.OK) "Join this trip" else check.message,
                                    fontWeight = FontWeight.Bold, color = if (check == JoinCheck.OK) Color(0xFF1A1A1A) else OnBackgroundGray
                                )
                            }
                            if (check == JoinCheck.OK) {
                                Text("You can leave any time until the day before the trip starts.",
                                    fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                            }
                        }
                    }
                }

                when (ask) {
                    GroupAsk.JOIN -> AlertDialog(
                        onDismissRequest = { ask = null },
                        title = { Text("Join \u201C${group.title}\u201D?") },
                        text = { Text("${GroupFormat.dateRange(group.startDate, group.endDate)} \u00B7 ${GroupFormat.price(group.pricePerPerson)}. The agent will see your name on the member list.") },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.join(); ask = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Amber)
                            ) { Text("Join", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = { TextButton(onClick = { ask = null }) { Text("Not now") } }
                    )
                    GroupAsk.LEAVE -> AlertDialog(
                        onDismissRequest = { ask = null },
                        title = { Text("Leave this group?") },
                        text = { Text("Your seat goes back to other travellers. You can join again later if a seat is still free.") },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.leave(); ask = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("Leave") }
                        },
                        dismissButton = { TextButton(onClick = { ask = null }) { Text("Stay") } }
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
