package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.utils.BackupRestoreHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = AppRepository(db)

    // Current State Parameters
    val currentMonthYear = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
    
    // Passcode lock parameters
    val isAppUnlocked = MutableStateFlow(false)
    val appLockPinInput = MutableStateFlow("")
    val appLockWrongAttempts = MutableStateFlow(0)
    val appLockLockoutUntil = MutableStateFlow<Long?>(null)
    val appLockLockoutRemainingSeconds = MutableStateFlow(0L)

    // Welcome Back Restore-on-reinstall detection parameters
    val showWelcomeBackScreen = MutableStateFlow(false)
    val welcomeBackupMetadata = MutableStateFlow<BackupRestoreHelper.BackupMetadata?>(null)

    // Status Message/Toast triggers
    val errorMessage = MutableStateFlow<String?>(null)
    val successMessage = MutableStateFlow<String?>(null)

    // Expose repository base flows
    val settingsState: StateFlow<AppSettings> = repository.settingsFlow
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val accountsState: StateFlow<List<Account>> = repository.accountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesState: StateFlow<List<Category>> = repository.categoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsState: StateFlow<List<Transaction>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtsState: StateFlow<List<Debt>> = repository.debtsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringState: StateFlow<List<RecurringTransaction>> = repository.recurringFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Monthly budgets
    val budgetsState: StateFlow<List<Budget>> = currentMonthYear
        .flatMapLatest { my -> repository.getBudgetsForMonth(my) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Evaluate App Lock status
        viewModelScope.launch {
            val settings = repository.getSettings()
            if (!settings.appLockEnabled || settings.appLockPin.isNullOrBlank()) {
                isAppUnlocked.value = true
            }

            // Check for surviving backup on first launch (Restore-on-reinstall detection)
            // If database has no user transactions or custom debts
            try {
                val txs = repository.getTransactionListDirect()
                val debtsCount = repository.getDebtListDirect().size
                if (txs.isEmpty() && debtsCount == 0) {
                    val metadata = BackupRestoreHelper.getLatestBackupMetadata(application)
                    if (metadata != null) {
                        welcomeBackupMetadata.value = metadata
                        showWelcomeBackScreen.value = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreFromWelcomeBackup() {
        viewModelScope.launch {
            try {
                val file = BackupRestoreHelper.getBackupFile(getApplication())
                if (file.exists()) {
                    val backupJson = file.readText()
                    val success = BackupRestoreHelper.restoreDatabaseFromJson(db, backupJson)
                    if (success) {
                        repository.seedDatabaseIfNecessary()
                        isAppUnlocked.value = true
                        showWelcomeBackScreen.value = false
                        successMessage.value = "Welcome back! Your ledger log backup has been restored."
                    } else {
                        errorMessage.value = "Failed to parse historical backup logs."
                    }
                } else {
                    errorMessage.value = "Backup file not found at path."
                }
            } catch (e: Exception) {
                errorMessage.value = "Welcome restore failed: ${e.message}"
            }
        }
    }

    fun startFreshWelcome() {
        viewModelScope.launch {
            showWelcomeBackScreen.value = false
            successMessage.value = "Starting fresh! Custom wallets have been seeded."
        }
    }

    fun clearMessages() {
        errorMessage.value = null
        successMessage.value = null
    }

    private fun startLockoutCountdown(durationMs: Long) {
        viewModelScope.launch {
            val endTime = System.currentTimeMillis() + durationMs
            appLockLockoutUntil.value = endTime
            errorMessage.value = "Too many wrong attempts! Locked for 30 seconds."
            while (System.currentTimeMillis() < endTime) {
                val remainingSec = ((endTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                appLockLockoutRemainingSeconds.value = remainingSec
                kotlinx.coroutines.delay(1000)
            }
            appLockLockoutUntil.value = null
            appLockLockoutRemainingSeconds.value = 0L
            appLockWrongAttempts.value = 0
            appLockPinInput.value = ""
        }
    }

    // PIN passcode unlock
    fun enterPinDigit(digit: String) {
        val lockoutTime = appLockLockoutUntil.value
        if (lockoutTime != null && System.currentTimeMillis() < lockoutTime) {
            errorMessage.value = "Temporarily locked out! Wait for countdown."
            return
        }

        val currentPin = appLockPinInput.value
        if (currentPin.length < 4) {
            val newPin = currentPin + digit
            appLockPinInput.value = newPin
            if (newPin.length == 4) {
                // Verify passcode match
                viewModelScope.launch {
                    val settings = repository.getSettings()
                    val isValid = com.example.utils.PinSecurityHelper.verifyPin(newPin, settings.appLockPin)
                    if (isValid) {
                        isAppUnlocked.value = true
                        appLockWrongAttempts.value = 0
                        appLockPinInput.value = ""
                    } else {
                        val attempts = appLockWrongAttempts.value + 1
                        appLockWrongAttempts.value = attempts
                        appLockPinInput.value = ""
                        
                        if (attempts >= 5) {
                            startLockoutCountdown(30000)
                        } else {
                            val remaining = 5 - attempts
                            errorMessage.value = "Incorrect Lock PIN code! $remaining attempts remaining."
                        }
                    }
                }
            }
        }
    }

    fun deletePinDigit() {
        val lockoutTime = appLockLockoutUntil.value
        if (lockoutTime != null && System.currentTimeMillis() < lockoutTime) {
            return
        }
        if (appLockPinInput.value.isNotEmpty()) {
            appLockPinInput.value = appLockPinInput.value.dropLast(1)
        }
    }

    fun onBiometricUnlockSucceeded() {
        isAppUnlocked.value = true
        appLockWrongAttempts.value = 0
        appLockLockoutUntil.value = null
        appLockPinInput.value = ""
        successMessage.value = "Biometrics verified successfully!"
    }

    fun onBiometricUnlockFailed() {
        errorMessage.value = "Biometric authentication failed. Use PIN instead."
    }

    fun updateAppLockSettings(enabled: Boolean, hashedPin: String?, biometric: Boolean) {
        viewModelScope.launch {
            val oldSettings = repository.getSettings()
            val updated = oldSettings.copy(
                appLockEnabled = enabled,
                appLockPin = hashedPin,
                biometricEnabled = biometric
            )
            repository.updateSettings(updated)
            if (!enabled) {
                isAppUnlocked.value = true
            }
            successMessage.value = if (enabled) "App lock details and PIN updated!" else "App lock disabled completely!"
        }
    }

    // Accounts actions
    fun zeroCashWallet() {
        viewModelScope.launch {
            try {
                val dbInstance = AppDatabase.getDatabase(getApplication())
                val wallet = dbInstance.accountDao().getWalletAccount()
                if (wallet != null) {
                    dbInstance.accountDao().updateAccount(wallet.copy(balance = 0.0))
                    successMessage.value = "Cash Wallet balance reset to zero (Rs.0.00) successfully!"
                    triggerOpportunisticBackup()
                } else {
                    errorMessage.value = "Cash Wallet not found."
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to reset Cash Wallet: ${e.message}"
            }
        }
    }

    fun addBankAccount(name: String, bankName: String, branchName: String, accountNumber: String, openingBalance: Double) {
        viewModelScope.launch {
            repository.addBankAccount(name, bankName, branchName, accountNumber, openingBalance)
                .onSuccess {
                    successMessage.value = "Bank account '$name' added successfully!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun editBankAccount(id: Long, name: String, bankName: String, branchName: String, accountNumber: String) {
        viewModelScope.launch {
            repository.editBankAccount(id, name, bankName, branchName, accountNumber)
                .onSuccess {
                    successMessage.value = "Account updated successfully!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun deleteBankAccount(id: Long) {
        viewModelScope.launch {
            repository.deleteBankAccount(id)
                .onSuccess {
                    successMessage.value = "Bank account deleted!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double, fee: Double, note: String?) {
        viewModelScope.launch {
            repository.transferFunds(fromAccountId, toAccountId, amount, fee, System.currentTimeMillis(), note)
                .onSuccess {
                    successMessage.value = "Transferred Rs.${String.format("%.2f", amount)} successfully!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    // Transactions actions
    fun addTransaction(type: String, amount: Double, accountId: Long, categoryId: Long?, date: Long, note: String?) {
        viewModelScope.launch {
            repository.addTransaction(type, amount, accountId, categoryId, date, note)
                .onSuccess {
                    successMessage.value = "Transaction added successfully!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
                .onSuccess {
                    successMessage.value = "Transaction deleted!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    // Categories
    fun addCategory(name: String, type: String, icon: String, color: String) {
        viewModelScope.launch {
            repository.addCategory(name, type, icon, color)
                .onSuccess {
                    successMessage.value = "Custom Category '$name' added!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
                .onSuccess {
                    successMessage.value = "Category deleted!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    // Debts
    fun addDebt(type: String, personName: String, amount: Double, accountId: Long, fee: Double, dueDate: Long?, description: String?, contactEmail: String?, contactPhone: String?) {
        viewModelScope.launch {
            repository.addDebt(type, personName, amount, accountId, fee, dueDate, description, contactEmail, contactPhone)
                .onSuccess {
                    successMessage.value = "New Loan (${type.lowercase()}) recorded to $personName!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun recordDebtPayment(debtId: Long, paymentAmount: Double, accountId: Long, fee: Double) {
        viewModelScope.launch {
            repository.recordDebtPayment(debtId, paymentAmount, accountId, fee)
                .onSuccess {
                    successMessage.value = "Payment of Rs.${String.format("%.2f", paymentAmount)} recorded successfully!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    // Budgets
    fun updateBudget(categoryId: Long, amount: Double) {
        viewModelScope.launch {
            repository.updateOrCreateBudget(categoryId, currentMonthYear.value, amount)
            successMessage.value = "Budget updated!"
            triggerOpportunisticBackup()
        }
    }

    // Recurring
    fun addRecurringPlan(accountId: Long, categoryId: Long, amount: Double, description: String?, type: String, frequency: String, startDate: Long) {
        viewModelScope.launch {
            repository.addRecurringTransaction(accountId, categoryId, amount, description, type, frequency, startDate)
                .onSuccess {
                    successMessage.value = "Saved recurring schedule rule!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun toggleRecurring(id: Long) {
        viewModelScope.launch {
            repository.toggleRecurringStatus(id)
            successMessage.value = "Recurring cycle status updated."
            triggerOpportunisticBackup()
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            repository.deleteRecurring(id)
            successMessage.value = "Recurring rule deleted."
            triggerOpportunisticBackup()
        }
    }

    fun executeRecurringNow(id: Long) {
        viewModelScope.launch {
            repository.executeRecurringNow(id)
                .onSuccess {
                    successMessage.value = "Recurring task triggered manually!"
                    triggerOpportunisticBackup()
                }
                .onFailure { errorMessage.value = it.message }
        }
    }

    // Settings actions
    fun saveSettings(
        userName: String,
        currencySymbol: String,
        currencyCode: String,
        timezone: String,
        dateFormat: String,
        appLockEnabled: Boolean,
        appLockPin: String?,
        avatarPath: String?,
        debtReminderDaysBefore: Int,
        debtInitialMsg: String,
        debtReminderMsg: String
    ) {
        viewModelScope.launch {
            val oldSettings = repository.getSettings()
            val updated = oldSettings.copy(
                userName = userName,
                currencySymbol = currencySymbol,
                currencyCode = currencyCode,
                timezone = timezone,
                dateFormat = dateFormat,
                appLockEnabled = appLockEnabled,
                appLockPin = appLockPin,
                avatarPath = avatarPath,
                debtReminderDaysBefore = debtReminderDaysBefore,
                debtInitialMessageTemplate = debtInitialMsg,
                debtReminderMessageTemplate = debtReminderMsg
            )
            repository.updateSettings(updated)
            successMessage.value = "Preferences saved cleanly!"
            
            // Adjust local state if app lock toggled off
            if (!appLockEnabled) {
                isAppUnlocked.value = true
            }
        }
    }

    // Google Sign-In and simulated Drive Backups
    fun connectGoogleAccount(email: String) {
        viewModelScope.launch {
            val oldSettings = repository.getSettings()
            repository.updateSettings(oldSettings.copy(googleAccountEmail = email, autoBackupEnabled = true))
            successMessage.value = "Google Account connected: $email"
            triggerOpportunisticBackup()
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch {
            val oldSettings = repository.getSettings()
            repository.updateSettings(oldSettings.copy(googleAccountEmail = null, autoBackupEnabled = false))
            successMessage.value = "Google Account disconnected!"
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val oldSettings = repository.getSettings()
            repository.updateSettings(oldSettings.copy(autoBackupEnabled = enabled))
            successMessage.value = if (enabled) "Auto Backup enabled!" else "Auto Backup disabled."
            if (enabled) {
                triggerOpportunisticBackup()
            }
        }
    }

    private var lastOpportunisticBackupTime = 0L

    fun triggerOpportunisticBackup() {
        val now = System.currentTimeMillis()
        if (now - lastOpportunisticBackupTime < 3 * 60 * 1000) {
            // Debounced to once every 3 minutes to avoid overhead
            return
        }
        viewModelScope.launch {
            val settings = repository.getSettings()
            if (settings.googleAccountEmail != null && settings.autoBackupEnabled) {
                try {
                    val backupJson = BackupRestoreHelper.exportDatabaseToJson(db)
                    BackupRestoreHelper.saveBackup(getApplication(), backupJson)
                    repository.updateSettings(settings.copy(lastBackupAt = System.currentTimeMillis()))
                    lastOpportunisticBackupTime = System.currentTimeMillis()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun backupDatabaseToDrive() {
        viewModelScope.launch {
            val settings = repository.getSettings()
            if (settings.googleAccountEmail == null) {
                errorMessage.value = "Please connect your Google Account first in Settings!"
                return@launch
            }

            try {
                // Generate database payload
                val backupJson = BackupRestoreHelper.exportDatabaseToJson(db)
                // Save using the robust file persistent location + rolling history
                BackupRestoreHelper.saveBackup(getApplication(), backupJson)

                val curTime = System.currentTimeMillis()
                repository.updateSettings(settings.copy(lastBackupAt = curTime))
                successMessage.value = "Backup successfully uploaded to your Google Drive folder!"
            } catch (e: Exception) {
                errorMessage.value = "Drive backup failed: ${e.message}"
            }
        }
    }

    fun restoreDatabaseFromDrive() {
        viewModelScope.launch {
            val settings = repository.getSettings()
            if (settings.googleAccountEmail == null) {
                errorMessage.value = "Please connect your Google Account first in Settings!"
                return@launch
            }

            try {
                // Fetch the simulated latest backup file
                val file = BackupRestoreHelper.getBackupFile(getApplication())
                if (!file.exists()) {
                    // Try to search in legacy directory
                    val legacyFile = File(getApplication<Application>().filesDir, "GoogleDriveBackup.json")
                    if (legacyFile.exists()) {
                        legacyFile.copyTo(file)
                    } else {
                        errorMessage.value = "No backup file found in your Google Drive 'drive.appdata' folder!"
                        return@launch
                    }
                }

                val backupJson = file.readText()
                val success = BackupRestoreHelper.restoreDatabaseFromJson(db, backupJson)
                if (success) {
                    // Seed defaults if everything wiped out
                    repository.seedDatabaseIfNecessary()
                    successMessage.value = "Database restored successfully from Google Drive backup!"
                } else {
                    errorMessage.value = "Failed to parse backup payload. Payload may be corrupt."
                }
            } catch (e: Exception) {
                errorMessage.value = "Restoration failed: ${e.message}"
            }
        }
    }

    // Local manual backup file Export and Import via SAF (Storage Access Framework) Urises
    fun exportBackupToUri(uri: android.net.Uri, context: Context) {
        viewModelScope.launch {
            try {
                val backupJson = BackupRestoreHelper.exportDatabaseToJson(db)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(backupJson.toByteArray(Charsets.UTF_8))
                }
                successMessage.value = "Database backup exported successfully!"
            } catch (e: Exception) {
                errorMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importBackupFromUri(uri: android.net.Uri, context: Context) {
        viewModelScope.launch {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (jsonStr.isNullOrBlank()) {
                    errorMessage.value = "The selected backup file is empty."
                    return@launch
                }
                val success = BackupRestoreHelper.restoreDatabaseFromJson(db, jsonStr)
                if (success) {
                    repository.seedDatabaseIfNecessary()
                    successMessage.value = "Database restored successfully from local backup file!"
                    isAppUnlocked.value = true
                } else {
                    errorMessage.value = "Failed to parse backup payload. File may be corrupted."
                }
            } catch (e: Exception) {
                errorMessage.value = "Import failed: ${e.message}"
            }
        }
    }
}
