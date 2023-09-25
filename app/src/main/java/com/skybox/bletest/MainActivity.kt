package com.skybox.bletest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.skybox.bletest.ui.screens.ScannerScreen
import com.skybox.bletest.ui.theme.BleTestTheme

class MainActivity : ComponentActivity() {


    private val bluetoothStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
            }
        }
    }

    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            perms.entries.forEach { permission ->
                Log.d(TAG, "${permission.key} = ${permission.value}")
            }
        }


    private var isBluetoothEnabled = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        isBluetoothEnabled.value = bluetoothManager.adapter.isEnabled

        setContent {
            BleTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val enabled by remember { isBluetoothEnabled }
                    val navController = rememberNavController()

                    if (enabled.not()) {
                        Button(onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    this, Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            }
                        }) {
                            Text(text = "Enable Bluetooth")
                        }
                    } else {
                        NavHost(navController = navController, startDestination = "/") {
                            composable("/") { ScannerScreen() }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        registerReceiver(
            bluetoothStateBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    override fun onResume() {
        super.onResume()

        requestBluetoothPermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        )
    }

    override fun onStop() {
        super.onStop()

        unregisterReceiver(bluetoothStateBroadcastReceiver)
    }


    private companion object {
        private const val TAG = "MainActivity"
    }
}