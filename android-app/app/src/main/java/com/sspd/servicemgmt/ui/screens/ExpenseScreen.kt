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
import com.sspd.servicemgmt.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(onBack: () -> Unit) {
    val vm: ExpenseViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (true) { vm.load(); delay(30_000) }
    }

    val total = state.items.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ကုန်ကျစရိတ်များ", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(ScreenBg)) {
            // Total card
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.5.dp, Danger)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("စုစုပေါင်း ကုန်ကျစရိတ်", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text("${total.fmt()} Ks", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Danger)
                }
            }

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
            } else if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ကုန်ကျစရိတ် မရှိပါ", color = TextMuted) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.items) { e ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.accountName ?: "-", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextMain)
                                    if (!e.description.isNullOrBlank()) {
                                        Text(e.description, fontSize = 11.sp, color = TextMuted, maxLines = 1)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Outlined.CalendarToday, null, tint = TextMuted, modifier = Modifier.size(11.dp))
                                        Text(e.expenseDate?.take(10) ?: "-", fontSize = 10.sp, color = TextMuted)
                                        if (!e.staffName.isNullOrBlank()) {
                                            Text("• ${e.staffName}", fontSize = 10.sp, color = TextMuted)
                                        }
                                    }
                                }
                                Text("${e.amount.fmt()} Ks", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Danger)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

private fun Long.fmt() = String.format("%,d", this)
