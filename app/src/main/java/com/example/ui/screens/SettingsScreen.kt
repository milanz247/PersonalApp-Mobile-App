package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.utils.BiometricAuthHelper
import com.example.utils.BiometricStatus
import com.example.utils.PinSecurityHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settingsState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Local controller states
    var nameInput by remember { mutableStateOf("") }
    var currencySymInput by remember { mutableStateOf("") }
    var currencyCodeInput by remember { mutableStateOf("") }
    var identityNumberInput by remember { mutableStateOf("") }
    var profileImagePath by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // Take persistable URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                profileImagePath = it.toString()
                viewModel.saveSettings(
                    userName = nameInput,
                    currencySymbol = currencySymInput,
                    currencyCode = currencyCodeInput,
                    identityNumber = identityNumberInput,
                    timezone = settings.timezone,
                    dateFormat = settings.dateFormat,
                    appLockEnabled = settings.appLockEnabled,
                    appLockPin = settings.appLockPin,
                    avatarPath = it.toString(),
                    debtReminderDaysBefore = settings.debtReminderDaysBefore,
                    debtInitialMsg = settings.debtInitialMessageTemplate,
                    debtReminderMsg = settings.debtReminderMessageTemplate
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Dialog flags
    var showDisableConfirmDialog by remember { mutableStateOf(false) }
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showVerifyPinDialog by remember { mutableStateOf(false) }

    // Setup PIN states
    var setupNewPin1 by remember { mutableStateOf("") }
    var setupNewPin2 by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(1) } // 1 = Enter PIN, 2 = Confirm PIN
    var setupError by remember { mutableStateOf<String?>(null) }

    // Verify current PIN state (for changing PIN)
    var verifyPinInput by remember { mutableStateOf("") }
    var verifyPinError by remember { mutableStateOf<String?>(null) }

    // Local file backup/recovery states
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackupToUri(it, context)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            showImportConfirmDialog = true
        }
    }

    // Set states on read
    LaunchedEffect(settings) {
        nameInput = settings.userName
        currencySymInput = settings.currencySymbol
        currencyCodeInput = settings.currencyCode
        identityNumberInput = settings.identityNumber ?: ""
        profileImagePath = settings.avatarPath
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(
                text = "GLOBAL CONSTANTS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "System Preferences",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Section A: Profile/Identity Preferences
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PremiumIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Ledger Identity Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Account Holder Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("username_settings_input"),
                    colors = premiumTextFieldColors(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = identityNumberInput,
                    onValueChange = { identityNumberInput = it },
                    label = { Text("ID Number / Legal Identity") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("identity_number_settings_input"),
                    colors = premiumTextFieldColors(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Profile Image",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (profileImagePath != null) "Change Image" else "Select Image")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currencySymInput,
                        onValueChange = { currencySymInput = it },
                        label = { Text("Currency Symbol (e.g. Rs.)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = premiumTextFieldColors(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currencyCodeInput,
                        onValueChange = { currencyCodeInput = it },
                        label = { Text("ISO Code (e.g. LKR)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = premiumTextFieldColors(),
                        singleLine = true
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Theming Mode Row
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "App Theme Appearance",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themes = listOf("SYSTEM" to "Auto Settings", "LIGHT" to "Light", "DARK" to "Dark")
                        themes.forEach { (key, label) ->
                            val isSelected = settings.themePref == key
                            OutlinedButton(
                                onClick = { viewModel.updateSettings(settings.copy(themePref = key)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) PremiumIndigo.copy(alpha = 0.1f) else Color.Transparent
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(0.5f)
                                )
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PremiumIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Passcode Switch controller row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Passcode Lock",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Shield ledger database logs behind a 4-digit PIN lock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.appLockEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                setupNewPin1 = ""
                                setupNewPin2 = ""
                                setupStep = 1
                                setupError = null
                                showSetupPinDialog = true
                            } else {
                                showDisableConfirmDialog = true
                            }
                        },
                        modifier = Modifier.testTag("applock_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PremiumIndigo
                        )
                    )
                }

                // Change PIN option (visible only when App Lock is enabled)
                if (settings.appLockEnabled) {
                    OutlinedButton(
                        onClick = {
                            verifyPinInput = ""
                            verifyPinError = null
                            showVerifyPinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("change_pin_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Change PIN Code", fontWeight = FontWeight.Bold, color = PremiumIndigo)
                    }
                }

                // Biometric toggle row (only enabled/visible when App Lock is ON)
                if (settings.appLockEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Fingerprint / Face Unlock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val statusMsg = if (settings.biometricEnabled) "Biometric unlock activated." else "Enable fingerprint or facial recognition as lock screen shortcut."
                            Text(
                                text = statusMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.biometricEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    val status = BiometricAuthHelper.isBiometricAvailable(context)
                                    if (status == BiometricStatus.AVAILABLE) {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            BiometricAuthHelper.showBiometricPrompt(
                                                activity = activity,
                                                title = "Enable Biometric",
                                                subtitle = "Verify fingerprint to link with App-Lock",
                                                onSuccess = {
                                                    viewModel.updateAppLockSettings(true, settings.appLockPin, true)
                                                },
                                                onError = { _, errStr ->
                                                    Toast.makeText(context, "Biometric link failed: $errStr", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            Toast.makeText(context, "FragmentActivity binding error.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        val userMsg = when (status) {
                                            BiometricStatus.NO_HARDWARE -> "No fingerprint or face sensor hardware found on this device."
                                            BiometricStatus.UNAVAILABLE -> "Biometric hardware is currently busy or unavailable."
                                            BiometricStatus.NONE_ENROLLED -> "No fingerprint or face credentials enrolled. Add secure entry in device Settings first."
                                            else -> "Biometric authentication setup unavailable."
                                        }
                                        Toast.makeText(context, userMsg, Toast.LENGTH_LONG).show()
                                        if (status == BiometricStatus.NONE_ENROLLED) {
                                            try {
                                                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                                                context.startActivity(enrollIntent)
                                            } catch (e: Exception) {
                                                // Fallback
                                            }
                                        }
                                    }
                                } else {
                                    viewModel.updateAppLockSettings(true, settings.appLockPin, false)
                                }
                            },
                            modifier = Modifier.testTag("biometric_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PremiumIndigo
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        viewModel.saveSettings(
                            userName = nameInput,
                            currencySymbol = currencySymInput,
                            currencyCode = currencyCodeInput,
                            identityNumber = identityNumberInput,
                            timezone = settings.timezone,
                            dateFormat = settings.dateFormat,
                            appLockEnabled = settings.appLockEnabled,
                            appLockPin = settings.appLockPin,
                            avatarPath = profileImagePath,
                            debtReminderDaysBefore = settings.debtReminderDaysBefore,
                            debtInitialMsg = settings.debtInitialMessageTemplate,
                            debtReminderMsg = settings.debtReminderMessageTemplate
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_settings_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumIndigo)
                ) {
                    Text("Save Identity & Preferences", fontWeight = FontWeight.Bold)
                }
            }
        }


        // Section C: Local File Backup & Recovery preferences
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = PremiumIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Local File Backup & Recovery",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Save a persistent copy of your financial ledger to secure offline storage or parse a previously exported backup file to restore full data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            createDocumentLauncher.launch("finance_ledger_backup_$sdf.json")
                        },
                        modifier = Modifier.weight(1f).testTag("export_backup_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumIndigo)
                    ) {
                        Text("Export File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.weight(1f).testTag("import_backup_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Import File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Professional Elegant Developer Footer Block
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "LEDGER • PREMIUM EDITION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 1.6.sp
            )
            Text(
                text = "Secure local-first double-entry engine running with complete sandbox safety.",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                lineHeight = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Developed by",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "Mila Madusanka",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PremiumIndigo,
                    letterSpacing = 0.2.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        // 1. App Lock Disable Confirmation Dialog
        if (showDisableConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDisableConfirmDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SystemRed.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = SystemRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "Deactivate App-Lock?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Text("Warning: Disabling App Lock will also clear your secure PIN shield, turn off Fingerprint/Face unlock, and allow direct entry on application startup.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateAppLockSettings(false, null, false)
                            showDisableConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SystemRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Unprotect Logs", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisableConfirmDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // 2. PIN Setup Dialog (Enter PIN -> Confirm PIN flow)
        if (showSetupPinDialog) {
            AlertDialog(
                onDismissRequest = { showSetupPinDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PremiumIndigo.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Password,
                                contentDescription = null,
                                tint = PremiumIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = if (setupStep == 1) "Create App Lock PIN" else "Confirm App Lock PIN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = if (setupStep == 1) 
                                "Define a secure 4-digit security PIN to restrict local access." 
                            else 
                                "Please re-type your chosen PIN passcode to verify visual accuracy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (setupStep == 1) {
                            OutlinedTextField(
                                value = setupNewPin1,
                                onValueChange = { setupNewPin1 = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("4-digit Secure PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("setup_pin_1_input"),
                                singleLine = true
                            )
                        } else {
                            OutlinedTextField(
                                value = setupNewPin2,
                                onValueChange = { setupNewPin2 = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("Verify 4-digit PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("setup_pin_2_input"),
                                singleLine = true
                            )
                        }

                        if (setupError != null) {
                            Text(
                                text = setupError ?: "",
                                color = SystemRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (setupStep == 1) {
                                if (setupNewPin1.length != 4) {
                                    setupError = "PIN length must be exactly 4 digits."
                                } else {
                                    setupStep = 2
                                    setupError = null
                                }
                            } else {
                                if (setupNewPin1 != setupNewPin2) {
                                    setupError = "PIN passcodes do not match! Re-verify."
                                    setupNewPin2 = ""
                                } else {
                                    // Securely Salt and Hash PIN
                                    val hashed = PinSecurityHelper.hashPin(setupNewPin1)
                                    viewModel.updateAppLockSettings(true, hashed, false)
                                    showSetupPinDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumIndigo),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (setupStep == 1) "Next Step" else "Activate Shield",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    if (setupStep == 2) {
                        TextButton(onClick = { 
                            setupStep = 1
                            setupNewPin2 = ""
                            setupError = null
                        }) {
                            Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        TextButton(onClick = { showSetupPinDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        }

        // 3. Verify Current PIN Dialog (For Change PIN trigger)
        if (showVerifyPinDialog) {
            AlertDialog(
                onDismissRequest = { showVerifyPinDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PremiumIndigo.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PremiumIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "Verify Current Passcode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "For security reasons, please input your current 4-digit pass-PIN before modifying lock settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = verifyPinInput,
                            onValueChange = { verifyPinInput = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Current PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("current_pin_verify_input"),
                            singleLine = true
                        )

                        if (verifyPinError != null) {
                            Text(
                                text = verifyPinError ?: "",
                                color = SystemRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val isValid = PinSecurityHelper.verifyPin(verifyPinInput, settings.appLockPin)
                            if (isValid) {
                                verifyPinError = null
                                showVerifyPinDialog = false
                                // Launch PIN setup dialog immediately to change PIN
                                setupNewPin1 = ""
                                setupNewPin2 = ""
                                setupStep = 1
                                setupError = null
                                showSetupPinDialog = true
                            } else {
                                verifyPinError = "Incorrect Lock PIN code! Re-verify."
                                verifyPinInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumIndigo),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Verify PIN", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVerifyPinDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        if (showImportConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showImportConfirmDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PremiumIndigo.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = PremiumIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "Overwrite Ledger Database?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Text(
                        text = "This will replace all current data with the contents of this backup file. This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showImportConfirmDialog = false
                            pendingImportUri?.let {
                                viewModel.importBackupFromUri(it, context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SystemRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Overwrite Database", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportConfirmDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
