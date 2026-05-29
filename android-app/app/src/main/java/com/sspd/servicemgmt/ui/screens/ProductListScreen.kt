package com.sspd.servicemgmt.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sspd.servicemgmt.api.ProductDTO
import com.sspd.servicemgmt.ui.theme.*
import com.sspd.servicemgmt.ui.viewmodel.ProductListViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(onBack: () -> Unit, onProductClick: (Int) -> Unit = {}) {
    val vm: ProductListViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        while (true) { vm.load(); delay(30_000) }
    }

    LaunchedEffect(state.scanError) {
        state.scanError?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearScanError()
        }
    }

    val filtered = when {
        state.scannedProductId != null ->
            state.items.filter { it.id == state.scannedProductId }
        state.search.isBlank() ->
            state.items
        else ->
            state.items.filter {
                it.name.contains(state.search, true) ||
                it.productCode.contains(state.search, true)
            }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("ကုန်ပစ္စည်းများ", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Primary, titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).background(ScreenBg)) {

                // Search field with scan button
                OutlinedTextField(
                    value = state.search,
                    onValueChange = { vm.setSearch(it) },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    placeholder = { Text("ကုန်ပစ္စည်း ရှာဖွေရန်...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.scanLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp, color = Primary
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                IconButton(onClick = { vm.showScanner() }) {
                                    Icon(Icons.Outlined.QrCodeScanner, "Scan", tint = Primary)
                                }
                            }
                            if (state.search.isNotBlank()) {
                                IconButton(onClick = { vm.setSearch("") }) {
                                    Icon(Icons.Outlined.Clear, null, tint = TextMuted)
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Scanned product banner
                if (state.scannedProductId != null) {
                    Surface(
                        color = PrimaryLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.QrCodeScanner, null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Scan ဖြင့် ရှာဖွေနေသည်", fontSize = 12.sp, color = Primary, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.clearScanResult() }) {
                                Text("ရှင်းမည်", fontSize = 12.sp, color = Primary)
                            }
                        }
                    }
                }

                if (state.loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.SearchOff, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("ကုန်ပစ္စည်း မတွေ့ပါ", color = TextMuted)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { p ->
                            ProductCard(p, onClick = { onProductClick(p.id) })
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }

        // Full-screen barcode scanner overlay
        if (state.showScanner) {
            BarcodeScannerView(
                onResult = { serial -> vm.onScanResult(serial) },
                onClose  = { vm.dismissScanner() }
            )
        }
    }
}

@Composable
private fun ProductCard(p: ProductDTO, onClick: () -> Unit = {}) {
    val imgBitmap = remember(p.photoBase64) {
        if (p.photoBase64.isNullOrBlank()) null
        else runCatching {
            // Backend stores photo as data URL: "data:image/jpeg;base64,/9j/..."
            // Strip the prefix before decoding
            val raw = p.photoBase64.substringAfter("base64,", p.photoBase64)
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // Photo thumbnail
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                if (imgBitmap != null) {
                    Image(
                        bitmap = imgBitmap,
                        contentDescription = p.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Outlined.Inventory2, null,
                        tint = Primary, modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Name / code / category
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        p.name, fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold, color = TextMain
                    )
                    val typeBg    = if (p.productType.equals("New", true)) PrimaryLight else WarningBg
                    val typeColor = if (p.productType.equals("New", true)) Primary else Warning
                    val typeLabel = if (p.productType.equals("New", true)) "အသစ်" else "အသုံးပြုပြီး"
                    Surface(color = typeBg, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            typeLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, color = typeColor
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(p.productCode, fontSize = 11.sp, color = TextMuted)
                if (!p.categoryName.isNullOrBlank()) {
                    Text(p.categoryName, fontSize = 11.sp, color = TextMuted)
                }
                if (!p.brandName.isNullOrBlank()) {
                    Text(p.brandName, fontSize = 10.sp, color = TextMuted)
                }
            }

            // Price & stock badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${p.sellingPrice.fmt()} Ks",
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Primary
                )
                Spacer(Modifier.height(4.dp))
                val displayQty = if (p.hasSerial == true) p.availableSerialCount ?: p.stockQty else p.stockQty
                val stockColor = when {
                    displayQty <= 0                                       -> Danger
                    p.reorderLevel != null && displayQty <= p.reorderLevel -> Warning
                    else                                                   -> Success
                }
                val stockLabel = when {
                    displayQty <= 0                                       -> "ပစ္စည်းကုန်"
                    p.reorderLevel != null && displayQty <= p.reorderLevel -> "နည်းပါး"
                    else                                                   -> "ရှိ"
                }
                val badgeBg = when (stockColor) { Danger -> DangerBg; Warning -> WarningBg; else -> SuccessBg }
                Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "$stockLabel ($displayQty)",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = stockColor
                    )
                }
            }
        }
    }
}

private fun Long.fmt() = String.format("%,d", this)
