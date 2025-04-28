package com.example.wapsscanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

// Utility: Calculate average RSSI
fun calculateAverageRssi(results: List<android.net.wifi.ScanResult>): Int {
    val validLevels = results.map { it.level }.filter { it != Int.MIN_VALUE }
    return if (validLevels.isNotEmpty()) validLevels.average().roundToInt() else 0
}

// Utility: Format RSSI range as string
fun rssiRangeString(results: List<android.net.wifi.ScanResult>): String {
    val validLevels = results.map { it.level }.filter { it != Int.MIN_VALUE }
    return when {
        validLevels.size >= 2 -> {
            val min = validLevels.minOrNull() ?: 0
            val max = validLevels.maxOrNull() ?: 0
            "$min to $max dBm (${max - min} diff)"
        }
        validLevels.size == 1 -> "${validLevels.first()} dBm"
        else -> "N/A"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiAnalyzerScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val locationList = listOf("Location 1", "Location 2", "Location 3")
    var locationMenuExpanded by remember { mutableStateOf(false) }
    var showLogSection by remember { mutableStateOf(false) }
    var logLocation by remember { mutableStateOf(locationList.first()) }

    val currentLocation by remember { derivedStateOf { viewModel.selectedLocation } }
    val isScanning by remember { derivedStateOf { viewModel.scanningActive } }
    val scanCount by remember { derivedStateOf { viewModel.completedScans } }
    val scanLimit = viewModel.maxScansPerLocation
    val scanResultsHere = viewModel.scanResultsByLocation[currentLocation] ?: emptyList()
    val allScanLogs = viewModel.scanResultsByLocation

    // Permissions array for runtime request
    val permissionsRequired = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.all { it.value }) {
            viewModel.initiateScan(context)
        }
    }

    // Helper: Start scan with permission check
    fun startScanWithPermissions(context: Context) {
        val notGranted = permissionsRequired.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            viewModel.initiateScan(context)
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WAPsScanner", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Location selector
            ExposedDropdownMenuBox(
                expanded = locationMenuExpanded,
                onExpandedChange = { if (!isScanning) locationMenuExpanded = !locationMenuExpanded }
            ) {
                OutlinedTextField(
                    value = currentLocation,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Location") },
                    trailingIcon = {
                        Icon(
                            imageVector = if (locationMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown Arrow"
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    enabled = !isScanning,
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = locationMenuExpanded,
                    onDismissRequest = { locationMenuExpanded = false }
                ) {
                    locationList.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location) },
                            onClick = {
                                viewModel.changeLocation(location)
                                locationMenuExpanded = false
                            },
                            enabled = !isScanning
                        )
                    }
                }
            }


            Spacer(Modifier.height(14.dp))

            // Scan controls
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { startScanWithPermissions(context) },
                    enabled = !isScanning,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text("Start Scan", color = Color.Black)
                }
                Button(
                    onClick = { viewModel.haltScan(context) },
                    enabled = isScanning,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Stop Scan", color = Color.Black)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Toggle scan log section
            Button(
                onClick = { showLogSection = !showLogSection },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) {
                Icon(
                    imageVector = if (showLogSection) Icons.Filled.Close else Icons.Filled.List,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(if (showLogSection) "Hide Scan Log" else "Show Scan Log", color = Color.White)
            }

            Spacer(Modifier.height(16.dp))

            // Scan progress and summary
            Text(
                "Scans: $scanCount / $scanLimit for $currentLocation",
                style = MaterialTheme.typography.bodyMedium
            )
            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            Text("Average RSSI: ${calculateAverageRssi(scanResultsHere.flatten())} dBm", style = MaterialTheme.typography.titleMedium)
            Text("RSSI Range: ${rssiRangeString(scanResultsHere.flatten())}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            // List unique APs for current location
            if (scanResultsHere.isEmpty()) {
                Text("No scan data for $currentLocation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    val groupedAps = scanResultsHere.flatten().groupBy { it.BSSID }
                    itemsIndexed(groupedAps.entries.toList()) { idx, (bssid, apScans) ->
                        val ssid = apScans.firstOrNull()?.SSID ?: "Unknown SSID"
                        val avgRssi = apScans.map { it.level }.average().roundToInt()
                        val minRssi = apScans.minOfOrNull { it.level } ?: 0
                        val maxRssi = apScans.maxOfOrNull { it.level } ?: 0
                        Card(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text("AP ${idx + 1}: $ssid", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("BSSID: $bssid", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Avg RSSI: $avgRssi dBm", fontSize = 13.sp)
                                Text("Range: $minRssi to $maxRssi dBm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Scan log/history section
            if (showLogSection) {
                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("Scan Log by Location", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                // Location selector
                var logMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = logMenuExpanded,
                    onExpandedChange = { logMenuExpanded = !logMenuExpanded }
                ) {
                    TextField(
                        value = logLocation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Log Location") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(logMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = logMenuExpanded,
                        onDismissRequest = { logMenuExpanded = false }
                    ) {
                        locationList.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    logLocation = loc
                                    logMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                val logScans = allScanLogs[logLocation] ?: emptyList()

                if (logScans.isEmpty()) {
                    Text("No scan logs for $logLocation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    // 🆕 Start of the unified LazyColumn (Top element = stats, following = scans)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        // 🆕 First item: Location Stats
                        item {
                            val flattenedAps = logScans.flatten()
                            val totalAps = flattenedAps.size
                            val avgRssiAll = calculateAverageRssi(flattenedAps)
                            val rssiRangeAll = rssiRangeString(flattenedAps)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Location Stats: $logLocation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Total APs Found: $totalAps", style = MaterialTheme.typography.bodyMedium)
                                    Text("Average RSSI: $avgRssiAll dBm", style = MaterialTheme.typography.bodyMedium)
                                    Text("RSSI Range: $rssiRangeAll", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        // 🆕 Following items: List each scan individually
                        itemsIndexed(logScans) { scanIdx, scanList ->
                            Card(
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    val avgRssi = calculateAverageRssi(scanList)
                                    val rssiRange = rssiRangeString(scanList)
                                    Text(
                                        "Scan ${scanIdx + 1} ($logLocation)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text("APs Found: ${scanList.size}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Avg RSSI: $avgRssi dBm", style = MaterialTheme.typography.bodyMedium)
                                    Text("RSSI Range: $rssiRange", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Divider()
                                    Spacer(Modifier.height(8.dp))
                                    if (scanList.isEmpty()) {
                                        Text("(No APs in this scan)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        scanList.forEach { ap ->
                                            Column(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                                                Text(
                                                    ap.SSID,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "RSSI: ${ap.level} dBm | BSSID: ${ap.BSSID}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
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
}