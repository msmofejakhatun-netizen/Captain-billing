package com.example.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.printer.PaperWidth
import com.example.data.printer.PrinterSettings
import com.example.data.printer.PrinterType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToTables: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val settings by viewModel.printerSettings.collectAsState()
    val status by viewModel.printerStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Forms local editing states
    var type by remember(settings) { mutableStateOf(settings.type) }
    var nameOrMac by remember(settings) { mutableStateOf(settings.nameOrMac) }
    var ip by remember(settings) { mutableStateOf(settings.ip) }
    var portString by remember(settings) { mutableStateOf(settings.port.toString()) }
    var paperWidth by remember(settings) { mutableStateOf(settings.paperWidth) }
    var autoPrintKot by remember(settings) { mutableStateOf(settings.autoPrintKot) }
    var autoPrintBill by remember(settings) { mutableStateOf(settings.autoPrintBill) }

    // Multi-Printer & Phase 6 Hardening Edit States
    var billingType by remember(settings) { mutableStateOf(settings.billingType) }
    var billingIp by remember(settings) { mutableStateOf(settings.billingIp) }
    var billingPortString by remember(settings) { mutableStateOf(settings.billingPort.toString()) }
    var billingNameOrMac by remember(settings) { mutableStateOf(settings.billingNameOrMac) }

    var enableFailover by remember(settings) { mutableStateOf(settings.enableFailover) }
    var failoverType by remember(settings) { mutableStateOf(settings.failoverType) }
    var failoverIp by remember(settings) { mutableStateOf(settings.failoverIp) }
    var failoverPortString by remember(settings) { mutableStateOf(settings.failoverPort.toString()) }
    var failoverNameOrMac by remember(settings) { mutableStateOf(settings.failoverNameOrMac) }

    var drinksPrinterIp by remember(settings) { mutableStateOf(settings.drinksPrinterIp) }
    var dessertPrinterIp by remember(settings) { mutableStateOf(settings.dessertPrinterIp) }
    var mainsPrinterIp by remember(settings) { mutableStateOf(settings.mainsPrinterIp) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.loadPrinterSettings()
        viewModel.checkPrinterStatus()
    }

    LaunchedEffect(viewModel.logoutFinished) {
        viewModel.logoutFinished.collect { finished ->
            if (finished) onNavigateToLogin()
        }
    }

    LaunchedEffect(viewModel.printResult) {
        viewModel.printResult.collect { result ->
            if (result.isSuccess) {
                snackbarHostState.showSnackbar("Print test page succeeded!")
            } else {
                snackbarHostState.showSnackbar("Print failed: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown Error"}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terminal Profile", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .testTag("profile_bottom_nav")
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
                    selected = true,
                    onClick = { /* Already here */ },
                    icon = { Icon(Icons.Default.AccountCircle, "Profile") },
                    label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        modifier = modifier.testTag("profile_screen_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Card Details
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            "User avatar",
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentUser?.username ?: "Michael Captain",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = "System Authorization: ${currentUser?.role ?: "CAPTAIN"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Connected Code:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentUser?.restaurantCode ?: "REST-PEER99", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Epsom Thermal Printer Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Printer Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Epson Thermal Printer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Printer Connection Status Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Printer Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                val dotColor = when (status) {
                                    "Connected" -> Color(0xFF2E7D32)
                                    "Checking..." -> Color(0xFFEF6C00)
                                    else -> Color(0xFFC62828)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.checkPrinterStatus() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, "Refresh status", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Connection Mode for Primary
                    Text("Primary Printer (KOT Default) Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = type == PrinterType.WIFI,
                            onClick = { type = PrinterType.WIFI },
                            label = { Text("LAN / WiFi") },
                            leadingIcon = if (type == PrinterType.WIFI) {
                                { Icon(Icons.Default.Wifi, "WiFi Mode", modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = type == PrinterType.BLUETOOTH,
                            onClick = { type = PrinterType.BLUETOOTH },
                            label = { Text("Bluetooth") },
                            leadingIcon = if (type == PrinterType.BLUETOOTH) {
                                { Icon(Icons.Default.Bluetooth, "Bluetooth Mode", modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Configuration fields depending on Selected Mode
                    if (type == PrinterType.WIFI) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = ip,
                                onValueChange = { ip = it },
                                label = { Text("Primary IP Address") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = portString,
                                onValueChange = { portString = it },
                                label = { Text("Primary TCP Gate Port") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = nameOrMac,
                            onValueChange = { nameOrMac = it },
                            label = { Text("Paired Bluetooth Name / MAC") },
                            placeholder = { Text("e.g. Epson_TM-T88") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Separate Dedicated Guest Billing Printer
                    Text("Separate Dedicated Guest Billing Printer", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = billingType == PrinterType.WIFI,
                            onClick = { billingType = PrinterType.WIFI },
                            label = { Text("Billing WiFi") },
                            leadingIcon = if (billingType == PrinterType.WIFI) {
                                { Icon(Icons.Default.Wifi, "WiFi", modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = billingType == PrinterType.BLUETOOTH,
                            onClick = { billingType = PrinterType.BLUETOOTH },
                            label = { Text("Billing Bluetooth") },
                            leadingIcon = if (billingType == PrinterType.BLUETOOTH) {
                                { Icon(Icons.Default.Bluetooth, "Bluetooth", modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (billingType == PrinterType.WIFI) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = billingIp,
                                onValueChange = { billingIp = it },
                                label = { Text("Billing IP Address") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = billingPortString,
                                onValueChange = { billingPortString = it },
                                label = { Text("Billing TCP Gate Port") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = billingNameOrMac,
                            onValueChange = { billingNameOrMac = it },
                            label = { Text("Billing Bluetooth Name / MAC") },
                            placeholder = { Text("e.g. Epson_Billing") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Kitchen Printer Routing (Multi-Kitchen)
                    Text("Department Kitchen Mapped Gateways", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(
                        value = drinksPrinterIp,
                        onValueChange = { drinksPrinterIp = it },
                        label = { Text("Drinks IP Gateway (Category matching)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dessertPrinterIp,
                        onValueChange = { dessertPrinterIp = it },
                        label = { Text("Desserts IP Gateway (Category matching)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mainsPrinterIp,
                        onValueChange = { mainsPrinterIp = it },
                        label = { Text("Mains / Food IP Gateway") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Auto Failover Settings Drawer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Auto Failover Printer Trigger", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                            Text("Reroutes immediately if target goes offline", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = enableFailover,
                            onCheckedChange = { enableFailover = it }
                        )
                    }

                    if (enableFailover) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = failoverType == PrinterType.WIFI,
                                    onClick = { failoverType = PrinterType.WIFI },
                                    label = { Text("Failover WiFi") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = failoverType == PrinterType.BLUETOOTH,
                                    onClick = { failoverType = PrinterType.BLUETOOTH },
                                    label = { Text("Failover Bluetooth") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (failoverType == PrinterType.WIFI) {
                                OutlinedTextField(
                                    value = failoverIp,
                                    onValueChange = { failoverIp = it },
                                    label = { Text("Failover IP Address") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = failoverPortString,
                                    onValueChange = { failoverPortString = it },
                                    label = { Text("Failover TCP Gate Port") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedTextField(
                                    value = failoverNameOrMac,
                                    onValueChange = { failoverNameOrMac = it },
                                    label = { Text("Failover Bluetooth Name / MAC") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Selection of Paper size (58mm vs 80mm)
                    Text("Paper Roll Width", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = paperWidth == PaperWidth.W58MM,
                            onClick = { paperWidth = PaperWidth.W58MM },
                            label = { Text("58mm Roll (32 col)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = paperWidth == PaperWidth.W80MM,
                            onClick = { paperWidth = PaperWidth.W80MM },
                            label = { Text("80mm Roll (48 col)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Automation Switches
                    Text("Auto Printing Rules", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Auto-Print KOT immediately", fontSize = 13.sp)
                        Switch(
                            checked = autoPrintKot,
                            onCheckedChange = { autoPrintKot = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Auto-Print Bill immediately", fontSize = 13.sp)
                        Switch(
                            checked = autoPrintBill,
                            onCheckedChange = { autoPrintBill = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Actions Row: Print Test Page and Save Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.printTestPage() },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Print, "Test page")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print Test", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val finalPort = portString.toIntOrNull() ?: 9100
                                val bPort = billingPortString.toIntOrNull() ?: 9100
                                val fPort = failoverPortString.toIntOrNull() ?: 9100
                                val newSettings = PrinterSettings(
                                    type = type,
                                    nameOrMac = nameOrMac,
                                    ip = ip,
                                    port = finalPort,
                                    paperWidth = paperWidth,
                                    autoPrintKot = autoPrintKot,
                                    autoPrintBill = autoPrintBill,
                                    
                                    billingType = billingType,
                                    billingIp = billingIp,
                                    billingPort = bPort,
                                    billingNameOrMac = billingNameOrMac,
                                    
                                    enableFailover = enableFailover,
                                    failoverType = failoverType,
                                    failoverIp = failoverIp,
                                    failoverPort = fPort,
                                    failoverNameOrMac = failoverNameOrMac,
                                    
                                    drinksPrinterIp = drinksPrinterIp,
                                    dessertPrinterIp = dessertPrinterIp,
                                    mainsPrinterIp = mainsPrinterIp
                                )
                                viewModel.savePrinterSettings(newSettings)
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Save, "Save configurations")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Setups", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout row
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_action_btn")
            ) {
                Icon(Icons.Default.ExitToApp, "Logout Icon")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secure Logout Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
