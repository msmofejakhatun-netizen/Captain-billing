package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.data.remote.UserDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTables: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTables by viewModel.totalTables.collectAsState()
    val runningOrders by viewModel.runningOrdersCount.collectAsState()
    val pendingBills by viewModel.pendingBillsCount.collectAsState()
    val activeCaptains by viewModel.activeCaptainsCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    val userRole by viewModel.currentUserRole.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Tab control for Owner/Admin
    var selectedDashboardTab by remember { mutableStateOf("MONITOR") }

    // Admin state flows
    val categories by viewModel.categories.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    val adminUsers by viewModel.adminUsers.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val rawTables by viewModel.tables.collectAsState()

    // Section controller within Admin Tab
    var activeAdminSection by remember { mutableStateOf("CATEGORIES") }

    // Modals & Dialogue States
    var showCategoryDialog by remember { mutableStateOf<String?>(null) } // "CREATE" or oldCategoryName
    var categoryInputText by remember { mutableStateOf("") }

    var showMenuDialog by remember { mutableStateOf<MenuItem?>(null) } // null = closed, id="new" = create
    var menuItemName by remember { mutableStateOf("") }
    var menuItemPrice by remember { mutableStateOf("") }
    var menuItemCategory by remember { mutableStateOf("") }
    var menuItemDesc by remember { mutableStateOf("") }
    var menuItemAvailable by remember { mutableStateOf(true) }

    var showTableDialog by remember { mutableStateOf<RestaurantTable?>(null) } // null = closed, id="new" = create
    var tableName by remember { mutableStateOf("") }
    var tableStatusState by remember { mutableStateOf(TableStatus.AVAILABLE) }

    var showUserDialog by remember { mutableStateOf<UserDto?>(null) }
    var usernameInput by remember { mutableStateOf("") }
    var roleInput by remember { mutableStateOf("CAPTAIN") }
    var codeInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var settingsName by remember { mutableStateOf("") }
    var settingsTax by remember { mutableStateOf("") }
    var settingsService by remember { mutableStateOf("") }
    var settingsGst by remember { mutableStateOf("") }
    var settingsFssai by remember { mutableStateOf("") }
    var settingsAddress by remember { mutableStateOf("") }
    var settingsPhone by remember { mutableStateOf("") }
    var settingsFooterMessage by remember { mutableStateOf("") }
    var settingsThankYouMessage by remember { mutableStateOf("") }

    // Trigger Admin Data Fetching
    LaunchedEffect(userRole) {
        if (userRole == "OWNER" || userRole == "ADMIN") {
            viewModel.loadCategories()
            viewModel.loadMenuItems()
            viewModel.loadUsers()
            viewModel.loadAuditLogs()
            viewModel.loadSettings()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (userRole == "OWNER" || userRole == "ADMIN") "A" else "C",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Column {
                                Text(
                                    text = settings?.restaurantName ?: "Spice Garden",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$userRole DASHBOARD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { viewModel.loadDashboardData() }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh button",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onNavigateToProfile() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👤",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Split Gated tab selector for Owners / Admins
                    if (false) {
                        TabRow(
                            selectedTabIndex = if (selectedDashboardTab == "MONITOR") 0 else 1,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().testTag("dashboard_role_tabs")
                        ) {
                            Tab(
                                selected = selectedDashboardTab == "MONITOR",
                                onClick = { selectedDashboardTab = "MONITOR" },
                                text = { Text("Live Terminal Monitor", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                            Tab(
                                selected = selectedDashboardTab == "ADMIN",
                                onClick = { selectedDashboardTab = "ADMIN" },
                                text = { Text("Admin POS Config Suite", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .testTag("dashboard_bottom_nav")
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already here */ },
                    icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                    label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToTables,
                    icon = { Icon(Icons.Default.TableRestaurant, "Tables Grid") },
                    label = { Text("Tables", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistory,
                    icon = { Icon(Icons.Default.History, "History") },
                    label = { Text("History", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = { Icon(Icons.Default.AccountCircle, "Profile") },
                    label = { Text("Settings", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        modifier = modifier.testTag("dashboard_root")
    ) { innerPadding ->
        val isOnline by viewModel.isOnline.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isOnline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().testTag("offline_warning_banner"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline Mode",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connection Lost. Order Operations Unavailable.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                if (selectedDashboardTab == "MONITOR") {
                    // Standard Terminals Dashboard
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("DEBUG INFO", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text("Loading: $isLoading", color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text("Tables Count: ${rawTables.size}", color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text("Error: ${errorMsg ?: "None"}", color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                        if (errorMsg != null) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.onErrorContainer)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        // Welcome Section with UTC time
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(
                                        text = "Live Restaurant Feed",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Terminal Stat Logs",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Wifi,
                                        "Connected, Realtime state active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Bento row 1
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BentoCard(
                                    emoji = "🪑",
                                    value = totalTables.toString(),
                                    label = "Total Tables",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_card_total_tables")
                                )

                                BentoCard(
                                    emoji = "👨‍🍳",
                                    value = String.format("%02d", activeCaptains),
                                    label = "Active Captains",
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_card_active_captains")
                                )
                            }
                        }

                        // Bento row 2
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("kpi_card_running_orders")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "RUNNING ORDERS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                            letterSpacing = 1.2.sp
                                        )
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = runningOrders.toString(),
                                                fontSize = 38.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "live",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy((-8).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                                .border(2.dp, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(20.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("T1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.tertiary)
                                                .border(2.dp, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(20.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("T3", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Management Shortcuts
                        item {
                            Text(
                                text = "Management Actions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        item {
                            Card(
                                onClick = onNavigateToTables,
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().testTag("pos_shortcut_action")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Restaurant, "Launch terminal UI", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1.0f)
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            "Captain Order Terminal UI",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Table statuses, taking order & dispatcher",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, "Arrow", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                    }
                } else {
                    // Admin Config Dashboard Tab completely disabled for terminal APK
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Restricted Terminal Mode")
                    }
                    if (false) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        // Horizontal selection chips for Admin Modules
                        ScrollableTabRow(
                            selectedTabIndex = when (activeAdminSection) {
                                "CATEGORIES" -> 0
                                "MENU_ITEMS" -> 1
                                "TABLES" -> 2
                                "USERS" -> 3
                                "AUDIT_LOGS" -> 4
                                else -> 5
                            },
                            edgePadding = 12.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(activeAdminSection == "CATEGORIES", onClick = { activeAdminSection = "CATEGORIES" }, text = { Text("Categories", fontSize = 12.sp) })
                            Tab(activeAdminSection == "MENU_ITEMS", onClick = { activeAdminSection = "MENU_ITEMS" }, text = { Text("Menu Items", fontSize = 12.sp) })
                            Tab(activeAdminSection == "TABLES", onClick = { activeAdminSection = "TABLES" }, text = { Text("Tables", fontSize = 12.sp) })
                            Tab(activeAdminSection == "USERS", onClick = { activeAdminSection = "USERS" }, text = { Text("Captains & Users", fontSize = 12.sp) })
                            Tab(activeAdminSection == "AUDIT_LOGS", onClick = { activeAdminSection = "AUDIT_LOGS" }, text = { Text("Audit Logs", fontSize = 12.sp) })
                            Tab(activeAdminSection == "SETTINGS", onClick = { activeAdminSection = "SETTINGS" }, text = { Text("Settings", fontSize = 12.sp) })
                        }

                        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                            when (activeAdminSection) {
                                "CATEGORIES" -> {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Category List", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Button(
                                                onClick = {
                                                    categoryInputText = ""
                                                    showCategoryDialog = "CREATE"
                                                },
                                                modifier = Modifier.testTag("add_category_btn")
                                            ) {
                                                Icon(Icons.Default.Add, "Add")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add New")
                                            }
                                        }

                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(categories) { cat ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(16.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(cat, fontWeight = FontWeight.Bold)
                                                        Row {
                                                            IconButton(onClick = {
                                                                categoryInputText = cat
                                                                showCategoryDialog = cat
                                                            }) {
                                                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                                                            }
                                                            IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "MENU_ITEMS" -> {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Menu Items", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Button(
                                                onClick = {
                                                    menuItemName = ""
                                                    menuItemCategory = categories.firstOrNull() ?: "Main"
                                                    menuItemPrice = ""
                                                    menuItemDesc = ""
                                                    menuItemAvailable = true
                                                    showMenuDialog = MenuItem("new", "", "", 0.0, "")
                                                },
                                                modifier = Modifier.testTag("add_menu_item_btn")
                                            ) {
                                                Icon(Icons.Default.Add, "Add")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Create Item")
                                            }
                                        }

                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(menuItems) { item ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(item.name, fontWeight = FontWeight.Bold)
                                                            Text(item.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                            Text("INR ${item.price}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                            Text(item.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        Row {
                                                            IconButton(onClick = {
                                                                menuItemName = item.name
                                                                menuItemPrice = item.price.toString()
                                                                menuItemCategory = item.category
                                                                menuItemDesc = item.description
                                                                menuItemAvailable = item.isAvailable
                                                                showMenuDialog = item
                                                            }) {
                                                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                                                            }
                                                            IconButton(onClick = { viewModel.deleteMenuItem(item.id) }) {
                                                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "TABLES" -> {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Tables CRUD", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Button(
                                                onClick = {
                                                    tableName = ""
                                                    tableStatusState = TableStatus.AVAILABLE
                                                    showTableDialog = RestaurantTable("new", "0", "Table new", TableStatus.AVAILABLE)
                                                },
                                                modifier = Modifier.testTag("add_table_btn")
                                            ) {
                                                Icon(Icons.Default.Add, "Add")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add Table")
                                            }
                                        }

                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(rawTables) { table ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(table.name, fontWeight = FontWeight.Bold)
                                                            Text("ID: ${table.id} | Status: ${table.status}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        Row {
                                                            IconButton(onClick = {
                                                                tableName = table.name
                                                                tableStatusState = table.status
                                                                showTableDialog = table
                                                            }) {
                                                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                                                            }
                                                            IconButton(onClick = { viewModel.deleteTable(table.id) }) {
                                                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "USERS" -> {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Staff & Users", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Button(
                                                onClick = {
                                                    usernameInput = ""
                                                    roleInput = "CAPTAIN"
                                                    codeInput = currentUser?.restaurantCode ?: ""
                                                    passwordInput = ""
                                                    showUserDialog = UserDto("new", "", "", "")
                                                },
                                                modifier = Modifier.testTag("add_user_btn")
                                            ) {
                                                Icon(Icons.Default.Add, "Add")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Invite Staff")
                                            }
                                        }

                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(adminUsers) { user ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(user.username, fontWeight = FontWeight.Bold)
                                                            Text("Role: ${user.role} | Tenant: ${user.restaurantCode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                        Row {
                                                            IconButton(onClick = {
                                                                usernameInput = user.username
                                                                roleInput = user.role
                                                                codeInput = user.restaurantCode ?: ""
                                                                passwordInput = ""
                                                                showUserDialog = user
                                                            }) {
                                                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                                                            }
                                                            IconButton(onClick = { viewModel.deleteUser(user.id) }) {
                                                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "AUDIT_LOGS" -> {
                                    Column {
                                        Text("Action Audit Trail Log", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(auditLogs) { log ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(log.action, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                                            Text("User: ${log.username}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(log.details, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "SETTINGS" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text("Restaurant Information Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text("Details", fontWeight = FontWeight.Bold)
                                                Text("Name: ${settings?.restaurantName ?: "Not Configured"}")
                                                Text("Tax rate: ${settings?.taxPercentage ?: 0.0}%")
                                                Text("Service rate: ${settings?.serviceChargePercentage ?: 0.0}%")
                                                Text("GST Number: ${settings?.gstNumber ?: "27AAAPS1234A1Z5"}")
                                                Text("FSSAI Number: ${settings?.fssaiNumber ?: "12345678901234"}")
                                                Text("Address: ${settings?.address ?: "123 Tech Park, Sector 5, Bangalore"}")
                                                Text("Phone Number: ${settings?.phoneNumber ?: "+91 98765 43210"}")
                                                Text("Footer: ${settings?.footerMessage ?: "GST & Service Charges applied as per govt norms."}")
                                                Text("Thank You: ${settings?.thankYouMessage ?: "Thank you for dining with us!"}")
                                                Text("Default currency: ${settings?.currency ?: "INR"}")

                                                Button(
                                                    onClick = {
                                                        settingsName = settings?.restaurantName ?: ""
                                                        settingsTax = (settings?.taxPercentage ?: 5.0).toString()
                                                        settingsService = (settings?.serviceChargePercentage ?: 10.0).toString()
                                                        settingsGst = settings?.gstNumber ?: "27AAAPS1234A1Z5"
                                                        settingsFssai = settings?.fssaiNumber ?: "12345678901234"
                                                        settingsAddress = settings?.address ?: "123 Tech Park, Sector 5, Bangalore"
                                                        settingsPhone = settings?.phoneNumber ?: "+91 98765 43210"
                                                        settingsFooterMessage = settings?.footerMessage ?: "GST & Service Charges applied as per govt norms."
                                                        settingsThankYouMessage = settings?.thankYouMessage ?: "Thank you for dining with us!"
                                                        showSettingsDialog = true
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Settings, "Config")
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Reconfigure settings")
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
        }

        // --- Categories Dialog ---
        if (showCategoryDialog != null) {
            AlertDialog(
                onDismissRequest = { showCategoryDialog = null },
                title = { Text(if (showCategoryDialog == "CREATE") "Create Category" else "Rename Category") },
                text = {
                    OutlinedTextField(
                        value = categoryInputText,
                        onValueChange = { categoryInputText = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val trimmed = categoryInputText.trim()
                        if (trimmed.isNotEmpty()) {
                            if (showCategoryDialog == "CREATE") {
                                viewModel.createCategory(trimmed)
                            } else {
                                viewModel.updateCategory(showCategoryDialog!!, trimmed)
                            }
                        }
                        showCategoryDialog = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCategoryDialog = null }) { Text("Dismiss") }
                }
            )
        }

        // --- Menu Item Dialog ---
        showMenuDialog?.let { currentItem ->
            AlertDialog(
                onDismissRequest = { showMenuDialog = null },
                title = { Text(if (currentItem.id == "new") "Create Menu Item" else "Update Menu Item") },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            OutlinedTextField(
                                value = menuItemName,
                                onValueChange = { menuItemName = it },
                                label = { Text("Item Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = menuItemPrice,
                                onValueChange = { menuItemPrice = it },
                                label = { Text("Price (Double)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = menuItemCategory,
                                onValueChange = { menuItemCategory = it },
                                label = { Text("Category") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = menuItemDesc,
                                onValueChange = { menuItemDesc = it },
                                label = { Text("Description") },
                                singleLine = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val priceNum = menuItemPrice.toDoubleOrNull() ?: 0.0
                        val randomId = if (currentItem.id == "new") "menu_" + (100..999).random() else currentItem.id
                        val itemObj = MenuItem(
                            id = randomId,
                            name = menuItemName.trim(),
                            category = menuItemCategory.trim().ifEmpty { "Main" },
                            price = priceNum,
                            description = menuItemDesc.trim(),
                            isAvailable = menuItemAvailable
                        )
                        if (currentItem.id == "new") {
                            viewModel.createMenuItem(itemObj)
                        } else {
                            viewModel.updateMenuItem(itemObj)
                        }
                        showMenuDialog = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMenuDialog = null }) { Text("Cancel") }
                }
            )
        }

        // --- Tables Dialog ---
        showTableDialog?.let { currentTable ->
            AlertDialog(
                onDismissRequest = { showTableDialog = null },
                title = { Text(if (currentTable.id == "new") "Create Restaurant Table" else "Modify Table") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = tableName,
                            onValueChange = { tableName = it },
                            label = { Text("Table Name/Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val randomId = if (currentTable.id == "new") "T" + (10..99).random() else currentTable.id
                        val tbl = RestaurantTable(
                            id = randomId,
                            tableNumber = randomId,
                            name = tableName.trim().ifEmpty { "Table " + randomId },
                            status = tableStatusState
                        )
                        if (currentTable.id == "new") {
                            viewModel.createTable(tbl)
                        } else {
                            viewModel.updateTable(tbl)
                        }
                        showTableDialog = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTableDialog = null }) { Text("Cancel") }
                }
            )
        }

        // --- User/Staff Dialog ---
        showUserDialog?.let { u ->
            AlertDialog(
                onDismissRequest = { showUserDialog = null },
                title = { Text(if (u.id == "new") "Invite Staff Member" else "Update Staff Member") },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = roleInput,
                                onValueChange = { roleInput = it },
                                label = { Text("Role (OWNER, ADMIN, CAPTAIN, BILLER, KITCHEN)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it },
                                label = { Text("Restaurant Tenant Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Access Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (usernameInput.trim().isNotEmpty()) {
                            if (u.id == "new") {
                                viewModel.createUser(
                                    usernameInput.trim(),
                                    roleInput.trim().uppercase(),
                                    codeInput.trim(),
                                    passwordInput.trim()
                                )
                            } else {
                                viewModel.updateUser(
                                    u.id,
                                    usernameInput.trim(),
                                    roleInput.trim().uppercase(),
                                    codeInput.trim(),
                                    passwordInput.trim().ifEmpty { "123456" }
                                )
                            }
                        }
                        showUserDialog = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUserDialog = null }) { Text("Dismiss") }
                }
            )
        }

        // --- Settings Dialog ---
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Restaurant Setup Configuration") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = settingsName,
                            onValueChange = { settingsName = it },
                            label = { Text("Restaurant Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = settingsTax,
                            onValueChange = { settingsTax = it },
                            label = { Text("Governing Tax Rate (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = settingsService,
                            onValueChange = { settingsService = it },
                            label = { Text("Service Charge (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = settingsGst,
                            onValueChange = { settingsGst = it },
                            label = { Text("GSTIN Certificate Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = settingsFssai,
                            onValueChange = { settingsFssai = it },
                            label = { Text("FSSAI License Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = settingsAddress,
                            onValueChange = { settingsAddress = it },
                            label = { Text("Registered Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = settingsPhone,
                            onValueChange = { settingsPhone = it },
                            label = { Text("Customer Helpline Phone") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = settingsFooterMessage,
                            onValueChange = { settingsFooterMessage = it },
                            label = { Text("Receipt Footer Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = settingsThankYouMessage,
                            onValueChange = { settingsThankYouMessage = it },
                            label = { Text("Thank You Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val tax = settingsTax.toDoubleOrNull() ?: 5.0
                        val srv = settingsService.toDoubleOrNull() ?: 10.0
                        viewModel.updateSettings(
                            RestaurantSettings(
                                id = settings?.id ?: "def",
                                restaurantName = settingsName.trim().ifEmpty { "Spice Garden" },
                                taxPercentage = tax,
                                serviceChargePercentage = srv,
                                currency = settings?.currency ?: "INR",
                                gstNumber = settingsGst,
                                fssaiNumber = settingsFssai,
                                address = settingsAddress,
                                phoneNumber = settingsPhone,
                                footerMessage = settingsFooterMessage,
                                thankYouMessage = settingsThankYouMessage
                            )
                        )
                        showSettingsDialog = false
                    }) {
                        Text("Re-Apply Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
}
}

@Composable
fun BentoCard(
    emoji: String,
    value: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        modifier = modifier.height(150.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp,
                textAlign = TextAlign.Start
            )
            Column {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = contentColor,
                    lineHeight = 32.sp
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
