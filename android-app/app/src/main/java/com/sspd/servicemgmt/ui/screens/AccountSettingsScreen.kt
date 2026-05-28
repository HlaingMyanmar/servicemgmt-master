package com.sspd.servicemgmt.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sspd.servicemgmt.ui.theme.*
import com.sspd.servicemgmt.ui.viewmodel.AccountSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(onBack: () -> Unit) {
    val vm: AccountSettingsViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("အကောင့်သတ်မှတ်ချက်", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(ScreenBg)
                .verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile card
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (state.username.firstOrNull()?.uppercaseChar() ?: 'U').toString(),
                            fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(state.displayName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(state.username, fontSize = 13.sp, color = TextMuted)
                }
            }

            // Server settings
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Server IP", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextMain)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.serverInput,
                        onValueChange = { vm.setServerInput(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("https://192.168.x.x:8080") },
                        leadingIcon = { Icon(Icons.Outlined.Wifi, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { vm.saveServerUrl() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Server IP သိမ်းရန်", fontWeight = FontWeight.Bold)
                    }
                    if (state.saved) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = Success, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("သိမ်းပြီးပါပြီ", fontSize = 12.sp, color = Success, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Note about password change
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PrimaryLight), border = BorderStroke(1.dp, Primary.copy(0.3f))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Text("စကားဝှက်ပြောင်းလဲရန် admin ထံ ဆက်သွယ်ပါ သို့မဟုတ် web system ကိုသုံးပါ", fontSize = 12.sp, color = TextMain, lineHeight = 18.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
