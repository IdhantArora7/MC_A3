package com.example.wapsscanner

import android.Manifest
import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val wifiService = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    var selectedLocation by mutableStateOf("Location 1")
        private set

    var scanResultsByLocation = mutableStateMapOf<String, MutableList<List<ScanResult>>>(
        "Location 1" to mutableListOf(),
        "Location 2" to mutableListOf(),
        "Location 3" to mutableListOf()
    )
        private set

    var scanningActive by mutableStateOf(false)
        private set

    var completedScans by mutableStateOf(0)
        private set

    val maxScansPerLocation = 100
    private val scanIntervalMillis = 500L

    private var wifiScanReceiver: BroadcastReceiver? = null
    private var scanCoroutine: Job? = null
    private val supervisorJob = SupervisorJob()
    private val mainScope = CoroutineScope(Dispatchers.Main + supervisorJob)

    fun changeLocation(location: String) {
        if (!scanningActive) {
            selectedLocation = location
            completedScans = scanResultsByLocation[selectedLocation]?.size ?: 0
        }
    }

    fun initiateScan(context: Context) {
        if (scanningActive) return

        if (!wifiService.isWifiEnabled) return

        if (!hasAllPermissions(context)) return

        scanningActive = true
        completedScans = 0
        scanResultsByLocation[selectedLocation]?.clear()
        setupReceiver(context)
        scheduleNextScan(context)
    }

    fun haltScan(context: Context) {
        if (!scanningActive) return
        scanningActive = false
        scanCoroutine?.cancel()
        removeReceiverSafely(context)
    }

    private fun scheduleNextScan(context: Context) {
        if (!scanningActive || completedScans >= maxScansPerLocation) {
            if (completedScans >= maxScansPerLocation) {
                haltScan(context)
            }
            return
        }

        if (!wifiService.isWifiEnabled) {
            haltScan(context)
            return
        }

        val scanStarted = wifiService.startScan()
        if (!scanStarted) {
            // If scan fails, try to use previous results
            try {
                @Suppress("MissingPermission")
                val previousResults = wifiService.scanResults
                if (previousResults.isNotEmpty()) {
                    val filtered = previousResults.filter { !it.SSID.isNullOrEmpty() }
                    scanResultsByLocation[selectedLocation]?.add(filtered)
                    completedScans = scanResultsByLocation[selectedLocation]?.size ?: 0
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
            scanCoroutine = mainScope.launch {
                delay(scanIntervalMillis + 200L)
                scheduleNextScan(context)
            }
        }
        // If scan started, results will be handled by receiver
    }

    private fun setupReceiver(context: Context) {
        if (wifiScanReceiver != null) return
        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                ctx ?: return
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == intent?.action) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true)
                    if (success) {
                        handleScanResults(ctx)
                    }
                    scanCoroutine?.cancel()
                    scanCoroutine = mainScope.launch {
                        delay(scanIntervalMillis)
                        scheduleNextScan(ctx)
                    }
                }
            }
        }
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(wifiScanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(wifiScanReceiver, filter)
            }
        } catch (_: SecurityException) {
            haltScan(context)
        } catch (_: Exception) {
            haltScan(context)
        }
    }

    private fun handleScanResults(context: Context) {
        if (!hasAllPermissions(context)) {
            haltScan(context)
            return
        }
        try {
            @Suppress("MissingPermission")
            val results = wifiService.scanResults
            if (results.isNotEmpty()) {
                val filtered = results.filter { !it.SSID.isNullOrEmpty() }
                scanResultsByLocation[selectedLocation]?.add(filtered)
                completedScans = scanResultsByLocation[selectedLocation]?.size ?: 0
            }
        } catch (se: SecurityException) {
            Log.d("WiFiScan", "SecurityException: ${se.message}")
            haltScan(context)
        } catch (e: Exception) {
            Log.d("WiFiScan", "Exception: ${e.message}")
        }
    }

    private fun removeReceiverSafely(context: Context) {
        if (wifiScanReceiver != null) {
            try {
                context.unregisterReceiver(wifiScanReceiver)
                wifiScanReceiver = null
            } catch (_: IllegalArgumentException) {
            } catch (e: Exception) {
                Log.d("WiFiScan", "Unregister error: ${e.message}")
            }
        }
    }

    private fun hasAllPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val nearbyDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val wifiState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val changeWifi = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CHANGE_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation && nearbyDevices && wifiState && changeWifi
    }

    override fun onCleared() {
        super.onCleared()
        removeReceiverSafely(getApplication())
        scanCoroutine?.cancel()
        supervisorJob.cancel()
    }
}

class AppViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
