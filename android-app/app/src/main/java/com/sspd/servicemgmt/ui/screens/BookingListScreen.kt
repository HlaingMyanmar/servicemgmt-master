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
import com.sspd.servicemgmt.ui.viewmodel.BookingListViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingListScreen(onBack: () -> Unit) {
    val vm: BookingListViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (true) { vm.load(); delay(30_000) }
    }

    val filtered = state.items.filter {
        state.search.isBlank() ||
        (it.invoiceNo?.contains(state.search, true) == true) ||
        (it.customerName?.contains(state.search, true) == true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking များ", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(ScreenBg)) {
            OutlinedTextField(
                value = state.search,
                onValueChange = { vm.setSearch(it) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Booking ရှာဖွေရန်...") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ဒေတာမရှိပါ", color = TextMuted) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { b ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(b.invoiceNo ?: "-", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
                                        Text(b.customerName ?: "-", fontSize = 13.sp, color = TextMain)
                                    }
                                    BookingStatusBadge(b.status)
                                }
                                Spacer(Modifier.height(6.dp))
                                if (!b.brand.isNullOrBlank() || !b.model.isNullOrBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Outlined.Smartphone, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                        Text("${b.brand ?: ""} ${b.model ?: ""}".trim(), fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Outlined.CalendarToday, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                        Text(b.bookingDate?.take(10) ?: "-", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Text("${(b.totalAmount ?: 0).fmt()} Ks", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
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
private fun BookingStatusBadge(status: String?) {
    val (bg, color, label) = when (status?.uppercase()) {
        "PENDING"     -> Triple(WarningBg, Warning, "စောင့်ဆိုင်း")
        "IN_PROGRESS" -> Triple(VioletBg, Violet, "လုပ်ဆဲ")
        "COMPLETED"   -> Triple(SuccessBg, Success, "ပြီးဆုံး")
        "DELIVERED"   -> Triple(PrimaryLight, Primary, "ပြန်ပေး")
        "CANCELLED"   -> Triple(DangerBg, Danger, "ပယ်ဖျက်")
        else          -> Triple(BorderColor, TextMuted, status ?: "-")
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun Long.fmt() = String.format("%,d", this)
