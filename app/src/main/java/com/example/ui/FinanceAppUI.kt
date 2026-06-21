package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FinanceViewModel
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAppUI(viewModel: FinanceViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe DB States
    val incomes by viewModel.incomes.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val appSettings by viewModel.settings.collectAsState()

    // Observe Computed Stats
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val totalBills by viewModel.totalBills.collectAsState()
    val totalGoalsSaved by viewModel.totalGoalsSaved.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    val unpaidBillsAmount by viewModel.unpaidBillsAmount.collectAsState()
    val netWorth by viewModel.netWorth.collectAsState()
    val healthScore by viewModel.financialHealthScore.collectAsState()
    val topSpendingCategory by viewModel.topSpendingCategory.collectAsState()

    // Active custom date selectors for easy logging
    val todayDateStr = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        sdf.format(java.util.Date())
    }
    var selectedIncomeDate by remember { mutableStateOf(todayDateStr) }
    var selectedExpenseDate by remember { mutableStateOf(todayDateStr) }

    // Observe AI Advisor
    val aiInsight by viewModel.aiInsight.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val operationalLog by viewModel.operationalLog.collectAsState()

    // Navigation and Menu states
    var currentTab by remember { mutableStateOf("overview") } // overview, income, expenses, bills, calendar, goals, settings, search
    var showNavigatorMenu by remember { mutableStateOf(false) } // Open quick navigation sheet

    // Forms triggers and editing data holders
    var activeIncomeToEdit by remember { mutableStateOf<Income?>(null) }
    var activeExpenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var activeBillToEdit by remember { mutableStateOf<Bill?>(null) }
    var activeGoalToFund by remember { mutableStateOf<Goal?>(null) }

    var showAddIncome by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddBill by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showCustomCategoryCreator by remember { mutableStateOf(false) }

    // Quick filter states for Search/Filter screen
    var searchQuery by remember { mutableStateOf("") }
    var searchTabFilter by remember { mutableStateOf("All") } // All, Income, Expenses

    // Quick Sheets
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    // Alert Triggers
    LaunchedEffect(operationalLog) {
        operationalLog?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearOperationalLog()
        }
    }

    val isArabic = appSettings.language == "Arabic"
    var isAppUnlocked by remember { mutableStateOf(false) }
    var showBiometricAuthPrompt by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanSuccess by remember { mutableStateOf(false) }

    MyApplicationTheme(themePreference = appSettings.theme) {
        if (appSettings.isBiometricLockEnabled && !isAppUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight().padding(vertical = 48.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(PremiumAccentMint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = PremiumAccentMint,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tawffer",
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isArabic) "وفّر أكتر مع لمسة أمان ذكية" else "Save more with smart, secure touches",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                isScanning = false
                                scanSuccess = false
                                showBiometricAuthPrompt = true
                            }
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(PremiumAccentMint.copy(alpha = 0.1f))
                                .border(1.5.dp, PremiumAccentMint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Unlock App",
                                tint = PremiumAccentMint,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isArabic) "اضغط هنا لفتح التطبيق بالبصمة" else "Tap to unlock Tawffer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PremiumAccentMint
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isArabic) "البيانات مشفرة ومحميّة بالكامل على جهازك" else "Data is fully encrypted and locally stored on your device",
                            fontSize = 11.sp,
                            color = Color.Gray.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            if (showBiometricAuthPrompt) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PremiumAccentMint,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isArabic) "إثبات الهوية لـ Tawffer" else "Verify your identity for Tawffer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) 
                                    "يرجى استخدام مستشعر البصمة أو الضغط لتأكيد الهوية البيومترية الآمنة والدخول للتطبيق."
                                    else "Please touch the fingerprint sensor or confirm to securely log into your account.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            LaunchedEffect(isScanning) {
                                if (isScanning) {
                                    kotlinx.coroutines.delay(1000)
                                    scanSuccess = true
                                    kotlinx.coroutines.delay(450)
                                    isAppUnlocked = true
                                    showBiometricAuthPrompt = false
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (scanSuccess) PremiumAccentMint.copy(alpha = 0.2f)
                                        else if (isScanning) PremiumAccentPurple.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    )
                                    .clickable(enabled = !isScanning) {
                                        isScanning = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (scanSuccess) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Success",
                                        tint = PremiumAccentMint,
                                        modifier = Modifier.size(38.dp)
                                    )
                                } else if (isScanning) {
                                    CircularProgressIndicator(
                                        color = PremiumAccentPurple,
                                        modifier = Modifier.size(48.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Tap to Scan",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (scanSuccess) 
                                    (if (isArabic) "تم التحقق بنجاح!" else "Identity Verified!") 
                                    else if (isScanning) (if (isArabic) "جاري المسح الحركي..." else "Scanning fingerprint...")
                                    else (if (isArabic) "اضغط هنا لتأكيد البصمة والمسح" else "Tap here to confirm & simulate scan"),
                                fontSize = 12.sp,
                                color = if (scanSuccess) PremiumAccentMint else if (isScanning) PremiumAccentPurple else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showBiometricAuthPrompt = false }
                                ) {
                                    Text(
                                        text = if (isArabic) "إلغاء البصمة" else "Cancel",
                                        color = PremiumAccentRed
                                    )
                                }

                                TextButton(
                                    onClick = { 
                                        isAppUnlocked = true
                                        showBiometricAuthPrompt = false
                                        android.widget.Toast.makeText(context, if (isArabic) "تم الدخول برمز المرور والنسخ الاحتياطي" else "Logged in via backup passcode", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = if (isArabic) "رمز المرور الاحتياطي" else "Use Backup PIN",
                                        color = PremiumAccentMint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                "overview" -> if (appSettings.language == "Arabic") "الملخص المالي" else "Finance Overview"
                                "income" -> if (appSettings.language == "Arabic") "الدخل الوارد" else "Income"
                                "expenses" -> if (appSettings.language == "Arabic") "المصاريف" else "Expenses"
                                "bills" -> if (appSettings.language == "Arabic") "الفواتير" else "Recurring Bills"
                                "calendar" -> if (appSettings.language == "Arabic") "تقويم المعاملات" else "Financial Calendar"
                                "goals" -> if (appSettings.language == "Arabic") "أهداف الادخار" else "Savings Goals"
                                "settings" -> if (appSettings.language == "Arabic") "الإعدادات" else "Settings"
                                "search" -> if (appSettings.language == "Arabic") "البحث والفرز" else "Search & Filter"
                                "transactions" -> if (appSettings.language == "Arabic") "سجل المعاملات الكلي" else "Transaction History"
                                else -> "Finance Tracker"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showNavigatorMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Navigation", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(onClick = { currentTab = "search" }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { currentTab = "settings" }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.96f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PremiumTabItem(
                                selected = currentTab == "overview",
                                onClick = { currentTab = "overview" },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Overview",
                                        tint = if (currentTab == "overview") PremiumAccentMint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                label = if (appSettings.language == "Arabic") "الملخص" else "Overview",
                                activeColor = PremiumAccentMint
                            )
                            PremiumTabItem(
                                selected = currentTab == "income",
                                onClick = { currentTab = "income" },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Income",
                                        tint = if (currentTab == "income") PremiumAccentMint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                label = if (appSettings.language == "Arabic") "الدخل" else "Income",
                                activeColor = PremiumAccentMint
                            )
                            PremiumTabItem(
                                selected = currentTab == "expenses",
                                onClick = { currentTab = "expenses" },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Expenses",
                                        tint = if (currentTab == "expenses") PremiumAccentRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                label = if (appSettings.language == "Arabic") "المصاريف" else "Expenses",
                                activeColor = PremiumAccentRed
                            )
                            PremiumTabItem(
                                selected = currentTab == "bills",
                                onClick = { currentTab = "bills" },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Bills",
                                        tint = if (currentTab == "bills") PremiumAccentPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                label = if (appSettings.language == "Arabic") "الفواتير" else "Bills",
                                activeColor = PremiumAccentPurple
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main navigation layout views
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val tabs = listOf("overview", "income", "expenses", "bills")
                        val fromIndex = tabs.indexOf(initialState)
                        val toIndex = tabs.indexOf(targetState)
                        if (toIndex > fromIndex) {
                            (slideInHorizontally(animationSpec = tween(280)) { width -> (width * 0.12f).toInt() } + fadeIn(animationSpec = tween(280)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> (-width * 0.12f).toInt() } + fadeOut(animationSpec = tween(150)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(280)) { width -> (-width * 0.12f).toInt() } + fadeIn(animationSpec = tween(280)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> (width * 0.12f).toInt() } + fadeOut(animationSpec = tween(150)))
                        }
                    },
                    label = "tab_transition"
                ) { targetScreen ->
                    when (targetScreen) {
                        "overview" -> OverviewScreen(
                            viewModel = viewModel,
                            totalIncome = totalIncome,
                            totalExpenses = totalExpenses,
                            totalBills = totalBills,
                            totalGoalsSaved = totalGoalsSaved,
                            netBalance = netBalance,
                            unpaidBillsAmount = unpaidBillsAmount,
                            netWorth = netWorth,
                            healthScore = healthScore,
                            topSpending = topSpendingCategory,
                            customCategories = customCategories,
                            appSettings = appSettings,
                            aiInsight = aiInsight,
                            aiLoading = aiLoading,
                            onChangeTab = { currentTab = it },
                            onQuickAddIncome = { showAddIncome = true },
                            onQuickAddExpense = { showAddExpense = true }
                        )
                        "income" -> IncomeScreen(
                            viewModel = viewModel,
                            incomes = incomes,
                            appSettings = appSettings,
                            selectedDateStr = selectedIncomeDate,
                            onDateSelected = { selectedIncomeDate = it },
                            onAddClick = { showAddIncome = true },
                            onEditClick = { activeIncomeToEdit = it }
                        )
                        "expenses" -> ExpensesScreen(
                            viewModel = viewModel,
                            expenses = expenses,
                            customCategories = customCategories,
                            appSettings = appSettings,
                            selectedDateStr = selectedExpenseDate,
                            onDateSelected = { selectedExpenseDate = it },
                            onAddClick = { showAddExpense = true },
                            onEditClick = { activeExpenseToEdit = it }
                        )
                        "bills" -> BillsScreen(
                            viewModel = viewModel,
                            bills = bills,
                            appSettings = appSettings,
                            onAddClick = { showAddBill = true },
                            onEditClick = { activeBillToEdit = it }
                        )
                        "calendar" -> CalendarScreen(
                            viewModel = viewModel,
                            incomes = incomes,
                            expenses = expenses,
                            appSettings = appSettings
                        )
                        "goals" -> GoalsScreen(
                            viewModel = viewModel,
                            goals = goals,
                            appSettings = appSettings,
                            onAddClick = { showAddGoal = true },
                            onFundClick = { activeGoalToFund = it }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            appSettings = appSettings,
                            customCategories = customCategories,
                            onShowLanguage = { showLanguageSheet = true },
                            onShowCurrency = { showCurrencySheet = true },
                            onShowLimit = { showLimitDialog = true },
                            onShowAddCategory = { showCustomCategoryCreator = true },
                            onShowClearData = { showClearDataDialog = true }
                        )
                        "search" -> SearchFilterScreen(
                            viewModel = viewModel,
                            incomes = incomes,
                            expenses = expenses,
                            appSettings = appSettings,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            searchTabFilter = searchTabFilter,
                            onFilterChange = { searchTabFilter = it },
                            onEditIncome = { activeIncomeToEdit = it },
                            onEditExpense = { activeExpenseToEdit = it }
                        )
                        "transactions" -> TransactionHistoryScreen(
                            viewModel = viewModel,
                            incomes = incomes,
                            expenses = expenses,
                            appSettings = appSettings,
                            customCategories = customCategories,
                            onBackToHome = { currentTab = "overview" }
                        )
                    }
                }

                // --- MODAL SHEETS & INTERACTIVE DIALOG OVERLAYS ---

                // Grid navigator drawer sheet
                if (showNavigatorMenu) {
                    AlertDialog(
                        onDismissRequest = { showNavigatorMenu = false },
                        title = { Text(if (appSettings.language == "Arabic") "انتقال سريع" else "Quick Navigation", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val labels = if (appSettings.language == "Arabic") {
                                    listOf("الملخص", "الدخل الوارد", "المصاريف", "سجل المعاملات", "الفواتير المتكررة", "أهداف الادخار", "تقويم المعاملات", "البحث والفرز", "إعدادات التطبيق")
                                } else {
                                    listOf("Overview", "Income Tracker", "Expenses", "Transaction History", "Recurring Bills", "Savings Goals", "Financial Calendar", "Search & Filter", "Settings Screen")
                                }
                                val tabs = listOf("overview", "income", "expenses", "transactions", "bills", "goals", "calendar", "search", "settings")
                                val icons = listOf(Icons.Default.Home, Icons.Default.KeyboardArrowDown, Icons.Default.KeyboardArrowUp, Icons.Default.List, Icons.Default.DateRange, Icons.Default.Star, Icons.Default.DateRange, Icons.Default.Search, Icons.Default.Settings)

                                tabs.forEachIndexed { index, tab ->
                                    Button(
                                        onClick = {
                                            currentTab = tab
                                            showNavigatorMenu = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentTab == tab) PremiumAccentMint else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Icon(icons[index], contentDescription = null, tint = if (currentTab == tab) Color.Black else MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(labels[index], color = if (currentTab == tab) Color.Black else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showNavigatorMenu = false }) { Text("إغلاق", color = PremiumAccentMint) }
                        }
                    )
                }

                // LANGUAGE BOTTOM SHEET (Dialog visual implementation)
                if (showLanguageSheet) {
                    AlertDialog(
                        onDismissRequest = { showLanguageSheet = false },
                        title = { Text(if (appSettings.language == "Arabic") "اختر لغة واجهة التطبيق" else "Choose Interface Language", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val languages = listOf("English", "Arabic")
                                val codes = listOf("English 🇺🇸", "المصري 🇪🇬")
                                languages.forEachIndexed { idx, lang ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.updateSettings(
                                                        theme = appSettings.theme,
                                                        language = lang,
                                                        currency = appSettings.currency,
                                                        monthlyLimit = appSettings.monthlyLimit
                                                    )
                                                    showLanguageSheet = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (lang == "Arabic" && appSettings.language == "Arabic") "المصري 🇪🇬" else codes[idx], fontWeight = FontWeight.Medium)
                                            if (appSettings.language == lang) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = PremiumAccentMint)
                                            }
                                        }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLanguageSheet = false }) { Text(if (appSettings.language == "Arabic") "إلغاء" else "Cancel", color = Color.Gray) }
                        }
                    )
                }

                // CURRENCY BOTTOM SHEET
                if (showCurrencySheet) {
                    AlertDialog(
                        onDismissRequest = { showCurrencySheet = false },
                        title = { Text(if (appSettings.language == "Arabic") "اختر عملة الحساب" else "Select Account Currency", fontWeight = FontWeight.Bold) },
                        text = {
                            Box(modifier = Modifier.height(300.dp)) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val currencies = listOf("USD", "EUR", "EGP", "SAR", "AED", "KWD")
                                    val desc = listOf("US Dollar ($)", "Euro (€)", "Egyptian Pound (ج.م)", "Saudi Riyal (SAR)", "Emirati Dirham (AED)", "Kuwaiti Dinar (KWD)")
                                    val descAr = listOf("دولار أمريكي ($)", "اليورو الأوروبي (€)", "الجنيه المصري (ج.م)", "الريال السعودي (ر.س)", "الدرهم الإماراتي (د.إ)", "الدينار الكويتي (د.ك)")
                                    items(currencies.zip(if (appSettings.language == "Arabic") descAr else desc)) { (code, textDesc) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.updateSettings(
                                                        theme = appSettings.theme,
                                                        language = appSettings.language,
                                                        currency = code,
                                                        monthlyLimit = appSettings.monthlyLimit
                                                    )
                                                    showCurrencySheet = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(code, fontWeight = FontWeight.Bold, color = PremiumAccentMint)
                                            Text(textDesc, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (appSettings.currency == code) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = PremiumAccentMint)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showCurrencySheet = false }) { Text(if (appSettings.language == "Arabic") "إلغاء" else "Cancel", color = Color.Gray) }
                        }
                    )
                }

                // LIMIT DIALOG
                if (showLimitDialog) {
                    var inputLimit by remember { mutableStateOf(appSettings.monthlyLimit.toString()) }
                    AlertDialog(
                        onDismissRequest = { showLimitDialog = false },
                        title = { Text(if (appSettings.language == "Arabic") "تحديث خط أحمر لميزانيتك" else "Monthly Spending Limit", fontWeight = FontWeight.Bold) },
                        text = {
                            OutlinedTextField(
                                value = inputLimit,
                                onValueChange = { inputLimit = it },
                                label = { Text(if (appSettings.language == "Arabic") "المبلغ الأقصى للمصاريف" else "Maximum spending budget") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val limVal = inputLimit.toDoubleOrNull() ?: 0.0
                                    viewModel.updateSettings(
                                        theme = appSettings.theme,
                                        language = appSettings.language,
                                        currency = appSettings.currency,
                                        monthlyLimit = limVal
                                    )
                                    showLimitDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint)
                            ) { Text(if (appSettings.language == "Arabic") "حفظ" else "Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLimitDialog = false }) { Text("إلغاء", color = Color.Gray) }
                        }
                    )
                }

                // CREATE CUSTOM CATEGORY DIALOG (Page 11)
                if (showCustomCategoryCreator) {
                    var catName by remember { mutableStateOf("") }
                    val colorPalette = listOf("#FF5733", "#E91E63", "#9C27B0", "#3F51B5", "#00BCD4", "#4CAF50", "#FFC107", "#FF9800", "#795548", "#607D8B", "#FF007F", "#00FF7F")
                    var selectedColor by remember { mutableStateOf(colorPalette.first()) }
                    var selectedIcon by remember { mutableStateOf("ic_food") }

                    AlertDialog(
                        onDismissRequest = { showCustomCategoryCreator = false },
                        title = { Text(if (appSettings.language == "Arabic") "إضافة تصنيف مخصص" else "Add Custom Category", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = catName,
                                    onValueChange = { catName = it },
                                    label = { Text(if (appSettings.language == "Arabic") "اسم الفئة" else "Category Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(if (appSettings.language == "Arabic") "اختر لوناً مميزاً:" else "Color selection:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    colorPalette.forEach { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(hex)))
                                                .border(
                                                    2.dp,
                                                    if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                    CircleShape
                                                )
                                                .clickable { selectedColor = hex }
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (catName.isNotBlank()) {
                                        viewModel.addCustomCategory(catName, selectedColor, selectedIcon)
                                    }
                                    showCustomCategoryCreator = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint)
                            ) { Text(if (appSettings.language == "Arabic") "إضافة الفئة" else "Add Category") }
                        }
                    )
                }

                // CLEAR ALL DATA CONFIRMATION DIALOG
                if (showClearDataDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDataDialog = false },
                        title = { Text(if (appSettings.language == "Arabic") "حذف كامل البيانات؟" else "Clear All Data", fontWeight = FontWeight.Bold, color = PremiumAccentRed) },
                        text = {
                            Text(
                                if (appSettings.language == "Arabic") "هذا الإجراء سيقوم بحذف جميع سجلات المعاملات والمدخولات والنفقات والخطط نهائياً من الذاكرة المحلية للتطبيق وبشكل لا يمكن التراجع عنه مطلقاً."
                                else "This will permanently delete all your income, expense, and bill records from Room database. This action is irreversible."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.clearAllData()
                                    showClearDataDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentRed)
                            ) { Text(if (appSettings.language == "Arabic") "نعم، احذف الكل" else "Clear All Data") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDataDialog = false }) { Text("إلغاء", color = Color.Gray) }
                        }
                    )
                }

                // ADD INCOME DIALOG (Page 3)
                if (showAddIncome || activeIncomeToEdit != null) {
                    val editItem = activeIncomeToEdit
                    var name by remember { mutableStateOf(editItem?.name ?: "") }
                    var amount by remember { mutableStateOf(editItem?.amount?.toString() ?: "") }
                    var selectedCategory by remember { mutableStateOf(editItem?.category ?: "Salary") }
                    var isRecurring by remember { mutableStateOf(editItem?.isRecurring ?: false) }
                    var notes by remember { mutableStateOf(editItem?.notes ?: "") }
                    var dateStr by remember { mutableStateOf(editItem?.let { viewModel.formatTimestampToDate(it.timestamp) } ?: selectedIncomeDate) }

                    val isAmountError = amount.isNotEmpty() && (amount.toDoubleOrNull() == null || amount.toDouble() <= 0.0)
                    val isNameError = name.isNotEmpty() && name.isBlank()
                    val isFormValid = name.isNotBlank() && amount.isNotEmpty() && !isAmountError

                    val incomeCategories = listOf("Salary", "Freelance", "Investment", "Rental", "Business", "Bonus", "Pension", "Dividends", "Royalties", "Commission", "Side Income", "Other")

                    AlertDialog(
                        onDismissRequest = {
                            showAddIncome = false
                            activeIncomeToEdit = null
                        },
                        title = { Text(if (editItem != null) LocalizedStrings.get("edit_income", appSettings.language == "Arabic") else LocalizedStrings.get("add_income", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold) },
                        text = {
                            Box(modifier = Modifier.height(350.dp)) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text(LocalizedStrings.get("name", appSettings.language == "Arabic")) },
                                        isError = isNameError,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = amount,
                                        onValueChange = { amount = it },
                                        label = { Text(LocalizedStrings.get("amount", appSettings.language == "Arabic")) },
                                        isError = isAmountError,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(LocalizedStrings.get("category", appSettings.language == "Arabic"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        incomeCategories.forEach { cat ->
                                            FilterChip(
                                                selected = selectedCategory == cat,
                                                onClick = { selectedCategory = cat },
                                                label = { Text(LocalizedStrings.localizeCategory(cat, appSettings.language == "Arabic")) }
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = dateStr,
                                        onValueChange = { dateStr = it },
                                        label = { Text(LocalizedStrings.get("date", appSettings.language == "Arabic") + " (YYYY-MM-DD)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(LocalizedStrings.get("recurring_monthly", appSettings.language == "Arabic"))
                                        Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                                    }
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text(LocalizedStrings.get("notes", appSettings.language == "Arabic") + " (" + (if (appSettings.language == "Arabic") "اختياري" else "Optional") + ")") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                                    if (editItem != null) {
                                        viewModel.updateIncome(editItem.id, name, amtVal, selectedCategory, dateStr, isRecurring, notes)
                                    } else {
                                        viewModel.addIncome(name, amtVal, selectedCategory, dateStr, isRecurring, notes)
                                    }
                                    showAddIncome = false
                                    activeIncomeToEdit = null
                                },
                                enabled = isFormValid,
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint)
                             ) { Text(if (editItem != null) (if (appSettings.language == "Arabic") "تعديل" else "Update") else (if (appSettings.language == "Arabic") "حفظ" else "Save")) }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showAddIncome = false
                                    activeIncomeToEdit = null
                                }
                            ) { Text(if (appSettings.language == "Arabic") "إلغاء" else "Cancel", color = Color.Gray) }
                        }
                    )
                }

                // ADD EXPENSE DIALOG (Page 4)
                if (showAddExpense || activeExpenseToEdit != null) {
                    val editItem = activeExpenseToEdit
                    var name by remember { mutableStateOf(editItem?.name ?: "") }
                    var amount by remember { mutableStateOf(editItem?.amount?.toString() ?: "") }
                    var selectedCategory by remember { mutableStateOf(editItem?.category ?: "Food") }
                    var notes by remember { mutableStateOf(editItem?.notes ?: "") }
                    var dateStr by remember { mutableStateOf(editItem?.let { viewModel.formatTimestampToDate(it.timestamp) } ?: selectedExpenseDate) }

                    val isAmountError = amount.isNotEmpty() && (amount.toDoubleOrNull() == null || amount.toDouble() <= 0.0)
                    val isNameError = name.isNotEmpty() && name.isBlank()
                    val isFormValid = name.isNotBlank() && amount.isNotEmpty() && !isAmountError

                    val standardCategories = listOf("Food", "Transport", "Housing", "Health", "Fun", "Shopping", "Utilities", "Education", "Other")
                    val expenseCategories = standardCategories + customCategories.map { it.name }

                    AlertDialog(
                        onDismissRequest = {
                            showAddExpense = false
                            activeExpenseToEdit = null
                        },
                        title = { Text(if (editItem != null) LocalizedStrings.get("edit_expense", appSettings.language == "Arabic") else LocalizedStrings.get("add_expense", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold) },
                        text = {
                            Box(modifier = Modifier.height(350.dp)) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text(LocalizedStrings.get("name", appSettings.language == "Arabic")) },
                                        isError = isNameError,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = amount,
                                        onValueChange = { amount = it },
                                        label = { Text(LocalizedStrings.get("amount", appSettings.language == "Arabic")) },
                                        isError = isAmountError,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(LocalizedStrings.get("category", appSettings.language == "Arabic"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        expenseCategories.forEach { cat ->
                                            FilterChip(
                                                selected = selectedCategory == cat,
                                                onClick = { selectedCategory = cat },
                                                label = { Text(LocalizedStrings.localizeCategory(cat, appSettings.language == "Arabic")) }
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text(LocalizedStrings.get("notes", appSettings.language == "Arabic") + " (" + (if (appSettings.language == "Arabic") "اختياري" else "Optional") + ")") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                                    if (editItem != null) {
                                        viewModel.updateExpense(editItem.id, name, amtVal, selectedCategory, dateStr, notes)
                                    } else {
                                        viewModel.addExpense(name, amtVal, selectedCategory, dateStr, notes)
                                    }
                                    showAddExpense = false
                                    activeExpenseToEdit = null
                                },
                                enabled = isFormValid,
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentRed)
                            ) { Text(if (editItem != null) (if (appSettings.language == "Arabic") "تعديل" else "Update") else (if (appSettings.language == "Arabic") "حفظ" else "Save")) }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showAddExpense = false
                                    activeExpenseToEdit = null
                                }
                            ) { Text(if (appSettings.language == "Arabic") "إلغاء" else "Cancel", color = Color.Gray) }
                        }
                    )
                }

                // ADD BILL DIALOG (Page 5)
                if (showAddBill || activeBillToEdit != null) {
                    val editItem = activeBillToEdit
                    var name by remember { mutableStateOf(editItem?.name ?: "") }
                    var amount by remember { mutableStateOf(editItem?.amount?.toString() ?: "") }
                    var frequency by remember { mutableStateOf(editItem?.frequency ?: "Monthly") }
                    var dueDay by remember { mutableStateOf(editItem?.dueDay?.toString() ?: "28") }
                    var selectedCategory by remember { mutableStateOf(editItem?.category ?: "Housing") }
                    var notes by remember { mutableStateOf(editItem?.notes ?: "") }

                    val isAmountError = amount.isNotEmpty() && (amount.toDoubleOrNull() == null || amount.toDouble() <= 0.0)
                    val isNameError = name.isNotEmpty() && name.isBlank()
                    val isDueDayError = dueDay.isNotEmpty() && (dueDay.toIntOrNull() == null || dueDay.toInt() < 1 || dueDay.toInt() > 31)
                    val isFormValid = name.isNotBlank() && amount.isNotEmpty() && !isAmountError && !isDueDayError

                    val billCategories = listOf("Housing", "Utilities", "Internet", "Phone", "Insurance", "Streaming", "Gym", "Subscription", "Transport", "Education", "Medical", "Food", "Savings", "Loan", "Tax", "Other")

                    AlertDialog(
                        onDismissRequest = {
                            showAddBill = false
                            activeBillToEdit = null
                        },
                        title = { Text(if (editItem != null) LocalizedStrings.get("edit_bill", appSettings.language == "Arabic") else LocalizedStrings.get("add_bill", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold) },
                        text = {
                            Box(modifier = Modifier.height(350.dp)) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text(LocalizedStrings.get("name", appSettings.language == "Arabic")) },
                                        isError = isNameError,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = amount,
                                        onValueChange = { amount = it },
                                        label = { Text(LocalizedStrings.get("amount", appSettings.language == "Arabic")) },
                                        isError = isAmountError,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(if (appSettings.language == "Arabic") "التكرار وموعد الدفع" else "Frequency", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Monthly", "Weekly", "Yearly").forEach { freq ->
                                            val freqText = when (freq) {
                                                "Monthly" -> if (appSettings.language == "Arabic") "شهري" else "Monthly"
                                                "Weekly" -> if (appSettings.language == "Arabic") "أسبوعي" else "Weekly"
                                                else -> if (appSettings.language == "Arabic") "سنوي" else "Yearly"
                                            }
                                            Button(
                                                onClick = { frequency = freq },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (frequency == freq) PremiumAccentPurple else MaterialTheme.colorScheme.surface
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(freqText, color = if (frequency == freq) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = dueDay,
                                        onValueChange = { dueDay = it },
                                        label = { Text(if (appSettings.language == "Arabic") "يوم الدفع في الشهر" else "Due Day of Month") },
                                        isError = isDueDayError,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(LocalizedStrings.get("category", appSettings.language == "Arabic"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        billCategories.forEach { cat ->
                                            FilterChip(
                                                selected = selectedCategory == cat,
                                                onClick = { selectedCategory = cat },
                                                label = { Text(LocalizedStrings.localizeCategory(cat, appSettings.language == "Arabic")) }
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text(LocalizedStrings.get("notes", appSettings.language == "Arabic") + " (" + (if (appSettings.language == "Arabic") "اختياري" else "Optional") + ")") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                                    val dayVal = dueDay.toIntOrNull() ?: 28
                                    if (editItem != null) {
                                        viewModel.updateBill(editItem.copy(name = name, amount = amtVal, frequency = frequency, dueDay = dayVal, category = selectedCategory, notes = notes))
                                    } else {
                                        viewModel.addBill(name, amtVal, frequency, dayVal, selectedCategory, notes)
                                    }
                                    showAddBill = false
                                    activeBillToEdit = null
                                },
                                enabled = isFormValid,
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentPurple)
                            ) { Text(if (editItem != null) (if (appSettings.language == "Arabic") "تعديل" else "Update") else (if (appSettings.language == "Arabic") "حفظ" else "Save")) }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showAddBill = false
                                    activeBillToEdit = null
                                }
                            ) { Text(if (appSettings.language == "Arabic") "إلغاء" else "Cancel", color = Color.Gray) }
                        }
                    )
                }

                // ADD GOAL DIALOG (Page 7)
                if (showAddGoal) {
                    var name by remember { mutableStateOf("") }
                    var targetAmount by remember { mutableStateOf("") }
                    var deadline by remember { mutableStateOf("") }
                    val colorPalette = listOf("#10B981", "#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#14B8A6")
                    var selectedColor by remember { mutableStateOf(colorPalette.first()) }
                    var selectedIcon by remember { mutableStateOf("ic_star") }

                    val isAmountError = targetAmount.isNotEmpty() && (targetAmount.toDoubleOrNull() == null || targetAmount.toDouble() <= 0.0)
                    val isNameError = name.isNotEmpty() && name.isBlank()
                    val isFormValid = name.isNotBlank() && targetAmount.isNotEmpty() && !isAmountError

                    AlertDialog(
                        onDismissRequest = { showAddGoal = false },
                        title = { Text(LocalizedStrings.get("add_goal", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold) },
                        text = {
                            Box(modifier = Modifier.height(350.dp)) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text(if (appSettings.language == "Arabic") "إسم الهدف المالي" else "Goal Name") },
                                        isError = isNameError,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = targetAmount,
                                        onValueChange = { targetAmount = it },
                                        label = { Text(if (appSettings.language == "Arabic") "المبلغ المستهدف" else "Target Amount") },
                                        isError = isAmountError,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = deadline,
                                        onValueChange = { deadline = it },
                                        label = { Text(if (appSettings.language == "Arabic") "تاريخ تحقيق الهدف (اختياري YYYY-MM-DD)" else "Deadline (optional YYYY-MM-DD)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(if (appSettings.language == "Arabic") "اختر لون الهدف المالي" else "Pick Color Theme", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        colorPalette.forEach { hex ->
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                                    .border(
                                                        2.dp,
                                                        if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                        CircleShape
                                                    )
                                                    .clickable { selectedColor = hex }
                                            )
                                        }
                                    }
                                    Text(if (appSettings.language == "Arabic") "اختر أيقونة الهدف" else "Pick Logo Icon", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("ic_car", "ic_home", "ic_flight", "ic_star", "ic_laptop", "ic_cash").forEach { tcon ->
                                            IconButton(
                                                onClick = { selectedIcon = tcon },
                                                modifier = Modifier.background(
                                                    if (selectedIcon == tcon) PremiumAccentMint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(8.dp)
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = when (tcon) {
                                                        "ic_car" -> Icons.Default.Done
                                                        "ic_home" -> Icons.Default.Home
                                                        "ic_flight" -> Icons.Default.Share
                                                        "ic_star" -> Icons.Default.Star
                                                        "ic_laptop" -> Icons.Default.Info
                                                        else -> Icons.Default.Star
                                                    },
                                                    contentDescription = null,
                                                    tint = PremiumAccentMint
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val amtVal = targetAmount.toDoubleOrNull() ?: 1000.0
                                    viewModel.addGoal(name, amtVal, deadline, selectedColor, selectedIcon, "")
                                    showAddGoal = false
                                },
                                enabled = isFormValid,
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint)
                            ) { Text(LocalizedStrings.get("add_goal", appSettings.language == "Arabic")) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddGoal = false }) { Text(if (appSettings.language == "Arabic") "إلغاء" else "Cancel", color = Color.Gray) }
                        }
                    )
                }

                // FUND GOAL BOTTOM SHEET - ADD AMOUNT (Page 6)
                if (activeGoalToFund != null) {
                    val goal = activeGoalToFund!!
                    var addAmtInput by remember { mutableStateOf("") }
                    val isFundAmountError = addAmtInput.isNotEmpty() && (addAmtInput.toDoubleOrNull() == null || addAmtInput.toDouble() <= 0.0)
                    val isFundFormValid = addAmtInput.isNotEmpty() && !isFundAmountError

                    AlertDialog(
                        onDismissRequest = { activeGoalToFund = null },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val gColor = remember(goal.colorHex) {
                                    try {
                                        Color(android.graphics.Color.parseColor(goal.colorHex))
                                    } catch (e: Exception) {
                                        PremiumAccentMint
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(gColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = gColor)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(goal.name, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    if (appSettings.language == "Arabic") "المحوش: ${viewModel.formatAmount(goal.savedAmount)} من أصل ${viewModel.formatAmount(goal.targetAmount)}" else "Saved: ${viewModel.formatAmount(goal.savedAmount)} / ${viewModel.formatAmount(goal.targetAmount)}",
                                    fontSize = 13.sp,
                                    color = PremiumTextSecondaryDark
                                )
                                OutlinedTextField(
                                    value = addAmtInput,
                                    onValueChange = { addAmtInput = it },
                                    label = { Text(if (appSettings.language == "Arabic") "أضف مبلغ محوش جديد" else "Log Savable Amount") },
                                    isError = isFundAmountError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val amtVal = addAmtInput.toDoubleOrNull() ?: 0.0
                                    viewModel.saveGoalAmount(goal, amtVal)
                                    activeGoalToFund = null
                                },
                                enabled = isFundFormValid,
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint)
                            ) { Text(if (appSettings.language == "Arabic") "تأكيد الإدخار" else "Add Savings") }
                        },
                        dismissButton = {
                            TextButton(onClick = { activeGoalToFund = null }) { Text(if (appSettings.language == "Arabic") "إلغاء" else "Cancel", color = Color.Gray) }
                        }
                    )
                }
            }
        }
    }
}
}

// --- OVERVIEW SUB-SCREEN ---
@Composable
fun OverviewScreen(
    viewModel: FinanceViewModel,
    totalIncome: Double,
    totalExpenses: Double,
    totalBills: Double,
    totalGoalsSaved: Double,
    netBalance: Double,
    unpaidBillsAmount: Double,
    netWorth: Double,
    healthScore: Int,
    topSpending: String,
    customCategories: List<CustomCategory>,
    appSettings: AppSettings,
    aiInsight: String?,
    aiLoading: Boolean,
    onChangeTab: (String) -> Unit,
    onQuickAddIncome: () -> Unit,
    onQuickAddExpense: () -> Unit
) {
    val context = LocalContext.current
    var isAmountMasked by remember { mutableStateOf(false) }
    var showHealthDetailsDialog by remember { mutableStateOf(false) }

    fun formatMasked(amount: Double): String {
        return if (isAmountMasked) "••••" else viewModel.formatAmount(amount)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Net Balance Card (with dynamic red-green gradients and privacy mask toggle)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LocalizedStrings.get("net_balance", appSettings.language == "Arabic"),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { isAmountMasked = !isAmountMasked },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isAmountMasked) Icons.Default.Lock else Icons.Default.Info,
                                contentDescription = "Toggle Privacy Masking",
                                tint = if (isAmountMasked) PremiumAccentRed else PremiumAccentMint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatMasked(netBalance),
                        color = if (netBalance >= 0) PremiumAccentMint else PremiumAccentRed,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OverviewMetricItem(
                            label = LocalizedStrings.get("income_cap", appSettings.language == "Arabic"),
                            valStr = formatMasked(totalIncome),
                            colorTheme = PremiumAccentMint
                        )
                        OverviewMetricItem(
                            label = LocalizedStrings.get("expenses_cap", appSettings.language == "Arabic"),
                            valStr = formatMasked(totalExpenses),
                            colorTheme = PremiumAccentRed
                        )
                        OverviewMetricItem(
                            label = LocalizedStrings.get("bills_cap", appSettings.language == "Arabic"),
                            valStr = formatMasked(totalBills),
                            colorTheme = PremiumAccentPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Quick Action Logging Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onQuickAddIncome,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint.copy(alpha = 0.12f), contentColor = PremiumAccentMint),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (appSettings.language == "Arabic") "+ إضافة دخل" else "+ Add Income", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = onQuickAddExpense,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentRed.copy(alpha = 0.12f), contentColor = PremiumAccentRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (appSettings.language == "Arabic") "+ إضافة مصروف" else "+ Add Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Circular financial health score out of 100 (Clickable for report dialog)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHealthDetailsDialog = true },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = LocalizedStrings.get("financial_health", appSettings.language == "Arabic"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Info, contentDescription = "Explain Score", tint = PremiumAccentOrange, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                healthScore >= 80 -> LocalizedStrings.get("excellent_health", appSettings.language == "Arabic")
                                healthScore >= 50 -> LocalizedStrings.get("moderate_health", appSettings.language == "Arabic")
                                else -> LocalizedStrings.get("needs_attention", appSettings.language == "Arabic")
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                healthScore >= 80 -> PremiumAccentMint
                                healthScore >= 50 -> PremiumAccentOrange
                                else -> PremiumAccentRed
                            },
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (appSettings.language == "Arabic") "اضغط لعرض النصائح المخصصة" else "Tap for personalized safety tips",
                            fontSize = 11.sp,
                            color = PremiumTextSecondaryDark
                        )
                    }

                    // Progress Arc Custom drawing
                    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .drawBehind {
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 8.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                drawArc(
                                    color = if (healthScore >= 80) PremiumAccentMint else if (healthScore >= 50) PremiumAccentOrange else PremiumAccentRed,
                                    startAngle = -90f,
                                    sweepAngle = (healthScore / 100f) * 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 8.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$healthScore%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Net Worth Card (Assets vs Liabilities)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            LocalizedStrings.get("net_worth", appSettings.language == "Arabic"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            formatMasked(netWorth),
                            fontWeight = FontWeight.ExtraBold,
                            color = PremiumAccentMint,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(LocalizedStrings.get("assets", appSettings.language == "Arabic"), fontSize = 11.sp, color = PremiumTextSecondaryDark)
                            Text(formatMasked(netBalance), fontWeight = FontWeight.Bold, color = PremiumAccentMint)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(LocalizedStrings.get("liabilities", appSettings.language == "Arabic"), fontSize = 11.sp, color = PremiumTextSecondaryDark)
                            Text(formatMasked(unpaidBillsAmount), fontWeight = FontWeight.Bold, color = PremiumAccentRed)
                        }
                    }
                }
            }
        }

        // Spending Breakdown segmented line bar (Page 13)
        item {
            SpendingBreakdownChart(
                totalExpenses = totalExpenses,
                totalBills = totalBills,
                topSpending = topSpending,
                appSettings = appSettings,
                viewModel = viewModel,
                isAmountMasked = isAmountMasked,
                onChangeTab = onChangeTab
            )
        }

        // INTELLIGENT AI FINANCIAL ADVISOR WIDGET (Direct Integration!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, PremiumAccentMint.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PremiumAccentMint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "AI", tint = PremiumAccentMint, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                LocalizedStrings.get("gemini_advisor", appSettings.language == "Arabic"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PremiumAccentMint
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshAiInsights() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = LocalizedStrings.get("gemini_refresh", appSettings.language == "Arabic"), tint = PremiumAccentMint)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (aiLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = PremiumAccentMint, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (appSettings.language == "Arabic") "تحليل جاري لحركاتك المالية..." else "Analyzing transactions...",
                                fontSize = 12.sp,
                                color = PremiumTextSecondaryDark
                            )
                        }
                    } else {
                        val adviceText = aiInsight ?: LocalizedStrings.get("gemini_default_insight", appSettings.language == "Arabic")
                        Text(
                            text = adviceText,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Top Spending Category banner (Page 13)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PremiumAccentOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PremiumAccentOrange)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    val localCat = LocalizedStrings.localizeCategory(topSpending, appSettings.language == "Arabic")
                    Text(
                        text = LocalizedStrings.get("top_spending_in", appSettings.language == "Arabic") + localCat,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumAccentOrange
                    )
                }
            }
        }

        // Recent Transactions Shortcut Card (Highly Interactive!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangeTab("transactions") },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PremiumAccentBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, tint = PremiumAccentBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (appSettings.language == "Arabic") "سجل كل المعاملات الكلي" else "Full Transaction History",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appSettings.language == "Arabic") "تصفح وبحث وحذف المعاملات بضغطة واحدة مطولة" else "Browse, filter & long-press to quick delete",
                                fontSize = 11.sp,
                                color = PremiumTextSecondaryDark
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Go",
                        tint = PremiumTextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Shortcuts Grid Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onChangeTab("goals") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Goals", tint = PremiumAccentMint)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saving Goals", fontSize = 12.sp)
                }
                Button(
                    onClick = { onChangeTab("calendar") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = PremiumAccentBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calendar", fontSize = 12.sp)
                }
            }
        }
    }

    if (showHealthDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showHealthDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Health",
                        tint = if (healthScore >= 80) PremiumAccentMint else if (healthScore >= 50) PremiumAccentOrange else PremiumAccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (appSettings.language == "Arabic") "تفاصيل الصحة المالية" else "Financial Health Details",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = (if (appSettings.language == "Arabic") "مؤشر صحتك الحالية هو " else "Your current health index is ") + "$healthScore / 100",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (healthScore >= 80) PremiumAccentMint else if (healthScore >= 50) PremiumAccentOrange else PremiumAccentRed
                    )
                    Text(
                        text = if (appSettings.language == "Arabic") {
                            "يتم حساب نتيجة الصحة المالية بتوازن ذكي بين مصادرك المالية ومدى إلتزامك في دفع الفواتير والإدخار لتحقيق الأهداف."
                        } else {
                            "Financial health score is calculated as a smart balance between your current assets, expenses, due bills unpaid, and savings progress."
                        },
                        fontSize = 13.sp,
                        color = PremiumTextSecondaryDark
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Text(
                        text = if (appSettings.language == "Arabic") "نصائح مخصصة لك:" else "Personalized Tips for You:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val tips = when {
                        healthScore >= 80 -> listOf(
                            if (appSettings.language == "Arabic") "صحتك المالية ممتازة! واصل الادخار الذكي وتجنب الديون." else "Your financial health is amazing! Keep up the smart savings habit and avoid debt.",
                            if (appSettings.language == "Arabic") "استثمر جزءاً من المدخرات لتنمية ثروتك على المدى الطويل." else "Invest a portion of your savings to grow long-term wealth."
                        )
                        healthScore >= 50 -> listOf(
                            if (appSettings.language == "Arabic") "صحتك معتدلة. حاول تقليل المصاريف الجانبية هذا الشهر." else "Your health is moderate. Try reducing non-essential expenses this month.",
                            if (appSettings.language == "Arabic") "تأكد من سداد الفواتير المستحقة قبل تراكمها." else "Ensure you pay off your pending bills before they accumulate."
                        )
                        else -> listOf(
                            if (appSettings.language == "Arabic") "تحتاج إلى مراجعة عاجلة لميزانيتك والحد من الهدر." else "Urgent budget review needed. Minimize discretionary spending.",
                            if (appSettings.language == "Arabic") "ابدأ بإضافة أهداف ادخار صغيرة والالتزام بنسبة 10% على الأقل." else "Start by setting small saving goals and budget at least 10%."
                        )
                    }
                    tips.forEach { tip ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", fontWeight = FontWeight.Bold, color = PremiumAccentMint)
                            Text(tip, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHealthDetailsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint)
                ) {
                    Text(if (appSettings.language == "Arabic") "حسناً" else "Okay")
                }
            }
        )
    }
}

@Composable
fun OverviewMetricItem(label: String, valStr: String, colorTheme: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colorTheme)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PremiumTextSecondaryDark)
        }
        Text(valStr, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// --- INCOME SUB-SCREEN ---
@Composable
fun IncomeScreen(
    viewModel: FinanceViewModel,
    incomes: List<Income>,
    appSettings: AppSettings,
    selectedDateStr: String,
    onDateSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Income) -> Unit
) {
    val totalAmt = incomes.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Total Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PremiumAccentMint)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    LocalizedStrings.get("total_income_this_month", appSettings.language == "Arabic"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.6f)
                )
                Text(
                    viewModel.formatAmount(totalAmt),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Text(
                    "${incomes.size} " + LocalizedStrings.get("sources_logged", appSettings.language == "Arabic"),
                    fontSize = 12.sp,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }

        // Horizontal Scrollable Calendar Strip for easy logging
        HorizontalCalendarStrip(
            selectedDateStr = selectedDateStr,
            onDateSelected = onDateSelected,
            isArabic = appSettings.language == "Arabic"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(LocalizedStrings.get("transactions", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.background(PremiumAccentMint, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = LocalizedStrings.get("add_income", appSettings.language == "Arabic"), tint = Color.Black)
            }
        }

        if (incomes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(LocalizedStrings.get("no_income_yet", appSettings.language == "Arabic"), color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Text(LocalizedStrings.get("add_income_instruction", appSettings.language == "Arabic"), fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(incomes) { incItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(incItem.name, fontWeight = FontWeight.Bold)
                                Row {
                                    Text(LocalizedStrings.localizeCategory(incItem.category, appSettings.language == "Arabic"), fontSize = 11.sp, color = PremiumAccentMint)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(viewModel.formatTimestampToDate(incItem.timestamp), fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "+" + viewModel.formatAmount(incItem.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PremiumAccentMint
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { onEditClick(incItem) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PremiumAccentBlue)
                                }
                                IconButton(onClick = { viewModel.deleteIncome(incItem) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PremiumAccentRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- EXPENSES SUB-SCREEN ---
@Composable
fun ExpensesScreen(
    viewModel: FinanceViewModel,
    expenses: List<Expense>,
    customCategories: List<CustomCategory>,
    appSettings: AppSettings,
    selectedDateStr: String,
    onDateSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Expense) -> Unit
) {
    val totalAmt = expenses.sumOf { it.amount }
    var selectedFilterCategory by remember { mutableStateOf("All") }

    val filterList = remember(expenses, selectedFilterCategory, selectedDateStr) {
        expenses.filter {
            val matchesCategory = selectedFilterCategory == "All" || it.category == selectedFilterCategory
            val matchesDate = viewModel.formatTimestampToDate(it.timestamp) == selectedDateStr
            matchesCategory && matchesDate
        }
    }

    val dailySum = remember(expenses, selectedDateStr) {
        expenses.filter { viewModel.formatTimestampToDate(it.timestamp) == selectedDateStr }.sumOf { it.amount }
    }

    val categoriesSummaryMap = remember(expenses) {
        expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Total Expenses Header Card (Crimson Visual)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PremiumAccentRed)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    LocalizedStrings.get("total_expenses_this_month", appSettings.language == "Arabic"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    viewModel.formatAmount(totalAmt),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "${expenses.size} " + LocalizedStrings.get("total_transactions_logged", appSettings.language == "Arabic"),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Horizontal Scrollable Calendar Strip for easy logging
        HorizontalCalendarStrip(
            selectedDateStr = selectedDateStr,
            onDateSelected = onDateSelected,
            isArabic = appSettings.language == "Arabic"
        )

        // Interactive Categories chip filter row (All, Food, Housing, etc.)
        Text(LocalizedStrings.get("by_category", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf("All", "Food", "Transport", "Housing", "Health", "Fun", "Shopping", "Utilities", "Education", "Other") + customCategories.map { it.name }
            chips.forEach { chipName ->
                FilterChip(
                    selected = selectedFilterCategory == chipName,
                    onClick = { selectedFilterCategory = chipName },
                    label = { Text(LocalizedStrings.localizeCategory(chipName, appSettings.language == "Arabic")) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(LocalizedStrings.get("transactions", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.background(PremiumAccentRed, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = LocalizedStrings.get("add_expense", appSettings.language == "Arabic"), tint = Color.Black)
            }
        }

        // Daily sum highlight banner
        val isArabic = appSettings.language == "Arabic"
        val labelText = if (isArabic) "مجموع مصاريف اليوم: " else "Today's expenses total: "
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PremiumAccentRed.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, PremiumAccentRed.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labelText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = viewModel.formatAmount(dailySum),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = PremiumAccentRed
                )
            }
        }

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(LocalizedStrings.get("no_expenses_yet", appSettings.language == "Arabic"), color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Text(LocalizedStrings.get("add_expense_instruction", appSettings.language == "Arabic"), fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else if (filterList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(if (appSettings.language == "Arabic") "مفيش مصاريف لليوم ده أو الفئة دي لسه!" else "No matching expenses for this day or category", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterList) { expItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(expItem.name, fontWeight = FontWeight.Bold)
                                Row {
                                    Text(LocalizedStrings.localizeCategory(expItem.category, appSettings.language == "Arabic"), fontSize = 11.sp, color = PremiumAccentRed)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(viewModel.formatTimestampToDate(expItem.timestamp), fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "-" + viewModel.formatAmount(expItem.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PremiumAccentRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { onEditClick(expItem) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PremiumAccentBlue)
                                }
                                IconButton(onClick = { viewModel.deleteExpense(expItem) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PremiumAccentRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- BILLS SUB-SCREEN ---
@Composable
fun BillsScreen(
    viewModel: FinanceViewModel,
    bills: List<Bill>,
    appSettings: AppSettings,
    onAddClick: () -> Unit,
    onEditClick: (Bill) -> Unit
) {
    val totalAmt = bills.sumOf { it.amount }
    val paidAmt = bills.filter { it.isPaid }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(LocalizedStrings.get("total_monthly", appSettings.language == "Arabic"), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(viewModel.formatAmount(totalAmt), fontSize = 20.sp, fontWeight = FontWeight.Black, color = PremiumAccentPurple)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(LocalizedStrings.get("paid_this_month", appSettings.language == "Arabic"), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(viewModel.formatAmount(paidAmt), fontSize = 20.sp, fontWeight = FontWeight.Black, color = PremiumAccentMint)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(LocalizedStrings.get("all_recurring_bills", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.background(PremiumAccentPurple, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = LocalizedStrings.get("add_bill", appSettings.language == "Arabic"), tint = Color.Black)
            }
        }

        if (bills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(LocalizedStrings.get("no_bills_yet", appSettings.language == "Arabic"), color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(LocalizedStrings.get("add_bill_instruction", appSettings.language == "Arabic"), fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bills) { billItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Top Row: Checkbox + Name on Left, Category Badge on Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Checkbox(
                                        checked = billItem.isPaid,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateBill(billItem.copy(isPaid = isChecked))
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = PremiumAccentMint)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = billItem.name, 
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(PremiumAccentPurple.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = LocalizedStrings.localizeCategory(billItem.category, appSettings.language == "Arabic"),
                                        fontSize = 11.sp,
                                        color = PremiumAccentPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom Row: Due Date on Left, Amount + Controls on Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val freqStr = when (billItem.frequency) {
                                    "Monthly" -> if (appSettings.language == "Arabic") "شهرياً" else "Monthly"
                                    "Weekly" -> if (appSettings.language == "Arabic") "أسبوعياً" else "Weekly"
                                    else -> if (appSettings.language == "Arabic") "سنوياً" else "Yearly"
                                }
                                Text(
                                    text = "${LocalizedStrings.get("due_day", appSettings.language == "Arabic")} ${billItem.dueDay} ($freqStr)",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = viewModel.formatAmount(billItem.amount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (billItem.isPaid) PremiumAccentMint else PremiumAccentRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onEditClick(billItem) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = PremiumAccentBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteBill(billItem) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = PremiumAccentRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- CALENDAR INTEGRATED SCREEN ---
@Composable
fun CalendarScreen(
    viewModel: FinanceViewModel,
    incomes: List<Income>,
    expenses: List<Expense>,
    appSettings: AppSettings
) {
    // Current active calendar state
    var currentCalendar by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    var selectedDay by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)) }

    val monthYearFormat = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US) }
    val monthYearArabicFormat = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("ar")) }
    val selectedMonthText = if (appSettings.language == "Arabic") {
        monthYearArabicFormat.format(currentCalendar.time)
    } else {
        monthYearFormat.format(currentCalendar.time)
    }

    val daysInMonth = currentCalendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDayOffset = remember(currentCalendar) {
        val tempCal = currentCalendar.clone() as java.util.Calendar
        tempCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        (tempCal.get(java.util.Calendar.DAY_OF_WEEK) - 1 + 7) % 7 // 0 for Sunday, 1 for Monday, etc.
    }

    val displayedIncomes = incomes.filter { inc ->
        val c = java.util.Calendar.getInstance().apply { timeInMillis = inc.timestamp }
        c.get(java.util.Calendar.MONTH) == currentCalendar.get(java.util.Calendar.MONTH) &&
        c.get(java.util.Calendar.YEAR) == currentCalendar.get(java.util.Calendar.YEAR)
    }

    val displayedExpenses = expenses.filter { exp ->
        val c = java.util.Calendar.getInstance().apply { timeInMillis = exp.timestamp }
        c.get(java.util.Calendar.MONTH) == currentCalendar.get(java.util.Calendar.MONTH) &&
        c.get(java.util.Calendar.YEAR) == currentCalendar.get(java.util.Calendar.YEAR)
    }

    val displayedBills = viewModel.bills.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newCal = currentCalendar.clone() as java.util.Calendar
                newCal.add(java.util.Calendar.MONTH, -1)
                currentCalendar = newCal
                // Keep selected day bounded
                selectedDay = selectedDay.coerceAtMost(newCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month") }
            
            Text(selectedMonthText, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            
            IconButton(onClick = {
                val newCal = currentCalendar.clone() as java.util.Calendar
                newCal.add(java.util.Calendar.MONTH, 1)
                currentCalendar = newCal
                selectedDay = selectedDay.coerceAtMost(newCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month") }
        }

        // Calendar Grid Layout
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Calendar Weekday Headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    val days = if (appSettings.language == "Arabic") {
                        listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
                    } else {
                        listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")
                    }
                    days.forEach { d ->
                        Text(
                            text = d,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Standard calendar grid construction with offset cells
                val totalCells = firstDayOffset + daysInMonth
                val rowsCount = (totalCells + 6) / 7
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until rowsCount) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..6) {
                                val cellIdx = row * 7 + col
                                val dateVal = cellIdx - firstDayOffset + 1
                                
                                if (cellIdx in firstDayOffset until totalCells) {
                                    val isSelected = selectedDay == dateVal
                                    
                                    val hasIncome = displayedIncomes.any { inc ->
                                        val c = java.util.Calendar.getInstance().apply { timeInMillis = inc.timestamp }
                                        c.get(java.util.Calendar.DAY_OF_MONTH) == dateVal
                                    }
                                    val hasExpense = displayedExpenses.any { exp ->
                                        val c = java.util.Calendar.getInstance().apply { timeInMillis = exp.timestamp }
                                        c.get(java.util.Calendar.DAY_OF_MONTH) == dateVal
                                    }
                                    val hasBill = displayedBills.any { b -> b.dueDay == dateVal }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) PremiumAccentMint else Color.Transparent
                                            )
                                            .clickable { selectedDay = dateVal },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(
                                                text = dateVal.toString(),
                                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            // Dots row indicator
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (hasIncome) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.Black else PremiumAccentMint)
                                                    )
                                                }
                                                if (hasExpense) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.Black else PremiumAccentRed)
                                                    )
                                                }
                                                if (hasBill) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.Black else PremiumAccentPurple)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Transaction List (Real Dynamic Records!)
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (appSettings.language == "Arabic") "تفاصيل اليوم المختار ($selectedDay)" else "Selected date details (Day $selectedDay)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                val dayIncomes = displayedIncomes.filter { inc ->
                    val c = java.util.Calendar.getInstance().apply { timeInMillis = inc.timestamp }
                    c.get(java.util.Calendar.DAY_OF_MONTH) == selectedDay
                }
                val dayExpenses = displayedExpenses.filter { exp ->
                    val c = java.util.Calendar.getInstance().apply { timeInMillis = exp.timestamp }
                    c.get(java.util.Calendar.DAY_OF_MONTH) == selectedDay
                }
                val dayBills = displayedBills.filter { b -> b.dueDay == selectedDay }

                if (dayIncomes.isEmpty() && dayExpenses.isEmpty() && dayBills.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = LocalizedStrings.get("no_transactions_today", appSettings.language == "Arabic"),
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(dayIncomes) { inc ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(PremiumAccentMint.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = PremiumAccentMint, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(inc.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(LocalizedStrings.localizeCategory(inc.category, appSettings.language == "Arabic"), fontSize = 11.sp, color = PremiumAccentMint)
                                    }
                                }
                                Text("+" + viewModel.formatAmount(inc.amount), fontWeight = FontWeight.Bold, color = PremiumAccentMint, fontSize = 13.sp)
                            }
                        }

                        items(dayExpenses) { exp ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(PremiumAccentRed.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = PremiumAccentRed, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(exp.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(LocalizedStrings.localizeCategory(exp.category, appSettings.language == "Arabic"), fontSize = 11.sp, color = PremiumAccentRed)
                                    }
                                }
                                Text("-" + viewModel.formatAmount(exp.amount), fontWeight = FontWeight.Bold, color = PremiumAccentRed, fontSize = 13.sp)
                            }
                        }

                        items(dayBills) { b ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(PremiumAccentPurple.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = PremiumAccentPurple, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(b.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(if (b.isPaid) (if (appSettings.language == "Arabic") "مصحوبة / مدفوع" else "Paid") else (if (appSettings.language == "Arabic") "في الانتظار / لسه" else "Unpaid"), fontSize = 11.sp, color = if (b.isPaid) PremiumAccentMint else PremiumAccentRed)
                                    }
                                }
                                Text(viewModel.formatAmount(b.amount), fontWeight = FontWeight.Bold, color = PremiumAccentPurple, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- GOAL SETS SCREEN ---
@Composable
fun GoalsScreen(
    viewModel: FinanceViewModel,
    goals: List<Goal>,
    appSettings: AppSettings,
    onAddClick: () -> Unit,
    onFundClick: (Goal) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(LocalizedStrings.get("tab_goals", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.background(PremiumAccentMint, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = LocalizedStrings.get("add_goal", appSettings.language == "Arabic"), tint = Color.Black)
            }
        }

        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(LocalizedStrings.get("no_goals_yet", appSettings.language == "Arabic"), color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(LocalizedStrings.get("add_goal_instruction", appSettings.language == "Arabic"), fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(goals) { goalItem ->
                    val color = remember(goalItem.colorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(goalItem.colorHex))
                        } catch (e: Exception) {
                            PremiumAccentMint
                        }
                    }
                    val progress = if (goalItem.targetAmount > 0.0) {
                        (goalItem.savedAmount / goalItem.targetAmount).toFloat().coerceIn(0f, 1f)
                    } else 1f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = color)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(goalItem.name, fontWeight = FontWeight.Bold)
                                        if (goalItem.deadline.isNotBlank()) {
                                            Text((if (appSettings.language == "Arabic") "مستهدف قبل: " else "Deadline: ") + goalItem.deadline, fontSize = 11.sp, color = color)
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(PremiumAccentMint.copy(alpha = 0.15f))
                                            .clickable { onFundClick(goalItem) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (appSettings.language == "Arabic") "+ حوّش" else "+ Fund", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PremiumAccentMint)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteGoal(goalItem) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Goal",
                                            tint = PremiumAccentRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${viewModel.formatAmount(goalItem.savedAmount)} / ${viewModel.formatAmount(goalItem.targetAmount)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SETTINGS VIEW (Page 8) ---
@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    appSettings: AppSettings,
    customCategories: List<CustomCategory>,
    onShowLanguage: () -> Unit,
    onShowCurrency: () -> Unit,
    onShowLimit: () -> Unit,
    onShowAddCategory: () -> Unit,
    onShowClearData: () -> Unit
) {
    var backupBoxText by remember { mutableStateOf("") }
    var importCsvBoxText by remember { mutableStateOf("") }

    val isArabic = appSettings.language == "Arabic"
    var isSmartAlertsEnabled by remember { mutableStateOf(true) }
    val isBiometricLockEnabled = appSettings.isBiometricLockEnabled

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Theme Appearance section (Light / Dark segmented selector)
        item {
            Column {
                Text(LocalizedStrings.get("appearance", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PremiumAccentMint)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Light", "Dark", "System").forEach { themeMode ->
                        val themeModeText = when (themeMode) {
                            "Light" -> if (appSettings.language == "Arabic") "فاتح" else "Light"
                            "Dark" -> if (appSettings.language == "Arabic") "ليلي" else "Dark"
                            else -> if (appSettings.language == "Arabic") "تلقائي" else "System"
                        }
                        Button(
                            onClick = { viewModel.updateSettings(themeMode, appSettings.language, appSettings.currency, appSettings.monthlyLimit) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appSettings.theme == themeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when (themeMode) {
                                    "Light" -> Icons.Default.Done
                                    "Dark" -> Icons.Default.Info
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                tint = if (appSettings.theme == themeMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = themeModeText,
                                fontSize = 11.sp,
                                color = if (appSettings.theme == themeMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Language and Currency buttons opens custom sheets
        item {
            Column {
                Text(LocalizedStrings.get("language_currency", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PremiumAccentMint)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowLanguage() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(LocalizedStrings.get("language", appSettings.language == "Arabic"))
                            Text((if (appSettings.language == "Arabic") "المصري 🇪🇬" else "English 🇺🇸") + " >", fontWeight = FontWeight.Bold, color = PremiumAccentMint)
                        }
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowCurrency() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(LocalizedStrings.get("currency", appSettings.language == "Arabic"))
                            Text("${appSettings.currency} >", fontWeight = FontWeight.Bold, color = PremiumAccentMint)
                        }
                    }
                }
            }
        }

        // Spending limits
        item {
            Column {
                Text(LocalizedStrings.get("monthly_spending_limit", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PremiumAccentMint)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(LocalizedStrings.get("current_budget_limit", appSettings.language == "Arabic"))
                            Text(
                                if (appSettings.monthlyLimit > 0.0) viewModel.formatAmount(appSettings.monthlyLimit) else LocalizedStrings.get("no_limit_set", appSettings.language == "Arabic"),
                                fontWeight = FontWeight.Bold,
                                color = PremiumAccentOrange
                            )
                        }
                        Button(
                            onClick = onShowLimit,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint)
                        ) {
                            Text(LocalizedStrings.get("adjust", appSettings.language == "Arabic"), color = Color.Black)
                        }
                    }
                }
            }
        }

        // Custom Categories Editor
        item {
            Column {
                Text(LocalizedStrings.get("custom_expense_categories", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PremiumAccentMint)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(LocalizedStrings.get("my_categories", appSettings.language == "Arabic"))
                            IconButton(onClick = onShowAddCategory) { Icon(Icons.Default.Add, contentDescription = null, tint = PremiumAccentMint) }
                        }
                        if (customCategories.isEmpty()) {
                            Text(LocalizedStrings.get("no_custom_categories", appSettings.language == "Arabic"), fontSize = 11.sp, color = Color.Gray)
                        } else {
                            customCategories.forEach { cat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val catColor = remember(cat.colorHex) {
                                            try {
                                                Color(android.graphics.Color.parseColor(cat.colorHex))
                                            } catch (e: Exception) {
                                                PremiumAccentMint
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat.name)
                                    }
                                    IconButton(onClick = { viewModel.deleteCustomCategory(cat) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = PremiumAccentRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CSV & Backup / Data Export Logic
        item {
            Column {
                Text(LocalizedStrings.get("export_data_backups", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PremiumAccentMint)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val csv = viewModel.exportToCSV()
                                importCsvBoxText = csv
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(LocalizedStrings.get("export_csv", appSettings.language == "Arabic"))
                        }

                        if (importCsvBoxText.isNotBlank()) {
                            OutlinedTextField(
                                value = importCsvBoxText,
                                onValueChange = { importCsvBoxText = it },
                                label = { Text(LocalizedStrings.get("csv_preview", appSettings.language == "Arabic")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.importFromCSV(importCsvBoxText)
                                    importCsvBoxText = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(LocalizedStrings.get("import_csv", appSettings.language == "Arabic"))
                            }
                        }

                        Button(
                            onClick = {
                                val json = viewModel.exportToJson()
                                backupBoxText = json
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(LocalizedStrings.get("export_json", appSettings.language == "Arabic"))
                        }

                        if (backupBoxText.isNotBlank()) {
                            OutlinedTextField(
                                value = backupBoxText,
                                onValueChange = { backupBoxText = it },
                                label = { Text(LocalizedStrings.get("json_preview", appSettings.language == "Arabic")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.importFromJson(backupBoxText)
                                    backupBoxText = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(LocalizedStrings.get("import_json", appSettings.language == "Arabic"))
                            }
                        }
                    }
                }
            }
        }

        // Additional Features (Smart alerts & fingerprint biometrics)
        item {
            Column {
                Text(LocalizedStrings.get("additional_features", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PremiumAccentMint)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(LocalizedStrings.get("smart_notifications", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(if (appSettings.language == "Arabic") "تنبيهات تلقائية لما تعدي 80% من الميزانية" else "Alerts when you exceed 80% of budget limits", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isSmartAlertsEnabled,
                                onCheckedChange = { isSmartAlertsEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PremiumAccentMint)
                            )
                        }
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(LocalizedStrings.get("fingerprint_lock", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(if (appSettings.language == "Arabic") "حماية فائقة لأمان حساباتك ومصاريفك" else "Advanced security lock using your touch ID", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isBiometricLockEnabled,
                                onCheckedChange = { viewModel.updateBiometricLock(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PremiumAccentMint)
                            )
                        }
                    }
                }
            }
        }

        // CLEAR DATA CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowClearData() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PremiumAccentRed.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, PremiumAccentRed.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = PremiumAccentRed)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(LocalizedStrings.get("clear_all_data", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, color = PremiumAccentRed)
                        Text(LocalizedStrings.get("clear_all_data_description", appSettings.language == "Arabic"), fontSize = 11.sp, color = PremiumAccentRed.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // About footer
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.96f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (appSettings.language == "Arabic") "تطبيق توفير Tawffer" else "Tawffer App",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "v1.2.0",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PremiumAccentMint
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (appSettings.language == "Arabic") "تصميم وبرمجة عبدالله" else "Designed & Built by Abdallah",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // BRAND SHOWCASE FOOTER (Displaced from top)
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.96f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PremiumAccentMint)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tawffer", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.Black)
                        Text("March 2026", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// --- SEARCH & FILTER VIEW (Page 21) ---
@Composable
fun SearchFilterScreen(
    viewModel: FinanceViewModel,
    incomes: List<Income>,
    expenses: List<Expense>,
    appSettings: AppSettings,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchTabFilter: String,
    onFilterChange: (String) -> Unit,
    onEditIncome: (Income) -> Unit,
    onEditExpense: (Expense) -> Unit
) {
    val matchingIncomes = remember(incomes, searchQuery) {
        if (searchQuery.isBlank()) incomes
        else incomes.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    val matchingExpenses = remember(expenses, searchQuery) {
        if (searchQuery.isBlank()) expenses
        else expenses.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(if (appSettings.language == "Arabic") "ابحث في معاملاتك الحالية..." else "Search transactions...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf("All", "Income", "Expenses")
            filters.forEach { filterLabel ->
                val labelText = when (filterLabel) {
                    "All" -> if (appSettings.language == "Arabic") "كل المعاملات" else "All"
                    "Income" -> if (appSettings.language == "Arabic") "الدخل الوارد" else "Income"
                    else -> if (appSettings.language == "Arabic") "المصروفات" else "Expenses"
                }
                Button(
                    onClick = { onFilterChange(filterLabel) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (searchTabFilter == filterLabel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (searchTabFilter == filterLabel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(labelText, fontSize = 11.sp)
                }
            }
        }

        Text(if (appSettings.language == "Arabic") "نتائج البحث" else "Results", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            if (searchTabFilter == "All" || searchTabFilter == "Income") {
                items(matchingIncomes) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Row {
                                    Text(LocalizedStrings.localizeCategory(item.category, appSettings.language == "Arabic"), color = PremiumAccentMint, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(viewModel.formatTimestampToDate(item.timestamp), color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+" + viewModel.formatAmount(item.amount), color = PremiumAccentMint, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { onEditIncome(item) }) { Icon(Icons.Default.Edit, contentDescription = null, tint = PremiumAccentBlue) }
                                IconButton(onClick = { viewModel.deleteIncome(item) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = PremiumAccentRed) }
                            }
                        }
                    }
                }
            }

            if (searchTabFilter == "All" || searchTabFilter == "Expenses") {
                items(matchingExpenses) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Row {
                                    Text(LocalizedStrings.localizeCategory(item.category, appSettings.language == "Arabic"), color = PremiumAccentRed, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(viewModel.formatTimestampToDate(item.timestamp), color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("-" + viewModel.formatAmount(item.amount), color = PremiumAccentRed, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { onEditExpense(item) }) { Icon(Icons.Default.Edit, contentDescription = null, tint = PremiumAccentBlue) }
                                IconButton(onClick = { viewModel.deleteExpense(item) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = PremiumAccentRed) }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CalendarDayItem(
    val dayNum: Int,
    val dateStr: String,
    val dayAbbrev: String
)

@Composable
fun HorizontalCalendarStrip(
    selectedDateStr: String,
    onDateSelected: (String) -> Unit,
    isArabic: Boolean
) {
    val calendar = remember { java.util.Calendar.getInstance() }
    val year = remember { calendar.get(java.util.Calendar.YEAR) }
    val month = remember { calendar.get(java.util.Calendar.MONTH) } // 0-indexed
    val maxDays = remember { calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH) }
    
    val dayNamesEn = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val dayNamesAr = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

    val lazyListState = rememberLazyListState()

    // Precompute days to prevent heavy Calendar instantiation inside LazyRow render iterations
    val calendarDays = remember(year, month, isArabic) {
        val list = ArrayList<CalendarDayItem>()
        val tempCal = java.util.Calendar.getInstance()
        tempCal.set(java.util.Calendar.YEAR, year)
        tempCal.set(java.util.Calendar.MONTH, month)
        for (dayNum in 1..maxDays) {
            val dateStr = "%d-%02d-%02d".format(year, month + 1, dayNum)
            tempCal.set(java.util.Calendar.DAY_OF_MONTH, dayNum)
            val dayOfWeek = tempCal.get(java.util.Calendar.DAY_OF_WEEK) // 1 = Sun, ..., 7 = Sat
            val dayAbbrev = if (isArabic) dayNamesAr[dayOfWeek - 1] else dayNamesEn[dayOfWeek - 1]
            list.add(CalendarDayItem(dayNum, dateStr, dayAbbrev))
        }
        list
    }

    // Scroll to the active selected date item instantly to avoid overlapping transitions stutter
    LaunchedEffect(selectedDateStr) {
        val selectedDay = selectedDateStr.split("-").lastOrNull()?.toIntOrNull() ?: 1
        val itemIndex = selectedDay - 1
        if (itemIndex >= 0 && itemIndex < maxDays) {
            lazyListState.scrollToItem(if (itemIndex - 2 < 0) 0 else itemIndex - 2)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "اضغط لاختيار يوم التسجيل:" else "Tap to choose transaction day:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            // Show selected date badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = selectedDateStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        LazyRow(
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(calendarDays) { dayItem ->
                val isSelected = selectedDateStr == dayItem.dateStr

                Card(
                    modifier = Modifier
                        .width(52.dp)
                        .clickable { onDateSelected(dayItem.dateStr) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = dayItem.dayAbbrev,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dayItem.dayNum.toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: FinanceViewModel,
    incomes: List<Income>,
    expenses: List<Expense>,
    appSettings: AppSettings,
    customCategories: List<CustomCategory>,
    onBackToHome: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabFilter by remember { mutableStateOf("All") } // All, Income, Expenses
    var selectedCategoryFilter by remember { mutableStateOf("All") } // All, or specific category
    var showDeleteConfirmDialog by remember { mutableStateOf<TransactionItem?>(null) }

    val isArabic = appSettings.language == "Arabic"

    // Consolidate both transaction sets chronologically (from newest to oldest)
    val transactionsList = remember(incomes, expenses) {
        val mappedIncomes = incomes.map {
            TransactionItem(
                id = it.id,
                name = it.name,
                amount = it.amount,
                category = it.category,
                timestamp = it.timestamp,
                notes = it.notes,
                isIncome = true,
                originalIncome = it
            )
        }
        val mappedExpenses = expenses.map {
            TransactionItem(
                id = it.id,
                name = it.name,
                amount = it.amount,
                category = it.category,
                timestamp = it.timestamp,
                notes = it.notes,
                isIncome = false,
                originalExpense = it
            )
        }
        (mappedIncomes + mappedExpenses).sortedByDescending { it.timestamp }
    }

    // Filtered Transactions
    val filteredTransactions = remember(transactionsList, searchQuery, selectedTabFilter, selectedCategoryFilter) {
        transactionsList.filter { item ->
            // Tab filter
            val matchesTab = when (selectedTabFilter) {
                "Income" -> item.isIncome
                "Expenses" -> !item.isIncome
                else -> true
            }

            // Category filter
            val matchesCategory = selectedCategoryFilter == "All" || item.category == selectedCategoryFilter

            // Search query filter
            val matchesQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true) ||
                    item.notes.contains(searchQuery, ignoreCase = true)

            matchesTab && matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isArabic) "ابحث عن معاملة (الاسم، فئة، ملاحظة)..." else "Search description, category...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Type Filter Tabs (Chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Income", "Expenses")
            val labels = if (isArabic) listOf("كل المعاملات", "الدخل فقط", "المصاريف فقط") else listOf("All Items", "Income Only", "Expenses Only")
            
            filters.forEachIndexed { i, filter ->
                val isSelected = selectedTabFilter == filter
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedTabFilter = filter
                        selectedCategoryFilter = "All" // Reset category filter on changing type
                    },
                    label = { Text(labels[i], fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = if (filter == "Income") PremiumAccentMint else if (filter == "Expenses") PremiumAccentRed else MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Category Filter list (Scroller)
        val allCategories = remember(selectedTabFilter, customCategories) {
            val standardList = if (selectedTabFilter == "Income") {
                listOf("Salary", "Freelance", "Investment", "Rental", "Business", "Bonus", "Pension", "Dividends", "Royalties", "Commission", "Side Income", "Other")
            } else if (selectedTabFilter == "Expenses") {
                listOf("Food", "Transport", "Housing", "Healthcare", "Entertainment", "Shopping", "Utilities", "Education", "Other")
            } else {
                listOf("Food", "Transport", "Housing", "Healthcare", "Entertainment", "Shopping", "Utilities", "Education", "Salary", "Freelance", "Investment", "Other")
            }
            val customNames = customCategories.map { it.name }
            listOf("All") + (standardList + customNames).distinct()
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            items(allCategories) { category ->
                val isSelected = selectedCategoryFilter == category
                val dispLabel = if (category == "All") (if (isArabic) "جميع الفئات" else "All Categories") else LocalizedStrings.localizeCategory(category, isArabic)
                
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = category },
                    label = { Text(dispLabel, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        // Stats card for currently loaded & filtered view
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (isArabic) "المعاملات المصفاة: " else "Filtered transactions: ") + "${filteredTransactions.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Instructions banner for quick usage
                Text(
                    text = if (isArabic) "💡 اضغط مطولاً للحذف السريع" else "💡 Long-press to quick delete",
                    fontSize = 11.sp,
                    color = PremiumAccentOrange,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Transaction list of items
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = PremiumTextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isArabic) "لم يتم العثور على أي معاملات بحسب فلاتر البحث الحالية" else "No matching transactions found with current filters",
                        textAlign = TextAlign.Center,
                        color = PremiumTextSecondaryDark,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredTransactions, key = { "${if(it.isIncome) "inc" else "exp"}_${it.id}" }) { trans ->
                    TransactionHistoryRowItem(
                        item = trans,
                        viewModel = viewModel,
                        isArabic = isArabic,
                        onLongClick = {
                            showDeleteConfirmDialog = trans
                        }
                    )
                }
            }
        }
    }

    // Deletion Modal Dialog
    if (showDeleteConfirmDialog != null) {
        val selectedTrans = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    if (isArabic) "حذف سريع للمعاملة" else "Quick Delete Transaction",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isArabic) {
                        "هل أنت متأكد من رغبتك في حذف المعاملة \"${selectedTrans.name}\" بقيمة ${viewModel.formatAmount(selectedTrans.amount)}؟ لن تتمكن من استرجاع هذا الإجراء."
                    } else {
                        "Are you sure you want to delete \"${selectedTrans.name}\" with amount ${viewModel.formatAmount(selectedTrans.amount)}? This action is irreversible."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTrans.isIncome && selectedTrans.originalIncome != null) {
                            viewModel.deleteIncome(selectedTrans.originalIncome)
                        } else if (!selectedTrans.isIncome && selectedTrans.originalExpense != null) {
                            viewModel.deleteExpense(selectedTrans.originalExpense)
                        }
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentRed)
                ) {
                    Text(if (isArabic) "نعم، احذف المعاملة" else "Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = null }
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionHistoryRowItem(
    item: TransactionItem,
    viewModel: FinanceViewModel,
    isArabic: Boolean,
    onLongClick: () -> Unit
) {
    // Elegant slide-up and fade entering animation
    val animState = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(key1 = item.id) {
        animState.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 400,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    val alpha = animState.value
    val density = androidx.compose.ui.platform.LocalDensity.current
    val slideUpPx = remember(density) { with(density) { 16.dp.toPx() } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = (1f - alpha) * slideUpPx
            }
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circle visual indicator of category/type
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isIncome) PremiumAccentMint.copy(alpha = 0.12f) else PremiumAccentRed.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isIncome) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = if (item.isIncome) PremiumAccentMint else PremiumAccentRed,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.localizeCategory(item.category, isArabic),
                            fontSize = 11.sp,
                            color = if (item.isIncome) PremiumAccentMint else PremiumAccentPurple,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = PremiumTextSecondaryDark
                        )
                        Text(
                            text = viewModel.formatTimestampToDate(item.timestamp),
                            fontSize = 11.sp,
                            color = PremiumTextSecondaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount formatted with currency symbol
            Text(
                text = (if (item.isIncome) "+" else "-") + viewModel.formatAmount(item.amount),
                color = if (item.isIncome) PremiumAccentMint else PremiumAccentRed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }
    }
}

data class TransactionItem(
    val id: Int,
    val name: String,
    val amount: Double,
    val category: String,
    val timestamp: Long,
    val notes: String = "",
    val isIncome: Boolean,
    val originalIncome: Income? = null,
    val originalExpense: Expense? = null
)

@Composable
fun SpendingBreakdownChart(
    totalExpenses: Double,
    totalBills: Double,
    topSpending: String,
    appSettings: AppSettings,
    viewModel: FinanceViewModel,
    isAmountMasked: Boolean,
    onChangeTab: (String) -> Unit
) {
    val isArabic = appSettings.language == "Arabic"
    val animationState = remember { androidx.compose.animation.core.Animatable(0f) }
    
    // Animate representation when total amounts change
    LaunchedEffect(totalExpenses, totalBills) {
        animationState.snapTo(0f)
        animationState.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 1000, 
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    val sumTotal = totalExpenses + totalBills
    val formatMasked = { amt: Double ->
        if (isAmountMasked) "••••" else viewModel.formatAmount(amt)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LocalizedStrings.get("spending_breakdown", isArabic),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Pulsing visual indicator
                Surface(
                    color = PremiumAccentOrange.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isArabic) "رسم بياني تفاعلي" else "Interactive Chart",
                        color = PremiumAccentOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left dynamic Donut Chart container
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(110.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        
                        // Draw grey background circle
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.10f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )

                        if (sumTotal > 0f) {
                            val expFraction = (totalExpenses / sumTotal).toFloat()
                            val billsFraction = (totalBills / sumTotal).toFloat()
                            
                            val expAngle = 360f * expFraction * animationState.value
                            val billsAngle = 360f * billsFraction * animationState.value
                            
                            val startAngle = -90f
                            
                            // Draw Expenses Slice
                            if (expAngle > 0f) {
                                drawArc(
                                    color = PremiumAccentRed,
                                    startAngle = startAngle,
                                    sweepAngle = expAngle,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }
                            
                            // Draw Bills Slice
                            if (billsAngle > 0f) {
                                drawArc(
                                    color = PremiumAccentPurple,
                                    startAngle = startAngle + expAngle,
                                    sweepAngle = billsAngle,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }
                        }
                    }

                    // Total Label in the center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isArabic) "المنصرم" else "Total Spent",
                            color = PremiumTextSecondaryDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatMasked(sumTotal),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Right detailed Legends and Top Spending Highlight
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val expPercent = if (sumTotal > 0.0) totalExpenses / sumTotal else 0.0
                    val billsPercent = if (sumTotal > 0.0) totalBills / sumTotal else 0.0
                    
                    // Expense row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChangeTab("expenses") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PremiumAccentRed))
                            Text(
                                text = if (isArabic) "المصاريف" else "Expenses",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "%d%%".format((expPercent * 100).toInt()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumAccentRed
                        )
                    }

                    // Unpaid/Paid Bills row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChangeTab("bills") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PremiumAccentPurple))
                            Text(
                                text = if (isArabic) "الفواتير" else "Bills",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "%d%%".format((billsPercent * 100).toInt()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumAccentPurple
                        )
                    }

                    // Top category premium panel
                    if (topSpending != "None" && topSpending.isNotBlank() && topSpending != "Other") {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PremiumAccentOrange.copy(alpha = 0.08f))
                                .border(0.5.dp, PremiumAccentOrange.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = PremiumAccentOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isArabic) "الأكثر إنفاقاً" else "Top Category",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PremiumAccentOrange
                                    )
                                    Text(
                                        text = LocalizedStrings.localizeCategory(topSpending, isArabic),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.PremiumTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    activeColor: Color
) {
    val scale by animateFloatAsState(if (selected) 1.05f else 1.0f, label = "tab_scale")
    
    Box(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
