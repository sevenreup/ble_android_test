package com.skybox.bletest.ui.screens

import android.annotation.SuppressLint
import androidx.bluetooth.ScanResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val devices by remember { viewModel.scanResults }
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
            Modifier
                .padding(innerPadding)
                .fillMaxWidth()) {
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
            items(devices.size) { index ->
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
        onClick = {
            onClick(result)
        },
        modifier = Modifier.fillMaxWidth()
    ){
        Text(text = result.device.name ?: "Unkonwn")
    }

}