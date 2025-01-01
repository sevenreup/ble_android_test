package com.skybox.bletest.ui.screens

import android.annotation.SuppressLint
import androidx.bluetooth.ScanResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {

    val context = LocalContext.current
    val devices by viewModel.scanResults.collectAsState(initial = listOf())
    val isScanning by remember {
        viewModel.isScanning
    }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(key1 = viewModel, block = {
        viewModel.setup(context)
        viewModel.startScan()
    })

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            Box() {
                Text("Scaffold Content")
            }
        }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(10.dp)
        ) {
            stickyHeader {
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp), onClick = {
                    viewModel.startScan()
                }) {
                    Column (
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp)){
                        if (isScanning)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(text = if(isScanning)  "Scanning" else "Devices")
                    }
                }
            }
            items(count = devices.size, key = {
                devices[it].deviceAddress.address
            }) { index ->
                val item = devices[index]

                DeviceCard(result = item) {
                   val index = viewModel.addDeviceConnectionIfNew(it.device)

                    scope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }
                }
            }
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun DeviceCard(result: ScanResult, onClick: (result: ScanResult) -> Unit) {
    Card (
        modifier = Modifier.fillMaxWidth()
    ){
        Column(Modifier.padding(8.dp)) {
            Text(text = result.deviceAddress.address)
            Text(text = result.device.name ?: "Unkonwn")
            Button(onClick = { onClick(result) }) {
                Text(text = "Connect")
            }
        }
    }

}