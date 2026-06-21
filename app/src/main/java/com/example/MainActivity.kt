package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.FinanceRepository
import com.example.ui.FinanceAppUI
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SQLite Room Local Database
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "finance-database"
        ).fallbackToDestructiveMigration().build()

        val repository = FinanceRepository(db.financeDao())
        val factory = FinanceViewModelFactory(application, repository)

        enableEdgeToEdge()
        setContent {
            val viewModel: FinanceViewModel = viewModel(factory = factory)
            FinanceAppUI(viewModel = viewModel)
        }
    }
}
