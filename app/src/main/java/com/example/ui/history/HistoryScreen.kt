package com.example.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Order
import com.example.domain.model.PaymentSettlement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToTables: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedOrders by viewModel.completedOrders.collectAsState()
    val settledPayments by viewModel.settledPayments.collectAsState()
    val totalEarnings by viewModel.totalEarnings.collectAsState()
    val ordersCount by viewModel.ordersCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Orders, 1: Settlements

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
                            Text("H", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("Financial Log Terminals", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { viewModel.loadHistory() }) {
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
                    .testTag("history_bottom_nav")
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
                    selected = true,
                    onClick = { /* Already here */ },
                    icon = { Icon(Icons.Default.History, "History") },
                    label = { Text("History", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
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
        modifier = modifier.testTag("history_screen_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Aggregate Summary KPIs
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Session Gross", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(String.format("$%.2f", totalEarnings), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Closed Orders", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("$ordersCount", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Realtime Sync", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Icon(Icons.Default.Check, "Sync Status OK", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Tab selectors
            TabRow(selectedTabIndex = activeTab, modifier = Modifier.fillMaxWidth().testTag("history_swappable_tabs")) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Completed Orders") })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Settle Receipts") })
            }

            if (isLoading && completedOrders.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }

                if (activeTab == 0) {
                    if (completedOrders.isEmpty()) {
                        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No completed orders today", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize().weight(1f).testTag("completed_orders_list")
                        ) {
                            items(completedOrders) { order ->
                                CompletedOrderRow(order = order)
                            }
                        }
                    }
                } else {
                    if (settledPayments.isEmpty()) {
                        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No settled receipts today", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize().weight(1f).testTag("settled_receipts_list")
                        ) {
                            items(settledPayments) { payment ->
                                SettlementRow(payment = payment)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedOrderRow(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("completed_order_row_" + order.id),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.id, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(String.format("$%.2f", order.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
            Text("Table: ${order.tableId} • Captain: ${order.captainId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 2.dp))
            Spacer(modifier = Modifier.height(6.dp))
            
            // Sub items list summary
            Text(order.items.joinToString { "${it.name} (x${it.quantity})" }, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettlementRow(payment: PaymentSettlement) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("settlement_row_" + payment.id),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(payment.id, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Bill Reference: ${payment.billId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Text("Method: ${payment.paymentType}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
            }
            Text(String.format("$%.2f", payment.cashAmount + payment.cardAmount + payment.upiAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
