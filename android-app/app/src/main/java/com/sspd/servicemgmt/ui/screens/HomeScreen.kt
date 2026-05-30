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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sspd.servicemgmt.R
import com.sspd.servicemgmt.navigation.LocalServerStatus
import com.sspd.servicemgmt.navigation.Screen
import com.sspd.servicemgmt.ui.theme.*
import com.sspd.servicemgmt.ui.viewmodel.HomeViewModel
import com.sspd.servicemgmt.ui.viewmodel.ServerStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

// ── Hero gradient colours ──────────────────────────────────────────────────────
private val HeroTop    = Color(0xFF1D4ED8)
private val HeroBottom = Color(0xFF3B82F6)

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onLogout:   () -> Unit
) {
    val vm: HomeViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    LaunchedEffect(state.isLoggedOut) { if (state.isLoggedOut) onLogout() }
    LaunchedEffect(Unit) { while (true) { vm.loadStats(); delay(30_000) } }

    val cal      = remember { Calendar.getInstance() }
    val hour     = remember { cal.get(Calendar.HOUR_OF_DAY) }
    val greeting = remember {
        when {
            hour < 12 -> "မင်္ဂလာ နံနက်ခင်းပါ"
            hour < 17 -> "မင်္ဂလာ နေ့လည်ပိုင်းပါ"
            else      -> "မင်္ဂလာ ညနေပိုင်းပါ"
        }
    }
    val days   = remember { arrayOf("တနင်္ဂနွေ","တနင်္လာ","အင်္ဂါ","ဗုဒ္ဓဟူး","ကြာသပတေး","သောကြာ","စနေ") }
    val months = remember { arrayOf("ဇန်နဝါရီ","ဖေဖော်ဝါရီ","မတ်","ဧပြီ","မေ","ဇွန်","ဇူလိုင်","သြဂုတ်","စက်တင်ဘာ","အောက်တိုဘာ","နိုဝင်ဘာ","ဒီဇင်ဘာ") }
    val dateStr = remember {
        "${days[cal.get(Calendar.DAY_OF_WEEK)-1]}၊ ${cal.get(Calendar.DAY_OF_MONTH)} ${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
    }

    ModalNavigationDrawer(
        drawerState   = drawerState,
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

            // ── Hero Banner ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Brush.verticalGradient(listOf(HeroTop, HeroBottom)))
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.padding(bottom = 20.dp)) {

                    // Top bar: menu | title | status+refresh
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        

                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            ServerStatusChip()
                            IconButton(onClick = { vm.loadStats() }) {
                                Icon(Icons.Outlined.Refresh, null, tint = Color.White.copy(0.85f))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Greeting
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            "$greeting,",
                            fontSize   = 12.sp,
                            color      = Color.White.copy(0.75f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            state.displayName.ifEmpty { state.username },
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CalendarToday, null,
                                tint     = Color.White.copy(0.60f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                dateStr,
                                fontSize = 11.sp,
                                color    = Color.White.copy(0.65f)
                            )
                        }
                    }
                }
            }

            // ── Body ──────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // ── Stats header ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "ယနေ့ ခြုံငုံ",
                        fontSize     = 13.sp,
                        fontWeight   = FontWeight.ExtraBold,
                        color        = TextMain,
                        letterSpacing = 0.5.sp
                    )
                    if (state.loading)
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color       = Primary
                        )
                }

                // ── 2×2 stat grid ─────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label    = "ရောင်းရငွေ",
                        value    = "${(state.stats.todaySalesAmount ?: 0).fmt()} Ks",
                        icon     = Icons.Outlined.Payments,
                        color    = Primary,
                        bg       = PrimaryLight
                    ) { onNavigate(Screen.Sales.route) }

                    StatCard(
                        modifier = Modifier.weight(1f),
                        label    = "ရောင်းမှု",
                        value    = "${state.stats.todaySalesCount ?: 0} ခု",
                        icon     = Icons.Outlined.Receipt,
                        color    = Success,
                        bg       = SuccessBg
                    ) { onNavigate(Screen.Sales.route) }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label    = "ကုန်နည်းနေ",
                        value    = "${state.stats.lowStockCount ?: 0} မျိုး",
                        icon     = Icons.Outlined.Warning,
                        color    = Warning,
                        bg       = WarningBg
                    ) { onNavigate(Screen.Products.route) }

                    StatCard(
                        modifier = Modifier.weight(1f),
                        label    = "ဆိုင်ခင်း Job",
                        value    = "${state.stats.pendingServiceJobs ?: 0} ခု",
                        icon     = Icons.Outlined.Build,
                        color    = Violet,
                        bg       = VioletBg
                    ) { onNavigate(Screen.ServiceJobs.route) }
                }

                Spacer(Modifier.height(24.dp))

                // ── Quick actions ─────────────────────────────────────────────
                Text(
                    "အမြန် လုပ်ဆောင်ချက်",
                    fontSize     = 13.sp,
                    fontWeight   = FontWeight.ExtraBold,
                    color        = TextMain,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(12.dp))

                val actions = listOf(
                    QuadItem("ကုန်ပစ္စည်းများ",         Icons.Outlined.Inventory2,            Color(0xFF0891B2), Screen.Products.route),
                    QuadItem("အရောင်းဆိုင်ရာ",           Icons.Outlined.Receipt,               Primary,          Screen.Sales.route),
                    QuadItem("Booking",                  Icons.Outlined.CalendarMonth,         Color(0xFF0369A1),Screen.Bookings.route),
                    QuadItem("ဝန်ဆောင်မှု Job",          Icons.Outlined.Build,                 Color(0xFF059669),Screen.ServiceJobs.route),
                    QuadItem("ကုန်ကျစရိတ်",              Icons.Outlined.AccountBalanceWallet,  Color(0xFFB45309),Screen.Expenses.route),
                    QuadItem("ကိန်းဂဏာန်း",              Icons.Outlined.BarChart,              Color(0xFF0891B2),Screen.Report.route),
                    QuadItem("ဝင်ငွေ/အမြတ်",             Icons.Outlined.TrendingUp,            Color(0xFF059669),Screen.IncomeReport.route),
                    QuadItem("ဝန်ဆောင်မှုများ",          Icons.Outlined.MiscellaneousServices, Color(0xFFD97706),Screen.ServiceMgmt.route),
                )

                actions.chunked(2).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { item ->
                            ActionGridCard(
                                modifier = Modifier.weight(1f),
                                label    = item.label,
                                icon     = item.icon,
                                color    = item.color,
                                onClick  = { onNavigate(item.route) }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ── Data holder ───────────────────────────────────────────────────────────────
private data class QuadItem(
    val label: String,
    val icon:  ImageVector,
    val color: Color,
    val route: String
)

// ── Stat Card ─────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(
    modifier: Modifier,
    label:    String,
    value:    String,
    icon:     ImageVector,
    color:    Color,
    bg:       Color,
    onClick:  () -> Unit
) {
    Card(
        modifier  = modifier.clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextMuted)
        }
    }
}

// ── Action Grid Card ──────────────────────────────────────────────────────────
@Composable
private fun ActionGridCard(
    modifier: Modifier,
    label:    String,
    icon:     ImageVector,
    color:    Color,
    onClick:  () -> Unit
) {
    Card(
        modifier  = modifier.clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Text(
                label,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = TextMain,
                maxLines   = 2,
                lineHeight = 16.sp,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Drawer ────────────────────────────────────────────────────────────────────
@Composable
fun DrawerContent(
    username:    String,
    displayName: String,
    onNavigate:  (String) -> Unit,
    onLogout:    () -> Unit
) {
    val initial = (username.firstOrNull()?.uppercaseChar() ?: 'U').toString()

    Column(modifier = Modifier.fillMaxSize()) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(HeroTop, HeroBottom)))
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp)
        ) {
            Column {
                // Logo badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter            = painterResource(R.drawable.logo),
                        contentDescription = "SSPD Logo",
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.size(56.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    "SSPD Manager",
                    fontSize   = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
                Text(
                    "IT Solution Center",
                    fontSize = 11.sp,
                    color    = Color.White.copy(0.65f)
                )

                Spacer(Modifier.height(16.dp))

                // User row
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.20f))
                            .border(1.5.dp, Color.White.copy(0.40f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Column {
                        Text(
                            displayName.ifEmpty { username },
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text(
                            username,
                            fontSize = 11.sp,
                            color    = Color.White.copy(0.6f)
                        )
                    }
                }
            }
        }

        // Menu items
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 8.dp)
        ) {
            DrawerSection("စီမံခန့်ခွဲမှု")
            DrawerMenuItem("ဝန်ဆောင်မှုများ",          Icons.Outlined.MiscellaneousServices, Screen.ServiceMgmt.route,   onNavigate)
            DrawerMenuItem("ကန့်တည်နေရာများ",          Icons.Outlined.LocationOn,            Screen.ShelfLocations.route, onNavigate)

            DrawerSection("ငွေကြေး")
            DrawerMenuItem("ကုန်ကျစရိတ်",              Icons.Outlined.AccountBalanceWallet,  Screen.Expenses.route,      onNavigate)
            DrawerMenuItem("ကိန်းဂဏာန်း",              Icons.Outlined.BarChart,              Screen.Report.route,        onNavigate)
            DrawerMenuItem("ဝင်ငွေ / အမြတ် စာရင်း",   Icons.Outlined.TrendingUp,            Screen.IncomeReport.route,  onNavigate)

            DrawerSection("အဖွဲ့")
            DrawerMenuItem("ဝန်ထမ်းစွမ်းဆောင်ရည်",     Icons.Outlined.BarChart,              Screen.StaffReport.route,   onNavigate)
            DrawerMenuItem("အဖွဲ့ Chat",               Icons.Outlined.Chat,                  Screen.Chat.route,          onNavigate)

            DrawerSection("စနစ်")
            DrawerMenuItem("Audit မှတ်တမ်း",           Icons.Outlined.Security,              Screen.AuditLog.route,      onNavigate)
            DrawerMenuItem("အကောင့်သတ်မှတ်ချက်",        Icons.Outlined.ManageAccounts,        Screen.Account.route,       onNavigate)
            DrawerMenuItem("အကြောင်းအရာ",               Icons.Outlined.Info,                  Screen.About.route,         onNavigate)
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        DrawerMenuItem(
            label     = "ထွက်ရန်",
            icon      = Icons.Outlined.Logout,
            route     = "",
            onNavigate = { onLogout() },
            iconTint  = Danger,
            textColor = Danger
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text     = title.uppercase(),
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
        fontSize = 10.sp,
        fontWeight   = FontWeight.ExtraBold,
        color        = TextMuted,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun DrawerMenuItem(
    label:     String,
    icon:      ImageVector,
    route:     String,
    onNavigate: (String) -> Unit,
    iconTint:  Color = Primary,
    textColor: Color = TextMain
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onNavigate(route) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (iconTint == Danger) Color(0xFFFFF1F2) else PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Text(
            label,
            modifier   = Modifier.weight(1f),
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = textColor
        )
        if (iconTint != Danger)
            Icon(Icons.Default.ChevronRight, null, tint = BorderColor, modifier = Modifier.size(16.dp))
    }
}

// ── Server status chip ────────────────────────────────────────────────────────
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
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = dotColor, modifier = Modifier.size(14.dp))
            Text(label, fontSize = 10.sp, color = dotColor, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Long.fmt() = String.format("%,d", this)
