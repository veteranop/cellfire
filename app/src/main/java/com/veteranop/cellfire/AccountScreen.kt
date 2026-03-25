package com.veteranop.cellfire

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.veteranop.cellfire.ui.theme.OrangeGlow
import kotlinx.coroutines.launch

// ─── Login gate (used from splash on first launch) ────────────────────────────
// "login" route — no back button out; on success goes to "start" clearing the stack.
@Composable
fun AccountScreen(navController: NavController, vm: CellFireViewModel) {
    val authState by vm.authState.collectAsState()
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()

    // First-launch path: once logged in, go to start and clear the login screen
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) {
            navController.navigate("start") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.cfbackground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.4f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_name),
                contentDescription = "Cellfire",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            when (val s = authState) {
                is AuthState.Loading, is AuthState.LoggedIn -> {
                    // LoggedIn triggers LaunchedEffect nav — show spinner meanwhile
                    Spacer(modifier = Modifier.height(80.dp))
                    CircularProgressIndicator(color = OrangeGlow)
                }
                is AuthState.LoggedOut, is AuthState.Error -> {
                    var showRegister by remember { mutableStateOf(false) }
                    val errorMsg = (s as? AuthState.Error)?.message
                    if (showRegister) {
                        RegisterForm(
                            errorMessage = errorMsg,
                            onRegister   = { user, email, pass -> scope.launch { vm.register(user, email, pass) } },
                            onBackToLogin = { showRegister = false; vm.clearError() }
                        )
                    } else {
                        LoginForm(
                            errorMessage = errorMsg,
                            onLogin      = { user, pass -> scope.launch { vm.login(user, pass) } },
                            onCreateAccount = { showRegister = true; vm.clearError() },
                            onSubscribe  = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://cellfire.io/pricing"))
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Account management (accessed from StartScreen "Account" button) ──────────
// "account" route — shows dashboard when signed in; if signed out navigates to login.
@Composable
fun AccountManagementScreen(navController: NavController, vm: CellFireViewModel) {
    val authState by vm.authState.collectAsState()
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()

    // If user signs out while on this screen, send them back to the login gate
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedOut || authState is AuthState.Error) {
            navController.navigate("login") {
                popUpTo("start") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.cfbackground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.4f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_name),
                contentDescription = "Cellfire",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            when (val s = authState) {
                is AuthState.Loading, is AuthState.LoggedOut, is AuthState.Error -> {
                    Spacer(modifier = Modifier.height(80.dp))
                    CircularProgressIndicator(color = OrangeGlow)
                }
                is AuthState.LoggedIn -> {
                    AccountDashboard(
                        username        = s.username,
                        license         = s.license,
                        onManageBilling = {
                            scope.launch {
                                vm.getPortalUrl().onSuccess { url ->
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    )
                                }
                            }
                        },
                        onSubscribe = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://cellfire.io/pricing"))
                            )
                        },
                        onLogout = { vm.logout() }
                    )
                }
            }
        }
    }
}

// ─── Login form ───────────────────────────────────────────────────────────────

@Composable
private fun LoginForm(
    errorMessage:    String?,
    onLogin:         (username: String, password: String) -> Unit,
    onCreateAccount: () -> Unit,
    onSubscribe:     () -> Unit
) {
    var username       by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(24.dp))

    Text("Sign In", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text(
        "Use your Cellfire account",
        color = Color.Gray, fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
    )

    OutlinedTextField(
        value         = username,
        onValueChange = { username = it },
        label         = { Text("Username or Email") },
        leadingIcon   = { Icon(Icons.Default.Person, null, tint = OrangeGlow) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = OrangeGlow, focusedLabelColor  = OrangeGlow,
            cursorColor          = OrangeGlow, focusedTextColor   = Color.White,
            unfocusedTextColor   = Color.White, unfocusedBorderColor = Color.DarkGray,
            unfocusedLabelColor  = Color.Gray
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value         = password,
        onValueChange = { password = it },
        label         = { Text("Password") },
        leadingIcon   = { Icon(Icons.Default.Lock, null, tint = OrangeGlow) },
        trailingIcon  = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null, tint = Color.Gray
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None
                               else PasswordVisualTransformation(),
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = OrangeGlow, focusedLabelColor  = OrangeGlow,
            cursorColor          = OrangeGlow, focusedTextColor   = Color.White,
            unfocusedTextColor   = Color.White, unfocusedBorderColor = Color.DarkGray,
            unfocusedLabelColor  = Color.Gray
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction    = ImeAction.Done
        )
    )

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            errorMessage, color = Color.Red, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick  = { onLogin(username.trim(), password) },
        enabled  = username.isNotBlank() && password.isNotBlank(),
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = OrangeGlow),
        shape    = RoundedCornerShape(8.dp)
    ) {
        Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
    }

    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider(color = Color.DarkGray)
    Spacer(modifier = Modifier.height(20.dp))

    // Create account — in-app
    Text("New to Cellfire?", color = Color.Gray, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick  = onCreateAccount,
        modifier = Modifier.fillMaxWidth(),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = OrangeGlow),
        border   = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(OrangeGlow)
        )
    ) { Text("Create Free Account — 30-Day Trial") }

    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onSubscribe, modifier = Modifier.fillMaxWidth()) {
        Text("View Plans & Pricing", color = Color.Gray, fontSize = 13.sp)
    }
}

// ─── Register form ────────────────────────────────────────────────────────────

@Composable
private fun RegisterForm(
    errorMessage:  String?,
    onRegister:    (username: String, email: String, password: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var username        by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(16.dp))

    Text("Create Account", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text(
        "Start your free 30-day trial",
        color = Color.Gray, fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
    )

    OutlinedTextField(
        value = username, onValueChange = { username = it },
        label = { Text("Username") },
        leadingIcon = { Icon(Icons.Default.Person, null, tint = OrangeGlow) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangeGlow, focusedLabelColor = OrangeGlow,
            cursorColor = OrangeGlow, focusedTextColor = Color.White,
            unfocusedTextColor = Color.White, unfocusedBorderColor = Color.DarkGray,
            unfocusedLabelColor = Color.Gray
        ),
        supportingText = { Text("3–32 chars, letters/numbers/underscore", color = Color.Gray, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = email, onValueChange = { email = it },
        label = { Text("Email Address") },
        leadingIcon = { Icon(Icons.Default.Person, null, tint = OrangeGlow) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangeGlow, focusedLabelColor = OrangeGlow,
            cursorColor = OrangeGlow, focusedTextColor = Color.White,
            unfocusedTextColor = Color.White, unfocusedBorderColor = Color.DarkGray,
            unfocusedLabelColor = Color.Gray
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = password, onValueChange = { password = it },
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = OrangeGlow) },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null, tint = Color.Gray
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangeGlow, focusedLabelColor = OrangeGlow,
            cursorColor = OrangeGlow, focusedTextColor = Color.White,
            unfocusedTextColor = Color.White, unfocusedBorderColor = Color.DarkGray,
            unfocusedLabelColor = Color.Gray
        ),
        supportingText = { Text("Minimum 8 characters", color = Color.Gray, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
    )

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(errorMessage, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick  = { onRegister(username.trim(), email.trim(), password) },
        enabled  = username.isNotBlank() && email.isNotBlank() && password.length >= 8,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = OrangeGlow),
        shape    = RoundedCornerShape(8.dp)
    ) {
        Text("Create Account & Start Trial", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
    }

    Spacer(modifier = Modifier.height(12.dp))
    TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
        Text("← Back to Sign In", color = Color.Gray, fontSize = 13.sp)
    }
}

// ─── Account dashboard ────────────────────────────────────────────────────────

@Composable
private fun AccountDashboard(
    username:        String,
    license:         LicenseInfo,
    onManageBilling: () -> Unit,
    onSubscribe:     () -> Unit,
    onLogout:        () -> Unit
) {
    var portalLoading by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(16.dp))

    Icon(
        Icons.Default.AccountCircle,
        contentDescription = null,
        tint     = OrangeGlow,
        modifier = Modifier.size(64.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(username, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Signed in", color = Color.Gray, fontSize = 13.sp)

    Spacer(modifier = Modifier.height(24.dp))

    // License card
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = Color(0xFF1A1A1A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Subscription", color = Color.Gray, fontSize = 12.sp)
                StatusBadge(license.isActive)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(license.planLabel, color = OrangeGlow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (license.expiresAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val label = if (license.planType == "demo") "Trial expires" else "Renews"
                Text("$label: ${license.expiresAt.take(10)}", color = Color.Gray, fontSize = 13.sp)
            }
            if (license.stripeStatus != null && license.stripeStatus != "active") {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Billing: ${license.stripeStatus}", color = Color(0xFFFFCC00), fontSize = 12.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (license.hasStripeCustomer) {
        Button(
            onClick  = { portalLoading = true; onManageBilling() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = OrangeGlow),
            shape    = RoundedCornerShape(8.dp)
        ) {
            if (portalLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Text("Manage Billing", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
            }
        }
    } else {
        Button(
            onClick  = onSubscribe,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = OrangeGlow),
            shape    = RoundedCornerShape(8.dp)
        ) {
            Text("Upgrade Plan →", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
        Text("Sign Out", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val (label, bg)   = if (isActive) "Active"   to Color(0xFF1B4332) else "Inactive" to Color(0xFF4A1B1B)
    val textColor     = if (isActive) Color(0xFF6FCF97) else Color(0xFFEB5757)
    Box(modifier = Modifier.background(bg, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
