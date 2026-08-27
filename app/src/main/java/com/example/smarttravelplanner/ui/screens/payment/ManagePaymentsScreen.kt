package com.example.smarttravelplanner.ui.screens.payment

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

@Composable
fun ManagePaymentsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showLinkUpiDialog by remember { mutableStateOf(false) }
    var activeLinkPartner by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090D14),
                        Color(0xFF0D121B),
                        Color(0xFF080B10)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ── TOP BAR ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Back button on start
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF131923))
                        .border(1.dp, Color(0xFF222C3B), CircleShape)
                        .clickable(onClick = onBackClick)
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Centered Title
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.W900,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        ) {
                            append("Manage ")
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                fontSize = 22.sp,
                                color = Amber
                            )
                        ) {
                            append("Payment ")
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        ) {
                            append("Options")
                        }
                    },
                    textAlign = TextAlign.Center
                )
            }

            // ── 1. ADD CARD SECTION ───────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showAddCardDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111722)),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF1D2635))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Add Card",
                        fontSize = 12.sp,
                        fontFamily = DmSansFontFamily,
                        color = Color(0xFF7E8E9F),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFF182230)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Card",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Add Card",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DmSansFontFamily,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Visa, Mastercard, RuPay & more",
                                fontSize = 12.sp,
                                fontFamily = DmSansFontFamily,
                                color = Color(0xFF7E8E9F)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 2. 1-CLICK UPI SECTION ────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showLinkUpiDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111722)),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF1D2635))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "1-Click UPI",
                        fontSize = 12.sp,
                        fontFamily = DmSansFontFamily,
                        color = Color(0xFF7E8E9F),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFF182230)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "1-Click UPI",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "1-Click UPI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DmSansFontFamily,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Link bank account",
                                fontSize = 12.sp,
                                fontFamily = DmSansFontFamily,
                                color = Color(0xFF7E8E9F)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 3. MORE OPTIONS CARD ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111722)),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF1D2635))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "MORE OPTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        color = Color(0xFF7E8E9F),
                        letterSpacing = 1.1.sp,
                        modifier = Modifier.padding(bottom = 14.dp, top = 2.dp)
                    )

                    // Option 1: Bajaj EMI Card with actual Bajaj Finserv logo
                    PartnerOptionRow(
                        iconContent = {
                            BajajFinservLogo()
                        },
                        title = "Bajaj EMI Card",
                        subtitle = "Instant No-Cost EMI approval",
                        subtext = null,
                        onLinkClick = { activeLinkPartner = "Bajaj EMI Card" }
                    )

                    HorizontalDivider(
                        color = Color(0xFF1C2534),
                        thickness = 0.7.dp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Option 2: Amazon Pay Balance with actual Amazon Pay logo
                    PartnerOptionRow(
                        iconContent = {
                            AmazonPayLogo()
                        },
                        title = "Amazon Pay Balance",
                        subtitle = "Fast 1-click checkout & cashback",
                        subtext = null,
                        onLinkClick = { activeLinkPartner = "Amazon Pay Balance" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── DIALOGS ──────────────────────────────────────────────────────────
        if (showAddCardDialog) {
            AddCardDialog(
                onDismiss = { showAddCardDialog = false },
                onAdd = { cardNumber, holder, expiry ->
                    showAddCardDialog = false
                    Toast.makeText(context, "Card ending in ${cardNumber.takeLast(4)} added!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showLinkUpiDialog) {
            LinkUpiDialog(
                onDismiss = { showLinkUpiDialog = false },
                onLink = { upiId ->
                    showLinkUpiDialog = false
                    Toast.makeText(context, "UPI ID $upiId linked successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        activeLinkPartner?.let { partner ->
            LinkPartnerDialog(
                partnerName = partner,
                onDismiss = { activeLinkPartner = null },
                onConfirm = {
                    val pName = activeLinkPartner
                    activeLinkPartner = null
                    Toast.makeText(context, "$pName linked successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun PartnerOptionRow(
    iconContent: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    subtext: String?,
    onLinkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            iconContent()

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DmSansFontFamily,
                    color = Color.White
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontFamily = DmSansFontFamily,
                        color = Color(0xFF8A99AC)
                    )
                }

                if (!subtext.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtext,
                        fontSize = 10.sp,
                        fontFamily = DmSansFontFamily,
                        color = Color(0xFF5B6B7C)
                    )
                }
            }
        }

        TextButton(
            onClick = onLinkClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Link",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily,
                color = Color(0xFFE89020)
            )
        }
    }
}

// ── LOGOS ───────────────────────────────────────────────────────────────────

@Composable
private fun BajajFinservLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_bajaj_finserv),
        contentDescription = "Bajaj Finserv Logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun AmazonPayLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF232F3E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = "pay",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DmSansFontFamily,
                color = Color.White,
                letterSpacing = (-0.4).sp
            )

            // Amazon signature smile arrow
            Canvas(modifier = Modifier.size(width = 28.dp, height = 9.dp)) {
                val w = size.width
                val h = size.height
                val smilePath = Path().apply {
                    moveTo(w * 0.08f, h * 0.20f)
                    cubicTo(w * 0.30f, h * 0.95f, w * 0.72f, h * 0.95f, w * 0.92f, h * 0.30f)
                }
                drawPath(
                    path = smilePath,
                    color = Color(0xFFFF9900),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Arrow tip pointing up-right
                val arrowTip = Path().apply {
                    moveTo(w * 0.92f, h * 0.30f)
                    lineTo(w * 0.72f, h * 0.35f)
                    lineTo(w * 0.88f, h * 0.05f)
                    close()
                }
                drawPath(arrowTip, color = Color(0xFFFF9900))
            }
        }
    }
}

// ── DIALOG IMPLEMENTATIONS ──────────────────────────────────────────────────

@Composable
private fun AddCardDialog(
    onDismiss: () -> Unit,
    onAdd: (cardNumber: String, holder: String, expiry: String) -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141A24),
        title = {
            Text(
                text = "Add New Card",
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily,
                color = Color.White,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16) cardNumber = it },
                    label = { Text("Card Number (16 Digits)", color = Color(0xFF8A99AC)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Amber,
                        unfocusedBorderColor = Color(0xFF2C384A)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = { Text("Cardholder Name", color = Color(0xFF8A99AC)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Amber,
                        unfocusedBorderColor = Color(0xFF2C384A)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { if (it.length <= 5) expiry = it },
                        label = { Text("MM/YY", color = Color(0xFF8A99AC)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Amber,
                            unfocusedBorderColor = Color(0xFF2C384A)
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { if (it.length <= 4) cvv = it },
                        label = { Text("CVV", color = Color(0xFF8A99AC)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Amber,
                            unfocusedBorderColor = Color(0xFF2C384A)
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(cardNumber, cardHolder, expiry) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC85A0D))
            ) {
                Text("Save Card", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8A99AC))
            }
        }
    )
}

@Composable
private fun LinkUpiDialog(
    onDismiss: () -> Unit,
    onLink: (String) -> Unit
) {
    var upiId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141A24),
        title = {
            Text(
                text = "Link 1-Click UPI",
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily,
                color = Color.White,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enter your Virtual Payment Address (VPA) / UPI ID:",
                    fontSize = 12.sp,
                    color = Color(0xFF8A99AC)
                )

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    placeholder = { Text("e.g. mobile@okaxis / user@upi", color = Color(0xFF556578)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Amber,
                        unfocusedBorderColor = Color(0xFF2C384A)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onLink(upiId) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC85A0D))
            ) {
                Text("Link UPI", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8A99AC))
            }
        }
    )
}

@Composable
private fun LinkPartnerDialog(
    partnerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var phoneOrAccount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141A24),
        title = {
            Text(
                text = "Link $partnerName",
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily,
                color = Color.White,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enter your registered mobile number or account ID linked with $partnerName:",
                    fontSize = 12.sp,
                    color = Color(0xFF8A99AC)
                )

                OutlinedTextField(
                    value = phoneOrAccount,
                    onValueChange = { phoneOrAccount = it },
                    placeholder = { Text("Registered Mobile / ID", color = Color(0xFF556578)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Amber,
                        unfocusedBorderColor = Color(0xFF2C384A)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC85A0D))
            ) {
                Text("Verify & Link", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8A99AC))
            }
        }
    )
}
