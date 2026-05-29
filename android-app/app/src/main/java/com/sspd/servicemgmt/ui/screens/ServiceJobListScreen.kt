package com.sspd.servicemgmt.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceJobListScreen(
    onBack:     () -> Unit,
    onJobClick: (Int) -> Unit = {},
    onNewJob:   () -> Unit    = {}
) {
    val vm: ServiceJobListViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) { vm.load(); delay(30_000) }
    }

    LaunchedEffect(state.deleteSuccess) {
        state.deleteSuccess?.let { snackbar.showSnackbar(it); vm.clearDeleteSuccess() }
    }

    // ── Date pickers ──────────────────────────────────────────────────────────
    if (showFromPicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = state.fromDate?.parseDateToMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { vm.setFromDate(it.formatMillisToDate()) }
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("ပယ်ဖျက်") }
            }
        ) { DatePicker(state = dpState) }
    }

    if (showToPicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = state.toDate?.parseDateToMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { vm.setToDate(it.formatMillisToDate()) }
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) { Text("ပယ်ဖျက်") }
            }
        ) { DatePicker(state = dpState) }
    }

    // ── Delete confirm dialog ─────────────────────────────────────────────────
    state.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!state.deleting) vm.cancelDelete() },
            icon  = { Icon(Icons.Outlined.DeleteForever, null, tint = Danger) },
            title = { Text("Job ဖျက်မည်", fontWeight = FontWeight.ExtraBold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Job \"${target.jobNo ?: "#${target.id}"}\" ကို ဖျက်မည်။ ဆက်လက်မည်လား?",
                        fontSize = 14.sp
                    )
                    state.deleteError?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 12.sp, color = Danger)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = { vm.delete() },
                    colors   = ButtonDefaults.buttonColors(containerColor = Danger),
                    enabled  = !state.deleting
                ) {
                    if (state.deleting)
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else
                        Text("ဖျက်မည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelDelete() }, enabled = !state.deleting) {
                    Text("မဖျက်ပါ")
                }
            }
        )
    }

    val filtered = if (state.filter == "ALL") state.items
                   else state.items.filter { it.status?.uppercase() == state.filter }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("ဝန်ဆောင်မှု Jobs", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewJob, containerColor = Primary) {
                Icon(Icons.Outlined.Add, null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(ScreenBg)) {

            // ── Date filter row ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.DateRange, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                OutlinedCard(
                    modifier = Modifier.weight(1f).clickable { showFromPicker = true },
                    shape    = RoundedCornerShape(8.dp),
                    border   = BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        text     = state.fromDate ?: "ရက်ရွေး",
                        fontSize = 12.sp,
                        color    = if (state.fromDate != null) TextMain else TextMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Text("—", fontSize = 12.sp, color = TextMuted)
                OutlinedCard(
                    modifier = Modifier.weight(1f).clickable { showToPicker = true },
                    shape    = RoundedCornerShape(8.dp),
                    border   = BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        text     = state.toDate ?: "ရက်ရွေး",
                        fontSize = 12.sp,
                        color    = if (state.toDate != null) TextMain else TextMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                if (state.fromDate != null || state.toDate != null) {
                    IconButton(
                        onClick  = { vm.clearDateFilter() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Outlined.Close, null, tint = Danger, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // ── Status filter chips ───────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL"         to "အားလုံး",
                    "PENDING"     to "စောင့်ဆိုင်း",
                    "IN_PROGRESS" to "လုပ်ဆဲ",
                    "COMPLETED"   to "ပြီး"
                ).forEach { (k, v) ->
                    FilterChip(
                        selected = state.filter == k,
                        onClick  = { vm.setFilter(k) },
                        label    = { Text(v, fontSize = 12.sp) }
                    )
                }
            }

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("ဒေတာမရှိပါ", color = TextMuted)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { job ->
                        Card(
                            shape    = RoundedCornerShape(12.dp),
                            colors   = CardDefaults.cardColors(containerColor = CardBg),
                            border   = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.clickable { job.id?.let { onJobClick(it) } }
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(job.jobNo ?: "-", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Violet)
                                        Text(job.customerName ?: "-", fontSize = 13.sp, color = TextMain)
                                    }
                                    JobStatusBadge(job.status)
                                    Spacer(Modifier.width(6.dp))
                                    IconButton(
                                        onClick  = { vm.confirmDelete(job) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.DeleteOutline, null, tint = Danger, modifier = Modifier.size(16.dp))
                                    }
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
                                    Text(
                                        "${String.format("%,.0f", job.netAmount ?: 0.0)} Ks",
                                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Primary
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
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
        Text(
            label,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            color      = color
        )
    }
}

private fun String.parseDateToMillis(): Long? = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .parse(this)?.time
} catch (_: Exception) { null }

private fun Long.formatMillisToDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(this))
