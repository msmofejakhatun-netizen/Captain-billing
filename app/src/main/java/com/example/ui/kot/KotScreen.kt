package com.example.ui.kot

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.KOT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KotScreen(
    tableId: String,
    viewModel: KotViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kots by viewModel.kots.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.sendResult.collect { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "KOT sent to kitchen successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, result.exceptionOrNull()?.message ?: "KOT failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.printResult.collect { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "KOT printed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Printer error: ${result.exceptionOrNull()?.localizedMessage ?: "Offline"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Table $tableId - KOT terminal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "BackBtn")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllKOTs() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.testTag("kot_screen_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Send KOT Action Block
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Kitchen, "Kitchen Terminal Icon", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Kitchen Dispatch Hub", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Ready to synchronize active order cart items with the chefs' display?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.sendKOT(tableId) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_kot_action_btn")
                    ) {
                        Text("Send Active KOT to Chef", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Historical KOT Tickets", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            if (isLoading && kots.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (kots.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No KOT logs yet", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("kot_lazy_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val filteredKots = kots.filter { it.tableId == tableId }
                    if (filteredKots.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("No KOT dispatches for Table $tableId", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        items(filteredKots, key = { it.id }) { kot ->
                            KotTicketCard(
                                kot = kot,
                                onPrint = { reprint, reason -> viewModel.printKOT(kot.id, reprint, reason) },
                                onUpdateStatus = { newStatus -> viewModel.updateKotStatus(kot.id, newStatus) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KotTicketCard(
    kot: KOT,
    onPrint: (reprint: Boolean, reason: String?) -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    var showReprintDialog by remember { mutableStateOf(false) }
    var reprintReasonInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    val bColor = when (kot.status.uppercase()) {
        "PENDING", "SENT" -> Color(0xFFE65100) // Dark Orange
        "PREPARING" -> Color(0xFFF57C00) // Orange
        "READY" -> Color(0xFF2E7D32) // Green
        "SERVED" -> Color(0xFF1976D2) // Blue
        else -> Color(0xFFC62828) // Red (Cancelled)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.5.dp, brush = androidx.compose.ui.graphics.SolidColor(bColor)),
        modifier = Modifier.fillMaxWidth().testTag("kot_card_" + kot.id)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(kot.id, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                
                Surface(
                    color = bColor,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = kot.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub items
            kot.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• ${item.name}", fontSize = 13.sp)
                    Text("x${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // KOT Status transition chips: SENT, PREPARING, READY, SERVED
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transition:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                
                val current = kot.status.uppercase()
                if (current == "PENDING" || current == "SENT") {
                    AssistChip(
                        onClick = { onUpdateStatus("PREPARING") },
                        label = { Text("Set Preparing", fontSize = 10.sp) }
                    )
                }
                if (current == "PREPARING") {
                    AssistChip(
                        onClick = { onUpdateStatus("READY") },
                        label = { Text("Set Ready", fontSize = 10.sp) }
                    )
                }
                if (current == "READY") {
                    AssistChip(
                        onClick = { onUpdateStatus("SERVED") },
                        label = { Text("Set Served", fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Manual print actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onPrint(false, null) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print Icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = {
                        reprintReasonInput = ""
                        showReprintDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Reprint Icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reprint KOT", fontSize = 12.sp)
                }
            }
        }
    }

    // Reprint Dialog layout
    if (showReprintDialog) {
        AlertDialog(
            onDismissRequest = { showReprintDialog = false },
            title = { Text("KOT Reprint Reason", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please enter a mandatory explanation for reprinting KOT sequence ticket.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    OutlinedTextField(
                        value = reprintReasonInput,
                        onValueChange = { reprintReasonInput = it },
                        label = { Text("Explanation Reason *") },
                        placeholder = { Text("e.g. Printer Jam, Kitchen Lost Original") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reprint_reason_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reprintReasonInput.trim().isEmpty()) {
                            Toast.makeText(context, "Explanation/Reason is mandatory!", Toast.LENGTH_SHORT).show()
                        } else {
                            onPrint(true, reprintReasonInput)
                            showReprintDialog = false
                        }
                    }
                ) {
                    Text("Submit & Reprint")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReprintDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
