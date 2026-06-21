package com.example.ui.menu

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    tableId: String,
    viewModel: MenuViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.filteredItems.collectAsState()
    val allItems by viewModel.menuItems.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    val menuRawJson by viewModel.menuRawJson.collectAsState()
    val menuParseError by viewModel.menuParseError.collectAsState()
    val menuApiCode by viewModel.menuApiCode.collectAsState()
    val menuApiUrl by viewModel.menuApiUrl.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.addItemResult.collect { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Item added to cart", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed selection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Menu Catalog", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Table: $tableId", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "BackBtn")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMenu() }) {
                        Icon(Icons.Default.Refresh, "Sync")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCart(tableId) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("floating_cart_fab")
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, "Cart")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Order Cart", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.testTag("menu_screen_root")
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

            // API Diagnostics Debugger Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔌 MENU API DEBUGGER",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "URL: ${menuApiUrl ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Status Code: ${menuApiCode ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (menuApiCode == 200) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    
                    if (menuParseError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "⚠️ PARSING / RUNTIME ERROR:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = menuParseError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Raw Response (First 2000 Chars):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    val rawBody = menuRawJson ?: "null / no response received"
                    val truncatedBody = if (rawBody.length > 2000) rawBody.substring(0, 2000) + "... (truncated)" else rawBody
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            item {
                                Text(
                                    text = truncatedBody,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            val debugInfo by viewModel.debugInfo.collectAsState()

            if (debugInfo != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("add_to_cart_debug_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (debugInfo!!.statusCode != 200 && debugInfo!!.statusCode != 201) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🛒 ADD TO CART DEBUG",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (debugInfo!!.statusCode != 200 && debugInfo!!.statusCode != 201) 
                                    MaterialTheme.colorScheme.onErrorContainer 
                                else 
                                    MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            IconButton(onClick = { viewModel.clearDebugInfo() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Debug Info",
                                    tint = if (debugInfo!!.statusCode != 200 && debugInfo!!.statusCode != 201) 
                                        MaterialTheme.colorScheme.onErrorContainer 
                                    else 
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        
                        val textColor = if (debugInfo!!.statusCode != 200 && debugInfo!!.statusCode != 201) 
                            MaterialTheme.colorScheme.onErrorContainer 
                        else 
                            MaterialTheme.colorScheme.onTertiaryContainer

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(textColor.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(8.dp))

                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("1. Endpoint: ${debugInfo!!.endpoint}", color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("2. Full URL: ${debugInfo!!.fullUrl}", color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("3. Method: ${debugInfo!!.httpMethod}", color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                
                                Text("4. Request Body JSON:", color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(
                                    debugInfo!!.requestBodyJson,
                                    color = textColor.copy(alpha = 0.85f),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .background(textColor.copy(alpha = 0.08f))
                                        .padding(6.dp)
                                        .fillMaxWidth()
                                )
                                
                                Text("5. HTTP Status Code: ${debugInfo!!.statusCode ?: "EXCEPTION"}", color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                
                                Text("6. Raw Response Body:", color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(
                                    debugInfo!!.rawResponseBody.ifEmpty { "Empty body / No error body" },
                                    color = textColor.copy(alpha = 0.85f),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .background(textColor.copy(alpha = 0.08f))
                                        .padding(6.dp)
                                        .fillMaxWidth()
                                )
                                
                                if (debugInfo!!.exceptionStacktrace.isNotEmpty()) {
                                    Text("7. Exception Stacktrace:", color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        debugInfo!!.exceptionStacktrace,
                                        color = textColor.copy(alpha = 0.85f),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .background(textColor.copy(alpha = 0.08f))
                                            .padding(6.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            SearchBarField(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("menu_search_bar")
            )

            // Dynamic horizontal scrollable categories tab
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("categories_row")
            ) {
                item {
                    val isAllSelected = selectedCategory == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All Categories") }
                    )
                }

                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading && items.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                if (errorMsg != null && allItems.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                    }
                }

                if (items.isEmpty()) {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No matching items found", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize().weight(1f).testTag("menu_items_list")
                    ) {
                        items(items, key = { it.id }) { item ->
                            MenuItemRow(
                                item = item,
                                tableId = tableId,
                                onAdd = { qty -> viewModel.addItemToTable(tableId, item.id, qty) },
                                isOnline = isOnline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search dishes, cuisines, drinks...") },
        leadingIcon = { Icon(Icons.Default.Search, "Search Icon") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear Icon")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = modifier
    )
}

@Composable
fun MenuItemRow(
    item: MenuItem,
    tableId: String,
    onAdd: (Int) -> Unit,
    isOnline: Boolean
) {
    var quantity by remember { mutableStateOf(1) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("menu_item_row_" + item.id),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food emoji placeholding cover art
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (item.category.lowercase()) {
                        "beverages" -> "🍹"
                        "desserts" -> "🍰"
                        "appetizers" -> "🥟"
                        else -> "🍛"
                    },
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1.0f)
            ) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                Text("$${item.price}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Quantity Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier.size(32.dp).testTag("decrease_qty_" + item.id)
                    ) {
                        Icon(Icons.Default.Remove, "Less", modifier = Modifier.size(16.dp))
                    }

                    Text("$quantity", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))

                    IconButton(
                        onClick = { quantity++ },
                        modifier = Modifier.size(32.dp).testTag("increase_qty_" + item.id)
                    ) {
                        Icon(Icons.Default.Add, "More", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onAdd(quantity)
                        quantity = 1 // Reset back to default
                    },
                    modifier = Modifier.height(32.dp).testTag("add_item_btn_" + item.id),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    enabled = isOnline
                ) {
                    Text("Add", fontSize = 12.sp)
                }
            }
        }
    }
}
