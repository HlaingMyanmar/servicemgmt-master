package com.sspd.servicemgmt.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sspd.servicemgmt.ui.theme.*
import com.sspd.servicemgmt.ui.viewmodel.ServiceJobListViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceJobListScreen(onBack: () -> Unit) {
    val vm: ServiceJobListViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (true) { vm.load(); delay(30_000) }
    }

    val filtered = if (state.filter == "ALL") state.items
                   else state.items.filter { it.status?.uppercase() == state.filter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ဝန်ဆောင်မှု Jobs", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(ScreenBg)) {
            // Filter chips
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "အားလုံး", "PENDING" to "စောင့်ဆိုင်း", "IN_PROGRESS" to "လုပ်ဆဲ", "COMPLETED" to "ပြီး").forEach { (k, v) ->
                    FilterChip(selected = state.filter == k, onClick = { vm.setFilter(k) }, label = { Text(v, fontSize = 12.sp) })
                }
            }
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ဒေတာမရှိပါ", color = TextMuted) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { job ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(job.jobNo ?: "-", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Violet)
                                        Text(job.customerName ?: "-", fontSize = 13.sp, color = TextMain)
                                    }
                                    JobStatusBadge(job.status)
                                }
                                Spacer(Modifier.height(6.dp))
                                if (!job.itemName.isNullOrBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Outlined.Devices, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                        Text(job.itemName, fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                                if (!job.problemDesc.isNullOrBlank()) {
                                    Text(job.problemDesc, fontSize = 11.sp, color = TextMuted, maxLines = 1)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Outlined.Person, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                        Text(job.assignedStaffName ?: "-", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Text("${(job.netAmount ?: 0).fmt()} Ks", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun JobStatusBadge(status: String?) {
    val (bg, color, label) = when (status?.uppercase()) {
        "COMPLETED"   -> Triple(SuccessBg, Success, "ပြီးဆုံး")
        "IN_PROGRESS" -> Triple(VioletBg,  Violet,  "လုပ်ဆဲ")
        "PENDING"     -> Triple(WarningBg, Warning, "စောင့်ဆိုင်း")
        "CANCELLED"   -> Triple(DangerBg,  Danger,  "ပယ်ဖျက်")
        else          -> Triple(BorderColor, TextMuted, status ?: "-")
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun Long.fmt() = String.format("%,d", this)
