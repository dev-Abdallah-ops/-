package com.example

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository
) : AndroidViewModel(application) {

    // Main database states
    val incomes = repository.allIncomes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bills = repository.allBills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals = repository.allGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customCategories = repository.customCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Settings reactive state (Defaults to Dark, English, USD)
    val settings = repository.settings.map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // AI advisor states
    private val _aiInsight = MutableStateFlow<String?>(null)
    val aiInsight: StateFlow<String?> = _aiInsight.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // Export & Restoring operational logs
    private val _operationalLog = MutableStateFlow<String?>(null)
    val operationalLog: StateFlow<String?> = _operationalLog.asStateFlow()

    // Secure Balance Visibility State
    private val _isBalanceHidden = MutableStateFlow(false)
    val isBalanceHidden: StateFlow<Boolean> = _isBalanceHidden.asStateFlow()

    fun toggleBalanceHidden() {
        _isBalanceHidden.value = !_isBalanceHidden.value
    }

    init {
        // Automatically fetch insights on startup or user requests
        viewModelScope.launch {
            // Trigger initial default settings row if database has none
            repository.settings.firstOrNull()?.let {
                // Settings exists already
            } ?: run {
                repository.insertSettings(AppSettings())
            }
        }
    }

    // --- Active Formatting Symbols ---
    fun getCurrencySymbol(): String {
        return when (settings.value.currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "SAR" -> "SR"
            "AED" -> "AED"
            "CNY" -> "¥"
            "INR" -> "₹"
            "CAD" -> "C$"
            "AUD" -> "A$"
            "CHF" -> "Fr"
            "BRL" -> "R$"
            "KWD" -> "KD"
            "MXN" -> "MX$"
            "EGP" -> "ج.م"
            else -> "$"
        }
    }

    fun formatAmount(amount: Double): String {
        val symbol = getCurrencySymbol()
        val isEgp = settings.value.currency == "EGP"
        return if (isEgp) {
            "%.2f %s".format(amount, symbol)
        } else {
            "%s%,.2f".format(symbol, amount)
        }
    }

    // --- Dynamic Analytics & Computations ---
    val totalIncome = incomes.map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpenses = expenses.map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBills = bills.map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalGoalsSaved = goals.map { list -> list.sumOf { it.savedAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netBalance = combine(totalIncome, totalExpenses) { inc, exp -> inc - exp }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val unpaidBillsAmount = bills.map { list -> list.filter { !it.isPaid }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netWorth = combine(netBalance, unpaidBillsAmount) { balance, unpaid ->
        balance - unpaid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val topSpendingCategory = expenses.map { list ->
        if (list.isEmpty()) {
            "None"
        } else {
            list.groupBy { it.category }
                .maxByOrNull { entry -> entry.value.sumOf { it.amount } }
                ?.key ?: "Other"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Other")

    // Realistic Financial Health Score out of 100
    val financialHealthScore = combine(totalIncome, totalExpenses, settings, goals, bills) { inc, exp, sett, gList, bList ->
        var score = 80
        
        // Expense ratio impact
        if (inc > 0) {
            val ratio = exp / inc
            if (ratio > 1.0) {
                score -= 30 // spending more than income
            } else if (ratio > 0.8) {
                score -= 15 // spending too high
            } else if (ratio < 0.5) {
                score += 10 // healthy savings margin
            }
        } else if (exp > 0) {
            score -= 40 // expenses but zero income
        }

        // Budget Limit Impact
        if (sett.monthlyLimit > 0.0 && exp > sett.monthlyLimit) {
            score -= 15
        }

        // Goals progress check
        if (gList.isNotEmpty()) {
            val progressSum = gList.sumOf { it.savedAmount }
            val targetSum = gList.sumOf { it.targetAmount }
            if (targetSum > 0.0) {
                val percent = progressSum / targetSum
                if (percent > 0.5) {
                    score += 10
                }
            }
        }

        // Unpaid bills impact
        val unpaidCount = bList.count { !it.isPaid }
        score -= (unpaidCount * 3)

        // Constraining
        score.coerceIn(0, 100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 80)

    // --- Actionable Methods ---

    // Incomes
    fun addIncome(name: String, amount: Double, category: String, dateStr: String, isRecurring: Boolean, notes: String) {
        viewModelScope.launch {
            repository.insertIncome(
                Income(
                    name = name.ifBlank { "Income Source" },
                    amount = amount,
                    category = category,
                    timestamp = parseDateToTimestamp(dateStr),
                    isRecurring = isRecurring,
                    notes = notes
                )
            )
            refreshAiInsights()
        }
    }

    fun updateIncome(id: Int, name: String, amount: Double, category: String, dateStr: String, isRecurring: Boolean, notes: String) {
        viewModelScope.launch {
            repository.updateIncome(
                Income(
                    id = id,
                    name = name,
                    amount = amount,
                    category = category,
                    timestamp = parseDateToTimestamp(dateStr),
                    isRecurring = isRecurring,
                    notes = notes
                )
            )
            refreshAiInsights()
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            repository.deleteIncome(income)
            refreshAiInsights()
        }
    }

    // Expenses
    fun addExpense(name: String, amount: Double, category: String, dateStr: String, notes: String) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    name = name.ifBlank { "Expense" },
                    amount = amount,
                    category = category,
                    timestamp = parseDateToTimestamp(dateStr),
                    notes = notes
                )
            )
            refreshAiInsights()
        }
    }

    fun updateExpense(id: Int, name: String, amount: Double, category: String, dateStr: String, notes: String) {
        viewModelScope.launch {
            repository.updateExpense(
                Expense(
                    id = id,
                    name = name,
                    amount = amount,
                    category = category,
                    timestamp = parseDateToTimestamp(dateStr),
                    notes = notes
                )
            )
            refreshAiInsights()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            refreshAiInsights()
        }
    }

    // Bills
    fun addBill(name: String, amount: Double, frequency: String, dueDay: Int, category: String, notes: String) {
        viewModelScope.launch {
            repository.insertBill(
                Bill(
                    name = name.ifBlank { "Bill" },
                    amount = amount,
                    frequency = frequency,
                    dueDay = dueDay.coerceIn(1, 31),
                    category = category,
                    notes = notes,
                    isPaid = false
                )
            )
            refreshAiInsights()
        }
    }

    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            repository.updateBill(bill)
            refreshAiInsights()
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            refreshAiInsights()
        }
    }

    // Goals
    fun addGoal(name: String, targetAmount: Double, deadline: String, colorHex: String, iconName: String, notes: String) {
        viewModelScope.launch {
            repository.insertGoal(
                Goal(
                    name = name.ifBlank { "Savings Goal" },
                    targetAmount = targetAmount,
                    savedAmount = 0.0,
                    deadline = deadline,
                    colorHex = colorHex,
                    iconName = iconName,
                    notes = notes
                )
            )
            refreshAiInsights()
        }
    }

    fun saveGoalAmount(goal: Goal, incrementalAmount: Double) {
        viewModelScope.launch {
            val updatedSaved = goal.savedAmount + incrementalAmount
            repository.updateGoal(goal.copy(savedAmount = updatedSaved.coerceAtLeast(0.0)))
            refreshAiInsights()
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            refreshAiInsights()
        }
    }

    // Settings
    fun updateSettings(theme: String, language: String, currency: String, monthlyLimit: Double) {
        viewModelScope.launch {
            val currentSettings = settings.value
            repository.insertSettings(
                currentSettings.copy(
                    theme = theme,
                    language = language,
                    currency = currency,
                    monthlyLimit = monthlyLimit
                )
            )
            // Re-fetch insights if language/currency changed
            refreshAiInsights()
        }
    }

    fun updateBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            repository.insertSettings(
                currentSettings.copy(isBiometricLockEnabled = enabled)
            )
        }
    }

    fun updateBackupPin(pin: String) {
        viewModelScope.launch {
            val currentSettings = settings.value
            repository.insertSettings(
                currentSettings.copy(backupPin = pin)
            )
        }
    }

    // Custom Category
    fun addCustomCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.insertCustomCategory(
                CustomCategory(
                    name = name,
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
        }
    }

    fun deleteCustomCategory(category: CustomCategory) {
        viewModelScope.launch {
            repository.deleteCustomCategory(category)
        }
    }

    // Clean Workspace
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _aiInsight.value = null
            _operationalLog.value = "All localized data records updated successfully."
        }
    }

    // --- CSV & JSON DATA PORTABILITY LOGIC ---

    fun exportToCSV(): String {
        val sb = java.lang.StringBuilder()
        sb.append("Type,Name,Amount,Category,DateOrSchedule,Notes\n")
        incomes.value.forEach {
            sb.append("Income,\"${it.name}\",${it.amount},\"${it.category}\",\"${formatTimestampToDate(it.timestamp)}\",\"${it.notes}\"\n")
        }
        expenses.value.forEach {
            sb.append("Expense,\"${it.name}\",${it.amount},\"${it.category}\",\"${formatTimestampToDate(it.timestamp)}\",\"${it.notes}\"\n")
        }
        bills.value.forEach {
            sb.append("Bill,\"${it.name}\",${it.amount},\"${it.category}\",\"Day ${it.dueDay} (${it.frequency})\",\"${it.notes}\"\n")
        }
        val dataStr = sb.toString()
        _operationalLog.value = "CSV report exported successfully with ${incomes.value.size + expenses.value.size + bills.value.size} active rows."
        return dataStr
    }

    fun importFromCSV(csvText: String) {
        viewModelScope.launch {
            try {
                val lines = csvText.lines()
                var count = 0
                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.isBlank()) continue
                    val parts = line.split(",")
                    if (parts.size >= 4) {
                        val type = parts[0].trim()
                        val name = parts[1].trim().replace("\"", "")
                        val amount = parts[2].trim().toDoubleOrNull() ?: 0.0
                        val category = parts[3].trim().replace("\"", "")
                        val dateOrSchedule = if (parts.size > 4) parts[4].trim().replace("\"", "") else ""
                        val notes = if (parts.size > 5) parts[5].trim().replace("\"", "") else ""

                        when (type) {
                            "Income" -> {
                                repository.insertIncome(Income(name = name, amount = amount, category = category, notes = notes))
                                count++
                            }
                            "Expense" -> {
                                repository.insertExpense(Expense(name = name, amount = amount, category = category, notes = notes))
                                count++
                            }
                            "Bill" -> {
                                repository.insertBill(Bill(name = name, amount = amount, frequency = "Monthly", dueDay = 28, category = category, notes = notes))
                                count++
                            }
                        }
                    }
                }
                _operationalLog.value = "CSV Imported Successfully! Hydrated $count financial transactions."
            } catch (e: Exception) {
                _operationalLog.value = "Failed parsing CSV dataset: ${e.localizedMessage}"
            }
        }
    }

    fun exportToJson(): String {
        return try {
            val root = JSONObject()
            
            val incArr = JSONArray()
            incomes.value.forEach {
                val obj = JSONObject().apply {
                    put("name", it.name)
                    put("amount", it.amount)
                    put("category", it.category)
                    put("timestamp", it.timestamp)
                    put("isRecurring", it.isRecurring)
                    put("notes", it.notes)
                }
                incArr.put(obj)
            }
            root.put("incomes", incArr)

            val expArr = JSONArray()
            expenses.value.forEach {
                val obj = JSONObject().apply {
                    put("name", it.name)
                    put("amount", it.amount)
                    put("category", it.category)
                    put("timestamp", it.timestamp)
                    put("notes", it.notes)
                }
                expArr.put(obj)
            }
            root.put("expenses", expArr)

            val billsArr = JSONArray()
            bills.value.forEach {
                val obj = JSONObject().apply {
                    put("name", it.name)
                    put("amount", it.amount)
                    put("frequency", it.frequency)
                    put("dueDay", it.dueDay)
                    put("category", it.category)
                    put("notes", it.notes)
                    put("isPaid", it.isPaid)
                }
                billsArr.put(obj)
            }
            root.put("bills", billsArr)

            val goalsArr = JSONArray()
            goals.value.forEach {
                val obj = JSONObject().apply {
                    put("name", it.name)
                    put("targetAmount", it.targetAmount)
                    put("savedAmount", it.savedAmount)
                    put("deadline", it.deadline)
                    put("colorHex", it.colorHex)
                    put("iconName", it.iconName)
                    put("notes", it.notes)
                }
                goalsArr.put(obj)
            }
            root.put("goals", goalsArr)

            _operationalLog.value = "JSON Database Backup exported successfully."
            root.toString(2)
        } catch (e: Exception) {
            "Error rendering JSON: ${e.message}"
        }
    }

    fun importFromJson(jsonStr: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonStr)
                var count = 0

                if (root.has("incomes")) {
                    val arr = root.getJSONArray("incomes")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertIncome(
                            Income(
                                name = obj.optString("name", "Income"),
                                amount = obj.optDouble("amount", 0.0),
                                category = obj.optString("category", "Other"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                isRecurring = obj.optBoolean("isRecurring", false),
                                notes = obj.optString("notes", "")
                            )
                        )
                        count++
                    }
                }

                if (root.has("expenses")) {
                    val arr = root.getJSONArray("expenses")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertExpense(
                            Expense(
                                name = obj.optString("name", "Expense"),
                                amount = obj.optDouble("amount", 0.0),
                                category = obj.optString("category", "Other"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                notes = obj.optString("notes", "")
                            )
                        )
                        count++
                    }
                }

                if (root.has("bills")) {
                    val arr = root.getJSONArray("bills")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertBill(
                            Bill(
                                name = obj.optString("name", "Bill"),
                                amount = obj.optDouble("amount", 0.0),
                                frequency = obj.optString("frequency", "Monthly"),
                                dueDay = obj.optInt("dueDay", 28),
                                category = obj.optString("category", "Other"),
                                notes = obj.optString("notes", ""),
                                isPaid = obj.optBoolean("isPaid", false)
                            )
                        )
                        count++
                    }
                }

                if (root.has("goals")) {
                    val arr = root.getJSONArray("goals")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertGoal(
                            Goal(
                                name = obj.optString("name", "Goal"),
                                targetAmount = obj.optDouble("targetAmount", 1000.0),
                                savedAmount = obj.optDouble("savedAmount", 0.0),
                                deadline = obj.optString("deadline", ""),
                                colorHex = obj.optString("colorHex", "#2ECC71"),
                                iconName = obj.optString("iconName", "ic_car"),
                                notes = obj.optString("notes", "")
                            )
                        )
                        count++
                    }
                }

                _operationalLog.value = "JSON dataset backup restored successfully. Re-populated $count elements."
            } catch (e: Exception) {
                _operationalLog.value = "Failed to restore backup: ${e.localizedMessage}"
            }
        }
    }

    fun clearOperationalLog() {
        _operationalLog.value = null
    }

    // --- AI Insight Refreshing Trigger ---
    fun refreshAiInsights() {
        viewModelScope.launch {
            _aiLoading.value = true
            try {
                val insight = GeminiAdvisor.getFinancialInsights(
                    incomeTotal = totalIncome.value,
                    expenseTotal = totalExpenses.value,
                    billsTotal = totalBills.value,
                    goalsTotal = totalGoalsSaved.value,
                    remainingLimit = if (settings.value.monthlyLimit > 0) (settings.value.monthlyLimit - totalExpenses.value) else 0.0,
                    topSpendingCategory = topSpendingCategory.value,
                    currencySymbol = getCurrencySymbol(),
                    languageName = settings.value.language
                )
                _aiInsight.value = insight
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Error fetching AI Advice", e)
            } finally {
                _aiLoading.value = false
            }
        }
    }

    // --- DateTime Helpers ---
    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val d = sdf.parse(dateStr)
            d?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun formatTimestampToDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date(timestamp))
    }
}
