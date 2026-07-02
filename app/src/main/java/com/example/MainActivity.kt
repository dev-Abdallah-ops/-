package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.FinanceRepository
import com.example.ui.FinanceAppUI
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.RecurringTransactionsWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SQLite Room Local Database via Singleton
        val db = AppDatabase.getDatabase(applicationContext)

        val repository = FinanceRepository(db.financeDao())
        val factory = FinanceViewModelFactory(application, repository)

        // Enqueue background processing of recurring transactions via WorkManager
        try {
            val recurringWorkRequest = PeriodicWorkRequestBuilder<RecurringTransactionsWorker>(
                12, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "RecurringTransactionsWork",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringWorkRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: FinanceViewModel = viewModel(factory = factory)
            FinanceAppUI(viewModel = viewModel)
        }
    }
}
