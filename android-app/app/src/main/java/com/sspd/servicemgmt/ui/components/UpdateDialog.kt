package com.sspd.servicemgmt.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sspd.servicemgmt.api.AppVersionDTO
import com.sspd.servicemgmt.ui.theme.Primary

@Composable
fun UpdateDialog(
    update:    AppVersionDTO,
    onDismiss: () -> Unit          // ignored when forceUpdate = true
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = { if (!update.forceUpdate) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress    = !update.forceUpdate,
            dismissOnClickOutside = !update.forceUpdate
        )
    ) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Icon
                Box(
                    modifier        = Modifier
                        .size(64.dp)
                        .background(Primary.copy(0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.SystemUpdate,
                        contentDescription = "Update available",
                        tint     = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    if (update.forceUpdate) "အပ်ဒိတ် လုပ်ရမည်" else "အပ်ဒိတ် ရှိပါသည်",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color(0xFF0F172A)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "v${update.versionName}",
                    fontSize = 13.sp,
                    color    = Primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (update.changelog.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color  = Color(0xFFF8FAFC),
                        shape  = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            update.changelog,
                            modifier   = Modifier.padding(12.dp),
                            fontSize   = 12.sp,
                            color      = Color(0xFF475569),
                            lineHeight = 20.sp,
                            textAlign  = TextAlign.Start
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Update button
                Button(
                    onClick = {
                        if (update.downloadUrl.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW,
                                Uri.parse(update.downloadUrl))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled  = update.downloadUrl.isNotBlank()
                ) {
                    Icon(Icons.Outlined.SystemUpdate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Update ရယူပါ", fontWeight = FontWeight.Bold)
                }

                // Skip button — hidden when forceUpdate
                if (!update.forceUpdate) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick  = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "ကျော်သွား",
                            color    = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
