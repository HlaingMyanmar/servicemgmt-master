package com.sspd.servicemgmt.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sspd.servicemgmt.navigation.LocalServerStatus
import com.sspd.servicemgmt.navigation.Screen
import com.sspd.servicemgmt.ui.theme.*
import com.sspd.servicemgmt.ui.viewmodel.HomeViewModel
import com.sspd.servicemgmt.ui.viewmodel.ServerStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val vm: HomeViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLogout()
    }

    LaunchedEffect(Unit) {
        while (true) { vm.loadStats(); delay(30_000) }
    }

    val cal      = remember { Calendar.getInstance() }
    val hour     = remember { cal.get(Calendar.HOUR_OF_DAY) }
    val greeting = remember {
        when {
            hour < 12 -> "မင်္ဂလာနံနက်ခင်းပါ"
            hour < 17 -> "မင်္ဂလာနေ့လည်ပိုင်းခင်းပါ"
            else      -> "မင်္ဂလာညနေခင်းပါ"
        }
    }
    val days   = remember { arrayOf("တနင်္ဂနွေ","တနင်္လာ","အင်္ဂါ","ဗုဒ္ဓဟူး","ကြာသပတေး","သောကြာ","စနေ") }
    val months = remember { arrayOf("ဇန်နဝါရီ","ဖေဖော်ဝါရီ","မတ်","ဧပြီ","မေ","ဇွန်","ဇူလိုင်","သြဂုတ်","စက်တင်ဘာ","အောက်တိုဘာ","နိုဝင်ဘာ","ဒီဇင်ဘာ") }
    val dateStr = remember {
        "${days[cal.get(Calendar.DAY_OF_WEEK) - 1]}၊ ${cal.get(Calendar.DAY_OF_MONTH)} ${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.fillMaxWidth(0.78f)
            ) {
                DrawerContent(
                    username    = state.username,
                    displayName = state.displayName,
                    onNavigate  = { route -> scope.launch { drawerState.close() }; onNavigate(route) },
                    onLogout    = { scope.launch { drawerState.close() }; vm.logout() }
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(ScreenBg)) {

            // Hero banner
            Column(
                modifier = Modifier.fillMaxWidth().background(Primary)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ServerStatusChip()
                        IconButton(onClick = { vm.loadStats() }) {
                            Icon(Icons.Outlined.Refresh, null, tint = Color.White.copy(0.8f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape)
                            .background(Color.White.copy(0.22f))
                            .border(2.dp, Color.White.copy(0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (state.username.firstOrNull()?.uppercaseChar() ?: 'U').toString(),
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                        )
                    }
                    Column {
                        Text("$greeting,", fontSize = 12.sp, color = Color.White.copy(0.72f), fontWeight = FontWeight.SemiBold)
                        Text(state.username, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(dateStr, fontSize = 11.sp, color = Color.White.copy(0.58f))
                    }
                }
            }

            // Body
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                // Stats header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ယနေ့ ခြုံငုံသုံးသပ်ချက်",
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = TextMuted, letterSpacing = 0.9.sp)
                    if (state.loading)
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Modifier.weight(1f), "ယနေ့ ရောင်းရငွေ",
                        "${(state.stats.todaySalesAmount ?: 0).fmt()} Ks", Icons.Outlined.Payments, Primary, PrimaryLight) { onNavigate(Screen.Sales.route) }
                    StatCard(Modifier.weight(1f), "ရောင်းချမှုအရေအတွက်",
                        "${state.stats.todaySalesCount ?: 0} ခု", Icons.Outlined.Receipt, Success, SuccessBg) { onNavigate(Screen.Sales.route) }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Modifier.weight(1f), "ကုန်သိုလှောင် နည်းပါး",
                        "${state.stats.lowStockCount ?: 0} မျိုး", Icons.Outlined.Warning, Warning, WarningBg) { onNavigate(Screen.Products.route) }
                    StatCard(Modifier.weight(1f), "ဆိုင်ခင်းအလုပ်",
                        "${state.stats.pendingServiceJobs ?: 0}", Icons.Outlined.Build, Violet, VioletBg) { onNavigate(Screen.ServiceJobs.route) }
                }

                Spacer(Modifier.height(22.dp))
                Text("အမြန် လုပ်ဆောင်ချက်များ",
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = TextMuted, letterSpacing = 0.9.sp)
                Spacer(Modifier.height(10.dp))

                listOf(
                    Triple("ကုန်ပစ္စည်းများ",           Icons.Default.Inventory2,    Screen.Products.route),
                    Triple("ရောင်းချမှုများ",            Icons.Default.Receipt,       Screen.Sales.route),
                    Triple("ပစ္စည်းလက်ခံ (Booking)",    Icons.Default.CalendarMonth, Screen.Bookings.route),
                    Triple("ဝန်ဆောင်မှုလုပ်ငန်း",       Icons.Default.Build,         Screen.ServiceJobs.route),
                    Triple("ဝန်ဆောင်မှုများ",           Icons.Default.MiscellaneousServices, Screen.ServiceMgmt.route),
                    Triple("ကန့်တည်နေရာများ",           Icons.Default.LocationOn,    Screen.ShelfLocations.route),
                    Triple("ဝန်ထမ်းစွမ်းဆောင်ရည်",     Icons.Default.BarChart,      Screen.StaffReport.route),
                ).forEachIndexed { i, (label, icon, route) ->
                    val colors = listOf(Color(0xFF0891B2), Primary, Violet, Color(0xFF059669), Color(0xFFD97706), Color(0xFF0891B2), Color(0xFF7C3AED))
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    ActionCard(label, icon, colors[i]) { onNavigate(route) }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
        }
    }
}

@Composable
private fun ActionCard(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = BorderColor)
        }
    }
}

@Composable
fun DrawerContent(
    username: String,
    displayName: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val initial = (username.firstOrNull()?.uppercaseChar() ?: 'U').toString()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth().background(Primary)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("S", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
            }
            Spacer(Modifier.height(10.dp))
            Text("SSPD Manager", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("IT Solution Center", fontSize = 11.sp, color = Color.White.copy(0.65f))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(Color.White.copy(0.2f))
                        .border(1.5.dp, Color.White.copy(0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Column {
                    Text(displayName.ifEmpty { username }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(username, fontSize = 11.sp, color = Color.White.copy(0.6f))
                }
            }
        }

        // Menu
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
            listOf(
                Triple("ဝန်ဆောင်မှုများ",            Icons.Outlined.MiscellaneousServices, Screen.ServiceMgmt.route),
                Triple("ကန့်တည်နေရာများ",            Icons.Outlined.LocationOn,            Screen.ShelfLocations.route),
                Triple("ကုန်ကျစရိတ်များ",            Icons.Outlined.AccountBalanceWallet,  Screen.Expenses.route),
                Triple("အဖွဲ့ Chat",                 Icons.Outlined.Chat,                  Screen.Chat.route),
                Triple("ဝန်ထမ်းစွမ်းဆောင်ရည်",       Icons.Outlined.BarChart,              Screen.StaffReport.route),
                Triple("ရောင်းအကောင်းဆုံးစာရင်း",   Icons.Outlined.EmojiEvents,           Screen.SalesRanking.route),
                Triple("Audit မှတ်တမ်းများ",          Icons.Outlined.Security,              Screen.AuditLog.route),
                Triple("အကောင့်သတ်မှတ်ချက်",         Icons.Outlined.ManageAccounts,         Screen.Account.route),
                Triple("အကြောင်းအရာ",                Icons.Outlined.Info,                   Screen.About.route),
            ).forEach { (label, icon, route) ->
                DrawerMenuItem(label, icon) { onNavigate(route) }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        DrawerMenuItem("ထွက်ရန်", Icons.Outlined.Logout, iconTint = Danger, textColor = Danger) { onLogout() }
        Text(
            "SSPD Management System",
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 4.dp),
            textAlign = TextAlign.Center, fontSize = 10.sp, color = TextMuted
        )
    }
}

@Composable
private fun DrawerMenuItem(
    label: String,
    icon: ImageVector,
    iconTint: Color = Primary,
    textColor: Color = TextMain,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
                .background(if (iconTint == Danger) Color(0xFFFFF1F2) else PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = BorderColor, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ServerStatusChip() {
    val status = LocalServerStatus.current

    val infiniteTransition = rememberInfiniteTransition(label = "ping")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse),
        label = "alpha"
    )

    val (icon, dotColor, label) = when (status) {
        ServerStatus.ONLINE   -> Triple(Icons.Outlined.Wifi,    Color(0xFF4ADE80), "Online")
        ServerStatus.OFFLINE  -> Triple(Icons.Outlined.WifiOff, Color(0xFFF87171), "Offline")
        ServerStatus.CHECKING -> Triple(Icons.Outlined.Wifi,    Color.White.copy(pulseAlpha), "...")
    }

    Surface(
        color  = Color.White.copy(alpha = 0.14f),
        shape  = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = dotColor, modifier = Modifier.size(14.dp))
            Text(label, fontSize = 10.sp, color = dotColor, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Long.fmt() = String.format("%,d", this)
