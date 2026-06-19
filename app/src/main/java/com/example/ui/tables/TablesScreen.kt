package com.example.ui.tables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.domain.model.RestaurantTable
import com.example.domain.model.TableStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(
    viewModel: TablesViewModel,
    onNavigateToOpenTable: (String) -> Unit,
    onNavigateToMenu: (String) -> Unit,
    onNavigateToCart: (String) -> Unit,
    onNavigateToKot: (String) -> Unit,
    onNavigateToBilling: (String) -> Unit,
    onNavigateToSettlement: (String, String, Double) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filter by viewModel.selectedFilter.collectAsState()
    val tables by viewModel.filteredTables.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var selectedTableForActions by remember { mutableStateOf<RestaurantTable?>(null) }
    var showRoleGatingMessage by remember { mutableStateOf<String?>(null) }

    // Dialog operations states
    var showTransferDialogTable by remember { mutableStateOf<RestaurantTable?>(null) }
    var showMergeDialogTable by remember { mutableStateOf<RestaurantTable?>(null) }
    var showChangeCaptainTable by remember { mutableStateOf<RestaurantTable?>(null) }
    var targetTableNameInput by remember { mutableStateOf("") }
    var captainNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("Tables Status Grid", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { viewModel.loadTables() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .testTag("tables_bottom_nav")
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToDashboard,
                    icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already here */ },
                    icon = { Icon(Icons.Default.TableRestaurant, "Tables Grid") },
                    label = { Text("Tables", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
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
        modifier = modifier.testTag("tables_screen_root")
    ) { innerPadding ->
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
                            Icons.Default.WifiOff,
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

            // Horizontally Scrollable Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = filter.ordinal,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth().testTag("table_filters_row")
            ) {
                TableFilter.values().forEach { category ->
                    Tab(
                        selected = filter == category,
                        onClick = { viewModel.setFilter(category) },
                        text = { Text(category.name, fontSize = 13.sp) }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                if (errorMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                    }
                }

                if (tables.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HourglassEmpty, "Empty", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No tables configured for filter", fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize().weight(1f).testTag("tables_lazy_grid")
                    ) {
                        items(tables, key = { it.id }) { table ->
                            TableBlock(
                                table = table,
                                onClick = {
                                    if (userRole == "KITCHEN") {
                                        showRoleGatingMessage = "Kitchen staffs can only process KOT Kitchen Dispatch screens."
                                    } else {
                                        when (table.status) {
                                            TableStatus.AVAILABLE -> {
                                                if (userRole == "BILLER") {
                                                    showRoleGatingMessage = "Billers cannot open tables. This is a Captain action."
                                                } else {
                                                    onNavigateToOpenTable(table.tableNumber)
                                                }
                                            }
                                            TableStatus.OPEN -> {
                                                if (userRole == "BILLER") {
                                                    showRoleGatingMessage = "Billers cannot manage menu ordering. This is a Captain action."
                                                } else {
                                                    onNavigateToMenu(table.id)
                                                }
                                            }
                                            TableStatus.RUNNING, TableStatus.BILLED -> {
                                                selectedTableForActions = table
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Show permission alert dialog if gated
        showRoleGatingMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { showRoleGatingMessage = null },
                title = { Text("Permission Restriction", fontWeight = FontWeight.Bold) },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { showRoleGatingMessage = null }) {
                        Text("Acknowledge")
                    }
                }
            )
        }

        // Action sheet dialog for interactive Table management workflows
        selectedTableForActions?.let { table ->
            AlertDialog(
                onDismissRequest = { selectedTableForActions = null },
                title = { Text("Table ${table.name} Actions", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Current table status is ${table.status.name}. Active User Role is: $userRole", fontSize = 14.sp)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        if (table.status == TableStatus.RUNNING) {
                            val isHeld = viewModel.isTableOnHold(table.id)
                            
                            if (isHeld) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Text(
                                        text = "🚨 WARNING: ORDER CURRENTLY ON HOLD",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            if (userRole != "BILLER") {
                                Button(
                                    onClick = {
                                        selectedTableForActions = null
                                        onNavigateToMenu(table.id)
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("modal_add_item_btn"),
                                    enabled = isOnline && !isHeld
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, "Add food")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Menu Items")
                                }

                                Button(
                                    onClick = {
                                        selectedTableForActions = null
                                        onNavigateToCart(table.id)
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("modal_view_cart_btn")
                                ) {
                                    Icon(Icons.Default.ShoppingCart, "View order")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Cart & Update Core Qtys")
                                }

                                Button(
                                    onClick = {
                                        selectedTableForActions = null
                                        onNavigateToKot(table.id)
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("modal_kot_btn")
                                ) {
                                    Icon(Icons.Default.Kitchen, "Kitchen")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Kitchen Dispatch / KOT Log")
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                                Text("Production Table Operations", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

                                // Hold Toggle Action
                                OutlinedButton(
                                    onClick = {
                                        viewModel.toggleTableHold(table.id)
                                        selectedTableForActions = null
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("modal_hold_btn")
                                ) {
                                    Icon(if (isHeld) Icons.Default.PlayArrow else Icons.Default.Pause, "Hold order toggle")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isHeld) "Resume Order / Dispatch Tasks" else "Hold Order (Freeze items)")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedTableForActions = null
                                            targetTableNameInput = ""
                                            showTransferDialogTable = table
                                        },
                                        modifier = Modifier.weight(1f).testTag("modal_transfer_btn")
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, "Transfer", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Transfer", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            selectedTableForActions = null
                                            targetTableNameInput = ""
                                            showMergeDialogTable = table
                                        },
                                        modifier = Modifier.weight(1f).testTag("modal_merge_btn")
                                    ) {
                                        Icon(Icons.Default.Merge, "Merge", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Merge", fontSize = 12.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedTableForActions = null
                                        captainNameInput = table.assignedCaptainId ?: ""
                                        showChangeCaptainTable = table
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("modal_captain_btn")
                                ) {
                                    Icon(Icons.Default.AssignmentInd, "Assign captain", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Change Captain Assignment")
                                }
                            }

                            if (userRole == "BILLER" || userRole == "ADMIN" || userRole == "OWNER" || userRole == "CAPTAIN") {
                                Button(
                                    onClick = {
                                        selectedTableForActions = null
                                        onNavigateToBilling(table.id)
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("modal_bill_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    enabled = isOnline && !isHeld
                                ) {
                                    Icon(Icons.Default.Receipt, "Bill generation")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Invoice Bill")
                                }
                            }
                        }

                        if (table.status == TableStatus.BILLED) {
                            if (userRole == "BILLER" || userRole == "ADMIN" || userRole == "OWNER") {
                                Button(
                                    onClick = {
                                        selectedTableForActions = null
                                        onNavigateToSettlement(table.id, table.activeOrderId ?: "B-801", 33.3)
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("modal_pay_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    enabled = isOnline
                                ) {
                                    Icon(Icons.Default.Payments, "Complete transaction")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Complete Bill Payment Settlement")
                                }
                            } else {
                                Text("Settlement is restricted to Biller, Owner or Admin roles only.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTableForActions = null }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // 1. Transfer Table Dialog
        showTransferDialogTable?.let { table ->
            AlertDialog(
                onDismissRequest = { showTransferDialogTable = null },
                title = { Text("Transfer Table ${table.name}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Specify the name / number of the unoccupied target Table to transfer this order details to:")
                        OutlinedTextField(
                            value = targetTableNameInput,
                            onValueChange = { targetTableNameInput = it },
                            label = { Text("Target Table Name (e.g. Table 2)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("transfer_target_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val dest = tables.find { it.name.lowercase().trim() == targetTableNameInput.lowercase().trim() } ?: tables.find { it.id.lowercase().trim() == targetTableNameInput.lowercase().trim() }
                            if (dest != null) {
                                viewModel.transferTable(table.id, dest.id) { res ->
                                    if (res.isSuccess) showTransferDialogTable = null
                                }
                            } else {
                                showTransferDialogTable = null
                            }
                        },
                        enabled = targetTableNameInput.isNotEmpty()
                    ) {
                        Text("Confirm Transfer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTransferDialogTable = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 2. Merge Tables Dialog
        showMergeDialogTable?.let { table ->
            AlertDialog(
                onDismissRequest = { showMergeDialogTable = null },
                title = { Text("Merge Table ${table.name}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Specify the name / number of the occupied target Table to combine orders with:")
                        OutlinedTextField(
                            value = targetTableNameInput,
                            onValueChange = { targetTableNameInput = it },
                            label = { Text("Target Table Name (e.g. Table 1)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("merge_target_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val dest = tables.find { it.name.lowercase().trim() == targetTableNameInput.lowercase().trim() } ?: tables.find { it.id.lowercase().trim() == targetTableNameInput.lowercase().trim() }
                            if (dest != null) {
                                viewModel.mergeTables(table.id, dest.id) { res ->
                                    if (res.isSuccess) showMergeDialogTable = null
                                }
                            } else {
                                showMergeDialogTable = null
                            }
                        },
                        enabled = targetTableNameInput.isNotEmpty()
                    ) {
                        Text("Confirm Merge")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMergeDialogTable = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 3. Change Captain Dialog
        showChangeCaptainTable?.let { table ->
            AlertDialog(
                onDismissRequest = { showChangeCaptainTable = null },
                title = { Text("Change Captain Assignment", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter the name of the new Captain leading table ${table.name}:")
                        OutlinedTextField(
                            value = captainNameInput,
                            onValueChange = { captainNameInput = it },
                            label = { Text("New Captain Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("captain_assigned_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.changeCaptain(table.id, "CAPT-" + captainNameInput.uppercase().take(4), captainNameInput) { res ->
                                if (res.isSuccess) showChangeCaptainTable = null
                            }
                        },
                        enabled = captainNameInput.isNotEmpty()
                    ) {
                        Text("Assign Leader")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangeCaptainTable = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TableBlock(
    table: RestaurantTable,
    onClick: () -> Unit
) {
    val bColor = when (table.status) {
        TableStatus.AVAILABLE -> Color(0xFF2E7D32) // Green
        TableStatus.OPEN -> Color(0xFF1565C0)      // Blue
        TableStatus.RUNNING -> Color(0xFFEF6C00)   // Orange
        TableStatus.BILLED -> Color(0xFFC62828)    // Red
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .testTag("table_block_" + table.id),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bColor.copy(alpha = 0.08f)),
        border = BorderStroke(2.dp, bColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = table.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(bColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = table.status.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
