package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AiSparkleGradientEnd
import com.example.ui.theme.AiSparkleGradientStart
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthTab {
    EMAIL_OTP,
    MOBILE_OTP,
    GOOGLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInOtpDialog(
    onDismiss: () -> Unit,
    onSendEmailOtp: (String, (Boolean, String) -> Unit) -> Unit,
    onVerifyEmailOtp: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onSendPhoneOtp: (String, (Boolean, String) -> Unit) -> Unit,
    onVerifyPhoneOtp: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onSignInWithGoogle: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(AuthTab.EMAIL_OTP) }

    // Email state
    var email by remember { mutableStateOf("sathoshisaitama@gmail.com") }
    var emailDisplayName by remember { mutableStateOf("Satoshi Saitama") }
    var emailOtpSent by remember { mutableStateOf(false) }
    var emailOtpCode by remember { mutableStateOf("") }
    var generatedEmailOtp by remember { mutableStateOf("") }
    var isSendingEmailOtp by remember { mutableStateOf(false) }
    var isVerifyingEmailOtp by remember { mutableStateOf(false) }
    var emailTimerSeconds by remember { mutableStateOf(0) }

    // Mobile state
    var selectedCountryCode by remember { mutableStateOf("+1") }
    var phoneNumber by remember { mutableStateOf("5550192834") }
    var mobileDisplayName by remember { mutableStateOf("Satoshi") }
    var mobileOtpSent by remember { mutableStateOf(false) }
    var mobileOtpCode by remember { mutableStateOf("") }
    var generatedMobileOtp by remember { mutableStateOf("") }
    var isSendingMobileOtp by remember { mutableStateOf(false) }
    var isVerifyingMobileOtp by remember { mutableStateOf(false) }
    var mobileTimerSeconds by remember { mutableStateOf(0) }

    val countryCodes = listOf(
        "+1 (US/CA)",
        "+91 (IN)",
        "+44 (UK)",
        "+61 (AU)",
        "+81 (JP)",
        "+49 (DE)",
        "+33 (FR)",
        "+86 (CN)",
        "+65 (SG)",
        "+971 (AE)"
    )
    var showCountryDropdown by remember { mutableStateOf(false) }

    // Countdown timers
    LaunchedEffect(emailTimerSeconds) {
        if (emailTimerSeconds > 0) {
            delay(1000)
            emailTimerSeconds -= 1
        }
    }

    LaunchedEffect(mobileTimerSeconds) {
        if (mobileTimerSeconds > 0) {
            delay(1000)
            mobileTimerSeconds -= 1
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("sign_in_otp_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, PrimaryIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sign In & Cloud Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Email OTP, Mobile OTP, or Google Account",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == AuthTab.EMAIL_OTP,
                        onClick = { selectedTab = AuthTab.EMAIL_OTP },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Email OTP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == AuthTab.MOBILE_OTP,
                        onClick = { selectedTab = AuthTab.MOBILE_OTP },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mobile OTP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == AuthTab.GOOGLE,
                        onClick = { selectedTab = AuthTab.GOOGLE },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content per Tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        AuthTab.EMAIL_OTP -> {
                            // ---------------- EMAIL OTP FORM ----------------
                            Text(
                                text = "Sign in using a one-time passcode sent directly to your email inbox.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    emailOtpSent = false
                                },
                                label = { Text("Email Address *") },
                                placeholder = { Text("name@example.com") },
                                leadingIcon = { Icon(Icons.Outlined.Mail, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input_field"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = emailDisplayName,
                                onValueChange = { emailDisplayName = it },
                                label = { Text("Full Name (Optional)") },
                                placeholder = { Text("e.g. Satoshi Saitama") },
                                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_name_field"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Send OTP Button
                            Button(
                                onClick = {
                                    if (email.isNotBlank() && email.contains("@")) {
                                        isSendingEmailOtp = true
                                        onSendEmailOtp(email) { success, codeOrErr ->
                                            isSendingEmailOtp = false
                                            if (success) {
                                                emailOtpSent = true
                                                generatedEmailOtp = codeOrErr
                                                emailTimerSeconds = 60
                                                Toast.makeText(context, "OTP Code sent to $email", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, codeOrErr, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = email.isNotBlank() && !isSendingEmailOtp && emailTimerSeconds == 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_email_otp_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                if (isSendingEmailOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sending Passcode...")
                                } else {
                                    Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (emailTimerSeconds > 0) "Resend in ${emailTimerSeconds}s"
                                        else if (emailOtpSent) "Resend Email OTP"
                                        else "Send 6-Digit Email OTP",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // OTP Verification Box
                            AnimatedVisibility(visible = emailOtpSent) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Verification Code Sent!",
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen,
                                                fontSize = 13.sp
                                            )
                                        }

                                        if (generatedEmailOtp.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Demo Verification Code: $generatedEmailOtp",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = PrimaryIndigo
                                                )
                                                TextButton(
                                                    onClick = { emailOtpCode = generatedEmailOtp },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Auto-Fill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = emailOtpCode,
                                            onValueChange = { if (it.length <= 6) emailOtpCode = it },
                                            label = { Text("Enter 6-Digit Code") },
                                            placeholder = { Text("123456") },
                                            leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("email_otp_input"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Button(
                                            onClick = {
                                                if (emailOtpCode.length >= 4) {
                                                    isVerifyingEmailOtp = true
                                                    onVerifyEmailOtp(email, emailOtpCode, emailDisplayName) { success, msg ->
                                                        isVerifyingEmailOtp = false
                                                        if (success) {
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            onDismiss()
                                                        } else {
                                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Please enter the 6-digit code", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            enabled = emailOtpCode.isNotBlank() && !isVerifyingEmailOtp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("verify_email_otp_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            if (isVerifyingEmailOtp) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Verifying...")
                                            } else {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Verify & Sign In", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AuthTab.MOBILE_OTP -> {
                            // ---------------- MOBILE OTP FORM ----------------
                            Text(
                                text = "Sign in using your mobile number with a secure SMS verification code.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Country Code selector
                                Box(modifier = Modifier.width(110.dp)) {
                                    OutlinedButton(
                                        onClick = { showCountryDropdown = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp)
                                    ) {
                                        Text(selectedCountryCode, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }

                                    DropdownMenu(
                                        expanded = showCountryDropdown,
                                        onDismissRequest = { showCountryDropdown = false }
                                    ) {
                                        countryCodes.forEach { codeStr ->
                                            val prefix = codeStr.substringBefore(" ")
                                            DropdownMenuItem(
                                                text = { Text(codeStr, fontSize = 12.sp) },
                                                onClick = {
                                                    selectedCountryCode = prefix
                                                    showCountryDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Mobile Number
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = {
                                        phoneNumber = it
                                        mobileOtpSent = false
                                    },
                                    label = { Text("Mobile Number *") },
                                    placeholder = { Text("555-0192") },
                                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("mobile_number_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = mobileDisplayName,
                                onValueChange = { mobileDisplayName = it },
                                label = { Text("Full Name (Optional)") },
                                placeholder = { Text("e.g. Satoshi Saitama") },
                                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mobile_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Send SMS Button
                            Button(
                                onClick = {
                                    val fullPhone = "$selectedCountryCode $phoneNumber".trim()
                                    if (phoneNumber.length >= 7) {
                                        isSendingMobileOtp = true
                                        onSendPhoneOtp(fullPhone) { success, codeOrErr ->
                                            isSendingMobileOtp = false
                                            if (success) {
                                                mobileOtpSent = true
                                                generatedMobileOtp = codeOrErr
                                                mobileTimerSeconds = 60
                                                Toast.makeText(context, "SMS Code sent to $fullPhone", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, codeOrErr, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = phoneNumber.isNotBlank() && !isSendingMobileOtp && mobileTimerSeconds == 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_mobile_otp_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                if (isSendingMobileOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sending SMS OTP...")
                                } else {
                                    Icon(Icons.Outlined.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (mobileTimerSeconds > 0) "Resend SMS in ${mobileTimerSeconds}s"
                                        else if (mobileOtpSent) "Resend SMS OTP"
                                        else "Send 6-Digit SMS OTP",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // SMS OTP Verification Box
                            AnimatedVisibility(visible = mobileOtpSent) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "SMS Passcode Sent!",
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen,
                                                fontSize = 13.sp
                                            )
                                        }

                                        if (generatedMobileOtp.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Demo SMS Code: $generatedMobileOtp",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = PrimaryIndigo
                                                )
                                                TextButton(
                                                    onClick = { mobileOtpCode = generatedMobileOtp },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Auto-Fill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = mobileOtpCode,
                                            onValueChange = { if (it.length <= 6) mobileOtpCode = it },
                                            label = { Text("Enter 6-Digit SMS Code") },
                                            placeholder = { Text("123456") },
                                            leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("mobile_otp_input"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Button(
                                            onClick = {
                                                val fullPhone = "$selectedCountryCode $phoneNumber".trim()
                                                if (mobileOtpCode.length >= 4) {
                                                    isVerifyingMobileOtp = true
                                                    onVerifyPhoneOtp(fullPhone, mobileOtpCode, mobileDisplayName) { success, msg ->
                                                        isVerifyingMobileOtp = false
                                                        if (success) {
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            onDismiss()
                                                        } else {
                                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Please enter the 6-digit SMS code", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            enabled = mobileOtpCode.isNotBlank() && !isVerifyingMobileOtp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("verify_mobile_otp_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            if (isVerifyingMobileOtp) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Verifying Mobile...")
                                            } else {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Verify & Sign In", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AuthTab.GOOGLE -> {
                            // ---------------- GOOGLE ONE-TAP FORM ----------------
                            Text(
                                text = "Sign in directly with your Google Account for seamless cross-device synchronization and Firebase Cloud backups.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Automatic Cloud Backups", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Devices, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sync resumes across multiple Android devices", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Encrypted & 100% private data isolation", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    onSignInWithGoogle()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("google_dialog_signin_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Continue with Google Account", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
