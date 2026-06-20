package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.navigation.compose.*
import com.example.ui.AppViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Unified Routing Values
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_ACCOUNTS = "accounts"
const val ROUTE_TRANSACTIONS = "transactions"
const val ROUTE_CATEGORIES = "categories"
const val ROUTE_BUDGETS = "budgets"
const val ROUTE_RECURRING = "recurring"
const val ROUTE_DEBTS = "debts"
const val ROUTE_REPORTS = "reports"
const val ROUTE_SETTINGS = "settings"

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val appViewModel: AppViewModel = viewModel()
                val locked by appViewModel.isAppUnlocked.collectAsState()
                val showWelcomeScreen by appViewModel.showWelcomeBackScreen.collectAsState()
                var isLaunching by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1800)
                    isLaunching = false
                }

                if (isLaunching) {
                    AppStartupSplashScreen()
                } else if (showWelcomeScreen) {
                    WelcomeBackScreen(viewModel = appViewModel)
                } else if (!locked) {
                    PinLockScreen(viewModel = appViewModel)
                } else {
                    AppContentLayout(viewModel = appViewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            val appViewModel = androidx.lifecycle.ViewModelProvider(this)[com.example.ui.AppViewModel::class.java]
            appViewModel.triggerOpportunisticBackup()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// -------------------------------------------------------------
// SECURE LOCK PASSCODE PIN SCREEN INJECTION
@Composable
fun PinLockScreen(viewModel: AppViewModel) {
    val inputPin by viewModel.appLockPinInput.collectAsState()
    val err by viewModel.errorMessage.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val lockoutUntil by viewModel.appLockLockoutUntil.collectAsState()
    val lockoutSeconds by viewModel.appLockLockoutRemainingSeconds.collectAsState()
    val wrongAttempts by viewModel.appLockWrongAttempts.collectAsState()

    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    // Handlers
    val onTriggerBiometrics = {
        if (settings.biometricEnabled && activity != null && lockoutUntil == null) {
            com.example.utils.BiometricAuthHelper.showBiometricPrompt(
                activity = activity,
                title = "Unlock App",
                subtitle = "Verify identity to continue",
                onSuccess = {
                    viewModel.onBiometricUnlockSucceeded()
                },
                onError = { code, errStr ->
                    if (code != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED && 
                        code != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        viewModel.onBiometricUnlockFailed()
                    }
                }
            )
        }
    }

    // Auto trigger biometric on start
    LaunchedEffect(settings.biometricEnabled, activity) {
        if (settings.appLockEnabled && settings.biometricEnabled && activity != null) {
            val isUnlocked = viewModel.isAppUnlocked.value
            if (!isUnlocked && lockoutUntil == null) {
                // Introduce a slight delay to allow layout stability
                kotlinx.coroutines.delay(300)
                onTriggerBiometrics()
            }
        }
    }

    // Dynamic Dot Shake Animation
    var shakeOffset by remember { mutableStateOf(0f) }
    LaunchedEffect(wrongAttempts) {
        if (wrongAttempts > 0 && lockoutUntil == null) {
            for (i in 1..6) {
                shakeOffset = if (i % 2 == 0) 12f else -12f
                kotlinx.coroutines.delay(45)
            }
            shakeOffset = 0f
        }
    }

    LaunchedEffect(err) {
        err?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (lockoutUntil != null) Icons.Default.Warning else Icons.Default.Lock,
                contentDescription = "App Locked",
                tint = if (lockoutUntil != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (lockoutUntil != null) {
                Text(
                    text = "TEMPORARY LOCKOUT SHIELD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Too many failed attempts. Try again in $lockoutSeconds seconds.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "DATABASE SHIELD LOCKED",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (settings.biometricEnabled) 
                        "Use fingerprint or 4-digit security PIN to unlock." 
                    else 
                        "Provide 4-digit security PIN to access ledgers.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Dot Displays with Shake Offset Modifier
            Row(
                modifier = Modifier.offset(x = shakeOffset.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    val active = inputPin.length >= i
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Local 10-Key keypad design (Integrating Face/Fingerprint shortcut)
            val numbers = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (settings.biometricEnabled) "BIO" else "", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                numbers.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { char ->
                            if (char.isEmpty()) {
                                Box(modifier = Modifier.size(64.dp))
                            } else if (char == "BIO") {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onTriggerBiometrics() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Trigger Biometric Unlock",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            if (char == "DEL") {
                                                viewModel.deletePinDigit()
                                            } else {
                                                viewModel.enterPinDigit(char)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char,
                                        fontSize = 18.sp,
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

// -------------------------------------------------------------
// FLOATING CORE NAVIGATION COMPONENT WITH HIGH FIDELITY DESIGN
@Composable
fun FloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = Spring.StiffnessMedium
        )
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 28.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) PremiumIndigo else Color(0xFF65676B),
                letterSpacing = 0.1.sp
            )
        }
    }
}

// -------------------------------------------------------------
// CORE APP SCREEN WRAPPER WITH ADAPTIVE TABS AND THE MENU PATTERN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContentLayout(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val settings by viewModel.settingsState.collectAsState()
    val accounts by viewModel.accountsState.collectAsState()
    val categories by viewModel.categoriesState.collectAsState()
    val currentMonthYear by viewModel.currentMonthYear.collectAsState()

    val context = LocalContext.current

    // Dynamic Local Toast alerts with custom animations
    val err by viewModel.errorMessage.collectAsState()
    val succ by viewModel.successMessage.collectAsState()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    var activeMessageType by remember { mutableStateOf("SUCCESS") } // "SUCCESS" | "ERROR"

    LaunchedEffect(err, succ) {
        if (err != null) {
            activeMessage = err
            activeMessageType = "ERROR"
            kotlinx.coroutines.delay(4000)
            activeMessage = null
            viewModel.clearMessages()
        } else if (succ != null) {
            activeMessage = succ
            activeMessageType = "SUCCESS"
            kotlinx.coroutines.delay(3500)
            activeMessage = null
            viewModel.clearMessages()
        }
    }

    // Sheet displays
    var showMenuSheet by remember { mutableStateOf(false) }
    var showQuickAddSheet by remember { mutableStateOf(false) }

    // Quick add form fields
    var qAmount by remember { mutableStateOf("") }
    var qType by remember { mutableStateOf("EXPENSE") }
    var qAccountId by remember { mutableLongStateOf(0L) }
    var qCategoryId by remember { mutableStateOf<Long?>(null) }
    var qNote by remember { mutableStateOf("") }

    // Dropdown Month Picker variables
    var showMonthDropdown by remember { mutableStateOf(false) }
    val monthOptions = remember {
        val today = LocalDate.now()
        (0..11).map { today.minusMonths(it.toLong()).format(DateTimeFormatter.ofPattern("yyyy-MM")) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    val subtitleText = when (currentRoute) {
                        ROUTE_DASHBOARD -> "DASHBOARD"
                        ROUTE_ACCOUNTS -> "ACCOUNTS"
                        ROUTE_TRANSACTIONS -> "LEDGERS"
                        ROUTE_CATEGORIES -> "CATEGORIES"
                        ROUTE_BUDGETS -> "BUDGETS"
                        ROUTE_RECURRING -> "RECURRING"
                        ROUTE_DEBTS -> "DEBTS"
                        ROUTE_REPORTS -> "REPORTS"
                        ROUTE_SETTINGS -> "SETTINGS"
                        else -> "PERSONAL FINANCE"
                    }
                    val titleText = when (currentRoute) {
                        ROUTE_DASHBOARD -> "Overview"
                        ROUTE_ACCOUNTS -> "Accounts Ledger"
                        ROUTE_TRANSACTIONS -> "All Ledgers"
                        ROUTE_CATEGORIES -> "Categories Catalog"
                        ROUTE_BUDGETS -> "Budgets Status"
                        ROUTE_RECURRING -> "Recurring Rules"
                        ROUTE_DEBTS -> "Debts & Loans"
                        ROUTE_REPORTS -> "Export Reports"
                        ROUTE_SETTINGS -> "App Preferences"
                        else -> "Personal Finance"
                    }
                    Column {
                        Text(
                            text = subtitleText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = titleText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    // Month Selection Spinner dropdown in App Bar
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        Card(
                            onClick = { showMonthDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(12.dp))
                                Text(currentMonthYear, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false }
                        ) {
                            monthOptions.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                                    onClick = {
                                        viewModel.currentMonthYear.value = month
                                        showMonthDropdown = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = Color(0xFFECECEC),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        },
        bottomBar = {
            // Redesigned Floating Bottom Navigation Panel with standardized items and exceptional spacing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.2.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 8.dp)
                    ) {
                        val containerWidth = maxWidth
                        val targetFraction = when (currentRoute) {
                            ROUTE_DASHBOARD -> 0f / 5.1f
                            ROUTE_ACCOUNTS -> 1f / 5.1f
                            ROUTE_TRANSACTIONS -> 3.1f / 5.1f
                            else -> 0f / 5.1f
                        }
                        val animatedFraction by animateFloatAsState(
                            targetValue = targetFraction,
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = 180f
                            )
                        )

                        if (currentRoute == ROUTE_DASHBOARD || currentRoute == ROUTE_ACCOUNTS || currentRoute == ROUTE_TRANSACTIONS) {
                            Box(
                                modifier = Modifier
                                    .offset(x = containerWidth * animatedFraction)
                                    .width(containerWidth / 5.1f)
                                    .height(48.dp)
                                    .background(
                                        color = PremiumIndigo.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 1. Dashboard (Home)
                            FloatingNavItem(
                                selected = currentRoute == ROUTE_DASHBOARD,
                                onClick = { navController.navigate(ROUTE_DASHBOARD) { popUpTo(ROUTE_DASHBOARD) { inclusive = true } } },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard", modifier = Modifier.size(20.dp), tint = if (currentRoute == ROUTE_DASHBOARD) PremiumIndigo else Color(0xFF65676B)) },
                                label = "Home",
                                modifier = Modifier.weight(1f)
                            )

                            // 2. Accounts
                            FloatingNavItem(
                                selected = currentRoute == ROUTE_ACCOUNTS,
                                onClick = { navController.navigate(ROUTE_ACCOUNTS) },
                                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Accounts", modifier = Modifier.size(20.dp), tint = if (currentRoute == ROUTE_ACCOUNTS) PremiumIndigo else Color(0xFF65676B)) },
                                label = "Accounts",
                                modifier = Modifier.weight(1f)
                            )

                            // 3. Central QuickBooks QuickBooker Action FAB Button
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(PremiumIndigo, Color(0xFF4F46E5))
                                                )
                                            )
                                            .clickable {
                                                qAccountId = accounts.firstOrNull()?.id ?: 0L
                                                qCategoryId = categories.firstOrNull { it.type == qType }?.id
                                                showQuickAddSheet = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "QuickBooker Action",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "QuickBook",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PremiumIndigo,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }

                            // 4. Ledger (Transactions)
                            FloatingNavItem(
                                selected = currentRoute == ROUTE_TRANSACTIONS,
                                onClick = { navController.navigate(ROUTE_TRANSACTIONS) },
                                icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Transactions", modifier = Modifier.size(20.dp), tint = if (currentRoute == ROUTE_TRANSACTIONS) PremiumIndigo else Color(0xFF65676B)) },
                                label = "Ledger",
                                modifier = Modifier.weight(1f)
                            )

                            // 5. Menu Catalog
                            FloatingNavItem(
                                selected = false,
                                onClick = { showMenuSheet = true },
                                icon = { Icon(Icons.Default.Menu, contentDescription = "Menu catalog", modifier = Modifier.size(20.dp), tint = Color(0xFF65676B)) },
                                label = "More",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {}
    ) { paddingValues ->
        // NavHost mapping back to View Composables with beautiful premium gradient backdrop
        NavHost(
            navController = navController,
            startDestination = ROUTE_DASHBOARD,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(getAppGradient())
        ) {
            composable(ROUTE_DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { navController.navigate(ROUTE_TRANSACTIONS) }
                )
            }
            composable(ROUTE_ACCOUNTS) {
                AccountsScreen(viewModel = viewModel)
            }
            composable(ROUTE_TRANSACTIONS) {
                TransactionsScreen(viewModel = viewModel)
            }
            composable(ROUTE_CATEGORIES) {
                CategoriesScreen(viewModel = viewModel)
            }
            composable(ROUTE_BUDGETS) {
                BudgetsScreen(viewModel = viewModel)
            }
            composable(ROUTE_RECURRING) {
                RecurringScreen(viewModel = viewModel)
            }
            composable(ROUTE_DEBTS) {
                DebtsScreen(viewModel = viewModel)
            }
            composable(ROUTE_REPORTS) {
                ReportsScreen(viewModel = viewModel)
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(viewModel = viewModel)
            }
        }

        // Section 6 Secondary Drawer mobile model
        if (showMenuSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMenuSheet = false },
                shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Expense Management Catalog",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val secondaryMenuItems = listOf(
                        Triple(ROUTE_CATEGORIES, Icons.Default.Label, "Categories"),
                        Triple(ROUTE_BUDGETS, Icons.Default.Adjust, "Budgets"),
                        Triple(ROUTE_RECURRING, Icons.Default.Repeat, "Recurring Cycles"),
                        Triple(ROUTE_DEBTS, Icons.Default.Handshake, "Debts & Loans"),
                        Triple(ROUTE_REPORTS, Icons.Default.BarChart, "Compile Reports"),
                        Triple(ROUTE_SETTINGS, Icons.Default.Settings, "Preferences")
                    )

                    secondaryMenuItems.chunked(3).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunk.forEach { (route, icon, label) ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showMenuSheet = false
                                            navController.navigate(route)
                                        },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick entry add transaction Sheet
        if (showQuickAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQuickAddSheet = false },
                shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Quick Bookkeeper Cash Ledger Log", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EXPENSE", "INCOME").forEach { type ->
                            val sel = qType == type
                            Card(
                                modifier = Modifier.weight(1f).clickable {
                                    qType = type
                                    qCategoryId = categories.firstOrNull { it.type == type }?.id
                                },
                                colors = CardDefaults.cardColors(containerColor = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                    Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = qAmount,
                        onValueChange = { qAmount = it },
                        label = { Text("Amount (Rs.)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("quick_amount_input"),
                        colors = premiumTextFieldColors(),
                        singleLine = true
                    )

                    Text("Target Ledger Category", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    var qCategoryExpanded by remember { mutableStateOf(false) }
                    val qActiveCategories = categories.filter { it.type == qType }
                    val qSelectedCategory = qActiveCategories.find { it.id == qCategoryId } ?: qActiveCategories.firstOrNull()

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = qSelectedCategory?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                IconButton(onClick = { qCategoryExpanded = !qCategoryExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand Category dropdown"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { qCategoryExpanded = !qCategoryExpanded },
                            colors = premiumTextFieldColors()
                        )

                        DropdownMenu(
                            expanded = qCategoryExpanded,
                            onDismissRequest = { qCategoryExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (qActiveCategories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No categories available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = { qCategoryExpanded = false }
                                )
                            } else {
                                qActiveCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val bulletColor = try {
                                                    Color(android.graphics.Color.parseColor(cat.color))
                                                } catch (e: Exception) {
                                                    Color.Gray
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(bulletColor)
                                                )
                                                Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        },
                                        onClick = {
                                            qCategoryId = cat.id
                                            qCategoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = qNote,
                        onValueChange = { qNote = it },
                        label = { Text("Short Note") },
                        placeholder = { Text("e.g. Milk powder") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = premiumTextFieldColors(),
                        singleLine = true
                    )

                    PremiumButton(
                        text = "Post Ledger Cash Entry",
                        onClick = {
                            val ob = qAmount.toDoubleOrNull() ?: 0.0
                            if (ob > 0.0 && qAccountId > 0L) {
                                viewModel.addTransaction(
                                    type = qType,
                                    amount = ob,
                                    accountId = qAccountId,
                                    categoryId = qCategoryId,
                                    date = System.currentTimeMillis(),
                                    note = qNote
                                )
                                showQuickAddSheet = false
                                qAmount = ""
                                qNote = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("quick_add_submit_button")
                    )
                }
            }
        }
    }

    // Custom premium animated alert Overlay
    AnimatedVisibility(
        visible = activeMessage != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 86.dp).zIndex(99f)
    ) {
        activeMessage?.let { msg ->
            val gradientColors = if (activeMessageType == "SUCCESS") {
                listOf(Color(0xFF10B981), Color(0xFF059669))
            } else {
                listOf(Color(0xFFEF4444), Color(0xFFDC2626))
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 450.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(Brush.linearGradient(gradientColors))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (activeMessageType == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
}

@Composable
fun WelcomeBackScreen(viewModel: AppViewModel) {
    val WelcomeMetadata by viewModel.welcomeBackupMetadata.collectAsState()
    var showStartFreshWarning by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!showStartFreshWarning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )

                        Text(
                            text = "Welcome Back!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "We discovered an existing database backup safely synced to your Google Account ledger.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Backup Details Card
                        WelcomeMetadata?.let { meta ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Google Account:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(meta.email ?: "", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Saved On:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        val formatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(meta.timestamp))
                                        Text(formatted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Ledger Entries:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${meta.transactionsCount} records", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.restoreFromWelcomeBackup() },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("welcome_restore_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Restore My Ledger Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        TextButton(
                            onClick = { showStartFreshWarning = true },
                            modifier = Modifier.testTag("welcome_start_fresh_button")
                        ) {
                            Text("Start Fresh (Skip Restore)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = SystemRed,
                            modifier = Modifier.size(64.dp)
                        )

                        Text(
                            text = "Are you absolutely sure?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Starting fresh will overwrite and permanently delete your existing cloud backup the next time an automatic synchronization executes. There is no way to retrieve overridden ledger details.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { viewModel.startFreshWelcome() },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("welcome_confirm_fresh_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemRed)
                        ) {
                            Text("I understand, Start Fresh", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        TextButton(
                            onClick = { showStartFreshWarning = false }
                        ) {
                            Text("Go back to Restore options", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppStartupSplashScreen() {
    var animValue by remember { mutableStateOf(0.7f) }
    var rotationValue by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            animValue = 1.08f
            kotlinx.coroutines.delay(1000)
            animValue = 0.88f
            kotlinx.coroutines.delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            rotationValue += 2.2f
            kotlinx.coroutines.delay(16)
        }
    }

    val scale by animateFloatAsState(
        targetValue = animValue,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF07050A) // Ultra pure pitch black-slate hybrid context to eliminate any brief latency white flash
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060408),
                            Color(0xFF0F0C20),
                            Color(0xFF0A0815)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                Spacer(modifier = Modifier.weight(1.2f))

                // Custom Monogram Circle Vault containing "Le"
                Box(
                    modifier = Modifier
                        .size(135.dp)
                        .drawBehind {
                            // Elegant gold and purple metallic rings
                            drawCircle(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFF6366F1))
                                ),
                                radius = size.minDimension / 2.05f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 3.2.dp.toPx()
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationZ = rotationValue * 0.08f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Exquisite "Le" Monogram Logo Font Representation
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "L",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700), // Pure Gold L
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text(
                                text = "e",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White, // Premium White e
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.offset(x = (-3).dp, y = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "L E D G E R",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 7.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "DOUBLE-ENTRY VAULT ENGINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF818CF8),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(36.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = Color(0xFFFBBF24),
                    trackColor = Color(0xFF1E1B4B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Initializing Vault Handshake...",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    letterSpacing = 0.4.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Signature & Version segment at the bottom as requested
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "DEVELOPED BY MILAN MADUSANKA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.3.sp
                    )
                    Text(
                        text = "Version 1.25.0 - Premium Edition",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.35f),
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

