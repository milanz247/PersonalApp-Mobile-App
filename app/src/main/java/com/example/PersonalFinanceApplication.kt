package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import com.example.workers.AppWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PersonalFinanceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize DB and run initial seeding inside background thread
        val db = AppDatabase.getDatabase(this)
        val repo = AppRepository(db)

        CoroutineScope(Dispatchers.IO).launch {
            repo.seedDatabaseIfNecessary()
        }

        // 2. Register WorkManager tasks
        AppWorkManager.schedulePeriodicJobs(this)
    }
}
