package com.example.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.domain.model.Bill
import com.example.domain.model.KOT
import com.example.domain.model.OrderItem
import com.example.domain.model.RestaurantSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class PrinterType {
    BLUETOOTH, WIFI
}

enum class PaperWidth {
    W58MM, W80MM
}

data class PrinterSettings(
    val type: PrinterType = PrinterType.WIFI,
    val nameOrMac: String = "",
    val ip: String = "192.168.1.100",
    val port: Int = 9100,
    val paperWidth: PaperWidth = PaperWidth.W80MM,
    val autoPrintKot: Boolean = false,
    val autoPrintBill: Boolean = false,

    // Billing Printer
    val billingType: PrinterType = PrinterType.WIFI,
    val billingNameOrMac: String = "",
    val billingIp: String = "192.168.1.101",
    val billingPort: Int = 9100,

    // Backup / Failover Printer
    val failoverType: PrinterType = PrinterType.WIFI,
    val failoverNameOrMac: String = "",
    val failoverIp: String = "192.168.1.105",
    val failoverPort: Int = 9100,
    val enableFailover: Boolean = true,

    // Multiple Kitchen Printer Mapping IPs (Port is assumed 9100 or same)
    val drinksPrinterIp: String = "192.168.1.102",
    val dessertPrinterIp: String = "192.168.1.103",
    val mainsPrinterIp: String = "192.168.1.104"
)

class EpsonPrinterManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)

    fun getSettings(): PrinterSettings {
        val typeStr = sharedPrefs.getString("type", PrinterType.WIFI.name) ?: PrinterType.WIFI.name
        val nameOrMac = sharedPrefs.getString("name_or_mac", "") ?: ""
        val ip = sharedPrefs.getString("ip", "192.168.1.100") ?: "192.168.1.100"
        val port = sharedPrefs.getInt("port", 9100)
        val paperWidthStr = sharedPrefs.getString("paper_width", PaperWidth.W80MM.name) ?: PaperWidth.W80MM.name
        val autoPrintKot = sharedPrefs.getBoolean("auto_print_kot", false)
        val autoPrintBill = sharedPrefs.getBoolean("auto_print_bill", false)

        val billingTypeStr = sharedPrefs.getString("billing_type", PrinterType.WIFI.name) ?: PrinterType.WIFI.name
        val billingNameOrMac = sharedPrefs.getString("billing_name_or_mac", "") ?: ""
        val billingIp = sharedPrefs.getString("billing_ip", "192.168.1.101") ?: "192.168.1.101"
        val billingPort = sharedPrefs.getInt("billing_port", 9100)

        val failoverTypeStr = sharedPrefs.getString("failover_type", PrinterType.WIFI.name) ?: PrinterType.WIFI.name
        val failoverNameOrMac = sharedPrefs.getString("failover_name_or_mac", "") ?: ""
        val failoverIp = sharedPrefs.getString("failover_ip", "192.168.1.105") ?: "192.168.1.105"
        val failoverPort = sharedPrefs.getInt("failover_port", 9100)
        val enableFailover = sharedPrefs.getBoolean("enable_failover", true)

        val drinksPrinterIp = sharedPrefs.getString("drinks_printer_ip", "192.168.1.102") ?: "192.168.1.102"
        val dessertPrinterIp = sharedPrefs.getString("dessert_printer_ip", "192.168.1.103") ?: "192.168.1.103"
        val mainsPrinterIp = sharedPrefs.getString("mains_printer_ip", "192.168.1.104") ?: "192.168.1.104"

        return PrinterSettings(
            type = PrinterType.valueOf(typeStr),
            nameOrMac = nameOrMac,
            ip = ip,
            port = port,
            paperWidth = PaperWidth.valueOf(paperWidthStr),
            autoPrintKot = autoPrintKot,
            autoPrintBill = autoPrintBill,
            billingType = PrinterType.valueOf(billingTypeStr),
            billingNameOrMac = billingNameOrMac,
            billingIp = billingIp,
            billingPort = billingPort,
            failoverType = PrinterType.valueOf(failoverTypeStr),
            failoverNameOrMac = failoverNameOrMac,
            failoverIp = failoverIp,
            failoverPort = failoverPort,
            enableFailover = enableFailover,
            drinksPrinterIp = drinksPrinterIp,
            dessertPrinterIp = dessertPrinterIp,
            mainsPrinterIp = mainsPrinterIp
        )
    }

    fun saveSettings(settings: PrinterSettings) {
        sharedPrefs.edit().apply {
            putString("type", settings.type.name)
            putString("name_or_mac", settings.nameOrMac)
            putString("ip", settings.ip)
            putInt("port", settings.port)
            putString("paper_width", settings.paperWidth.name)
            putBoolean("auto_print_kot", settings.autoPrintKot)
            putBoolean("auto_print_bill", settings.autoPrintBill)

            putString("billing_type", settings.billingType.name)
            putString("billing_name_or_mac", settings.billingNameOrMac)
            putString("billing_ip", settings.billingIp)
            putInt("billing_port", settings.billingPort)

            putString("failover_type", settings.failoverType.name)
            putString("failover_name_or_mac", settings.failoverNameOrMac)
            putString("failover_ip", settings.failoverIp)
            putInt("failover_port", settings.failoverPort)
            putBoolean("enable_failover", settings.enableFailover)

            putString("drinks_printer_ip", settings.drinksPrinterIp)
            putString("dessert_printer_ip", settings.dessertPrinterIp)
            putString("mains_printer_ip", settings.mainsPrinterIp)
            apply()
        }
    }

    suspend fun checkStatus(): Boolean = withContext(Dispatchers.IO) {
        val settings = getSettings()
        when (settings.type) {
            PrinterType.WIFI -> {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(settings.ip, settings.port), 1500)
                    socket.close()
                    true
                } catch (e: Exception) {
                    Log.e("EpsonPrinterManager", "WIFI printer ping check failed: ${e.message}")
                    false
                }
            }
            PrinterType.BLUETOOTH -> {
                try {
                    val target = settings.nameOrMac
                    if (target.isEmpty()) return@withContext false
                    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val adapter = bluetoothManager.adapter ?: return@withContext false
                    if (!adapter.isEnabled) return@withContext false
                    
                    val device = adapter.bondedDevices.find { it.name == target || it.address == target }
                        ?: return@withContext false
                    true
                } catch (e: Exception) {
                    Log.e("EpsonPrinterManager", "Bluetooth printer check failed: ${e.message}")
                    false
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printBytesToConfig(
        bytes: ByteArray,
        targetType: PrinterType,
        targetIp: String,
        targetPort: Int,
        targetNameOrMac: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var lastErr: Exception? = null
        for (attempt in 1..3) {
            try {
                when (targetType) {
                    PrinterType.WIFI -> {
                        var socket: Socket? = null
                        var outputStream: OutputStream? = null
                        try {
                            socket = Socket()
                            socket.connect(InetSocketAddress(targetIp, targetPort), 3000)
                            outputStream = socket.getOutputStream()
                            outputStream.write(bytes)
                            outputStream.flush()
                            return@withContext Result.success(Unit)
                        } finally {
                            try { outputStream?.close() } catch (ex: Exception) {}
                            try { socket?.close() } catch (ex: Exception) {}
                        }
                    }
                    PrinterType.BLUETOOTH -> {
                        if (targetNameOrMac.isEmpty()) {
                            return@withContext Result.failure(Exception("Bluetooth Printer not configured."))
                        }
                        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val adapter = bluetoothManager.adapter ?: return@withContext Result.failure(Exception("Bluetooth adapter not available."))
                        if (!adapter.isEnabled) {
                            return@withContext Result.failure(Exception("Bluetooth is disabled."))
                        }
                        val device = adapter.bondedDevices.find { it.name == targetNameOrMac || it.address == targetNameOrMac }
                            ?: return@withContext Result.failure(Exception("Paired Bluetooth device '$targetNameOrMac' not found."))

                        var socket: BluetoothSocket? = null
                        var outputStream: OutputStream? = null
                        try {
                            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                            socket = device.createRfcommSocketToServiceRecord(uuid)
                            socket.connect()
                            outputStream = socket.outputStream
                            outputStream.write(bytes)
                            outputStream.flush()
                            return@withContext Result.success(Unit)
                        } finally {
                            try { outputStream?.close() } catch (ex: Exception) {}
                            try { socket?.close() } catch (ex: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                lastErr = e
                Log.w("EpsonPrinterManager", "Printer connection attempt $attempt on $targetIp failed: ${e.message}. Retrying...")
                try { Thread.sleep(400) } catch (ex: Exception) {}
            }
        }
        Result.failure(Exception("Printer unreachable after 3 attempts. Error: ${lastErr?.localizedMessage}"))
    }

    suspend fun printBytesWithFailover(
        bytes: ByteArray,
        primaryType: PrinterType,
        primaryIp: String,
        primaryPort: Int,
        primaryNameOrMac: String
    ): Result<Unit> {
        val primaryResult = printBytesToConfig(bytes, primaryType, primaryIp, primaryPort, primaryNameOrMac)
        if (primaryResult.isSuccess) {
            return primaryResult
        }
        val settings = getSettings()
        if (settings.enableFailover) {
            Log.w("EpsonPrinterManager", "Primary printer offline. Auto Failover to Backup Printer...")
            return printBytesToConfig(
                bytes,
                settings.failoverType,
                settings.failoverIp,
                settings.failoverPort,
                settings.failoverNameOrMac
            )
        }
        return primaryResult
    }

    @SuppressLint("MissingPermission")
    suspend fun printBytes(bytes: ByteArray): Result<Unit> {
        val settings = getSettings()
        return printBytesWithFailover(bytes, settings.type, settings.ip, settings.port, settings.nameOrMac)
    }

    // Composing ESC/POS print jobs
    fun generateTestPageBytes(width: PaperWidth): ByteArray {
        val stream = ByteArrayOutputStream()
        val cols = if (width == PaperWidth.W58MM) 32 else 48
        
        // POS commands
        val init = byteArrayOf(0x1B, 0x40) // ESC @
        val alignCenter = byteArrayOf(0x1B, 0x61, 0x01)
        val alignLeft = byteArrayOf(0x1B, 0x61, 0x00)
        val fontBoldOn = byteArrayOf(0x1B, 0x45, 0x01)
        val fontBoldOff = byteArrayOf(0x1B, 0x45, 0x00)
        val doubleSize = byteArrayOf(0x1D, 0x21, 0x11)
        val normalSize = byteArrayOf(0x1D, 0x21, 0x00)
        val paperCut = byteArrayOf(0x1D, 0x56, 0x41, 0x00) // Cut command

        stream.write(init)
        
        // Header
        stream.write(alignCenter)
        stream.write(doubleSize)
        stream.write(fontBoldOn)
        stream.write("EPSON THERMAL\n".toByteArray())
        stream.write("TEST OK\n".toByteArray())
        stream.write(normalSize)
        stream.write(fontBoldOff)
        
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        stream.write(alignLeft)
        stream.write("Device: Android Captain Terminal\n".toByteArray())
        stream.write("Paper Width: ${if (width == PaperWidth.W58MM) "58mm" else "80mm"} ($cols cols)\n".toByteArray())
        val dateFormatter = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        stream.write("Time: ${dateFormatter.format(Date())}\n".toByteArray())
        
        stream.write(alignCenter)
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        stream.write("Print Engine fully operational.\nReady for orders & billing.\n\n\n\n\n".toByteArray())
        
        stream.write(paperCut)
        return stream.toByteArray()
    }

    fun generateKOTBytes(
        kot: KOT,
        width: PaperWidth,
        reprint: Boolean = false,
        reprintCount: Int = 0,
        reprintReason: String? = null,
        kotSeqNumber: String? = null
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        val cols = if (width == PaperWidth.W58MM) 32 else 48

        val init = byteArrayOf(0x1B, 0x40)
        val alignCenter = byteArrayOf(0x1B, 0x61, 0x01)
        val alignLeft = byteArrayOf(0x1B, 0x61, 0x00)
        val fontBoldOn = byteArrayOf(0x1B, 0x45, 0x01)
        val fontBoldOff = byteArrayOf(0x1B, 0x45, 0x00)
        val doubleSize = byteArrayOf(0x1D, 0x21, 0x11)
        val normalSize = byteArrayOf(0x1D, 0x21, 0x00)
        val paperCut = byteArrayOf(0x1D, 0x56, 0x41, 0x00)

        stream.write(init)
        
        stream.write(alignCenter)
        stream.write(doubleSize)
        stream.write(fontBoldOn)
        if (reprint) {
            stream.write("KOT [REPRINT]\n".toByteArray())
        } else {
            stream.write("KITCHEN ORDER\n".toByteArray())
        }
        stream.write(normalSize)
        stream.write(fontBoldOff)
        
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        stream.write(alignLeft)
        stream.write(fontBoldOn)
        stream.write("TABLE: ${kot.tableName.uppercase()}\n".toByteArray())
        stream.write(fontBoldOff)
        
        val dateStr = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(kot.createdAt))
        if (!kotSeqNumber.isNullOrEmpty()) {
            stream.write("KOT Number: $kotSeqNumber\n".toByteArray())
        }
        stream.write("KOT Ref: ${kot.id}\n".toByteArray())
        stream.write("Time: $dateStr\n".toByteArray())
        stream.write("Status: ${kot.status.uppercase()}\n".toByteArray())
        
        if (reprint) {
            stream.write("Reprint Count: $reprintCount\n".toByteArray())
            if (!reprintReason.isNullOrEmpty()) {
                stream.write("Reprint Reason: $reprintReason\n".toByteArray())
            }
        }
        
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        // Item Table Header
        stream.write(fontBoldOn)
        stream.write(formatTwoColumns("QTY  ITEM", "", cols).toByteArray())
        stream.write("\n".toByteArray())
        stream.write(fontBoldOff)
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        for (item in kot.items) {
            val prefix = "${item.quantity} x "
            val maxItemNameLen = cols - prefix.length
            val name = if (item.name.length > maxItemNameLen) item.name.take(maxItemNameLen - 3) + "..." else item.name
            stream.write("$prefix$name\n".toByteArray())
        }
        
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        stream.write(alignCenter)
        stream.write("\n\n\n\n\n".toByteArray())
        stream.write(paperCut)
        
        return stream.toByteArray()
    }

    fun generateBillBytes(bill: Bill, items: List<OrderItem>, settings: RestaurantSettings?, width: PaperWidth): ByteArray {
        val stream = ByteArrayOutputStream()
        val cols = if (width == PaperWidth.W58MM) 32 else 48

        val init = byteArrayOf(0x1B, 0x40)
        val alignCenter = byteArrayOf(0x1B, 0x61, 0x01)
        val alignLeft = byteArrayOf(0x1B, 0x61, 0x00)
        val fontBoldOn = byteArrayOf(0x1B, 0x45, 0x01)
        val fontBoldOff = byteArrayOf(0x1B, 0x45, 0x00)
        val doubleSize = byteArrayOf(0x1D, 0x21, 0x11)
        val normalSize = byteArrayOf(0x1D, 0x21, 0x00)
        val paperCut = byteArrayOf(0x1D, 0x56, 0x41, 0x00)

        stream.write(init)
        
        // Header
        stream.write(alignCenter)
        
        // Render ASCII visual restaurant logo if name is present
        stream.write("      * * ✿ * *\n".toByteArray())
        stream.write(doubleSize)
        stream.write(fontBoldOn)
        val rName = settings?.restaurantName ?: "SPICE GARDEN"
        stream.write("${rName.uppercase()}\n".toByteArray())
        stream.write(normalSize)
        stream.write(fontBoldOff)
        
        // Business Address, Phone, GST & FSSAI Details
        val address = settings?.address ?: "123 Tech Park, Sector 5, Bangalore"
        val phone = settings?.phoneNumber ?: "+91 98765 43210"
        val gst = settings?.gstNumber ?: "27AAAPS1234A1Z5"
        val fssai = settings?.fssaiNumber ?: "12345678901234"
        
        stream.write("$address\n".toByteArray())
        stream.write("Ph: $phone\n".toByteArray())
        
        stream.write("GSTIN: $gst\n".toByteArray())
        stream.write("FSSAI: $fssai\n".toByteArray())
        
        stream.write("GUEST BILL PREVIEW\n".toByteArray())
        
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        stream.write(alignLeft)
        stream.write("Invoice No: ${bill.invoiceNumber}\n".toByteArray())
        stream.write("Table: ${bill.tableName}\n".toByteArray())
        val dateStr = SimpleDateFormat("dd MMM-yyyy HH:mm", Locale.getDefault()).format(Date(bill.createdAt))
        stream.write("Date: $dateStr\n".toByteArray())
        
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        // Items
        stream.write(fontBoldOn)
        stream.write(formatTwoColumns("QTY  ITEM", "PRICE", cols).toByteArray())
        stream.write("\n".toByteArray())
        stream.write(fontBoldOff)
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        val currSym = settings?.currency ?: "INR"
        for (item in items) {
            val prefix = "${item.quantity} x "
            val priceStr = String.format(Locale.getDefault(), "%.2f", item.total)
            val maxNameLen = cols - prefix.length - priceStr.length - 1
            val name = if (item.name.length > maxNameLen) {
                item.name.take(maxNameLen - 3) + "..."
            } else {
                item.name
            }
            val paddedName = name + " ".repeat(maxOf(0, maxNameLen - name.length))
            stream.write("$prefix$paddedName $priceStr\n".toByteArray())
        }
        
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        // Calculations
        val taxLabel = "Tax (${settings?.taxPercentage ?: 5.0}%)"
        val serviceLabel = "Service Charge (${settings?.serviceChargePercentage ?: 10.0}%)"
        
        val appliedDiscount = if (bill.discountAmount > 0.0) {
            bill.discountAmount
        } else if (bill.discountPercent > 0.0) {
            (bill.totalAmount + bill.taxAmount + bill.serviceCharge) * (bill.discountPercent / 100.0)
        } else {
            0.0
        }
        val finalGrandTotal = maxOf(0.0, bill.grandTotal - appliedDiscount)

        stream.write(formatTwoColumns("Subtotal", String.format(Locale.getDefault(), "%.2f", bill.totalAmount), cols).toByteArray())
        stream.write("\n".toByteArray())
        stream.write(formatTwoColumns(taxLabel, String.format(Locale.getDefault(), "%.2f", bill.taxAmount), cols).toByteArray())
        stream.write("\n".toByteArray())
        stream.write(formatTwoColumns(serviceLabel, String.format(Locale.getDefault(), "%.2f", bill.serviceCharge), cols).toByteArray())
        stream.write("\n".toByteArray())
        
        if (appliedDiscount > 0.0) {
            val label = if (bill.discountPercent > 0.0) "Discount (${bill.discountPercent}%)" else "Discount"
            stream.write(formatTwoColumns(label, String.format(Locale.getDefault(), "-%.2f", appliedDiscount), cols).toByteArray())
            stream.write("\n".toByteArray())
            if (!bill.discountReason.isNullOrEmpty()) {
                stream.write("Reason: ${bill.discountReason}\n".toByteArray())
            }
        }

        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        stream.write(fontBoldOn)
        stream.write(formatTwoColumns("GRAND TOTAL ($currSym)", String.format(Locale.getDefault(), "%.2f", finalGrandTotal), cols).toByteArray())
        stream.write("\n".toByteArray())
        stream.write(fontBoldOff)
        
        stream.write(getSeparator(width).toByteArray())
        stream.write("\n".toByteArray())
        
        stream.write(alignCenter)
        val tyMsg = settings?.thankYouMessage ?: "Thank you for dining with us!"
        val footerMsg = settings?.footerMessage ?: "GST & Service Charges applied as per govt norms."
        stream.write("$tyMsg\n".toByteArray())
        stream.write("$footerMsg\n".toByteArray())
        stream.write("\n\n\n\n\n".toByteArray())
        stream.write(paperCut)
        
        return stream.toByteArray()
    }

    private fun getSeparator(width: PaperWidth): String {
        return if (width == PaperWidth.W58MM) {
            "--------------------------------"
        } else {
            "------------------------------------------------"
        }
    }

    private fun formatTwoColumns(left: String, right: String, totalCols: Int): String {
        val spacesNeeded = totalCols - left.length - right.length
        return if (spacesNeeded <= 0) {
            val truncatedLeft = left.take(maxOf(0, totalCols - right.length - 1))
            truncatedLeft + " " + right
        } else {
            left + " ".repeat(spacesNeeded) + right
        }
    }
}
