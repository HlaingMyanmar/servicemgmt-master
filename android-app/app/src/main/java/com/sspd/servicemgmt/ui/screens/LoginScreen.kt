package com.sspd.servicemgmt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sspd.servicemgmt.ui.theme.*
import com.sspd.servicemgmt.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(onSuccess: () -> Unit) {
    val vm: LoginViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    var username  by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) onSuccess()
    }

    Box(modifier = Modifier.fillMaxSize().background(Primary)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoSection()

            Spacer(Modifier.height(28.dp))

            LoginCard(
                username          = username,
                password          = password,
                pwVisible         = pwVisible,
                loading           = state.loading,
                error             = state.error,
                onUsernameChange  = { username = it },
                onPasswordChange  = { password = it },
                onTogglePwVisible = { pwVisible = !pwVisible },
                onLogin           = { vm.login(username, password) }
            )

            Spacer(Modifier.height(24.dp))
            Text("SSPD IT Solution Center", fontSize = 11.sp, color = Color.White.copy(0.45f))
        }
    }
}

@Composable
private fun LogoSection() {
    Surface(
        modifier = Modifier.size(72.dp),
        shape    = RoundedCornerShape(20.dp),
        color    = Color.White.copy(alpha = 0.2f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("S", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("SSPD Manager", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    Text("ကုန်ပစ္စည်းနှင့် ရောင်းချမှုစနစ်", fontSize = 12.sp, color = Color.White.copy(0.7f))
}

@Composable
private fun LoginCard(
    username:          String,
    password:          String,
    pwVisible:         Boolean,
    loading:           Boolean,
    error:             String,
    onUsernameChange:  (String) -> Unit,
    onPasswordChange:  (String) -> Unit,
    onTogglePwVisible: () -> Unit,
    onLogin:           () -> Unit,
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                "အကောင့်ဝင်ရောက်မည်",
                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))

            FieldLabel("အသုံးပြုသူနာမည်")
            OutlinedTextField(
                value           = username,
                onValueChange   = onUsernameChange,
                modifier        = Modifier.fillMaxWidth(),
                placeholder     = { Text("Username") },
                leadingIcon     = { Icon(Icons.Outlined.Person, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine      = true,
                shape           = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(14.dp))

            FieldLabel("စကားဝှက်")
            OutlinedTextField(
                value                = password,
                onValueChange        = onPasswordChange,
                modifier             = Modifier.fillMaxWidth(),
                placeholder          = { Text("Password") },
                leadingIcon          = { Icon(Icons.Outlined.Lock, null) },
                trailingIcon         = {
                    IconButton(onClick = onTogglePwVisible) {
                        Icon(
                            if (pwVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            null
                        )
                    }
                },
                keyboardOptions      = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done
                ),
                visualTransformation = if (pwVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                singleLine           = true,
                shape                = RoundedCornerShape(12.dp)
            )

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    error, color = Danger, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick  = onLogin,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled  = !loading
            ) {
                if (loading)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else
                    Text("ဝင်ရောက်မည်", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
    Spacer(Modifier.height(6.dp))
}
