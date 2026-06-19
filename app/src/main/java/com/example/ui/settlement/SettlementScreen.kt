package com.example.ui.settlement

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PaymentSettlement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    tableId: String,
    billId: String,
    grandTotal: Double,
    viewModel: SettlementViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTables: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    var paymentType by remember { mutableStateOf("CASH") } // "CASH", "CARD", "UPI", "MIXED"

    // Inputs & Payment Enhancements
    var cashAmountStr by remember { mutableStateOf("") }
    var cardAmountStr by remember { mutableStateOf("") }
    var upiAmountStr by remember { mutableStateOf("") }
    var isPartialPayment by remember { mutableStateOf(false) }
    var paymentReferenceNo by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Set initial values based on selection
    LaunchedEffect(paymentType) {
        when (paymentType) {
            "CASH" -> {
                cashAmountStr = String.format("%.2f", grandTotal)
                cardAmountStr = "0.0"
                upiAmountStr = "0.0"
            }
            "CARD" -> {
                cashAmountStr = "0.0"
                cardAmountStr = String.format("%.2f", grandTotal)
                upiAmountStr = "0.0"
            }
            "UPI" -> {
                cashAmountStr = "0.0"
                cardAmountStr = "0.0"
                upiAmountStr = String.format("%.2f", grandTotal)
            }
            "MIXED" -> {
                cashAmountStr = ""
                cardAmountStr = ""
                upiAmountStr = ""
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.settlementResult.collect { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Settle bill complete. Table released!", Toast.LENGTH_SHORT).show()
                onNavigateToTables()
            }
        }
    }

    // Dynamic calculations
    val cashVal = cashAmountStr.toDoubleOrNull() ?: 0.0
    val cardVal = cardAmountStr.toDoubleOrNull() ?: 0.0
    val upiVal = upiAmountStr.toDoubleOrNull() ?: 0.0
    val totalPaid = cashVal + cardVal + upiVal
    val balanceRemaining = (grandTotal - totalPaid).coerceAtLeast(0.0)
    val extraChange = (totalPaid - grandTotal).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settlement - Bill $billId") },
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
        modifier = modifier.testTag("settlement_screen_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMsg != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }

            // Receipt Block
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Amount Outstanding", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format("Table $tableId - Bill: $billId"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(String.format("$%.2f", grandTotal), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Payment Types Horizontal tab row
            ScrollableTabRow(
                selectedTabIndex = when (paymentType) {
                    "CASH" -> 0
                    "CARD" -> 1
                    "UPI" -> 2
                    else -> 3
                },
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth().testTag("payment_types_tabs")
            ) {
                Tab(selected = paymentType == "CASH", onClick = { paymentType = "CASH" }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Money, "Cash logo", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cash")
                    }
                }
                Tab(selected = paymentType == "CARD", onClick = { paymentType = "CARD" }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, "Card logo", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Card")
                    }
                }
                Tab(selected = paymentType == "UPI", onClick = { paymentType = "UPI" }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, "UPI QR logo", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("UPI QR")
                    }
                }
                Tab(selected = paymentType == "MIXED", onClick = { paymentType = "MIXED" }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, "Mixed logo", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mixed")
                    }
                }
            }

            // Inputs Based on Selection
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (paymentType == "CASH" || paymentType == "MIXED") {
                        OutlinedTextField(
                            value = cashAmountStr,
                            onValueChange = { cashAmountStr = it },
                            label = { Text("Cash Tendered ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("cash_tendered_input")
                        )
                    }

                    if (paymentType == "CARD" || paymentType == "MIXED") {
                        OutlinedTextField(
                            value = cardAmountStr,
                            onValueChange = { cardAmountStr = it },
                            label = { Text("Card Charged Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("card_charged_input")
                        )
                    }

                    if (paymentType == "UPI" || paymentType == "MIXED") {
                        OutlinedTextField(
                            value = upiAmountStr,
                            onValueChange = { upiAmountStr = it },
                            label = { Text("UPI Trx Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("upi_payment_input")
                        )

                        if (paymentType == "UPI") {
                            // Present dynamic gateway settlement instructions
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.QrCode, "QR Code", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text("Dynamic Terminal QR Active", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Payment Reference Number (Mandatory for Card & UPI modes)
                    if (paymentType == "CARD" || paymentType == "UPI" || paymentType == "MIXED") {
                        OutlinedTextField(
                            value = paymentReferenceNo,
                            onValueChange = { paymentReferenceNo = it },
                            label = { Text("Payment Reference/Approval RRN *") },
                            placeholder = { Text("e.g. UPI trans ID or Card txn code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("payment_reference_input")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Partial Payment Option Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Split/Partial Payment Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Allows collecting a split portion outstanding", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = isPartialPayment,
                            onCheckedChange = { isPartialPayment = it },
                            modifier = Modifier.testTag("partial_payment_switch")
                        )
                    }

                    // Bottom info metrics
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Entered Total Paid:", fontSize = 14.sp)
                        Text(String.format("$%.2f", totalPaid), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    if (balanceRemaining > 0.01) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pending Balance Left:", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                            Text(String.format("$%.2f", balanceRemaining), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (extraChange > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Refunding Change Cash:", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Text(String.format("$%.2f", extraChange), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Validation checks
            val isReferenceNoFilled = if (paymentType == "CARD" || paymentType == "UPI" || paymentType == "MIXED") {
                paymentReferenceNo.trim().isNotEmpty()
            } else true

            val canSubmitSettle = !isLoading && isReferenceNoFilled && (
                if (isPartialPayment) totalPaid > 0.1 else totalPaid >= (grandTotal - 0.05)
            )

            // Finish Settlement CTA
            Button(
                onClick = {
                    if (paymentType != "CASH" && paymentReferenceNo.trim().isEmpty()) {
                        Toast.makeText(context, "Payment reference is mandatory for non-cash settlement!", Toast.LENGTH_SHORT).show()
                    } else if (isPartialPayment && totalPaid > grandTotal) {
                        Toast.makeText(context, "Partial charge cannot exceed grand total!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.settleBill(billId, cashVal, cardVal, upiVal, paymentType)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settle_payment_action_btn"),
                enabled = canSubmitSettle,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    val label = if (isPartialPayment) "Confirm Split Partial Payment" else "Complete Full Settlement & Free Table"
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
