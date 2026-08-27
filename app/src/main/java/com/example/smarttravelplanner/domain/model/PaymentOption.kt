package com.example.smarttravelplanner.domain.model

data class SavedCard(
    val id: String,
    val cardLast4: String,
    val cardNetwork: String, // "Visa", "Mastercard", "RuPay"
    val cardHolderName: String,
    val expiry: String
)

data class SavedUpi(
    val id: String,
    val upiId: String,
    val bankName: String,
    val isPrimary: Boolean = false
)

data class PartnerCreditOption(
    val id: String,
    val name: String,
    val subtitle: String,
    val subtext: String? = null,
    val iconType: String, // "smart_credit", "bajaj", "amazon_pay", "lazypay"
    val isLinked: Boolean = false
)
