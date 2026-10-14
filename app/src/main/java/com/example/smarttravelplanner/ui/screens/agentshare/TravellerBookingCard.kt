package com.example.smarttravelplanner.ui.screens.agentshare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.agent.BookingActor
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.util.BookingRules
import com.example.smarttravelplanner.ui.screens.agent.CancelDialog
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Red = Color(0xFFF87171)
private val Blue = Color(0xFF60A5FA)

/**
 * The traveller's view of the booking with the agent they chose:
 * waiting for quote → review quote (accept/decline) → booking in progress → booked → completed.
 */
@Composable
internal fun TravellerBookingCard(
    state: TravellerBookingUiState,
    onAccept: (shareId: Long) -> Unit,
    onDecline: (shareId: Long, reason: String) -> Unit,
    onCancel: (shareId: Long, reason: String) -> Unit,
    onRate: (shareId: Long, stars: Int, comment: String) -> Unit = { _, _, _ -> }
) {
    val share = state.share
    val booking = state.booking
    val agent = state.row.agent.displayName
    var showDecline by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }
    var editingRating by remember { mutableStateOf(false) }
    val expired = BookingRules.isQuoteExpired(booking?.quoteValidUntil, LocalDate.now())

    val (title, body, color) = when (share.status) {
        ShareStatus.CONFIRMED -> Triple(
            "\u2713 GOING AHEAD WITH ${agent.uppercase()}",
            "Waiting for $agent to send their final quote (price, what's included, how to pay).",
            ApprovedGreen
        )
        ShareStatus.QUOTE_SENT -> Triple(
            if (expired) "QUOTE EXPIRED" else "QUOTE FROM ${agent.uppercase()} \u2014 PLEASE REVIEW",
            if (expired) "This quote is no longer valid. Ask $agent to send an updated one."
            else "Accept to let $agent book everything, or decline to choose another agent.",
            if (expired) Red else Amber
        )
        ShareStatus.QUOTE_ACCEPTED -> Triple(
            "\u2713 QUOTE ACCEPTED",
            "$agent is booking your trip. You'll see the booking reference here once it's done.",
            ApprovedGreen
        )
        ShareStatus.BOOKED -> if (state.needsReconfirm) Triple(
            "BOOKED \u2014 WAITING FOR RE-CONFIRMATION",
            "You changed the trip after it was booked. $agent needs to check and re-confirm the bookings.",
            Amber
        ) else Triple("\u2713 BOOKED WITH ${agent.uppercase()}", "Your trip is booked. Have a great trip!", ApprovedGreen)
        ShareStatus.COMPLETED -> Triple(
            "TRIP COMPLETED",
            if (state.review == null) "Hope you enjoyed it! Rate $agent to help other travellers choose."
            else "Thanks for rating $agent.",
            Blue
        )
        ShareStatus.CANCELLED -> Triple(
            "BOOKING CANCELLED",
            buildString {
                append(if (booking?.cancelledBy == BookingActor.AGENT) "$agent cancelled" else "You cancelled")
                if (!booking?.cancelReason.isNullOrBlank()) append(": \u201C${booking?.cancelReason}\u201D")
                append(". You can compare the other offers and choose again.")
            },
            Red
        )
        else -> return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, fontFamily = DmSansFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp, color = color)
        Text(body, fontFamily = DmSansFontFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

        // Quote details
        if (booking?.quoteTotal != null && share.status != ShareStatus.CANCELLED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("Total: \u20B9" + String.format(Locale.US, "%,d", booking.quoteTotal.toLong()),
                    fontFamily = DmSansFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                if (booking.quoteIncludes.isNotBlank()) Detail("Includes", booking.quoteIncludes)
                if (booking.quoteExcludes.isNotBlank()) Detail("Not included", booking.quoteExcludes)
                if (booking.paymentNote.isNotBlank()) Detail("How to pay", booking.paymentNote)
                booking.quoteValidUntil?.let {
                    Detail("Valid until", it.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)))
                }
            }
        }

        // Booking reference
        if (booking != null && booking.bookingReference.isNotBlank() &&
            (share.status == ShareStatus.BOOKED || share.status == ShareStatus.COMPLETED)
        ) {
            Text("Booking ref: ${booking.bookingReference}", fontFamily = DmSansFontFamily, fontSize = 15.sp,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (booking.bookingNotes.isNotBlank()) {
                Text(booking.bookingNotes, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
            }
        }

        // Actions
        if (share.status == ShareStatus.QUOTE_SENT && !expired) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showDecline = true }, enabled = !state.isBusy, modifier = Modifier.weight(1f)) {
                    Text("Decline")
                }
                Button(
                    onClick = { onAccept(share.id) },
                    enabled = !state.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = ApprovedGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isBusy) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF0B111B), strokeWidth = 2.dp)
                    else Text("Accept quote", fontWeight = FontWeight.Bold, color = Color(0xFF0B111B))
                }
            }
        }
        // Rating after the trip
        if (share.status == ShareStatus.COMPLETED) {
            val review = state.review
            if (review == null || editingRating) {
                RatingInput(
                    agentName = agent,
                    initialStars = review?.stars ?: 0,
                    initialComment = review?.comment.orEmpty(),
                    isBusy = state.isBusy,
                    onSubmit = { stars, comment ->
                        onRate(share.id, stars, comment)
                        editingRating = false
                    }
                )
            } else {
                Stars(review.stars, size = 20.dp)
                if (review.comment.isNotBlank()) {
                    Text("\u201C${review.comment}\u201D", fontFamily = DmSansFontFamily, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                if (review.hasReply) {
                    // The agent's public answer to this rating
                    Column(
                        modifier = Modifier
                            .padding(top = 6.dp, start = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text("Reply from $agent", fontFamily = DmSansFontFamily, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = OnBackgroundGray)
                        Text(review.reply, fontFamily = DmSansFontFamily, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                TextButton(onClick = { editingRating = true }) { Text("Edit rating", fontSize = 12.sp) }
            }
        }

        if (BookingRules.canCancel(share.status)) {
            TextButton(onClick = { showCancel = true }, enabled = !state.isBusy) {
                Text("Cancel booking", fontSize = 12.sp, color = Red)
            }
        }
    }

    if (showDecline) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDecline = false },
            title = { Text("Decline this quote?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$agent will see your reason. The other agents' offers will open again.")
                    OutlinedTextField(
                        value = reason, onValueChange = { reason = it.take(300) },
                        label = { Text("Reason (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDecline(share.id, reason)
                        showDecline = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text("Decline quote") }
            },
            dismissButton = { TextButton(onClick = { showDecline = false }) { Text("Back") } }
        )
    }
    if (showCancel) {
        CancelDialog(
            onConfirm = {
                onCancel(share.id, it)
                showCancel = false
            },
            onDismiss = { showCancel = false }
        )
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Text("$label: $value", fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
}
