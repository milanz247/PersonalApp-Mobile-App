package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class RecurringTransactionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val repo = AppRepository(db)

        val activeList = repo.recurringDao.getActiveRecurring()
        val todayMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        for (rec in activeList) {
            if (rec.nextDate <= todayMillis) {
                repo.triggerRecurringExecution(rec)
                showRecurringNotification(rec.id, rec.description ?: "Scheduled Task", rec.amount)
            }
        }
        return Result.success()
    }

    private fun showRecurringNotification(recurringId: Long, name: String, amount: Double) {
        val channelId = "recurring_transactions"
        val notificationId = recurringId.toInt() + 20000

        // Create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Recurring Transactions"
            val descriptionText = "Notifications for automatically processed recurring transactions."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            recurringId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "Automatically processed: $name for Rs.${String.format("%.2f", amount)}"
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recurring Transaction Executed")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission checked or neglected on older builds
        }
    }
}

class DebtReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val repo = AppRepository(db)

        val settings = repo.getSettings()
        val allDebts = repo.debtDao.getAllDebts()
        val today = LocalDate.now()

        val warningDays = settings.debtReminderDaysBefore
        var reminderCount = 0

        for (debt in allDebts) {
            if (debt.type == "LENT" && (debt.status == "PENDING" || debt.status == "PARTIALLY_PAID") && debt.dueDate != null) {
                val dueLocalDate = java.time.Instant.ofEpochMilli(debt.dueDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, dueLocalDate)
                if (daysRemaining in 0..warningDays) {
                    showDebtNotification(debt.id, debt.personName, debt.remainingAmount, daysRemaining)
                    reminderCount++
                }
            }
        }

        return Result.success()
    }

    private fun showDebtNotification(debtId: Long, personName: String, remainingAmount: Double, days: Long) {
        val channelId = "debt_reminders"
        val notificationId = debtId.toInt() + 10000

        // Create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Debt Reminders"
            val descriptionText = "Notifications for money lent that is due soon."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("DEBT_ID", debtId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            debtId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "Loan of Rs.${String.format("%.2f", remainingAmount)} to $personName is due in $days days! Tap to send a reminder."
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Repayment Due Soon!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            // check permission but since it's already declared in manifest we can notify safely
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission checked or neglected on older builds
        }
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val repo = AppRepository(db)

        val settings = repo.getSettings()
        if (settings.autoBackupEnabled) {
            try {
                val backupJson = com.example.utils.BackupRestoreHelper.exportDatabaseToJson(db)
                com.example.utils.BackupRestoreHelper.saveBackup(applicationContext, backupJson, settings.userName)
                repo.updateSettings(settings.copy(lastBackupAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                e.printStackTrace()
                return Result.retry()
            }
        }
        return Result.success()
    }
}

object AppWorkManager {
    fun schedulePeriodicJobs(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val recurringWork = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        val debtWork = PeriodicWorkRequestBuilder<DebtReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        val backupConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val autoBackupWork = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(backupConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "recurring_transactions",
            ExistingPeriodicWorkPolicy.KEEP,
            recurringWork
        )

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "debt_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            debtWork
        )

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "auto_backup",
            ExistingPeriodicWorkPolicy.KEEP,
            autoBackupWork
        )
    }
}
