package com.sspd.servicemgmt.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sspd.servicemgmt.api.ProductSerialDTO
import com.sspd.servicemgmt.ui.theme.*
import com.sspd.servicemgmt.ui.viewmodel.ProductDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(onBack: () -> Unit) {
    val vm: ProductDetailViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ကုန်ပစ္စည်း အချက်အလက်", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Outlined.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary, titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        val p = state.product
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("ကုန်ပစ္စည်း မတွေ့ပါ", color = TextMuted)
                }
            }
            return@Scaffold
        }

        // Decode photo
        val imgBitmap = remember(p.photoBase64) {
            if (p.photoBase64.isNullOrBlank()) null
            else runCatching {
                val raw = p.photoBase64.substringAfter("base64,", p.photoBase64)
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(ScreenBg),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Photo + Name header ──────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp).background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (imgBitmap != null) {
                        Image(
                            bitmap = imgBitmap,
                            contentDescription = p.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Dark overlay for text readability
                        Box(Modifier.fillMaxSize().background(Color(0x66000000)))
                    } else {
                        Icon(
                            Icons.Outlined.Inventory2, null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    // Name + badges overlay
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TypeBadge(p.productType)
                            Surface(color = Color.White.copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    p.productCode,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }

            // ── Price card ──────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                SectionCard(
                    icon = Icons.Outlined.AttachMoney,
                    title = "ဈေးနှုန်း",
                    iconColor = Primary
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PriceBox("ရောင်းဈေး", "${p.sellingPrice.fmt()} Ks", Primary)
                        if (p.costPrice != null && p.costPrice > 0) {
                            VerticalDivider(modifier = Modifier.height(40.dp))
                            PriceBox("ဝယ်ဈေး", "${p.costPrice.fmt()} Ks", TextMuted)
                        }
                    }
                }
            }

            // ── Stock card ──────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SectionCard(
                    icon = Icons.Outlined.Inventory,
                    title = "Stock အခြေအနေ",
                    iconColor = Success
                ) {
                    val displayQty = if (p.hasSerial == true) p.availableSerialCount ?: p.stockQty else p.stockQty
                    val stockColor = when {
                        displayQty <= 0 -> Danger
                        p.reorderLevel != null && displayQty <= p.reorderLevel -> Warning
                        else -> Success
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StockBox("လက်ကျန်", "$displayQty ခု", stockColor)
                        if (p.reorderLevel != null) {
                            VerticalDivider(modifier = Modifier.height(40.dp))
                            StockBox("Reorder Level", "${p.reorderLevel} ခု", Warning)
                        }
                        if (p.shortageQty != null && p.shortageQty > 0) {
                            VerticalDivider(modifier = Modifier.height(40.dp))
                            StockBox("ပြည့်ရမည်", "${p.shortageQty} ခု", Danger)
                        }
                    }
                }
            }

            // ── Details card ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SectionCard(
                    icon = Icons.Outlined.Info,
                    title = "အချက်အလက်များ",
                    iconColor = Violet
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!p.categoryName.isNullOrBlank())
                            DetailRow(Icons.Outlined.Category, "အမျိုးအစား", p.categoryName)
                        if (!p.brandName.isNullOrBlank())
                            DetailRow(Icons.Outlined.Sell, "Brand", p.brandName)
                        if (!p.unitName.isNullOrBlank())
                            DetailRow(Icons.Outlined.Scale, "Unit", p.unitName)

                        val warranty = when {
                            !p.warrantyTerms.isNullOrBlank() -> p.warrantyTerms
                            (p.warrantyMonths ?: 0) > 0      -> "${p.warrantyMonths} လ"
                            else                             -> null
                        }
                        if (warranty != null)
                            DetailRow(Icons.Outlined.Shield, "အာမခံ", warranty)
                        if (!p.remark.isNullOrBlank())
                            DetailRow(Icons.Outlined.Notes, "မှတ်ချက်", p.remark)

                        DetailRow(
                            Icons.Outlined.QrCode2, "Serial ခြေရာခံ",
                            if (p.hasSerial == true) "ခြေရာခံသည်" else "ခြေရာမခံ"
                        )
                    }
                }
            }

            // ── Serials section ─────────────────────────────────────────────
            if (p.hasSerial == true && state.serials.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Spacer(Modifier.height(0.dp)) // section header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.QrCode2, null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text(
                            "Serial Numbers (${state.serials.size})",
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextMain
                        )
                    }
                }
                items(state.serials) { serial ->
                    SerialRow(serial)
                }
            }
        }
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun TypeBadge(type: String) {
    val (bg, color, label) = when (type.uppercase()) {
        "NEW"        -> Triple(Color(0xFF4ADE80), Color(0xFF14532D), "အသစ်")
        "SECOND_NEW" -> Triple(Color(0xFFA78BFA), Color(0xFF4C1D95), "Second New")
        else         -> Triple(Color(0xFFFBBF24), Color(0xFF78350F), "အသုံးပြုပြီး")
    }
    Surface(color = bg, shape = RoundedCornerShape(4.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = color
        )
    }
}

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = iconColor)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderColor)
            content()
        }
    }
}

@Composable
private fun PriceBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun StockBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(15.dp).padding(top = 1.dp))
        Text(label, fontSize = 12.sp, color = TextMuted, modifier = Modifier.width(90.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SerialRow(serial: ProductSerialDTO) {
    val (statusBg, statusColor, statusLabel) = when (serial.status?.uppercase()) {
        "AVAILABLE"  -> Triple(SuccessBg, Success, "ရှိ")
        "SOLD"       -> Triple(DangerBg,  Danger,  "ရောင်းပြီး")
        "IN_SERVICE" -> Triple(WarningBg, Warning, "ဝန်ဆောင်မှုဆဲ")
        "DEFECTIVE"  -> Triple(DangerBg,  Danger,  "ချွတ်ယွင်းချက်")
        "RETURNED"   -> Triple(WarningBg, Warning, "ပြန်လည်")
        else         -> Triple(BorderColor, TextMuted, serial.status ?: "-")
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.QrCode2, null,
                tint = TextMuted, modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                serial.serialNumber,
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain,
                modifier = Modifier.weight(1f)
            )
            Surface(color = statusBg, shape = RoundedCornerShape(6.dp)) {
                Text(
                    statusLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor
                )
            }
        }
        if (serial.warrantyEndDate != null) {
            Text(
                "အာမခံကုန်ဆုံး: ${serial.warrantyEndDate.take(10)}",
                fontSize = 10.sp, color = TextMuted,
                modifier = Modifier.padding(start = 36.dp, bottom = 8.dp)
            )
        }
    }
}

private fun Long.fmt() = String.format("%,d", this)
