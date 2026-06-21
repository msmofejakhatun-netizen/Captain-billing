package com.example.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.OrderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    tableId: String,
    viewModel: CartViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToKot: (String) -> Unit,
    onNavigateToBilling: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeOrder by viewModel.activeOrder.collectAsState()
    val tableNumber by viewModel.tableNumber.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    val subtotal by viewModel.subtotal.collectAsState()
    val taxAmount by viewModel.taxAmount.collectAsState()
    val serviceCharge by viewModel.serviceCharge.collectAsState()
    val grandTotal by viewModel.grandTotal.collectAsState()

    LaunchedEffect(tableId) {
        viewModel.loadCart(tableId)
        viewModel.setupRealtimeUpdates(tableId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Table ${tableNumber ?: tableId} Cart") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "BackBtn")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.testTag("cart_screen_root")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val items = activeOrder?.items ?: emptyList()

            if (isLoading && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Current table has no active food", fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Browse Menu")
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
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

                    if (errorMsg != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                        }
                    }

                    // Cart Items List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.0f)
                            .testTag("cart_items_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            CartItemRow(
                                item = item,
                                tableId = tableId,
                                onIncrease = { viewModel.increaseQuantity(tableId, item.id, item.quantity) },
                                onDecrease = { viewModel.decreaseQuantity(tableId, item.id, item.quantity) },
                                onRemove = { viewModel.removeItem(tableId, item.id) },
                                isOnline = isOnline
                            )
                        }
                    }

                    // Cost estimation sheet & Checkout Actions
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Order Stat Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", fontSize = 14.sp)
                                Text(String.format("$%.2f", subtotal), fontSize = 14.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CGST + SGST (5%)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("$%.2f", taxAmount), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Service Charge (10%)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("$%.2f", serviceCharge), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(String.format("$%.2f", grandTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bottom Buttons bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToKot(tableId) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cart_send_kot_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    enabled = isOnline
                                ) {
                                    Icon(Icons.Default.Kitchen, "Kitchen")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Send KOT", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { onNavigateToBilling(tableId) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cart_generate_bill_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    enabled = isOnline
                                ) {
                                    Icon(Icons.Default.Receipt, "Bill")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Get Bill", fontSize = 13.sp)
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
fun CartItemRow(
    item: OrderItem,
    tableId: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    isOnline: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_block_" + item.id)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.0f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = String.format("$%.2f x %d", item.price, item.quantity),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = String.format("$%.2f", item.total),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Dynamic Counter Layout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(32.dp).testTag("cart_item_minus_" + item.id),
                    enabled = isOnline
                ) {
                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text("${item.quantity}", fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(32.dp).testTag("cart_item_plus_" + item.id),
                    enabled = isOnline
                ) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp).testTag("cart_item_delete_" + item.id),
                enabled = isOnline
            ) {
                Icon(Icons.Default.Delete, "Delete item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}
