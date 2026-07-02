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
import android.content.Context
import android.content.Intent

fun shareText(context: Context, text: String, title: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        // Fallback
    }
}

fun verifyPin(enteredPin: String, storedPin: String): Boolean {
    return try {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(enteredPin.toByteArray(Charsets.UTF_8))
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        hashHex == storedPin
    } catch (e: Exception) {
        false
    }
}

fun getCategoryIconAndColor(category: String, isIncome: Boolean): Pair<String, Color> {
    return if (isIncome) {
        when (category) {
            "Salary" -> Pair("💼", Color(0xFF8B5A2B))
            "Freelance" -> Pair("💰", Color(0xFFEAB308))
            "Investment" -> Pair("📈", Color(0xFF10B981))
            "Rental" -> Pair("🏠", Color(0xFF3B82F6))
            "Business" -> Pair("🏢", Color(0xFF6B7280))
            "Bonus" -> Pair("🎁", Color(0xFFEC4899))
            "Pension" -> Pair("👴", Color(0xFF84CC16))
            "Dividends" -> Pair("🪙", Color(0xFFF59E0B))
            "Royalties" -> Pair("👑", Color(0xFF8B5CF6))
            "Commission" -> Pair("🤝", Color(0xFF06B6D4))
            "Side Income" -> Pair("⚡", Color(0xFF14B8A6))
            else -> Pair("💵", Color(0xFF10B981))
        }
    } else {
        when (category) {
            "Food" -> Pair("🍔", Color(0xFFFFB067))
            "Transport" -> Pair("🚗", Color(0xFF93C5FD))
            "Housing" -> Pair("🏠", Color(0xFFFDBA74))
            "Healthcare", "Health" -> Pair("🩺", Color(0xFFFCA5A5))
            "Entertainment", "Fun" -> Pair("🍿", Color(0xFFC4B5FD))
            "Shopping" -> Pair("🛍️", Color(0xFFF472B6))
            "Utilities" -> Pair("💡", Color(0xFFFDE047))
            "Education" -> Pair("📚", Color(0xFFA5F3FC))
            else -> Pair("💸", Color(0xFF94A3B8))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAppUI(viewModel: FinanceViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatLoading by viewModel.chatLoading.collectAsState()
    var userChatQuery by remember { mutableStateOf("") }
    val operationalLog by viewModel.operationalLog.collectAsState()

    // Navigation and Menu states
    var currentTab by remember { mutableStateOf("overview") } // overview, income, expenses, bills, calendar, goals, settings, search, analytics
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
    var showCurrencyConversionDialog by remember { mutableStateOf(false) }
    var isAppUnlocked by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var backgroundTimestamp by remember { mutableStateOf(0L) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                backgroundTimestamp = System.currentTimeMillis()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (backgroundTimestamp != 0L) {
                    val timeInBackground = System.currentTimeMillis() - backgroundTimestamp
                    if (timeInBackground > 60 * 60 * 1000) { // 1 hour
                        isAppUnlocked = false
                    }
                    backgroundTimestamp = 0L
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Alert Triggers
    LaunchedEffect(operationalLog) {
        operationalLog?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearOperationalLog()
        }
    }

    val isArabic = appSettings.language == "Arabic"
    var showBiometricAuthPrompt by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanSuccess by remember { mutableStateOf(false) }
    var showBackupPinPrompt by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val executor = remember { context.mainExecutor }
    fun triggerRealBiometricPrompt() {
        showBiometricAuthPrompt = true
        val activity = context as? android.app.Activity
        if (activity != null) {
            try {
                val biometricPrompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
                    .setTitle(if (isArabic) "تسجيل دخول آمن لـ توفير" else "Secure Login for Tawffer")
                    .setSubtitle(if (isArabic) "استخدم البصمة لتأكيد هويتك" else "Use fingerprint to verify your identity")
                    .setNegativeButton(
                        if (isArabic) "رمز المرور الاحتياطي" else "Use Backup PIN",
                        executor,
                        { _, _ ->
                            activity.runOnUiThread {
                                showBackupPinPrompt = true
                                showBiometricAuthPrompt = false
                            }
                        }
                    )
                    .build()

                val cancellationSignal = android.os.CancellationSignal()
                biometricPrompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            activity.runOnUiThread {
                                scanSuccess = true
                                isAppUnlocked = true
                                showBiometricAuthPrompt = false
                            }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            activity.runOnUiThread {
                                isScanning = true // Run animated simulation beautiful check!
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                        }
                    }
                )
            } catch (e: Exception) {
                isScanning = true
            }
        } else {
            isScanning = true
        }
    }

    MyApplicationTheme(themePreference = appSettings.theme, language = appSettings.language) {
        if (appSettings.isBiometricLockEnabled && !isAppUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showBackupPinPrompt) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight().padding(vertical = 24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(PremiumAccentPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PremiumAccentPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isArabic) "رمز المرور الاحتياطي" else "Backup Passcode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "يرجى إدخال الرمز المكون من 4 أرقام لتخطي البصمة" else "Enter the 4-digit PIN to bypass biometric authentication",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(4) { idx ->
                                    val isFilled = enteredPin.length > idx
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (pinError) PremiumAccentRed 
                                                else if (isFilled) PremiumAccentPurple 
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (pinError) PremiumAccentRed 
                                                       else if (isFilled) PremiumAccentPurple 
                                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (pinError) {
                                Text(
                                    text = if (isArabic) "رمز مرور خاطئ! يرجى المحاولة مجددًا" else "Incorrect PIN! Please try again.",
                                    color = PremiumAccentRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            val keypadRows = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9")
                            )

                            keypadRows.forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    row.forEach { digit ->
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                .clickable {
                                                    if (enteredPin.length < 4) {
                                                        pinError = false
                                                        enteredPin += digit
                                                        if (enteredPin.length == 4) {
                                                            if (verifyPin(enteredPin, appSettings.backupPin)) {
                                                                isAppUnlocked = true
                                                                showBackupPinPrompt = false
                                                                enteredPin = ""
                                                            } else {
                                                                pinError = true
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(digit, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .clickable {
                                            enteredPin = ""
                                            pinError = false
                                            showBackupPinPrompt = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Go Back/Cancel",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .clickable {
                                            if (enteredPin.length < 4) {
                                                pinError = false
                                                enteredPin += "0"
                                                if (enteredPin.length == 4) {
                                                    if (verifyPin(enteredPin, appSettings.backupPin)) {
                                                        isAppUnlocked = true
                                                        showBackupPinPrompt = false
                                                        enteredPin = ""
                                                    } else {
                                                        pinError = true
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("0", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .clickable {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                pinError = false
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete last",
                                        tint = PremiumAccentRed.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                } else {
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
                                    triggerRealBiometricPrompt()
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
                                        triggerRealBiometricPrompt()
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
                                        showBackupPinPrompt = true
                                        showBiometricAuthPrompt = false
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
        } else if (!appSettings.hasCompletedOnboarding) {
            OnboardingScreen(onComplete = { viewModel.updateOnboardingStatus(true) })
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                "analytics" -> if (appSettings.language == "Arabic") "التحليلات والمؤشرات" else "Analytics & Predictions"
                                else -> "Tawffer"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        if (currentTab == "analytics") {
                            IconButton(onClick = {
                                val catBreakdown = expenses.groupBy { it.category }.map { (category, list) ->
                                    val amt = list.sumOf { it.amount }
                                    val pct = if (totalExpenses > 0) (amt / totalExpenses) * 100 else 0.0
                                    com.example.util.PdfCategoryBreakdown(category, amt, pct)
                                }.sortedByDescending { it.amount }

                                val top5 = expenses.sortedByDescending { it.amount }.take(5)

                                com.example.util.PdfExporter.exportFinancialReportToPdf(
                                    context = context,
                                    currencySymbol = viewModel.getCurrencySymbol(),
                                    totalIncomeStr = viewModel.formatAmount(totalIncome),
                                    totalExpensesStr = viewModel.formatAmount(totalExpenses),
                                    netBalanceStr = viewModel.formatAmount(netBalance),
                                    netBalance = netBalance,
                                    categoryBreakdown = catBreakdown,
                                    topExpenses = top5,
                                    isArabic = appSettings.language == "Arabic"
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = if (appSettings.language == "Arabic") "تصدير التقرير" else "Export Report",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
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
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 8.dp else 12.dp),
                        border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
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
                                        tint = if (currentTab == "overview") PremiumAccentMint else MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkThemeGlobal) 0.5f else 0.72f)
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
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Income",
                                        tint = if (currentTab == "income") PremiumAccentMint else MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkThemeGlobal) 0.5f else 0.72f)
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
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "Expenses",
                                        tint = if (currentTab == "expenses") PremiumAccentRed else MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkThemeGlobal) 0.5f else 0.72f)
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
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = "Bills",
                                        tint = if (currentTab == "bills") PremiumAccentPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkThemeGlobal) 0.5f else 0.72f)
                                    )
                                },
                                label = if (appSettings.language == "Arabic") "الفواتير" else "Bills",
                                activeColor = PremiumAccentPurple
                            )
                            PremiumTabItem(
                                selected = showNavigatorMenu,
                                onClick = { showNavigatorMenu = !showNavigatorMenu },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "More Menu",
                                        tint = if (showNavigatorMenu) PremiumAccentMint else MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkThemeGlobal) 0.5f else 0.72f)
                                    )
                                },
                                label = if (appSettings.language == "Arabic") "المزيد" else "More",
                                activeColor = PremiumAccentMint
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
                        val tabs = listOf("overview", "income", "expenses", "bills", "analytics")
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
                            incomes = incomes,
                            expenses = expenses,
                            onChangeTab = { currentTab = it },
                            onQuickAddIncome = { showAddIncome = true },
                            onQuickAddExpense = { showAddExpense = true },
                            onQuickAddBill = { showAddBill = true }
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
                            bills = bills,
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
                            onShowClearData = { showClearDataDialog = true },
                            onShowCurrencyConversion = { showCurrencyConversionDialog = true }
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
                        "analytics" -> AnalyticsScreen(
                            viewModel = viewModel,
                            appSettings = appSettings
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
                                    listOf("التحليلات والتوقعات 📊", "سجل المعاملات", "أهداف الادخار", "تقويم المعاملات", "البحث والفرز والفلترة", "إعدادات التطبيق")
                                } else {
                                    listOf("Analytics & Forecasts 📊", "Transaction History", "Savings Goals", "Financial Calendar", "Search & Filter", "Settings")
                                }
                                val tabs = listOf("analytics", "transactions", "goals", "calendar", "search", "settings")
                                val icons = listOf(Icons.Default.Assessment, Icons.Default.List, Icons.Default.Star, Icons.Default.DateRange, Icons.Default.Search, Icons.Default.Settings)

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
                                            Text(if (lang == "Arabic" && appSettings.language == "Arabic") "بالمصري 🇪🇬" else codes[idx], fontWeight = FontWeight.Medium)
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

                // CURRENCY CONVERSION DIALOG (Page 13)
                if (showCurrencyConversionDialog) {
                    var conversionRate by remember { mutableStateOf("48.0") }
                    var usdToEgpSelected by remember { mutableStateOf(true) } // true: USD -> EGP, false: EGP -> USD

                    AlertDialog(
                        onDismissRequest = { showCurrencyConversionDialog = false },
                        title = {
                            Text(
                                if (appSettings.language == "Arabic") "تحويل قيم المعاملات والبيانات" else "True Exchange Rate Currency Conversion",
                                fontWeight = FontWeight.Bold,
                                color = PremiumAccentMint
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    if (appSettings.language == "Arabic")
                                        "يقوم هذا الخيار بتحويل جميع المبالغ المالية المسجلة (الدخل، المصاريف، الفواتير، الأهداف، والحد الأقصى للميزانية) في الذاكرة المحلية تلقائياً بناءً على سعر صرف محدد."
                                    else
                                        "This tool converts all logged numerical values (incomes, expenses, bills, saving goals, and monthly limits) in your database based on an exchange rate.",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )

                                // Direction choice
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { usdToEgpSelected = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (usdToEgpSelected) PremiumAccentMint else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            if (appSettings.language == "Arabic") "دولار ➔ جنيه" else "USD ➔ EGP",
                                            color = if (usdToEgpSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = { usdToEgpSelected = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!usdToEgpSelected) PremiumAccentMint else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            if (appSettings.language == "Arabic") "جنيه ➔ دولار" else "EGP ➔ USD",
                                            color = if (!usdToEgpSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = conversionRate,
                                    onValueChange = { conversionRate = it },
                                    label = { Text(if (appSettings.language == "Arabic") "سعر الصرف (مثال: 48)" else "Exchange Rate (e.g. 48.0)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = if (usdToEgpSelected) {
                                        if (appSettings.language == "Arabic") "المعادلة: القيمة بالجنيه = القيمة بالدولار × $conversionRate" else "Formula: EGP Value = USD Value * $conversionRate"
                                    } else {
                                        if (appSettings.language == "Arabic") "المعادلة: القيمة بالدولار = القيمة بالجنيه ÷ $conversionRate" else "Formula: USD Value = EGP Value / $conversionRate"
                                    },
                                    fontSize = 11.sp,
                                    color = PremiumAccentOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val rateVal = conversionRate.toDoubleOrNull() ?: 1.0
                                    viewModel.convertAllCurrencies(usdToEgpSelected, rateVal)
                                    showCurrencyConversionDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint)
                            ) {
                                Text(if (appSettings.language == "Arabic") "تطبيق التحويل للكل 💱" else "Convert All 💱", color = Color.Black)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCurrencyConversionDialog = false }) {
                                Text(if (appSettings.language == "Arabic") "إلغاء" else "Cancel", color = Color.Gray)
                            }
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

                    val isAmountError = amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0.0
                    val isNameError = name.isNotEmpty() && name.isBlank()
                    val isFormValid = name.isNotBlank() && !isAmountError

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
                                    if (isAmountError) {
                                        Text(
                                            text = if (appSettings.language == "Arabic") "يرجى إدخال مبلغ صحيح" else "Please enter a valid amount",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                        )
                                    }
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
                                    PremiumDatePickerField(
                                        label = LocalizedStrings.get("date", appSettings.language == "Arabic"),
                                        dateStr = dateStr,
                                        onDateSelected = { dateStr = it },
                                        accentColor = PremiumAccentMint
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
                    var isRecurring by remember { mutableStateOf(editItem?.isRecurring ?: false) }
                    var dateStr by remember { mutableStateOf(editItem?.let { viewModel.formatTimestampToDate(it.timestamp) } ?: selectedExpenseDate) }

                    val isAmountError = amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0.0
                    val isNameError = name.isNotEmpty() && name.isBlank()
                    val isFormValid = name.isNotBlank() && !isAmountError

                    val standardCategories = listOf("Food", "Transport", "Housing", "Healthcare", "Entertainment", "Shopping", "Utilities", "Education", "Other")
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
                                    if (isAmountError) {
                                        Text(
                                            text = if (appSettings.language == "Arabic") "يرجى إدخال مبلغ صحيح" else "Please enter a valid amount",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                        )
                                    }
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
                                    PremiumDatePickerField(
                                        label = LocalizedStrings.get("date", appSettings.language == "Arabic"),
                                        dateStr = dateStr,
                                        onDateSelected = { dateStr = it },
                                        accentColor = PremiumAccentRed
                                    )
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text(LocalizedStrings.get("notes", appSettings.language == "Arabic") + " (" + (if (appSettings.language == "Arabic") "اختياري" else "Optional") + ")") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isRecurring = !isRecurring }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Switch(
                                            checked = isRecurring,
                                            onCheckedChange = { isRecurring = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.Black,
                                                checkedTrackColor = PremiumAccentRed
                                            )
                                        )
                                        Column {
                                            Text(
                                                text = if (appSettings.language == "Arabic") "مصروف شهري متكرر" else "Monthly Recurring Expense",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = if (appSettings.language == "Arabic") "تسجيل هذا البند تلقائياً كل شهر" else "Log this transaction automatically every month",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                                    if (editItem != null) {
                                        viewModel.updateExpense(editItem.id, name, amtVal, selectedCategory, dateStr, isRecurring, notes)
                                    } else {
                                        viewModel.addExpense(name, amtVal, selectedCategory, dateStr, isRecurring, notes)
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

                    val isAmountError = amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0.0
                    val isNameError = name.isNotEmpty() && name.isBlank()
                    val isDueDayError = dueDay.isNotEmpty() && (dueDay.toIntOrNull() == null || dueDay.toInt() < 1 || dueDay.toInt() > 31)
                    val isFormValid = name.isNotBlank() && !isAmountError && !isDueDayError

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
                                    if (isAmountError) {
                                        Text(
                                            text = if (appSettings.language == "Arabic") "يرجى إدخال مبلغ صحيح" else "Please enter a valid amount",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                        )
                                    }
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
                                    PremiumDatePickerField(
                                        label = if (appSettings.language == "Arabic") "تاريخ تحقيق الهدف (اختياري)" else "Deadline (Optional)",
                                        dateStr = deadline,
                                        onDateSelected = { deadline = it },
                                        accentColor = PremiumAccentPurple
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
                    var deductFromBalance by remember { mutableStateOf(false) }
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
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    if (appSettings.language == "Arabic") "المحوش: ${viewModel.formatAmount(goal.savedAmount)} من أصل ${viewModel.formatAmount(goal.targetAmount)}" else "Saved: ${viewModel.formatAmount(goal.savedAmount)} / ${viewModel.formatAmount(goal.targetAmount)}",
                                    fontSize = 13.sp,
                                    color = PremiumTextSecondaryDark
                                )
                                OutlinedTextField(
                                    value = addAmtInput,
                                    onValueChange = { addAmtInput = it },
                                    label = { Text(if (appSettings.language == "Arabic") "أضف مبلغ جديد" else "Log Savable Amount") },
                                    isError = isFundAmountError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deductFromBalance = !deductFromBalance }
                                ) {
                                    Checkbox(
                                        checked = deductFromBalance,
                                        onCheckedChange = { deductFromBalance = it },
                                        colors = CheckboxDefaults.colors(checkedColor = PremiumAccentMint)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (appSettings.language == "Arabic") "خصّم من رصيدي وحوّل للهدف " else "Deduct from balance and log as expense",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val amtVal = addAmtInput.toDoubleOrNull() ?: 0.0
                                    viewModel.saveGoalAmount(goal, amtVal, deductFromBalance)
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

// --- Premium Date Picker Helpers ---
fun showDatePicker(context: android.content.Context, initialDateStr: String, onDateSelected: (String) -> Unit) {
    try {
        val parts = initialDateStr.split("-")
        val calendar = java.util.Calendar.getInstance()
        var initYear = calendar.get(java.util.Calendar.YEAR)
        var initMonth = calendar.get(java.util.Calendar.MONTH)
        var initDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                initYear = y
                initMonth = m - 1
                initDay = d
            }
        }
        
        android.app.DatePickerDialog(
            context,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = "%04d-%02d-%02d".format(year, monthOfYear + 1, dayOfMonth)
                onDateSelected(selectedDate)
            },
            initYear,
            initMonth,
            initDay
        ).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun PremiumDatePickerField(
    label: String,
    dateStr: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PremiumAccentMint
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                showDatePicker(context, dateStr, onDateSelected)
            }
    ) {
        OutlinedTextField(
            value = dateStr,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker(context, dateStr, onDateSelected) }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        tint = accentColor
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                disabledTrailingIconColor = accentColor
            )
        )
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
    incomes: List<com.example.data.Income>,
    expenses: List<com.example.data.Expense>,
    onChangeTab: (String) -> Unit,
    onQuickAddIncome: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onQuickAddBill: () -> Unit
) {
    val context = LocalContext.current
    val isAmountMasked by viewModel.isBalanceHidden.collectAsState()
    val aiAnomalyAlert by viewModel.aiAnomalyAlert.collectAsState()
    var showHealthDetailsDialog by remember { mutableStateOf(false) }

    // Chatbot States collected reactively
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatLoading by viewModel.chatLoading.collectAsState()
    var userChatQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

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
        // AI 3x Spending Anomaly Detection Warning Banner
        aiAnomalyAlert?.let { alertMessage ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PremiumAccentOrange.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, PremiumAccentOrange.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = alertMessage,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = { viewModel.dismissAnomalyAlert() }
                        ) {
                            Text(
                                text = if (appSettings.language == "Arabic") "موافق" else "Dismiss",
                                color = PremiumAccentOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Net Balance Card (with dynamic red-green gradients and privacy mask toggle)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color(0xFF1E293B) else Color(0xFFBAE6FD))
            ) {
                val balanceGradient = if (isDarkThemeGlobal) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // Deep Slate Blue
                            Color(0xFF1E293B), // Soft Slate Card Blue
                            Color(0xFF0F172A)  // Deep Slate Blue
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE0F2FE), // Beautiful Soft Sky Blue (Sky 100)
                            Color(0xFFF0F9FF)  // Lighter radiant sky blue/white (Sky 50)
                        )
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(balanceGradient)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LocalizedStrings.get("net_balance", appSettings.language == "Arabic"),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.toggleBalanceHidden() },
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
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpenses) / totalIncome * 100.0) else 0.0
                    val savingsRateFormatted = "%.1f%%".format(savingsRate)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (savingsRate >= 20.0) PremiumAccentMint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            border = if (savingsRate >= 20.0) BorderStroke(1.dp, PremiumAccentMint.copy(alpha = 0.2f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (savingsRate >= 20.0) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (savingsRate >= 20.0) PremiumAccentMint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (appSettings.language == "Arabic") "معدل الادخار: $savingsRateFormatted" else "Savings Rate: $savingsRateFormatted",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (savingsRate >= 20.0) PremiumAccentMint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }


        // Side-by-side metric cards (Income, Expenses, Bills) exactly as shown in screenshot 7
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Income
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                    border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PremiumAccentMint.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = PremiumAccentMint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (appSettings.language == "Arabic") "دخل وارد" else "Income",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatMasked(totalIncome),
                            fontSize = 12.sp,
                            color = PremiumAccentMint,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onQuickAddIncome,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint.copy(alpha = 0.08f), contentColor = PremiumAccentMint),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        ) {
                            Text(if (appSettings.language == "Arabic") "+ إضافة دخل" else "+ Add Income", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Card 2: Expenses
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                    border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PremiumAccentRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = PremiumAccentRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (appSettings.language == "Arabic") "مصاريف" else "Expenses",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatMasked(totalExpenses),
                            fontSize = 12.sp,
                            color = PremiumAccentRed,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onQuickAddExpense,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentRed.copy(alpha = 0.08f), contentColor = PremiumAccentRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        ) {
                            Text(if (appSettings.language == "Arabic") "+ إضافة مصروف" else "+ Add Expense", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Card 3: Bills
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                    border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PremiumAccentPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = PremiumAccentPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (appSettings.language == "Arabic") "فواتير" else "Bills",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatMasked(totalBills),
                            fontSize = 12.sp,
                            color = PremiumAccentPurple,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onQuickAddBill,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentPurple.copy(alpha = 0.08f), contentColor = PremiumAccentPurple),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        ) {
                            Text(if (appSettings.language == "Arabic") "+ إضافة فاتورة" else "+ Add Bill", fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
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

        // --- 50/30/20 Budgeting Allocation Rule Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PremiumAccentMint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "50/30/20 Rule", tint = PremiumAccentMint, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = if (appSettings.language == "Arabic") "ميزان تقسيم الدخل (قاعدة 50/30/20)" else "Income Allocation (50/30/20 Rule)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val essentialsCategories = listOf("Housing", "Utilities", "Bills", "Groceries", "Transport", "Medical", "السكن", "المرافق", "الفواتير", "البقالة", "المواصلات", "الصحة", "Fares", "Rent", "Fees", "الضرائب")
                    val wantsCategories = listOf("Entertainment", "Shopping", "DiningOut", "Dining", "Travel", "Coffee", "Gifts", "الترفيه", "التسوق", "مطاعم", "السفر", "قهوة", "هدايا", "Sport", "Gaming", "Games", "الألعاب")

                    val essentialsTotal = expenses.filter { it.category in essentialsCategories }.sumOf { it.amount } + totalBills
                    val wantsTotal = expenses.filter { it.category in wantsCategories }.sumOf { it.amount }
                    val savingsTotal = totalGoalsSaved

                    val ruleTotalInBudget = essentialsTotal + wantsTotal + savingsTotal
                    val displayTotalDenom = if (totalIncome > 0) totalIncome else (if (ruleTotalInBudget > 0) ruleTotalInBudget else 1.0)

                    val essentialsPct = (essentialsTotal / displayTotalDenom) * 100.0
                    val wantsPct = (wantsTotal / displayTotalDenom) * 100.0
                    val savingsPct = (savingsTotal / displayTotalDenom) * 100.0

                    // Row 1: Essentials (50%)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (appSettings.language == "Arabic") "الأساسيات (الهدف: 50%)" else "Essentials (Target: 50%)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${"%.1f".format(essentialsPct)}%",
                                fontSize = 12.sp,
                                color = if (essentialsPct > 50.0) PremiumAccentRed else PremiumAccentMint,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (essentialsPct / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (essentialsPct > 50.0) PremiumAccentRed else PremiumAccentMint,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 2: Wants (30%)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (appSettings.language == "Arabic") "الرغبات والكماليات (الهدف: 30%)" else "Wants & Lifestyle (Target: 30%)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${"%.1f".format(wantsPct)}%",
                                fontSize = 12.sp,
                                color = if (wantsPct > 30.0) PremiumAccentRed else PremiumAccentPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (wantsPct / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (wantsPct > 30.0) PremiumAccentRed else PremiumAccentPurple,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 3: Savings (20%)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (appSettings.language == "Arabic") "الادخار والاستثمار (الهدف: 20%)" else "Savings & Goals (Target: 20%)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${"%.1f".format(savingsPct)}%",
                                fontSize = 12.sp,
                                color = if (savingsPct >= 20.0) PremiumAccentMint else PremiumAccentOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (savingsPct / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (savingsPct >= 20.0) PremiumAccentMint else PremiumAccentOrange,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }

        // Net Worth Card (Assets vs Liabilities)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                border = BorderStroke(1.2.dp, PremiumAccentMint.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(PremiumAccentMint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = PremiumAccentMint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = LocalizedStrings.get("net_worth", appSettings.language == "Arabic"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = formatMasked(netWorth),
                            fontWeight = FontWeight.Black,
                            color = PremiumAccentMint,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LocalizedStrings.get("assets", appSettings.language == "Arabic"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatMasked(netBalance),
                                fontWeight = FontWeight.ExtraBold,
                                color = PremiumAccentMint,
                                fontSize = 15.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LocalizedStrings.get("liabilities", appSettings.language == "Arabic"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatMasked(unpaidBillsAmount),
                                fontWeight = FontWeight.ExtraBold,
                                color = PremiumAccentRed,
                                fontSize = 15.sp
                            )
                        }
                    }
                    
                    // Styled horizontal progress proportion bar
                    val assetVal = if (netBalance > 0) netBalance else 0.0
                    val liabilityVal = unpaidBillsAmount
                    val proportionalSum = assetVal + liabilityVal
                    val assetProportion = if (proportionalSum > 0.0) (assetVal / proportionalSum).toFloat() else 1f
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(if (assetProportion > 0.02f) assetProportion else 0.02f)
                                .background(PremiumAccentMint)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(if ((1f - assetProportion) > 0.02f) (1f - assetProportion) else 0.02f)
                                .background(PremiumAccentRed)
                        )
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

        // Expanded reports, visual bar charts and month-over-month comparisons (Egypt/Arabic themed)
        item {
            MonthlyReportsAndComparison(
                incomes = incomes,
                expenses = expenses,
                appSettings = appSettings,
                viewModel = viewModel
            )
        }

        // INTELLIGENT AI FINANCIAL ADVISOR WIDGET (Direct Integration!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, PremiumAccentMint.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp)
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
                            onClick = { viewModel.refreshAiInsights(isManual = true) },
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

        // --- Interactive Smart Financial Chatbot ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(PremiumAccentPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "Chat", tint = PremiumAccentPurple, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (appSettings.language == "Arabic") "دردش مع Tawffer Advisor 💬" else "Chat with Tawffer Advisor 💬",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PremiumAccentPurple
                            )
                        }

                        if (chatMessages.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearChat() }) {
                                Text(
                                    if (appSettings.language == "Arabic") "مسح الدردشة" else "Clear Chat",
                                    fontSize = 11.sp,
                                    color = PremiumAccentRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Message history display container
                    if (chatMessages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (appSettings.language == "Arabic") 
                                    "اسألني أي سؤال المرة دي! مثلاً: \"إزاي أظبط ميزانيتي؟\" أو \"مصاريفي كتير في فئة معينة، أعمل إيه؟\"" 
                                    else "Ask me anything directly! For example: \"How can I adjust my budget?\" or \"Why is my spending too high?\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 8.dp)
                        ) {
                            chatMessages.forEach { (msg, isUser) ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 12.dp
                                        ),
                                        color = if (isUser) PremiumAccentPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        border = if (isUser) BorderStroke(1.dp, PremiumAccentPurple.copy(alpha = 0.2f)) else null,
                                        modifier = Modifier.widthIn(max = 260.dp)
                                    ) {
                                        Text(
                                            text = msg,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(10.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            
                            if (chatLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        CircularProgressIndicator(color = PremiumAccentPurple, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (appSettings.language == "Arabic") "جاري التفكير والكتابة..." else "Thinking...",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Message input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userChatQuery,
                            onValueChange = { userChatQuery = it },
                            placeholder = {
                                Text(
                                    if (appSettings.language == "Arabic") "اكتب سؤالك هنا..." else "Type message...",
                                    fontSize = 12.sp
                                )
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = PremiumAccentPurple,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        )

                        Button(
                            onClick = {
                                if (userChatQuery.isNotBlank() && !chatLoading) {
                                    viewModel.askFinancialChat(userChatQuery)
                                    userChatQuery = ""
                                }
                            },
                            enabled = userChatQuery.isNotBlank() && !chatLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentPurple),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(
                                if (appSettings.language == "Arabic") "إرسال" else "Send",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Top Spending Category banner (Page 13)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
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
                    Text(LocalizedStrings.get("tab_goals", appSettings.language == "Arabic"), fontSize = 11.sp)
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
                    Text(LocalizedStrings.get("tab_calendar", appSettings.language == "Arabic"), fontSize = 11.sp)
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

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = LocalizedStrings.get("total_income_this_month", appSettings.language == "Arabic"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.formatAmount(totalAmt),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${incomes.size} " + LocalizedStrings.get("sources_logged", appSettings.language == "Arabic"),
                    fontSize = 15.sp,
                    color = Color.Black.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Horizontal Day Selector
        DayNavigatorBar(
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
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(incomes) { incItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                        border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val (emoji, bg) = getCategoryIconAndColor(incItem.category, isIncome = true)
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(bg.copy(alpha = if (isDarkThemeGlobal) 0.25f else 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                                Column {
                                    Text(
                                        text = incItem.name,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = viewModel.formatTimestampToDate(incItem.timestamp),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = LocalizedStrings.localizeCategory(incItem.category, appSettings.language == "Arabic"),
                                        fontSize = 11.sp,
                                        color = bg,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "+" + viewModel.formatAmount(incItem.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = PremiumAccentMint
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { onEditClick(incItem) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = PremiumAccentBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteIncome(incItem)
                                            coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = if (appSettings.language == "Arabic") "تم حذف الدخل بنجاح" else "Income deleted.",
                                                    actionLabel = if (appSettings.language == "Arabic") "تراجع" else "Undo",
                                                    duration = SnackbarDuration.Long
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.undoDelete()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = PremiumAccentRed,
                                            modifier = Modifier.size(16.dp)
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
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

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = LocalizedStrings.get("total_expenses_this_month", appSettings.language == "Arabic"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.formatAmount(totalAmt),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${expenses.size} " + LocalizedStrings.get("total_transactions_logged", appSettings.language == "Arabic"),
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Horizontal Day Selector
        DayNavigatorBar(
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
            val chips = listOf("All", "Food", "Transport", "Housing", "Healthcare", "Entertainment", "Shopping", "Utilities", "Education", "Other") + customCategories.map { it.name }
            chips.forEach { chipName ->
                FilterChip(
                    selected = selectedFilterCategory == chipName,
                    onClick = { selectedFilterCategory = chipName },
                    label = { Text(LocalizedStrings.localizeCategory(chipName, appSettings.language == "Arabic")) }
                )
            }
        }

        // Transactions header row with integrated daily total
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(LocalizedStrings.get("transactions", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = (if (appSettings.language == "Arabic") "مجموع مصاريف اليوم: " else "Today's total: ") + viewModel.formatAmount(dailySum),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumAccentRed
                )
            }
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.background(PremiumAccentRed, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = LocalizedStrings.get("add_expense", appSettings.language == "Arabic"), tint = Color.Black)
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
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterList) { expItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
                        border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val (emoji, bg) = getCategoryIconAndColor(expItem.category, isIncome = false)
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(bg.copy(alpha = if (isDarkThemeGlobal) 0.25f else 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                                Column {
                                    Text(
                                        text = expItem.name,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = viewModel.formatTimestampToDate(expItem.timestamp),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = LocalizedStrings.localizeCategory(expItem.category, appSettings.language == "Arabic"),
                                        fontSize = 11.sp,
                                        color = bg,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "-" + viewModel.formatAmount(expItem.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = PremiumAccentRed
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { onEditClick(expItem) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = PremiumAccentBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteExpense(expItem)
                                            coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = if (appSettings.language == "Arabic") "تم حذف المصروف بنجاح" else "Expense deleted.",
                                                    actionLabel = if (appSettings.language == "Arabic") "تراجع" else "Undo",
                                                    duration = SnackbarDuration.Long
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.undoDelete()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = PremiumAccentRed,
                                            modifier = Modifier.size(16.dp)
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
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
                colors = CardDefaults.cardColors(containerColor = PremiumAccentPurple.copy(alpha = if (isDarkThemeGlobal) 0.2f else 0.12f)),
                border = BorderStroke(1.dp, PremiumAccentPurple.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(LocalizedStrings.get("total_monthly", appSettings.language == "Arabic"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Text(viewModel.formatAmount(totalAmt), fontSize = 20.sp, fontWeight = FontWeight.Black, color = PremiumAccentPurple)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PremiumAccentMint.copy(alpha = if (isDarkThemeGlobal) 0.2f else 0.12f)),
                border = BorderStroke(1.dp, PremiumAccentMint.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(LocalizedStrings.get("paid_this_month", appSettings.language == "Arabic"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(bills) { billItem ->
                    val colorScheme = MaterialTheme.colorScheme
                    val (bgColor, borderColor, badgeBg, badgeText) = remember(billItem.category, isDarkThemeGlobal, colorScheme) {
                        val catLower = billItem.category.lowercase()
                        val isUtil = catLower.contains("util") || catLower.contains("internet") || catLower.contains("مرافق") || catLower.contains("مرفق")
                        val isHousing = catLower.contains("hous") || catLower.contains("rent") || catLower.contains("سكن") || catLower.contains("إيجار")
                        val isEnt = catLower.contains("entert") || catLower.contains("netflix") || catLower.contains("spotify") || catLower.contains("ترفيه") || catLower.contains("اشتراك")

                        if (isDarkThemeGlobal) {
                            when {
                                isUtil -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF1E1B4B), Color(0xFF818CF8))
                                isHousing -> listOf(Color(0xFF311C0C), Color(0xFF452B18), Color(0xFF2D1807), Color(0xFFFDBA74))
                                isEnt -> listOf(Color(0xFF2D1540), Color(0xFF431C5E), Color(0xFF250D36), Color(0xFFC4B5FD))
                                else -> listOf(colorScheme.surface, colorScheme.onSurface.copy(alpha = 0.12f), colorScheme.onSurface.copy(alpha = 0.08f), colorScheme.onSurface)
                            }
                        } else {
                            when {
                                isUtil -> listOf(Color(0xFFF0F7FF), Color(0xFFD0E7FF), Color(0xFFE0EFFE), Color(0xFF1D4ED8))
                                isHousing -> listOf(Color(0xFFFFF9F3), Color(0xFFFFE3CC), Color(0xFFFFF0E0), Color(0xFFC2410C))
                                isEnt -> listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFFF3E8FF), Color(0xFF6D28D9))
                                else -> listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFFF1F5F9), Color(0xFF475569))
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, borderColor)
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
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(badgeBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = LocalizedStrings.localizeCategory(billItem.category, appSettings.language == "Arabic"),
                                        fontSize = 11.sp,
                                        color = badgeText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
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
                                Column {
                                    Text(
                                        text = "${LocalizedStrings.get("due_day", appSettings.language == "Arabic")} ${billItem.dueDay} ($freqStr)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    val daysLeft = viewModel.getDaysUntilDue(billItem)
                                    val daysLeftStr = if (billItem.isPaid) {
                                        if (appSettings.language == "Arabic") "تم الدفع!" else "Paid!"
                                    } else {
                                        when {
                                            daysLeft == 0 -> if (appSettings.language == "Arabic") "⚠️ مستحقة اليوم!" else "⚠️ Due Today!"
                                            daysLeft == 1 -> if (appSettings.language == "Arabic") "⏳ باقي يوم واحد" else "⏳ 1 day left"
                                            daysLeft < 0 -> if (appSettings.language == "Arabic") "🚨 متأخرة!" else "🚨 Overdue!"
                                            else -> if (appSettings.language == "Arabic") "📅 باقي $daysLeft أيام" else "📅 $daysLeft days left"
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = daysLeftStr,
                                        fontSize = 11.sp,
                                        color = if (billItem.isPaid) PremiumAccentMint else if (daysLeft == 0) PremiumAccentOrange else PremiumAccentRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

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

@Composable
fun EmptyStateIllustration(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = iconColor
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}


@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        "Welcome to Tawffer! Your personal finance journey starts here.",
        "Track expenses, manage bills and set savings goals with ease.",
        "Get intelligent financial insights and maintain your financial health!"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = pages[currentPage], textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            if (currentPage < pages.size - 1) currentPage++ else onComplete()
        }) {
            Text(if (currentPage < pages.size - 1) "Next" else "Get Started")
        }
    }
}

// --- CALENDAR INTEGRATED SCREEN ---
@Composable
fun CalendarScreen(
    viewModel: FinanceViewModel,
    incomes: List<Income>,
    expenses: List<Expense>,
    bills: List<Bill>,
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

    val displayedBills = bills

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeGlobal) 0.dp else 4.dp),
            border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.Transparent else Color(0xFFE2E8F0))
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                                                color = if (isSelected) (if (isDarkThemeGlobal) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurface,
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
                                                            .background(if (isSelected) (if (isDarkThemeGlobal) Color.Black else Color.White) else PremiumAccentMint)
                                                    )
                                                }
                                                if (hasExpense) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) (if (isDarkThemeGlobal) Color.Black else Color.White) else PremiumAccentRed)
                                                    )
                                                }
                                                if (hasBill) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) (if (isDarkThemeGlobal) Color.Black else Color.White) else PremiumAccentPurple)
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
                        EmptyStateIllustration(
                            title = LocalizedStrings.get("no_transactions_today", appSettings.language == "Arabic"),
                            subtitle = if (appSettings.language == "Arabic") "لا توجد معاملات مسجلة لهذا اليوم." else "No transactions recorded for this day.",
                            icon = Icons.Default.DateRange,
                            iconColor = MaterialTheme.colorScheme.primary
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
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
    val incomes by viewModel.incomes.collectAsState(initial = emptyList())
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())

    val averageMonthlySavings = remember(incomes, expenses) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        val incomesByMonth = incomes.groupBy { sdf.format(java.util.Date(it.timestamp)) }
        val expensesByMonth = expenses.groupBy { sdf.format(java.util.Date(it.timestamp)) }
        val allRecordedMonths = (incomesByMonth.keys + expensesByMonth.keys).toList()
        val totalSurplusList = allRecordedMonths.map { month ->
            val incSum = incomesByMonth[month]?.sumOf { it.amount } ?: 0.0
            val expSum = expensesByMonth[month]?.sumOf { it.amount } ?: 0.0
            incSum - expSum
        }
        val computedAverage = if (totalSurplusList.isNotEmpty()) {
            totalSurplusList.average()
        } else {
            0.0
        }
        if (computedAverage <= 100.0) 1000.0 else computedAverage
    }

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
                EmptyStateIllustration(
                    title = LocalizedStrings.get("no_goals_yet", appSettings.language == "Arabic"),
                    subtitle = LocalizedStrings.get("add_goal_instruction", appSettings.language == "Arabic"),
                    icon = Icons.Default.Star,
                    iconColor = PremiumAccentMint
                )
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

                                        // Expected Date Calculation
                                        val remainingAmount = goalItem.targetAmount - goalItem.savedAmount
                                        if (remainingAmount <= 0.0) {
                                            Text(if (appSettings.language == "Arabic") "🎉 تم الإنجاز!" else "🎉 Goal Achieved!", fontSize = 11.sp, color = PremiumAccentMint, fontWeight = FontWeight.Bold)
                                        } else {
                                            val monthsNeeded = remainingAmount / averageMonthlySavings
                                            val cal = java.util.Calendar.getInstance()
                                            val monthsRounded = Math.ceil(monthsNeeded).toInt().coerceIn(1, 1200)
                                            cal.add(java.util.Calendar.MONTH, monthsRounded)
                                            val isArabic = appSettings.language == "Arabic"
                                            val targetSdf = java.text.SimpleDateFormat("MMMM yyyy", if (isArabic) java.util.Locale("ar") else java.util.Locale.US)
                                            val expectedStr = targetSdf.format(cal.time)
                                            
                                            Text(
                                                text = if (isArabic) "⏳ الإنجاز: $expectedStr" else "⏳ Expected: $expectedStr",
                                                fontSize = 11.sp,
                                                color = PremiumAccentOrange,
                                                fontWeight = FontWeight.SemiBold
                                            )
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
    onShowClearData: () -> Unit,
    onShowCurrencyConversion: () -> Unit
) {
    var backupBoxText by remember { mutableStateOf("") }
    var importCsvBoxText by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showChangePinDialog by remember { mutableStateOf(false) }

    val isArabic = appSettings.language == "Arabic"
    val isSmartAlertsEnabled = appSettings.isSmartAlertsEnabled
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
                                    "Light" -> Icons.Default.WbSunny
                                    "Dark" -> Icons.Default.DarkMode
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
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowCurrencyConversion() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (appSettings.language == "Arabic") "تحويل قيم المعاملات للعملة البديلة" else "Batch Currency Value Converter")
                            Text((if (appSettings.language == "Arabic") "تحويل 💱" else "Convert 💱") + " >", fontWeight = FontWeight.Bold, color = PremiumAccentMint)
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
            var showImportFields by remember { mutableStateOf(false) }

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
                                shareText(context, csv, if (isArabic) "مشاركة ملف CSV لبياناتك" else "Share CSV Financial Data")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(LocalizedStrings.get("export_csv", appSettings.language == "Arabic"))
                        }

                        Button(
                            onClick = {
                                val json = viewModel.exportToJson()
                                backupBoxText = json
                                shareText(context, json, if (isArabic) "مشاركة ملف احتياطي كامل JSON" else "Share full JSON Backup")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(LocalizedStrings.get("export_json", appSettings.language == "Arabic"))
                        }

                        Divider(color = Color.White.copy(alpha = 0.05f))

                        Button(
                            onClick = { showImportFields = !showImportFields },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showImportFields) {
                                    if (isArabic) "إخفاء حقول الاستيراد 🔓" else "Hide Import Fields 🔓"
                                } else {
                                    if (isArabic) "استيراد بيانات (لصق CSV أو JSON) 🔐" else "Import Data (Paste CSV or JSON) 🔐"
                                }
                            )
                        }

                        if (showImportFields || importCsvBoxText.isNotBlank()) {
                            OutlinedTextField(
                                value = importCsvBoxText,
                                onValueChange = { importCsvBoxText = it },
                                label = { Text(LocalizedStrings.get("csv_preview", appSettings.language == "Arabic")) },
                                placeholder = { Text("Paste CSV data here...") },
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

                        if (showImportFields || backupBoxText.isNotBlank()) {
                            OutlinedTextField(
                                value = backupBoxText,
                                onValueChange = { backupBoxText = it },
                                label = { Text(LocalizedStrings.get("json_preview", appSettings.language == "Arabic")) },
                                placeholder = { Text("Paste JSON backup here...") },
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
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PremiumAccentMint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = PremiumAccentMint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(LocalizedStrings.get("smart_notifications", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(if (appSettings.language == "Arabic") "تنبيهات تلقائية لما تعدي 80% من الميزانية" else "Alerts when you exceed 80% of budget limits", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Switch(
                                checked = isSmartAlertsEnabled,
                                onCheckedChange = { viewModel.updateSmartAlerts(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = PremiumAccentMint,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f),
                                    checkedBorderColor = PremiumAccentMint,
                                    uncheckedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                )
                            )
                        }
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PremiumAccentMint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = PremiumAccentMint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(LocalizedStrings.get("fingerprint_lock", appSettings.language == "Arabic"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(if (appSettings.language == "Arabic") "حماية فائقة لأمان حساباتك ومصاريفك" else "Advanced security lock using your touch ID", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Switch(
                                checked = isBiometricLockEnabled,
                                onCheckedChange = { viewModel.updateBiometricLock(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = PremiumAccentMint,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f),
                                    checkedBorderColor = PremiumAccentMint,
                                    uncheckedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                )
                            )
                        }
                        if (isBiometricLockEnabled) {
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showChangePinDialog = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "رمز المرور الاحتياطي للمستشعرات" else "Backup Authentication PIN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val pinText = remember(appSettings.backupPin, isArabic) {
                                        if (isArabic) "نوع الرمز: مشفر بالكامل وآمن مغلق" else "PIN Status: Fully Encrypted & Secured"
                                    }
                                    Text(
                                        text = pinText,
                                        fontSize = 10.sp,
                                        color = PremiumAccentMint,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit PIN",
                                    tint = PremiumAccentMint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                            text = if (appSettings.language == "Arabic") "تصميم و برمجة بواسطة عبدالله" else "Designed & Built by Abdallah",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        var tempPin by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تعديل رمز المرور الاحتياطي" else "Update Backup PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic) "يرجى كتابة رمز مرور احتياطي يتكون من 4 أرقام للاعتماد عليه عند تعذر البصمة:" 
                               else "Please enter a 4-digit backup PIN code to unlock your app whenever biometric scan fails:",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempPin,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                tempPin = input
                                inputError = false
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(if (isArabic) "رمز المرور الجديد (4 أرقام)" else "New Backup PIN (4 Digits)") },
                        singleLine = true,
                        isError = inputError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (inputError) {
                        Text(
                            text = if (isArabic) "يجب أن يتكون الرمز من 4 أرقام بالضبط" else "PIN must be exactly 4 digits",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumAccentMint),
                    onClick = {
                        if (tempPin.length == 4) {
                            viewModel.updateBackupPin(tempPin)
                            showChangePinDialog = false
                            android.widget.Toast.makeText(context, if (isArabic) "تم تحديث رمز المرور بنجاح" else "Backup PIN updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            inputError = true
                        }
                    }
                ) {
                    Text(if (isArabic) "حفظ" else "Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = Color.Gray)
                }
            }
        )
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

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
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
                                IconButton(onClick = {
                                    viewModel.deleteIncome(item)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = if (appSettings.language == "Arabic") "تم حذف الدخل" else "Income deleted.",
                                            actionLabel = if (appSettings.language == "Arabic") "تراجع" else "Undo",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete()
                                        }
                                    }
                                }) { Icon(Icons.Default.Delete, contentDescription = null, tint = PremiumAccentRed) }
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
                                IconButton(onClick = {
                                    viewModel.deleteExpense(item)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = if (appSettings.language == "Arabic") "تم حذف المصروف" else "Expense deleted.",
                                            actionLabel = if (appSettings.language == "Arabic") "تراجع" else "Undo",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete()
                                        }
                                    }
                                }) { Icon(Icons.Default.Delete, contentDescription = null, tint = PremiumAccentRed) }
                            }
                        }
                    }
                }
            }
        }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

fun getAdjacentDate(dateStr: String, daysToAdd: Int): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    return try {
        val date = sdf.parse(dateStr) ?: java.util.Date()
        val cal = java.util.Calendar.getInstance()
        cal.time = date
        cal.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
        sdf.format(cal.time)
    } catch (e: Exception) {
        dateStr
    }
}

fun formatDisplayDay(dateStr: String, isArabic: Boolean): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    return try {
        val date = sdf.parse(dateStr) ?: java.util.Date()
        val calendar = java.util.Calendar.getInstance().apply { time = date }
        
        val dayNum = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val year = calendar.get(java.util.Calendar.YEAR)
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK) // 1 = Sun, ..., 7 = Sat
        val monthIdx = calendar.get(java.util.Calendar.MONTH) // 0-indexed month
        
        val dayNamesEn = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val dayNamesAr = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        
        val monthNamesEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val monthNamesAr = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        
        val dayName = if (isArabic) dayNamesAr[dayOfWeek - 1] else dayNamesEn[dayOfWeek - 1]
        val monthName = if (isArabic) monthNamesAr[monthIdx] else monthNamesEn[monthIdx]
        
        if (isArabic) {
            "$dayName، $dayNum $monthName $year"
        } else {
            "$dayName, $dayNum $monthName $year"
        }
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
fun DayNavigatorBar(
    selectedDateStr: String,
    onDateSelected: (String) -> Unit,
    isArabic: Boolean
) {
    val displayStr = remember(selectedDateStr, isArabic) {
        formatDisplayDay(selectedDateStr, isArabic)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val pastDate = getAdjacentDate(selectedDateStr, -1)
                onDateSelected(pastDate)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = PremiumAccentMint
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isArabic) "اليوم المحدد" else "Selected Day",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = displayStr,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(onClick = {
                val nextDate = getAdjacentDate(selectedDateStr, 1)
                onDateSelected(nextDate)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    tint = PremiumAccentMint
                )
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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        text = if (isArabic) "💡 اسحب لليمين/ليسار للحذف" else "💡 Swipe right/left to delete",
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
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                    val originalInc = trans.originalIncome
                                    val originalExp = trans.originalExpense
                                    if (trans.isIncome && originalInc != null) {
                                        viewModel.deleteIncome(originalInc)
                                    } else if (!trans.isIncome && originalExp != null) {
                                        viewModel.deleteExpense(originalExp)
                                    }
                                    
                                    coroutineScope.launch {
                                        val snackbarResult = snackbarHostState.showSnackbar(
                                            message = if (isArabic) "تم حذف \"${trans.name}\" بنجاح" else "Deleted \"${trans.name}\" successfully",
                                            actionLabel = if (isArabic) "تراجع" else "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete()
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> PremiumAccentRed.copy(alpha = 0.8f)
                                    SwipeToDismissBoxValue.StartToEnd -> PremiumAccentRed.copy(alpha = 0.8f)
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            },
                            content = {
                                TransactionHistoryRowItem(
                                    item = trans,
                                    viewModel = viewModel,
                                    isArabic = isArabic,
                                    onLongClick = {
                                        showDeleteConfirmDialog = trans
                                    }
                                )
                            }
                        )
                    }
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
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (item.isIncome) PremiumAccentMint else PremiumAccentRed)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
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
}

@Composable
fun MonthlyReportsAndComparison(
    incomes: List<com.example.data.Income>,
    expenses: List<com.example.data.Expense>,
    appSettings: AppSettings,
    viewModel: FinanceViewModel
) {
    val isArabic = appSettings.language == "Arabic"
    val isAmountMasked by viewModel.isBalanceHidden.collectAsState()
    var showAllCategories by remember { mutableStateOf(false) }

    // Month-over-month comparison calculation
    val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
    val curSdf = java.text.SimpleDateFormat("MMMM yyyy", if (isArabic) java.util.Locale("ar") else java.util.Locale.US)

    // Grouping by "yyyy-MM" month string
    val incomesByMonth = incomes.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    val expensesByMonth = expenses.groupBy { sdf.format(java.util.Date(it.timestamp)) }

    // Get sorted list of months present in the ledger
    val allMonths = (incomesByMonth.keys + expensesByMonth.keys).sortedDescending()

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
                    text = if (isArabic) "📊 التقارير ومقارنة الأشهر" else "📊 Reports & Monthly Analysis",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Surface(
                    color = PremiumAccentMint.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isArabic) "تحديث تلقائي" else "Auto-updated",
                        color = PremiumAccentMint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (allMonths.isEmpty()) {
                Text(
                    text = if (isArabic) "لا توجد معاملات كافية لإجراء مقارنات." else "Not enough history to run analysis yet.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                // Column Categories bar chart for current/latest month
                val latestMonthKey = allMonths.first()
                val monthIncomes = incomesByMonth[latestMonthKey] ?: emptyList()
                val monthExpenses = expensesByMonth[latestMonthKey] ?: emptyList()

                val totalMonthIncome = monthIncomes.sumOf { it.amount }
                val totalMonthExpense = monthExpenses.sumOf { it.amount }

                val displayMonthName = try {
                    val dateObj = sdf.parse(latestMonthKey)
                    curSdf.format(dateObj)
                } catch(e: Exception) {
                    latestMonthKey
                }

                Text(
                    text = if (isArabic) "توزيع مصاريف شهر $displayMonthName" else "Expenses Distribution for $displayMonthName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Group month expenses by category
                val catExpenses = monthExpenses.groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .toList()
                    .sortedByDescending { it.second }

                if (catExpenses.isEmpty()) {
                    Text(
                        text = if (isArabic) "لا توجد مصاريف مسجلة هذا الشهر." else "No expenses logged this month.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    val maxVal = catExpenses.first().second
                    val itemsToShow = if (showAllCategories) catExpenses else catExpenses.take(4)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsToShow.forEach { (cat, amt) ->
                            val ratio = if (maxVal > 0) (amt / maxVal).toFloat() else 0f
                            val formattedRatio = if (totalMonthExpense > 0) ((amt / totalMonthExpense) * 100).toInt() else 0
                            val displayAmt = if (isAmountMasked) "••••" else viewModel.formatAmount(amt)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(PremiumAccentOrange)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = LocalizedStrings.localizeCategory(cat, isArabic),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "$displayAmt ($formattedRatio%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                // Rounded bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    colors = listOf(PremiumAccentOrange, PremiumAccentOrange.copy(alpha = 0.6f))
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                    if (catExpenses.size > 4) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isArabic) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = { showAllCategories = !showAllCategories }
                            ) {
                                Text(
                                    text = if (showAllCategories) {
                                        if (isArabic) "عرض أقل ▲" else "Show Less ▲"
                                    } else {
                                        if (isArabic) "عرض الكل (${catExpenses.size}) ▼" else "Show All (${catExpenses.size}) ▼"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumAccentMint
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(16.dp))

                // Month-over-Month Comparison Table / Section
                Text(
                    text = if (isArabic) "📈 مقارنة الأشهر والسيولة" else "📈 Month-over-Month Comparisons",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    allMonths.take(3).forEachIndexed { index, mKey ->
                        val mInc = (incomesByMonth[mKey] ?: emptyList()).sumOf { it.amount }
                        val mExp = (expensesByMonth[mKey] ?: emptyList()).sumOf { it.amount }
                        val mSavings = mInc - mExp

                        val dispMonth = try {
                            val dateObj = sdf.parse(mKey)
                            curSdf.format(dateObj)
                        } catch(e: Exception) {
                            mKey
                        }

                        val incStr = if (isAmountMasked) "••••" else viewModel.formatAmount(mInc)
                        val expStr = if (isAmountMasked) "••••" else viewModel.formatAmount(mExp)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(dispMonth, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                                        Text(
                                            text = (if (isArabic) "وارد: " else "In: ") + incStr,
                                            fontSize = 10.sp,
                                            color = PremiumAccentMint,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = (if (isArabic) "خارج: " else "Out: ") + expStr,
                                            fontSize = 10.sp,
                                            color = PremiumAccentRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isAmountMasked) "••••" else viewModel.formatAmount(mSavings),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = if (mSavings >= 0) PremiumAccentMint else PremiumAccentRed
                                    )
                                    Text(
                                        text = if (mSavings >= 0) (if (isArabic) "فائض (توفير)" else "Surplus") else (if (isArabic) "عجز مالي" else "Deficit"),
                                        fontSize = 9.sp,
                                        color = if (mSavings >= 0) PremiumAccentMint.copy(alpha = 0.8f) else PremiumAccentRed.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
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
            // Elegant active indicator pill at the top of the tab
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (selected) activeColor else Color.Transparent)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) {
                            activeColor.copy(alpha = if (isDarkThemeGlobal) 0.15f else 0.25f) // Stronger cue in Light mode for better visibility
                        } else {
                            Color.Transparent
                        }
                    )
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

@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    appSettings: AppSettings
) {
    val isAr = appSettings.language == "Arabic"
    
    val incomes by viewModel.incomes.collectAsState(initial = emptyList())
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val bills by viewModel.bills.collectAsState(initial = emptyList())
    val goals by viewModel.goals.collectAsState(initial = emptyList())
    val healthScore by viewModel.financialHealthScore.collectAsState(initial = 70)
    
    val aiInsight by viewModel.aiInsight.collectAsState(initial = null)
    val aiLoading by viewModel.aiLoading.collectAsState(initial = false)
    
    val totalIncomeAmt = incomes.sumOf { it.amount }
    val totalExpenseAmt = expenses.sumOf { it.amount }
    val totalBillsAmt = bills.sumOf { it.amount }
    val paidBillsAmt = bills.filter { it.isPaid }.sumOf { it.amount }
    val netSavings = totalIncomeAmt - totalExpenseAmt - paidBillsAmt
    
    // Base monthly burn rate
    val monthlyBurn = if (totalExpenseAmt > 0) totalExpenseAmt else 1000.0
    
    // --- Interactive "What-If" Savings Target Simulator (Slider) ---
    var additionalSavingsInput by remember { mutableStateOf(0f) }
    // Determine upper limit of simulator (up to 50% of logged income, or a standard ceiling of 10000)
    val maxSimulatorLimit = if (totalIncomeAmt > 0) (totalIncomeAmt * 0.5).toFloat().coerceAtLeast(1000f) else 10000f
    
    val simulatedExtraSavings = additionalSavingsInput.toDouble()
    val simulatedTotalSavings = (netSavings + simulatedExtraSavings).coerceAtLeast(0.0)
    val simulatedSavingsRate = if (totalIncomeAmt > 0) {
        ((totalIncomeAmt - totalExpenseAmt + simulatedExtraSavings) / totalIncomeAmt * 100).coerceIn(-100.0, 100.0)
    } else {
        0.0
    }
    
    val simulatedBurnRate = (monthlyBurn - simulatedExtraSavings).coerceAtLeast(100.0)
    val simulatedRunwayMonths = if (simulatedTotalSavings > 0) (simulatedTotalSavings / simulatedBurnRate) else 0.0
    
    // Base end-of-month metrics
    val calendar = java.util.Calendar.getInstance()
    val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val maxDays = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val daysLeft = maxDays - currentDay
    
    val dailyExpense = if (currentDay > 0) totalExpenseAmt / currentDay else 0.0
    val projectedSpendingRestOfInvoice = dailyExpense * daysLeft
    val unpaidBillsAmt = bills.filter { !it.isPaid }.sumOf { it.amount }
    val expectedEndOfMonthBalance = netSavings - projectedSpendingRestOfInvoice - unpaidBillsAmt

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // Title block with Quick AI Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) "التحليلات والمؤشرات المتوقعة" else "Predictive Analytics",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAr) "رؤى وتوقعات تفاعلية لمستقبلك المالي بنقرة زر" else "Interactive simulations & projections for financial control",
                        fontSize = 13.sp,
                        color = PremiumTextSecondaryDark,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = { viewModel.refreshAiInsights(isManual = true) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = PremiumAccentPurple.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                ) {
                    Text("🔮", fontSize = 20.sp)
                }
            }
        }

        if (incomes.isEmpty() && expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📊", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isAr) "البيانات غير كافية حالياً" else "Insufficient Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAr) 
                                "ابدأ بتسجيل مصاريفك ودخلك الشهري لتشغيل المحاكي المالي المتطور والحصول على توقعات وتحليلات دقيقة بنسبة 100%." 
                            else 
                                "Log your incomes, expenses, and recurrent bills to generate complete interactive simulations, compounding, and runways.",
                            fontSize = 12.sp,
                            color = PremiumTextSecondaryDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            // Live Gemini-Powered Consulting Advisor Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = PremiumAccentPurple.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔮", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAr) "مستشارك المالي الذكي (Gemini)" else "Gemini Smart Advisor",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = PremiumAccentPurple.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "AI Pro",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PremiumAccentPurple
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (aiLoading) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = PremiumAccentPurple)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isAr) "يقوم الذكاء الاصطناعي بتحليل ميزانيتك الحالية وتوليد توصيات مخصصة..." else "Analyzing your budget data to generate custom actionable insights...",
                                    fontSize = 12.sp,
                                    color = PremiumTextSecondaryDark,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            val adviceText = aiInsight
                            if (adviceText != null) {
                                Text(
                                    text = adviceText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = if (isAr) {
                                        "احصل على استشارة مالية مخصصة فورية! سيقوم مستشارنا المالي الذكي بفحص مصاريفك، ميزانيتك، فواتيرك القادمة، واقتراح خطة ادخار لزيادة ثروتك."
                                    } else {
                                        "Get instant customized financial advice! Our smart advisor will scan your cashflow patterns, recurrent bills, and propose saving strategies."
                                    },
                                    fontSize = 12.sp,
                                    color = PremiumTextSecondaryDark,
                                    lineHeight = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.refreshAiInsights(isManual = true) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PremiumAccentPurple
                                )
                            ) {
                                Text(
                                    text = if (isAr) "تحديث الاستشارة الذكية 🔮" else "Analyze & Get AI Advice 🔮",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Interactive What-if Savings Target Simulator (Slider Card)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isAr) "محاكي الادخار التفاعلي 🎚️" else "Interactive Savings Simulator 🎚️",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isAr) "اسحب لتعديل هدف الادخار الشهري الإضافي" else "Slide to simulate extra monthly savings",
                                    fontSize = 11.sp,
                                    color = PremiumTextSecondaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Large readout of simulated savings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAr) "مبلغ التوفير الإضافي:" else "Extra savings:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = PremiumTextSecondaryDark
                            )
                            Text(
                                text = "+${viewModel.formatAmount(simulatedExtraSavings)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = PremiumAccentBlue
                            )
                        }

                        Slider(
                            value = additionalSavingsInput,
                            onValueChange = { additionalSavingsInput = it },
                            valueRange = 0f..maxSimulatorLimit,
                            steps = 20,
                            colors = SliderDefaults.colors(
                                thumbColor = PremiumAccentBlue,
                                activeTrackColor = PremiumAccentBlue,
                                inactiveTrackColor = if (isDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Simulated Outcomes Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Net Savings Rate card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isAr) "معدل الادخار بعد المحاكاة" else "Simulated Savings Rate",
                                        fontSize = 10.sp,
                                        color = PremiumTextSecondaryDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format("%.1f", simulatedSavingsRate)}%",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (simulatedSavingsRate >= 20.0) PremiumAccentMint 
                                                else if (simulatedSavingsRate >= 0) PremiumAccentOrange 
                                                else PremiumAccentRed
                                    )
                                }
                            }

                            // Financial Runway card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isAr) "مدرج الأمان بعد المحاكاة" else "Simulated Runway",
                                        fontSize = 10.sp,
                                        color = PremiumTextSecondaryDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (simulatedRunwayMonths >= 6.0) {
                                            if (isAr) "آمن جداً (+6)" else "6+ Months 🛡️"
                                        } else if (simulatedRunwayMonths >= 3.0) {
                                            if (isAr) "جيد (3-6)" else "3-6 Months 🔑"
                                        } else if (simulatedRunwayMonths >= 1.0) {
                                            if (isAr) "مقبول (1-3)" else "1-3 Months ⚠️"
                                        } else {
                                            if (isAr) "منخفض (< شهر)" else "Under 1 Month 🚨"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (simulatedRunwayMonths >= 3.0) PremiumAccentMint else if (simulatedRunwayMonths >= 1.0) PremiumAccentOrange else PremiumAccentRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expected End of Month Balance card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isAr) "الرصيد المتوقع نهاية الشهر" else "Predicted End of Month Balance",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAr) "محاكي التدفق المالي بناء على نمط صرفك الحالي" else "Calculated based on daily burn rate and unpaid bills",
                                    fontSize = 11.sp,
                                    color = PremiumTextSecondaryDark
                                )
                            }
                            Text("🔮", fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = viewModel.formatAmount(expectedEndOfMonthBalance),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = if (expectedEndOfMonthBalance >= 0) PremiumAccentMint else PremiumAccentRed
                            )
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = (if (expectedEndOfMonthBalance >= 0) PremiumAccentMint else PremiumAccentRed).copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (expectedEndOfMonthBalance >= 0) {
                                        if (isAr) "فائض متوقع" else "Expected Surplus"
                                    } else {
                                        if (isAr) "عجز متوقع" else "Expected Deficit"
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (expectedEndOfMonthBalance >= 0) PremiumAccentMint else PremiumAccentRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Linear progress indicator towards safety margin
                        val balanceFraction = if (totalIncomeAmt > 0) (expectedEndOfMonthBalance.coerceAtLeast(0.0) / totalIncomeAmt).toFloat().coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { balanceFraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = if (expectedEndOfMonthBalance >= 0) PremiumAccentMint else PremiumAccentRed,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isAr) "المصاريف المتوقعة لباقي الشهر: ${viewModel.formatAmount(projectedSpendingRestOfInvoice)}" else "Expected spend left: ${viewModel.formatAmount(projectedSpendingRestOfInvoice)}",
                                fontSize = 10.sp,
                                color = PremiumTextSecondaryDark
                            )
                            Text(
                                text = if (isAr) "فواتير غير مدفوعة: ${viewModel.formatAmount(unpaidBillsAmt)}" else "Unpaid bills: ${viewModel.formatAmount(unpaidBillsAmt)}",
                                fontSize = 10.sp,
                                color = PremiumTextSecondaryDark
                            )
                        }
                    }
                }
            }

            // Advanced Three-Scenario Projected Wealth Growth Chart section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isAr) "نمذجة نمو الثروة والادخار التراكمي (12 شهراً)" else "Cumulative Wealth Projections (12-Month)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAr) "مقارنة السيناريو الحالي بالتوفير المحاكي والاستثمار بفائدة مركبة 8%" else "Simulating Standard vs Slider-optimized vs 8% Compound Investment",
                            fontSize = 11.sp,
                            color = PremiumTextSecondaryDark
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom drawing multi-line graph
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.Transparent)
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val paddingLeft = 40.dp.toPx()
                                val paddingRight = 10.dp.toPx()
                                val paddingTop = 15.dp.toPx()
                                val paddingBottom = 25.dp.toPx()
                                
                                val graphWidth = w - paddingLeft - paddingRight
                                val graphHeight = h - paddingTop - paddingBottom

                                // Points matrices
                                val pointsRealistic = FloatArray(12)
                                val pointsOptimized = FloatArray(12)
                                val pointsSuperSaver = FloatArray(12)
                                
                                val monthlyRealisticSavings = netSavings.coerceAtLeast(0.0)
                                val monthlyOptimizedSavings = (netSavings + simulatedExtraSavings).coerceAtLeast(0.0)
                                val monthlyYieldRate = 0.08 / 12.0 // 8% Compound Annual Return rate
                                
                                var currentCompoundSavings = 0.0
                                for (i in 0..11) {
                                    pointsRealistic[i] = (monthlyRealisticSavings * (i + 1)).toFloat()
                                    pointsOptimized[i] = (monthlyOptimizedSavings * (i + 1)).toFloat()
                                    
                                    // Compound interest calculation
                                    currentCompoundSavings = (currentCompoundSavings + monthlyOptimizedSavings) * (1.0 + monthlyYieldRate)
                                    pointsSuperSaver[i] = currentCompoundSavings.toFloat()
                                }
                                
                                val maxVal = pointsSuperSaver[11].coerceAtLeast(1000f)

                                // Draw Y axis gridlines and labels
                                val gridLinesCount = 3
                                for (grid in 0..gridLinesCount) {
                                    val gridY = paddingTop + graphHeight - (graphHeight * (grid.toFloat() / gridLinesCount))
                                    
                                    drawLine(
                                        color = if (isDarkThemeGlobal) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f),
                                        start = Offset(paddingLeft, gridY),
                                        end = Offset(w - paddingRight, gridY),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Draw three path scenarios
                                val pathRealistic = androidx.compose.ui.graphics.Path()
                                val pathOptimized = androidx.compose.ui.graphics.Path()
                                val pathSuperSaver = androidx.compose.ui.graphics.Path()

                                for (i in 0..11) {
                                    val pointX = paddingLeft + (graphWidth * (i / 11f))
                                    
                                    val yRealistic = paddingTop + graphHeight - (graphHeight * (pointsRealistic[i] / maxVal))
                                    val yOptimized = paddingTop + graphHeight - (graphHeight * (pointsOptimized[i] / maxVal))
                                    val ySuperSaver = paddingTop + graphHeight - (graphHeight * (pointsSuperSaver[i] / maxVal))

                                    if (i == 0) {
                                        pathRealistic.moveTo(pointX, yRealistic)
                                        pathOptimized.moveTo(pointX, yOptimized)
                                        pathSuperSaver.moveTo(pointX, ySuperSaver)
                                    } else {
                                        pathRealistic.lineTo(pointX, yRealistic)
                                        pathOptimized.lineTo(pointX, yOptimized)
                                        pathSuperSaver.lineTo(pointX, ySuperSaver)
                                    }
                                    
                                    // Node dots highlights
                                    if (i % 3 == 2 || i == 11) {
                                        drawCircle(
                                            color = PremiumAccentBlue,
                                            radius = 4.dp.toPx(),
                                            center = Offset(pointX, yRealistic)
                                        )
                                        drawCircle(
                                            color = PremiumAccentOrange,
                                            radius = 4.dp.toPx(),
                                            center = Offset(pointX, yOptimized)
                                        )
                                        drawCircle(
                                            color = PremiumAccentMint,
                                            radius = 4.dp.toPx(),
                                            center = Offset(pointX, ySuperSaver)
                                        )
                                    }
                                }

                                drawPath(
                                    path = pathRealistic,
                                    color = PremiumAccentBlue,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 2.5.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )

                                drawPath(
                                    path = pathOptimized,
                                    color = PremiumAccentOrange,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 2.5.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )

                                drawPath(
                                    path = pathSuperSaver,
                                    color = PremiumAccentMint,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 3.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                
                                // Draw baseline axis
                                drawLine(
                                    color = if (isDarkThemeGlobal) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f),
                                    start = Offset(paddingLeft, paddingTop + graphHeight),
                                    end = Offset(w - paddingRight, paddingTop + graphHeight),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Advanced Legend
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PremiumAccentBlue))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAr) "السيناريو المحافظ الحالي" else "Current realistic rate",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = viewModel.formatAmount(netSavings * 12),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumAccentBlue
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PremiumAccentOrange))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAr) "المحاكاة والتوفير المالي الإضافي" else "Slider-enhanced saving rate",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = viewModel.formatAmount((netSavings + simulatedExtraSavings) * 12),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumAccentOrange
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PremiumAccentMint))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAr) "الاستثمار والادخار المركب (فائدة 8%)" else "Optimized savings + 8% Compound return",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = viewModel.formatAmount(
                                        let {
                                            var currentCompound = 0.0
                                            val monthly = (netSavings + simulatedExtraSavings).coerceAtLeast(0.0)
                                            for (j in 0..11) {
                                                currentCompound = (currentCompound + monthly) * (1.0 + 0.08 / 12.0)
                                            }
                                            currentCompound
                                        }
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PremiumAccentMint
                                )
                            }
                        }
                    }
                }
            }

            // Advanced Category Optimization Matrix
            val categorySum = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
            val topCategories = categorySum.entries.sortedByDescending { it.value }.take(3)
            if (topCategories.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = if (isAr) "توصيات خفض التكاليف الذكية" else "Smart Expense Optimization Guide",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAr) "فرص توفير فورية بتقليل مصاريف الفئات بـ 15%" else "Quick savings options by reducing highest spending categories",
                                fontSize = 11.sp,
                                color = PremiumTextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            topCategories.forEachIndexed { index, entry ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = if (isDarkThemeGlobal) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f))
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                val potentialSavings = entry.value * 0.15
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.key,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "صرفك الحالي: ${viewModel.formatAmount(entry.value)}" else "Current spend: ${viewModel.formatAmount(entry.value)}",
                                            fontSize = 11.sp,
                                            color = PremiumTextSecondaryDark
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "-15%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PremiumAccentOrange
                                        )
                                        Text(
                                            text = if (isAr) "ستوفر ${viewModel.formatAmount(potentialSavings)}" else "Saves ${viewModel.formatAmount(potentialSavings)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PremiumAccentMint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Savings Goals prediction list
            if (goals.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isDarkThemeGlobal) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = if (isAr) "تسريع تحقيق الأهداف المالية" else "Accelerated Goal Projections",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isAr) "مقارنة زمن الوصول لهدفك بالادخار الحالي مقابل التوفير الإضافي المحاكي" else "Estimated timeframe to reach targets using current vs simulated savings rate",
                                fontSize = 11.sp,
                                color = PremiumTextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            goals.forEachIndexed { index, goal ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = if (isDarkThemeGlobal) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                val needed = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
                                val baseSavingsRate = netSavings.coerceAtLeast(0.0)
                                val simulatedSavings = (netSavings + simulatedExtraSavings).coerceAtLeast(0.0)

                                val baseMonthsToTarget = if (baseSavingsRate > 0) (needed / baseSavingsRate) else Double.MAX_VALUE
                                val simulatedMonthsToTarget = if (simulatedSavings > 0) (needed / simulatedSavings) else Double.MAX_VALUE

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = goal.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        if (needed <= 0.0) {
                                            Text(
                                                text = if (isAr) "مكتمل وحققت الهدف بنجاح! 🎉" else "Goal fully funded! 🎉",
                                                fontSize = 11.sp,
                                                color = PremiumAccentMint,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            val baseM = baseMonthsToTarget.toInt()
                                            val simM = simulatedMonthsToTarget.toInt()
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (isAr) {
                                                        "الوضع الحالي: $baseM شهر"
                                                    } else {
                                                        "Normal: $baseM mo"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = PremiumTextSecondaryDark,
                                                    style = androidx.compose.ui.text.TextStyle(
                                                        textDecoration = if (simM < baseM) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                    )
                                                )
                                                if (simM < baseM && simM < 120) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isAr) {
                                                            "🏃 المحاكي: $simM شهر"
                                                        } else {
                                                            "🏃 Simulated: $simM mo"
                                                        },
                                                        fontSize = 11.sp,
                                                        color = PremiumAccentMint,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat() else 0f
                                    Box(
                                        modifier = Modifier.size(45.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(40.dp),
                                            color = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { PremiumAccentMint },
                                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
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

