package com.example.smarttravelplanner.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.smarttravelplanner.data.remote.service.GoogleAuthHelper
import kotlinx.coroutines.launch
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.ui.theme.*

private enum class AuthTab {
    SIGN_IN,
    CREATE_ACCOUNT
}

// High-resolution internet image of Taj Mahal at dawn
private const val TAJ_MAHAL_ONLINE_IMAGE =
    "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1400&q=80"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: (isNewAccount: Boolean) -> Unit,
    /** Called instead of [onLoginSuccess] when a travel-agent account signs in. */
    onAgentLoginSuccess: () -> Unit = { onLoginSuccess(false) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val resendCountdown by viewModel.resendCountdown.collectAsState()
    val focusManager = LocalFocusManager.current

    // Synchronize with active theme mode (Dark / Light / System)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var selectedTab by rememberSaveable { mutableStateOf(AuthTab.SIGN_IN) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var regOtpCode by rememberSaveable { mutableStateOf("") }

    // Agent account states (opened from "Agent account" next to Forgot password)
    var agentEmail by rememberSaveable { mutableStateOf("") }
    var agentPassword by rememberSaveable { mutableStateOf("") }
    var agentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var agentFullName by rememberSaveable { mutableStateOf("") }
    var agentAgencyName by rememberSaveable { mutableStateOf("") }
    var agentCity by rememberSaveable { mutableStateOf("") }
    var agentOtpCode by rememberSaveable { mutableStateOf("") }

    // Forgot Password states
    var resetEmail by rememberSaveable { mutableStateOf("") }
    var resetOtpCode by rememberSaveable { mutableStateOf("") }
    var resetNewPassword by rememberSaveable { mutableStateOf("") }
    var resetConfirmPassword by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showGoogleSetupDialog by remember { mutableStateOf(false) }

    // Pre-populate email in forgot password if user already typed in login
    LaunchedEffect(uiState.viewMode) {
        when (val mode = uiState.viewMode) {
            is AuthViewMode.ForgotPasswordEmail -> {
                if (mode.initialEmail.isNotBlank()) resetEmail = mode.initialEmail
                resetOtpCode = ""
                resetNewPassword = ""
                resetConfirmPassword = ""
            }
            is AuthViewMode.ForgotPasswordOtp -> {
                if (mode.email.isNotBlank()) resetEmail = mode.email
            }
            else -> {}
        }
    }

    // Scroll state for scrollable container
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            if (uiState.sessionRole == UserRole.AGENT) {
                onAgentLoginSuccess()
            } else {
                onLoginSuccess(uiState.isNewAccount)
            }
        }
    }

    val isSubmitEnabled = when (selectedTab) {
        AuthTab.SIGN_IN -> email.isNotBlank() && password.isNotBlank() && !uiState.isLoading
        AuthTab.CREATE_ACCOUNT -> fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !uiState.isLoading
    }

    // Theme adaptive color tokens matching app theme (Color.kt)
    val screenBackground = if (isDark) BackgroundDark else BackgroundLight
    val cardBackground = if (isDark) SurfaceDark.copy(alpha = 0.95f) else SurfaceLight.copy(alpha = 0.97f)
    val cardBorder = if (isDark) CardBorderDark else CardBorderLight

    val segmentedTrackColor = if (isDark) SurfaceElevated else SegmentedTrackLight
    val segmentedPillColor = if (isDark) SegmentedPillDark else Color.White

    val textPrimary = if (isDark) OnBackgroundWhite else OnBackgroundDark
    val textMuted = if (isDark) OnBackgroundGray else OnBackgroundMedium
    val labelColor = if (isDark) Color(0xFF8E8E8E) else OnBackgroundMedium

    val fieldBackground = if (isDark) SurfaceElevated else FieldBackgroundLight
    val fieldBorder = if (isDark) FieldBorderDark else FieldBorderLight
    val fieldPlaceholder = if (isDark) OnBackgroundMuted else OnBackgroundSubtle
    val fieldIcon = if (isDark) OnBackgroundGray else OnBackgroundMedium

    val submitBtnBg = if (isDark) CardBorderDark else OnBackgroundDark
    val submitBtnDisabledBg = if (isDark) Color(0xFF1C1C1C).copy(alpha = 0.6f) else SegmentedTrackLight
    val submitBtnTextColor = if (isSubmitEnabled) Color.White else (if (isDark) OnBackgroundMuted else OnBackgroundSubtle)

    val googleBtnBg = if (isDark) Color(0xFF222222) else Color.White
    val googleBtnBorder = if (isDark) FieldBorderDark else FieldBorderLight
    val googleBtnTextColor = if (isDark) Color.White else OnBackgroundDark

    val dividerColor = if (isDark) DividerDark else DividerLight
    val dividerTextColor = if (isDark) OnBackgroundMuted else OnBackgroundSubtle
    val forgotPasswordColor = Amber

    val errorMessage = uiState.errorMessage

    val agentFormColors = AgentFormColors(
        textPrimary = textPrimary,
        textMuted = textMuted,
        labelColor = labelColor,
        fieldBackground = fieldBackground,
        fieldBorder = fieldBorder,
        fieldPlaceholder = fieldPlaceholder,
        fieldIcon = fieldIcon,
        submitBtnBg = submitBtnBg,
        submitBtnDisabledBg = submitBtnDisabledBg,
        submitTextEnabled = Color.White,
        submitTextDisabled = if (isDark) OnBackgroundMuted else OnBackgroundSubtle,
        accent = forgotPasswordColor
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        // ── 1. Full-bleed Internet Image with local fallback (Coil background decode)
        val loginImageRequest = remember {
            ImageRequest.Builder(context)
                .data(TAJ_MAHAL_ONLINE_IMAGE)
                .error(R.drawable.dest_agra)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = loginImageRequest,
            contentDescription = "Taj Mahal Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
                .align(Alignment.TopCenter)
        )

        // ── 2. Top Scrim & Bottom Theme Scrim ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xDD000000),
                            0.18f to Color(0x66000000),
                            0.40f to Color(0x11000000),
                            0.58f to screenBackground.copy(alpha = 0.85f),
                            0.74f to screenBackground,
                            1.0f to screenBackground
                        )
                    )
                )
        )

        // ── 3. Main Scrollable Container (Keyboard-aware with imePadding) ─────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top Title at very top ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                color = Color.White
                            )
                        ) {
                            append("Every journey\n")
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                fontSize = 40.sp,
                                color = Color.White
                            )
                        ) {
                            append("starts ")
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                color = Color.White
                            )
                        ) {
                            append("here.")
                        }
                    },
                    lineHeight = 46.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // ── Bottom Sheet / Card Container ─────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(28.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (!isDark) 8.dp else 0.dp,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // ── Error Banner ──────────────────────────────────────────
                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                fontFamily = DmSansFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Info banner (e.g. password reset link sent)
                    uiState.infoMessage?.let { info ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = info,
                                fontFamily = DmSansFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // ── Animated Switcher: Forms, OTPs, and Forgot Password ───
                    AnimatedContent(
                        targetState = uiState.viewMode,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        label = "AuthViewTransition"
                    ) { mode ->
                        when (mode) {
                            is AuthViewMode.RegistrationOtp -> {
                                OtpVerificationView(
                                    email = mode.email,
                                    otp = regOtpCode,
                                    onOtpChange = {
                                        regOtpCode = it
                                        viewModel.clearError()
                                    },
                                    resendCountdown = resendCountdown,
                                    isLoading = uiState.isLoading,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textMuted = textMuted,
                                    submitBtnBg = submitBtnBg,
                                    submitBtnDisabledBg = submitBtnDisabledBg,
                                    submitBtnTextColor = submitBtnTextColor,
                                    forgotPasswordColor = forgotPasswordColor,
                                    onVerify = {
                                        focusManager.clearFocus()
                                        viewModel.verifyOtpAndCreateAccount(
                                            fullName = fullName,
                                            email = mode.email,
                                            password = password,
                                            otp = regOtpCode
                                        )
                                    },
                                    onResend = { viewModel.resendOtp(mode.email) },
                                    onCancel = {
                                        regOtpCode = ""
                                        viewModel.cancelOtp()
                                    }
                                )
                            }

                            is AuthViewMode.ForgotPasswordEmail -> {
                                ForgotPasswordEmailView(
                                    email = resetEmail,
                                    onEmailChange = {
                                        resetEmail = it
                                        viewModel.clearError()
                                    },
                                    isLoading = uiState.isLoading,
                                    textPrimary = textPrimary,
                                    textMuted = textMuted,
                                    labelColor = labelColor,
                                    fieldBackground = fieldBackground,
                                    fieldBorder = fieldBorder,
                                    fieldPlaceholder = fieldPlaceholder,
                                    fieldIcon = fieldIcon,
                                    submitBtnBg = submitBtnBg,
                                    submitBtnDisabledBg = submitBtnDisabledBg,
                                    submitBtnTextColor = submitBtnTextColor,
                                    onSendResetCode = {
                                        focusManager.clearFocus()
                                        viewModel.sendPasswordResetOtp(resetEmail)
                                    },
                                    onBack = { viewModel.cancelForgotPassword() }
                                )
                            }

                            is AuthViewMode.ForgotPasswordOtp -> {
                                ForgotPasswordResetView(
                                    email = mode.email,
                                    otp = resetOtpCode,
                                    onOtpChange = {
                                        resetOtpCode = it
                                        viewModel.clearError()
                                    },
                                    newPassword = resetNewPassword,
                                    onNewPasswordChange = {
                                        resetNewPassword = it
                                        viewModel.clearError()
                                    },
                                    confirmPassword = resetConfirmPassword,
                                    onConfirmPasswordChange = {
                                        resetConfirmPassword = it
                                        viewModel.clearError()
                                    },
                                    resendCountdown = resendCountdown,
                                    isLoading = uiState.isLoading,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textMuted = textMuted,
                                    labelColor = labelColor,
                                    fieldBackground = fieldBackground,
                                    fieldBorder = fieldBorder,
                                    fieldPlaceholder = fieldPlaceholder,
                                    fieldIcon = fieldIcon,
                                    submitBtnBg = submitBtnBg,
                                    submitBtnDisabledBg = submitBtnDisabledBg,
                                    submitBtnTextColor = submitBtnTextColor,
                                    forgotPasswordColor = forgotPasswordColor,
                                    onResetPassword = {
                                        focusManager.clearFocus()
                                        viewModel.resetPassword(
                                            email = mode.email,
                                            otp = resetOtpCode,
                                            newPassword = resetNewPassword,
                                            confirmPassword = resetConfirmPassword
                                        )
                                    },
                                    onResend = { viewModel.resendPasswordResetOtp(mode.email) },
                                    onBack = { viewModel.cancelForgotPassword() }
                                )
                            }

                            is AuthViewMode.AgentSignIn -> {
                                AgentSignInView(
                                    email = agentEmail,
                                    onEmailChange = {
                                        agentEmail = it
                                        viewModel.clearError()
                                    },
                                    password = agentPassword,
                                    onPasswordChange = {
                                        agentPassword = it
                                        viewModel.clearError()
                                    },
                                    passwordVisible = agentPasswordVisible,
                                    onTogglePasswordVisibility = { agentPasswordVisible = !agentPasswordVisible },
                                    isLoading = uiState.isLoading,
                                    colors = agentFormColors,
                                    onSignIn = {
                                        focusManager.clearFocus()
                                        viewModel.agentLogin(agentEmail, agentPassword)
                                    },
                                    onUseDemoAccount = { demoEmail, demoPassword ->
                                        agentEmail = demoEmail
                                        agentPassword = demoPassword
                                        viewModel.clearError()
                                    },
                                    onCreateAgentAccount = { viewModel.startAgentSignUp() },
                                    onBack = { viewModel.cancelAgent() }
                                )
                            }

                            is AuthViewMode.AgentSignUp -> {
                                AgentSignUpView(
                                    fullName = agentFullName,
                                    onFullNameChange = {
                                        agentFullName = it
                                        viewModel.clearError()
                                    },
                                    agencyName = agentAgencyName,
                                    onAgencyNameChange = {
                                        agentAgencyName = it
                                        viewModel.clearError()
                                    },
                                    city = agentCity,
                                    onCityChange = {
                                        agentCity = it
                                        viewModel.clearError()
                                    },
                                    email = agentEmail,
                                    onEmailChange = {
                                        agentEmail = it
                                        viewModel.clearError()
                                    },
                                    password = agentPassword,
                                    onPasswordChange = {
                                        agentPassword = it
                                        viewModel.clearError()
                                    },
                                    passwordVisible = agentPasswordVisible,
                                    onTogglePasswordVisibility = { agentPasswordVisible = !agentPasswordVisible },
                                    isLoading = uiState.isLoading,
                                    colors = agentFormColors,
                                    onSubmit = {
                                        focusManager.clearFocus()
                                        viewModel.requestAgentRegistrationOtp(
                                            fullName = agentFullName,
                                            email = agentEmail,
                                            password = agentPassword,
                                            agencyName = agentAgencyName
                                        )
                                    },
                                    onBack = { viewModel.startAgentSignIn() }
                                )
                            }

                            is AuthViewMode.AgentRegistrationOtp -> {
                                OtpVerificationView(
                                    email = mode.email,
                                    otp = agentOtpCode,
                                    onOtpChange = {
                                        agentOtpCode = it
                                        viewModel.clearError()
                                    },
                                    resendCountdown = resendCountdown,
                                    isLoading = uiState.isLoading,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textMuted = textMuted,
                                    submitBtnBg = submitBtnBg,
                                    submitBtnDisabledBg = submitBtnDisabledBg,
                                    submitBtnTextColor = if (agentOtpCode.length == 6) Color.White
                                                         else agentFormColors.submitTextDisabled,
                                    forgotPasswordColor = forgotPasswordColor,
                                    onVerify = {
                                        focusManager.clearFocus()
                                        viewModel.verifyAgentOtpAndCreateAccount(
                                            fullName = agentFullName,
                                            email = mode.email,
                                            password = agentPassword,
                                            otp = agentOtpCode,
                                            agencyName = agentAgencyName,
                                            city = agentCity
                                        )
                                    },
                                    onResend = { viewModel.resendOtp(mode.email) },
                                    onCancel = {
                                        agentOtpCode = ""
                                        viewModel.backToAgentSignUp()
                                    }
                                )
                            }

                            is AuthViewMode.StandardForm -> {
                                StandardAuthForm(
                                    selectedTab = selectedTab,
                                    onTabSelected = { tab ->
                                        selectedTab = tab
                                        viewModel.clearError()
                                    },
                                    fullName = fullName,
                                    onFullNameChange = {
                                        fullName = it
                                        viewModel.clearError()
                                    },
                                    email = email,
                                    onEmailChange = {
                                        email = it
                                        viewModel.clearError()
                                    },
                                    password = password,
                                    onPasswordChange = {
                                        password = it
                                        viewModel.clearError()
                                    },
                                    passwordVisible = passwordVisible,
                                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                                    isSubmitEnabled = isSubmitEnabled,
                                    isLoading = uiState.isLoading,
                                    isDark = isDark,
                                    segmentedTrackColor = segmentedTrackColor,
                                    segmentedPillColor = segmentedPillColor,
                                    textPrimary = textPrimary,
                                    textMuted = textMuted,
                                    labelColor = labelColor,
                                    fieldBackground = fieldBackground,
                                    fieldBorder = fieldBorder,
                                    fieldPlaceholder = fieldPlaceholder,
                                    fieldIcon = fieldIcon,
                                    submitBtnBg = submitBtnBg,
                                    submitBtnDisabledBg = submitBtnDisabledBg,
                                    submitBtnTextColor = submitBtnTextColor,
                                    googleBtnBg = googleBtnBg,
                                    googleBtnBorder = googleBtnBorder,
                                    googleBtnTextColor = googleBtnTextColor,
                                    dividerColor = dividerColor,
                                    dividerTextColor = dividerTextColor,
                                    forgotPasswordColor = forgotPasswordColor,
                                    focusManager = focusManager,
                                    onSubmit = {
                                        focusManager.clearFocus()
                                        if (selectedTab == AuthTab.SIGN_IN) {
                                            viewModel.login(email, password)
                                        } else {
                                            viewModel.requestRegistrationOtp(fullName, email, password)
                                        }
                                    },
                                    onForgotPasswordClick = {
                                        viewModel.startForgotPassword(email)
                                    },
                                    onAgentAccountClick = { viewModel.startAgentSignIn() },
                                    onGoogleSignIn = {
                                        if (GoogleAuthHelper.isConfigured) {
                                            coroutineScope.launch {
                                                GoogleAuthHelper.signInWithGoogle(context)
                                                    .onSuccess { googleUser ->
                                                        viewModel.loginWithGoogle(
                                                            email = googleUser.email,
                                                            fullName = googleUser.displayName,
                                                            profilePictureUrl = googleUser.profilePictureUri
                                                        )
                                                    }
                                                    .onFailure { error ->
                                                        if (error !is GetCredentialCancellationException) {
                                                            if (error is NoCredentialException || error.message?.contains("No credential") == true) {
                                                                showGoogleSetupDialog = true
                                                            } else {
                                                                viewModel.setError(error.message ?: "Google Sign-In failed.")
                                                            }
                                                        }
                                                    }
                                            }
                                        } else {
                                            showGoogleSetupDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Google Web Client ID Setup / Test Sign-In Dialog ──────────────────
        if (showGoogleSetupDialog) {
            AlertDialog(
                onDismissRequest = { showGoogleSetupDialog = false },
                title = {
                    Text(
                        text = "Google Sign-In",
                        fontFamily = PlayfairFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                text = {
                    Text(
                        text = "Google Play Services requires an \"Android\" OAuth Client ID in your Google Cloud Console matching this app:\n\n• Package: com.example.smarttravelplanner\n• SHA-1: 01:70:B5:D7:FC:BA:1E:16:41:C0:53:55:DB:87:53:5D:90:E3:5D:E3\n\nWould you like to sign in using the Google Test Account for now?",
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        color = textMuted,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showGoogleSetupDialog = false
                            viewModel.loginWithGoogle(
                                email = "google.traveler@gmail.com",
                                fullName = "Google Traveler",
                                profilePictureUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Continue as Test User",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoogleSetupDialog = false }) {
                        Text(
                            text = "Cancel",
                            fontFamily = DmSansFontFamily,
                            color = textMuted
                        )
                    }
                },
                containerColor = cardBackground,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Forgot Password - Step 1: Enter Email
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ForgotPasswordEmailView(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    textPrimary: Color,
    textMuted: Color,
    labelColor: Color,
    fieldBackground: Color,
    fieldBorder: Color,
    fieldPlaceholder: Color,
    fieldIcon: Color,
    submitBtnBg: Color,
    submitBtnDisabledBg: Color,
    submitBtnTextColor: Color,
    onSendResetCode: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reset Password",
            fontFamily = PlayfairFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter your registered email and we'll send you a 6-digit code to reset your password.",
            fontFamily = DmSansFontFamily,
            fontSize = 13.sp,
            color = textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        AuthFormField(
            label = "REGISTERED EMAIL",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "you@example.com",
            leadingIcon = Icons.Outlined.Email,
            labelColor = labelColor,
            fieldBackground = fieldBackground,
            fieldBorder = fieldBorder,
            fieldPlaceholder = fieldPlaceholder,
            fieldIcon = fieldIcon,
            textPrimary = textPrimary,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = { if (email.isNotBlank()) onSendResetCode() }
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onSendResetCode,
            enabled = email.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = submitBtnBg,
                contentColor = submitBtnTextColor,
                disabledContainerColor = submitBtnDisabledBg,
                disabledContentColor = submitBtnTextColor
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = submitBtnTextColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Send Reset Code →",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = submitBtnTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "← Back to Sign In",
            fontFamily = DmSansFontFamily,
            fontSize = 13.sp,
            color = textMuted,
            modifier = Modifier.clickable(onClick = onBack)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Forgot Password - Step 2: Enter OTP & New Password
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ForgotPasswordResetView(
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    resendCountdown: Int,
    isLoading: Boolean,
    isDark: Boolean,
    textPrimary: Color,
    textMuted: Color,
    labelColor: Color,
    fieldBackground: Color,
    fieldBorder: Color,
    fieldPlaceholder: Color,
    fieldIcon: Color,
    submitBtnBg: Color,
    submitBtnDisabledBg: Color,
    submitBtnTextColor: Color,
    forgotPasswordColor: Color,
    onResetPassword: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val isResetEnabled = otp.length == 6 &&
            newPassword.length >= 6 &&
            confirmPassword.isNotBlank() &&
            !isLoading

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create New Password",
            fontFamily = PlayfairFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter the 6-digit code sent to\n$email",
            fontFamily = DmSansFontFamily,
            fontSize = 13.sp,
            color = textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))

        // 6-digit OTP
        OtpInputField(
            otp = otp,
            onOtpChange = onOtpChange,
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Resend Timer Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (resendCountdown > 0) {
                Text(
                    text = "Resend code in ${resendCountdown}s",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = textMuted
                )
            } else {
                Text(
                    text = "Resend Code",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = forgotPasswordColor,
                    modifier = Modifier.clickable(onClick = onResend)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // New Password field
        AuthFormField(
            label = "NEW PASSWORD",
            value = newPassword,
            onValueChange = onNewPasswordChange,
            placeholder = "At least 6 characters",
            leadingIcon = Icons.Outlined.Lock,
            labelColor = labelColor,
            fieldBackground = fieldBackground,
            fieldBorder = fieldBorder,
            fieldPlaceholder = fieldPlaceholder,
            fieldIcon = fieldIcon,
            textPrimary = textPrimary,
            isPassword = true,
            passwordVisible = newPasswordVisible,
            onTogglePasswordVisibility = { newPasswordVisible = !newPasswordVisible },
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Password field
        AuthFormField(
            label = "CONFIRM NEW PASSWORD",
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "Repeat new password",
            leadingIcon = Icons.Outlined.Lock,
            labelColor = labelColor,
            fieldBackground = fieldBackground,
            fieldBorder = fieldBorder,
            fieldPlaceholder = fieldPlaceholder,
            fieldIcon = fieldIcon,
            textPrimary = textPrimary,
            isPassword = true,
            passwordVisible = confirmPasswordVisible,
            onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = { if (isResetEnabled) onResetPassword() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onResetPassword,
            enabled = isResetEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = submitBtnBg,
                contentColor = submitBtnTextColor,
                disabledContainerColor = submitBtnDisabledBg,
                disabledContentColor = submitBtnTextColor
            ),
            border = if (isResetEnabled && isDark) BorderStroke(0.8.dp, Color(0xFF333D52)) else null
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = submitBtnTextColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Reset Password & Sign In →",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = submitBtnTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "← Back to Sign In",
            fontFamily = DmSansFontFamily,
            fontSize = 13.sp,
            color = textMuted,
            modifier = Modifier.clickable(onClick = onBack)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Registration OTP Verification View
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun OtpVerificationView(
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    resendCountdown: Int,
    isLoading: Boolean,
    isDark: Boolean,
    textPrimary: Color,
    textMuted: Color,
    submitBtnBg: Color,
    submitBtnDisabledBg: Color,
    submitBtnTextColor: Color,
    forgotPasswordColor: Color,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Verify Your Email",
            fontFamily = PlayfairFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter the 6-digit code sent to\n$email",
            fontFamily = DmSansFontFamily,
            fontSize = 13.sp,
            color = textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(22.dp))

        // 6-digit OTP Input Boxes
        OtpInputField(
            otp = otp,
            onOtpChange = onOtpChange,
            isDark = isDark,
            onDone = onVerify
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Verify & Create Account button
        Button(
            onClick = onVerify,
            enabled = otp.length == 6 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = submitBtnBg,
                contentColor = submitBtnTextColor,
                disabledContainerColor = submitBtnDisabledBg,
                disabledContentColor = submitBtnTextColor
            ),
            border = if (otp.length == 6 && isDark) BorderStroke(0.8.dp, Color(0xFF333D52)) else null
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = submitBtnTextColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Verify & Complete →",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = submitBtnTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Resend Timer Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (resendCountdown > 0) {
                Text(
                    text = "Resend code in ${resendCountdown}s",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    color = textMuted
                )
            } else {
                Text(
                    text = "Resend Code",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = forgotPasswordColor,
                    modifier = Modifier.clickable(onClick = onResend)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cancel / Back to form
        Text(
            text = "← Change email / Back",
            fontFamily = DmSansFontFamily,
            fontSize = 13.sp,
            color = textMuted,
            modifier = Modifier.clickable(onClick = onCancel)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  6-Digit OTP Input Component
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OtpInputField(
    otp: String,
    onOtpChange: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = otp,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(6)
            onOtpChange(digits)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (otp.length == 6) onDone()
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { focusRequester.requestFocus() },
                contentAlignment = Alignment.Center
            ) {
                // Keep innerTextField active so soft keyboard inputs and selection are received
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .alpha(0.001f)
                ) {
                    innerTextField()
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(6) { index ->
                        val char = otp.getOrNull(index)?.toString() ?: ""
                        val isFocused = otp.length == index || (otp.length == 6 && index == 5)
                        val boxBorder = when {
                            isFocused -> Amber
                            char.isNotEmpty() -> if (isDark) Color(0xFF4A5568) else Color(0xFF94A3B8)
                            else -> if (isDark) Color(0xFF333333) else Color(0xFFD1D5DB)
                        }
                        val boxBg = if (isDark) SurfaceElevated else Color(0xFFF9FAFB)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(boxBg)
                                .border(if (isFocused) 1.5.dp else 1.dp, boxBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Standard Sign In / Create Account Form
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StandardAuthForm(
    selectedTab: AuthTab,
    onTabSelected: (AuthTab) -> Unit,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isSubmitEnabled: Boolean,
    isLoading: Boolean,
    isDark: Boolean,
    segmentedTrackColor: Color,
    segmentedPillColor: Color,
    textPrimary: Color,
    textMuted: Color,
    labelColor: Color,
    fieldBackground: Color,
    fieldBorder: Color,
    fieldPlaceholder: Color,
    fieldIcon: Color,
    submitBtnBg: Color,
    submitBtnDisabledBg: Color,
    submitBtnTextColor: Color,
    googleBtnBg: Color,
    googleBtnBorder: Color,
    googleBtnTextColor: Color,
    dividerColor: Color,
    dividerTextColor: Color,
    forgotPasswordColor: Color,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onSubmit: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onAgentAccountClick: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Smooth Segmented Switcher (Sign In vs Create Account) ─────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(segmentedTrackColor)
                .padding(4.dp)
        ) {
            val tabWidth = maxWidth / 2
            val targetOffset = if (selectedTab == AuthTab.SIGN_IN) 0.dp else tabWidth
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "tab_indicator_offset"
            )

            // Smooth sliding indicator pill
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
                    .background(segmentedPillColor)
            )

            // Tab click targets & labels
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTabSelected(AuthTab.SIGN_IN) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        fontFamily = DmSansFontFamily,
                        fontWeight = if (selectedTab == AuthTab.SIGN_IN) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (selectedTab == AuthTab.SIGN_IN) textPrimary else textMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTabSelected(AuthTab.CREATE_ACCOUNT) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create Account",
                        fontFamily = DmSansFontFamily,
                        fontWeight = if (selectedTab == AuthTab.CREATE_ACCOUNT) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (selectedTab == AuthTab.CREATE_ACCOUNT) textPrimary else textMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Full Name Field (Smooth expand/collapse for Create Account) ───────
        AnimatedVisibility(
            visible = selectedTab == AuthTab.CREATE_ACCOUNT,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
        ) {
            Column {
                AuthFormField(
                    label = "FULL NAME",
                    value = fullName,
                    onValueChange = onFullNameChange,
                    placeholder = "Your name",
                    leadingIcon = Icons.Outlined.Person,
                    labelColor = labelColor,
                    fieldBackground = fieldBackground,
                    fieldBorder = fieldBorder,
                    fieldPlaceholder = fieldPlaceholder,
                    fieldIcon = fieldIcon,
                    textPrimary = textPrimary,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // ── Email Field ───────────────────────────────────────────────────────
        AuthFormField(
            label = "EMAIL",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "you@example.com",
            leadingIcon = Icons.Outlined.Email,
            labelColor = labelColor,
            fieldBackground = fieldBackground,
            fieldBorder = fieldBorder,
            fieldPlaceholder = fieldPlaceholder,
            fieldIcon = fieldIcon,
            textPrimary = textPrimary,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Password Field ────────────────────────────────────────────────────
        AuthFormField(
            label = "PASSWORD",
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "••••••••",
            leadingIcon = Icons.Outlined.Lock,
            labelColor = labelColor,
            fieldBackground = fieldBackground,
            fieldBorder = fieldBorder,
            fieldPlaceholder = fieldPlaceholder,
            fieldIcon = fieldIcon,
            textPrimary = textPrimary,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = {
                if (isSubmitEnabled) onSubmit()
            }
        )

        // ── Forgot Password Link (Only in Sign In) ─────────────────────────────
        AnimatedVisibility(
            visible = selectedTab == AuthTab.SIGN_IN,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Opens the separate travel-agent sign-in (same pattern as Forgot password)
                Text(
                    text = "Agent account",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = forgotPasswordColor,
                    modifier = Modifier
                        .clickable(onClick = onAgentAccountClick)
                        .padding(vertical = 4.dp)
                )
                Text(
                    text = "Forgot password?",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = forgotPasswordColor,
                    modifier = Modifier
                        .clickable(onClick = onForgotPasswordClick)
                        .padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── Submit Button (Disabled when fields empty) ────────────────────────
        Button(
            onClick = onSubmit,
            enabled = isSubmitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = submitBtnBg,
                contentColor = submitBtnTextColor,
                disabledContainerColor = submitBtnDisabledBg,
                disabledContentColor = submitBtnTextColor
            ),
            border = if (isSubmitEnabled && isDark) BorderStroke(0.8.dp, Color(0xFF333D52)) else null
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = submitBtnTextColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (selectedTab == AuthTab.SIGN_IN) "Sign In →" else "Create Account →",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = submitBtnTextColor
                )
            }
        }

        // ── Divider ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor
            )
            Text(
                text = "  or continue with  ",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                color = dividerTextColor,
                fontSize = 14.sp
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor
            )
        }

        // ── Google Sign In Button with Official Multi-Color Logo ───────────────
        Button(
            onClick = onGoogleSignIn,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = googleBtnBg,
                contentColor = googleBtnTextColor
            ),
            border = BorderStroke(1.dp, googleBtnBorder),
            elevation = if (!isDark) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    fontFamily = ProductSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = googleBtnTextColor
                )
            }
        }
    }
}

// ── Reusable Form Field Component ────────────────────────────────────────────
@Composable
internal fun AuthFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    labelColor: Color,
    fieldBackground: Color,
    fieldBorder: Color,
    fieldPlaceholder: Color,
    fieldIcon: Color,
    textPrimary: Color,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(fieldBackground)
                .border(0.8.dp, fieldBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = fieldIcon,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = DmSansFontFamily,
                            fontSize = 14.sp,
                            color = fieldPlaceholder
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = DmSansFontFamily,
                            fontSize = 14.sp,
                            color = textPrimary
                        ),
                        cursorBrush = SolidColor(Amber),
                        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (isPassword) {
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = fieldIcon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
