package com.example.smarttravelplanner.ui.screens.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.agent.Booking
import com.example.smarttravelplanner.domain.model.agent.BookingActor
import com.example.smarttravelplanner.domain.model.agent.QuoteInput
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.util.BookingRules
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = Color(0xFF4ADE80)
private val Red = Color(0xFFF87171)

/** What the agent can do from the booking panel. */
internal data class AgentBookingActions(
    val onSendQuote: (QuoteInput) -> Unit,
    val onMarkBooked: (reference: String, notes: String) -> Unit,
    val onReconfirm: () -> Unit,
    val onCancel: (reason: String) -> Unit
)

/**
 * The agent's side of the booking once the traveller chose them:
 * Chosen → send quote → (traveller accepts) → mark booked → re-confirm if the trip changes.
 */
@Composable
internal fun AgentBookingPanel(state: AgentTripUiState.Success, actions: AgentBookingActions) {
    val share = state.incoming.share
    val booking = state.booking
    val context = LocalContext.current
    var showQuote by remember { mutableStateOf(false) }
    var showBooked by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }

    val (title, color) = when (share.status) {
        ShareStatus.CONFIRMED -> "\u2713 THE TRAVELLER CHOSE YOU \u2014 SEND A QUOTE" to Green
        ShareStatus.QUOTE_SENT -> "QUOTE SENT \u2014 WAITING FOR THE TRAVELLER" to Amber
        ShareStatus.QUOTE_ACCEPTED -> "\u2713 QUOTE ACCEPTED \u2014 READY TO BOOK" to Green
        ShareStatus.BOOKED -> (if (state.needsReconfirm) "BOOKED \u2014 TRIP CHANGED, PLEASE RE-CONFIRM" else "\u2713 BOOKED") to
            (if (state.needsReconfirm) Amber else Green)
        ShareStatus.COMPLETED -> "TRIP COMPLETED" to Color(0xFF60A5FA)
        ShareStatus.CANCELLED -> "CANCELLED" to Red
        else -> return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, fontFamily = DmSansFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp, color = color)

        // ── What's happening ────────────────────────────────────────────────
        val body = when (share.status) {
            ShareStatus.CONFIRMED ->
                "${state.incoming.travellerName} compared offers and went ahead with you. " +
                    "Send your final price, what's included and how to pay."
            ShareStatus.QUOTE_SENT -> "The traveller can accept or decline. You can still update the quote."
            ShareStatus.QUOTE_ACCEPTED ->
                "Book the hotels, transport and tickets, then mark it as booked with a reference number."
            ShareStatus.BOOKED -> if (state.needsReconfirm) {
                "The traveller changed the trip after you booked it. Check the plan below, adjust your bookings, then re-confirm."
            } else "Everything is booked. The trip will be marked completed after it ends."
            ShareStatus.COMPLETED -> "The trip has ended. The traveller can now rate you."
            ShareStatus.CANCELLED -> buildString {
                append(if (booking?.cancelledBy == BookingActor.AGENT) "You cancelled this booking" else "The traveller cancelled")
                if (!booking?.cancelReason.isNullOrBlank()) append(": \u201C${booking?.cancelReason}\u201D")
                append(".")
            }
            else -> ""
        }
        Text(body, fontFamily = DmSansFontFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

        // ── Quote / booking summary ─────────────────────────────────────────
        if (booking?.quoteTotal != null && share.status != ShareStatus.CANCELLED) QuoteSummary(booking)
        if (booking != null && booking.bookingReference.isNotBlank()) {
            Text("Booking ref: ${booking.bookingReference}", fontFamily = DmSansFontFamily, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (booking.bookingNotes.isNotBlank()) {
                Text(booking.bookingNotes, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
            }
        }

        // ── Actions ─────────────────────────────────────────────────────────
        val busy = state.isBookingBusy
        when (share.status) {
            ShareStatus.CONFIRMED -> PrimaryButton("Send quote", color, busy) { showQuote = true }
            ShareStatus.QUOTE_SENT -> OutlinedButton(onClick = { showQuote = true }, enabled = !busy) { Text("Update quote") }
            ShareStatus.QUOTE_ACCEPTED -> PrimaryButton("Mark as booked", color, busy) { showBooked = true }
            ShareStatus.BOOKED -> if (state.needsReconfirm) PrimaryButton("Re-confirm booking", color, busy) { actions.onReconfirm() }
            else -> Unit
        }
        if (share.status.isChosen) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { contactTraveller(context, state) }) { Text("Contact traveller", fontSize = 13.sp) }
                if (BookingRules.canCancel(share.status)) {
                    TextButton(onClick = { showCancel = true }, enabled = !busy) {
                        Text("Cancel", fontSize = 13.sp, color = Red)
                    }
                }
            }
        }
    }

    if (showQuote) {
        QuoteSheet(
            existing = booking,
            onSend = {
                actions.onSendQuote(it)
                showQuote = false
            },
            onDismiss = { showQuote = false }
        )
    }
    if (showBooked) {
        MarkBookedSheet(
            onSave = { ref, notes ->
                actions.onMarkBooked(ref, notes)
                showBooked = false
            },
            onDismiss = { showBooked = false }
        )
    }
    if (showCancel) {
        CancelDialog(
            onConfirm = {
                actions.onCancel(it)
                showCancel = false
            },
            onDismiss = { showCancel = false }
        )
    }
}

@Composable
private fun QuoteSummary(booking: Booking) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Quote: " + rupees(booking.quoteTotal ?: 0.0), fontFamily = DmSansFontFamily, fontSize = 15.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (booking.quoteIncludes.isNotBlank()) SummaryLine("Includes", booking.quoteIncludes)
        if (booking.quoteExcludes.isNotBlank()) SummaryLine("Not included", booking.quoteExcludes)
        if (booking.paymentNote.isNotBlank()) SummaryLine("Payment", booking.paymentNote)
        booking.quoteValidUntil?.let { SummaryLine("Valid until", it.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))) }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Text("$label: $value", fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
}

@Composable
private fun PrimaryButton(text: String, color: Color, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF0B111B), strokeWidth = 2.dp)
        else Text(text, fontWeight = FontWeight.Bold, color = Color(0xFF0B111B))
    }
}

// ── Sheets & dialogs ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteSheet(existing: Booking?, onSend: (QuoteInput) -> Unit, onDismiss: () -> Unit) {
    var totalText by remember { mutableStateOf(existing?.quoteTotal?.toLong()?.toString().orEmpty()) }
    var includes by remember { mutableStateOf(existing?.quoteIncludes.orEmpty()) }
    var excludes by remember { mutableStateOf(existing?.quoteExcludes.orEmpty()) }
    var payment by remember { mutableStateOf(existing?.paymentNote.orEmpty()) }
    // Validity as "days from today" chips; null = no expiry
    var validDays by remember { mutableStateOf<Int?>(7) }
    var problems by remember { mutableStateOf(emptyList<String>()) }
    val colors = fieldColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (existing?.quoteTotal == null) "Send a quote" else "Update quote", fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
                value = totalText, onValueChange = { totalText = it.filter(Char::isDigit).take(9); problems = emptyList() },
                label = { Text("Total price for the trip (₹) *") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = colors, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = includes, onValueChange = { includes = it.take(400) },
                label = { Text("What's included") }, placeholder = { Text("e.g. 3 nights hotel, breakfast, cab, entry tickets") },
                minLines = 2, colors = colors, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = excludes, onValueChange = { excludes = it.take(400) },
                label = { Text("Not included") }, placeholder = { Text("e.g. flights, lunch & dinner") },
                minLines = 2, colors = colors, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = payment, onValueChange = { payment = it.take(300) },
                label = { Text("How to pay") }, placeholder = { Text("e.g. 30% advance via UPI to agency@upi, rest on arrival") },
                minLines = 2, colors = colors, modifier = Modifier.fillMaxWidth()
            )
            Text("Quote valid for", fontSize = 12.sp, color = OnBackgroundGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 7, 14, null).forEach { days ->
                    val selected = validDays == days
                    Text(
                        text = days?.let { "$it days" } ?: "No limit",
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Amber else MaterialTheme.colorScheme.background)
                            .clickable { validDays = days }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            problems.forEach { Text("\u2022 $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val total = totalText.toDoubleOrNull()
                    val validUntil = validDays?.let { LocalDate.now().plusDays(it.toLong()) }
                    val found = BookingRules.validateQuote(total, validUntil, LocalDate.now())
                    if (found.isNotEmpty()) {
                        problems = found
                        return@Button
                    }
                    onSend(QuoteInput(total!!, includes, excludes, payment, validUntil))
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Send to traveller", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkBookedSheet(onSave: (reference: String, notes: String) -> Unit, onDismiss: () -> Unit) {
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val colors = fieldColors()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mark as booked", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
                value = reference, onValueChange = { reference = it.take(40) },
                label = { Text("Booking reference *") }, placeholder = { Text("e.g. WJ-2931") },
                singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it.take(500) },
                label = { Text("Notes for the traveller") },
                placeholder = { Text("e.g. Hotel: Alsisar Haveli. Pickup 9 AM from Jaipur airport.") },
                minLines = 3, colors = colors, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(reference, notes) },
                enabled = reference.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Confirm booking", fontWeight = FontWeight.Bold, color = Color(0xFF0B111B)) }
        }
    }
}

@Composable
internal fun CancelDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel this booking?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The other side will see your reason. The traveller can then choose another agent.")
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it.take(300) },
                    label = { Text("Reason *") }, minLines = 2, colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) { Text("Cancel booking") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep it") } }
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Amber,
    focusedLabelColor = Amber,
    cursorColor = Amber
)

private fun rupees(amount: Double): String = "\u20B9" + String.format(Locale.US, "%,d", amount.toLong())

/** Opens the phone's email app addressed to the traveller (free, no in-app messaging). */
internal fun contactTraveller(context: Context, state: AgentTripUiState.Success) {
    val trip = state.trip
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(state.incoming.share.travellerEmail))
        putExtra(Intent.EXTRA_SUBJECT, "Your trip to ${trip.destination}")
        putExtra(
            Intent.EXTRA_TEXT,
            "Hi ${state.incoming.travellerName},\n\nThanks for choosing us for your trip to " +
                "${trip.destination} (${formatTripDates(trip.startDate.toString(), trip.endDate.toString())}).\n"
        )
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Contact traveller")) }
        .onFailure { Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show() }
}
