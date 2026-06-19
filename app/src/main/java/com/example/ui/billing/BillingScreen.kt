package com.example.ui.billing

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.Percent
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Bill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    tableId: String,
    viewModel: BillingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettlement: (String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val bill by viewModel.bill.collectAsState()
    val runningOrder by viewModel.runningOrder.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    var showDiscountDialog by remember { mutableStateOf(false) }
    var discountIsPercent by remember { mutableStateOf(true) }
    var discountValueInput by remember { mutableStateOf("") }
    var discountReasonInput by remember { mutableStateOf("") }
    var managerPinInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(tableId) {
        viewModel.loadBillInfo(tableId)
        viewModel.setupRealtimeBilling(tableId)
    }

    LaunchedEffect(Unit) {
        viewModel.printResult.collect { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Guest bill printed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Printer error: ${result.exceptionOrNull()?.localizedMessage ?: "Offline"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Table $tableId - Billing Hub") },
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
        modifier = modifier.testTag("billing_screen_root")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (errorMsg != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                        }
                    }

                    if (bill == null && runningOrder == null) {
                        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No unpaid orders available to generate bills for.", color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                        }
                    } else if (bill != null) {
                        // Display Final Finalized Bill
                        Column(
                            modifier = Modifier.fillMaxSize().weight(1.0f)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, "Locked Receipt", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Bill Invoice Finalized", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Table structure frozen. Ready for settlement.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                                    }
                                }
                            }

                            // Final Bill Details
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                        Icon(Icons.Default.ReceiptLong, "Receipt Logo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                    }
                                    Text("INVOICE RECEIPT", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    Text("Bill Reference: ${bill!!.id}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Subtotal Level:", fontSize = 14.sp)
                                        Text(String.format("$%.2f", bill!!.totalAmount), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }

                                    if ((bill!!.discountPercent ?: 0.0) > 0.0 || (bill!!.discountAmount ?: 0.0) > 0.0) {
                                        val label = if ((bill!!.discountPercent ?: 0.0) > 0.0) {
                                            String.format("Discount Applied (%.1f%%):", bill!!.discountPercent)
                                        } else {
                                            "Discount Applied (Flat):"
                                        }
                                        val value = if ((bill!!.discountPercent ?: 0.0) > 0.0) {
                                            (bill!!.discountPercent ?: 0.0) / 100.0 * bill!!.totalAmount
                                        } else {
                                            bill!!.discountAmount ?: 0.0
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(label, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(String.format("-$%.2f", value), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        bill!!.discountReason?.let { reason ->
                                            if (reason.trim().isNotEmpty()) {
                                                Text("Reason: $reason", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
                                            }
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Service Surcharges (10%):", fontSize = 14.sp)
                                        Text(String.format("$%.2f", bill!!.serviceCharge), fontSize = 14.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("CGST & SGST (5%):", fontSize = 14.sp)
                                        Text(String.format("$%.2f", bill!!.taxAmount), fontSize = 14.sp)
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("GRAND TOTAL:", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(String.format("$%.2f", bill!!.grandTotal), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Manual print guest bill option
                            OutlinedButton(
                                onClick = { viewModel.printBill(bill!!.id, tableId) },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Print, "Print Guest Bill")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Print Guest Bill Preview", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Apply Manager Discount Action Button
                            OutlinedButton(
                                onClick = {
                                    discountValueInput = ""
                                    discountReasonInput = ""
                                    managerPinInput = ""
                                    showDiscountDialog = true
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("apply_manager_discount_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Percent, "Discount Icon")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply Manager Discount", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onNavigateToSettlement(tableId, bill!!.id, bill!!.grandTotal) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("bill_settle_cta_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Payments, "Complete flow")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Navigate to Settle Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (runningOrder != null) {
                        // Display active items and the option to generate the bill
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("Pending Order Items:", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                            LazyColumn(
                                modifier = Modifier.weight(1.0f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(runningOrder!!.items) { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${item.name} (x${item.quantity})", fontSize = 14.sp)
                                            Text(String.format("$%.2f", item.total), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Subtotal", fontSize = 14.sp)
                                        Text(String.format("$%.2f", runningOrder!!.totalAmount), fontSize = 14.sp)
                                    }
                                    Text("Tax & Service charges calculated upon finalized invoice creation.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            Button(
                                onClick = { viewModel.generateBill(tableId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("generate_bill_primary_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Finalize Invoice & Lock Bill", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Apply Discount Alert Dialog
        if (showDiscountDialog) {
            AlertDialog(
                onDismissRequest = { showDiscountDialog = false },
                title = { Text("Apply Manager Discount", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Discount Type & Authentication", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = discountIsPercent, onClick = { discountIsPercent = true })
                                Text("Percentage (%)", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = !discountIsPercent, onClick = { discountIsPercent = false })
                                Text("Fixed Cash ($)", fontSize = 13.sp)
                            }
                        }

                        OutlinedTextField(
                            value = discountValueInput,
                            onValueChange = { discountValueInput = it },
                            label = { Text(if (discountIsPercent) "Discount Percentage (%)" else "Discount Cash Value ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("discount_value_field")
                        )

                        OutlinedTextField(
                            value = discountReasonInput,
                            onValueChange = { discountReasonInput = it },
                            label = { Text("Reason for Discount (Mandatory)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("discount_reason_field")
                        )

                        OutlinedTextField(
                            value = managerPinInput,
                            onValueChange = { managerPinInput = it },
                            label = { Text("Manager Passcode Token PIN") },
                            placeholder = { Text("Enter 4-digit PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("manager_pin_field")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = discountValueInput.toDoubleOrNull() ?: 0.0
                            if (amt <= 0.0) {
                                Toast.makeText(context, "Please enter a positive non-zero value", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (discountReasonInput.trim().isEmpty()) {
                                Toast.makeText(context, "Discount reason is mandatory!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (managerPinInput.trim().isEmpty()) {
                                Toast.makeText(context, "Manager Approval PIN is required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.applyDiscount(
                                billId = bill!!.id,
                                tableId = tableId,
                                isPercent = discountIsPercent,
                                amount = amt,
                                reason = discountReasonInput,
                                managerPin = managerPinInput
                            ) { result ->
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Manager Authorized Discount Applied!", Toast.LENGTH_LONG).show()
                                    showDiscountDialog = false
                                } else {
                                    Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Apply & Lock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscountDialog = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}
