package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.BookingEntity
import com.example.data.Flight
import com.example.data.FlightSearchEngine
import com.example.ui.theme.MyApplicationTheme
import com.example.data.PriceAlertEntity
import com.example.data.TravelerEntity
import com.example.network.AiRecommendation
import com.example.viewmodel.BookingStep
import com.example.viewmodel.DashboardTab
import com.example.viewmodel.LuxeViewModel
import com.example.viewmodel.NotificationModel
import java.text.SimpleDateFormat
import java.util.*

// --- FROSTED GLASS THEME EXTENSIONS ---
fun Modifier.frostedMeshBackground(isDark: Boolean): Modifier = this.drawBehind {
    if (isDark) {
        // Dark Theme Frosted Glass Background
        drawRect(color = Color(0xFF05111B))

        // Neon Blue Spot top-left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3B005FB0), Color.Transparent),
                center = Offset(-size.width * 0.1f, -size.height * 0.1f),
                radius = size.width * 1.1f
            ),
            radius = size.width * 1.1f,
            center = Offset(-size.width * 0.1f, -size.height * 0.1f)
        )

        // Neon Purple Spot bottom-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x337C4DFF), Color.Transparent),
                center = Offset(size.width * 1.1f, size.height * 1.1f),
                radius = size.width * 1.2f
            ),
            radius = size.width * 1.2f,
            center = Offset(size.width * 1.1f, size.height * 1.1f)
        )
    } else {
        // Light Theme Frosted Ice Background
        drawRect(color = Color(0xFFEBF4FC))

        // Soft sky blue bubble top-left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x4FB3E5FF), Color.Transparent),
                center = Offset(-size.width * 0.1f, -size.height * 0.1f),
                radius = size.width * 1.0f
            ),
            radius = size.width * 1.0f,
            center = Offset(-size.width * 0.1f, -size.height * 0.1f)
        )

        // Soft lilac bubble bottom-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x28E0C3FC), Color.Transparent),
                center = Offset(size.width * 1.1f, size.height * 1.1f),
                radius = size.width * 1.1f
            ),
            radius = size.width * 1.1f,
            center = Offset(size.width * 1.1f, size.height * 1.1f)
        )
    }
}

fun Modifier.glassmorphicCard(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    isDark: Boolean = true
): Modifier = this
    .clip(shape)
    .background(
        if (isDark) Color(0x13FFFFFF) else Color(0xC6FFFFFF) // ultra-thin transparent glass overlay on dark
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.05f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.5f),
                    Color.White.copy(alpha = 0.15f)
                )
            }
        ),
        shape = shape
    )

@Composable
fun MainLuxeApp(viewModel: LuxeViewModel) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    MyApplicationTheme(darkTheme = isDarkMode) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .frostedMeshBackground(isDark = isDarkMode),
            color = Color.Transparent
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !onboardingCompleted -> {
                        OnboardingScreen(onFinish = { viewModel.completeOnboarding() })
                    }
                    !isLoggedIn -> {
                        LoginScreen(viewModel = viewModel)
                    }
                    else -> {
                        DashboardScreen(viewModel = viewModel)
                    }
                }

                // Push notifications overlay
                NotificationOverlay(viewModel = viewModel)
            }
        }
    }
}

// --- ONBOARDING VIEW ---
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Luxury Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FlightTakeoff,
                contentDescription = "AeroLuxe Emblem",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 8.dp)
            )
            Text(
                text = "A E R O L U X E",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Beautiful Graphic & description content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "OnboardingStep"
            ) { targetStep ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Luxurious Icon Ring representation
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .drawBehind {
                                drawCircle(
                                    color = DarkColorsSchemeCustom.GoldColor.copy(alpha = 0.15f),
                                    radius = size.width / 2f
                                )
                                drawCircle(
                                    color = DarkColorsSchemeCustom.GoldColor,
                                    radius = size.width / 2.2f,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (targetStep) {
                                0 -> Icons.Default.Star
                                1 -> Icons.Default.AirlineSeatReclineExtra
                                else -> Icons.Default.Language
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = when (targetStep) {
                            0 -> "Unparalleled Voyages"
                            1 -> "Immersive Spaces"
                            else -> "Smart Travel Concierge"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (targetStep) {
                            0 -> "Gain instant reservation clearance to executive lounges, private taxi lanes, and first-class cabins across elite global airlines."
                            1 -> "Select custom micro-climate lay-flat bedroom suites. Taste culinary menus designed dynamically by Michelin starred chefs."
                            else -> "Our intelligent concierge manages price adjustments, real-time gate notifications offline, and yields tailored destinations."
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }

        // Stepper Bullets and Next triggers
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                for (i in 0..2) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (step == i) 14.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (step == i) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (step < 2) {
                        step++
                    } else {
                        onFinish()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (step == 2) "ENTER AEROLUXE" else "CONTINUE",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// --- SIGN IN SCREEN ---
@Composable
fun LoginScreen(viewModel: LuxeViewModel) {
    val isFirebaseAvailable by viewModel.isFirebaseAvailable.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()

    var isSignUpMode by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("Sir Alistair Thorne") }
    var email by remember { mutableStateOf("alistair.thorne@chamber.org") }
    var password by remember { mutableStateOf("aeroluxe123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passport by remember { mutableStateOf("GBR950183") }
    var flyer by remember { mutableStateOf("EK-8840291") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High-Security Status Indicator Badge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(if (isFirebaseAvailable) Color(0x1B00E676) else Color(0x1BFFD600))
                .border(
                    width = 1.dp,
                    color = if (isFirebaseAvailable) Color(0x6600E676) else Color(0x66FFD600),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isFirebaseAvailable) Color(0xFF00E676) else Color(0xFFFFD600))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isFirebaseAvailable) "SECURE CLOUD ENGINE ACTIVE" else "DEMO SANDBOX ACTIVE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFirebaseAvailable) Color(0xFF00E676) else Color(0xFFFFD600),
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Brand Shield
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlightTakeoff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AeroLuxe Terminal",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Initiate your premium private travel journey",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Switcher Tabs in standard elegant AeroLuxe layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isSignUpMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.Transparent)
                    .clickable { isSignUpMode = false }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Secure Sign In",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (!isSignUpMode) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSignUpMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.Transparent)
                    .clickable { isSignUpMode = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Register Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isSignUpMode) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Text Fields Card for Inputs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphicCard(isDark = true)
                .padding(20.dp)
        ) {
            Column {
                if (isSignUpMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("username_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Premium Email Access", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.secondary) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Secured Passphrase", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.secondary) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isSignUpMode) 12.dp else 0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    singleLine = true
                )

                if (isSignUpMode) {
                    OutlinedTextField(
                        value = passport,
                        onValueChange = { passport = it },
                        label = { Text("Passport Number", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = flyer,
                        onValueChange = { flyer = it },
                        label = { Text("Frequent Flyer ID (Optional)", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.secondary) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Informative Sandbox Advice for Prototype Credentials
        if (!isFirebaseAvailable) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "💡 Configure FIREBASE_API_KEY in AI Studio's Secrets panel to link with a live Cloud Auth database.",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Custom error warning display
        authError?.let { err ->
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x11FF1744)),
                border = BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFF1744), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(err, fontSize = 12.sp, color = Color(0xFFFF8A80), modifier = Modifier.weight(1f))
                }
            }
        }

        // Biometric Quick secure bypass
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .clickable {
                    viewModel.completeLogin("Sir Alistair Thorne", "alistair.thorne@chamber.org", "GBR950183", "EK-8840291")
                }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Biometric Clearance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Instant Sandbox Bypass (Development)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.secondary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isAuthLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(20.dp))
        }

        Button(
            onClick = {
                if (isSignUpMode) {
                    viewModel.signUpWithEmailPassword(email, password, name, passport, flyer)
                } else {
                    viewModel.signInWithEmailPassword(email, password)
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isAuthLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_button")
        ) {
            Text(
                text = if (isSignUpMode) "REGISTER & DEPLOY PROFILE" else "SECURE LOGIN",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Visual layout helpers
        Text("Or secure credentials with", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SocialLoginButton(text = "Google Service", icon = Icons.Default.AccountCircle) {
                viewModel.completeLogin("Alex Mercer", "alex.mercer@gmail.com", "USG90123", "QR-239103")
            }
            SocialLoginButton(text = "Apple Account", icon = Icons.Default.Lock) {
                viewModel.completeLogin("Baroness Sophia", "sophia@crown.gb", "GBR102030", "LH-1234")
            }
        }
    }
}

@Composable
fun SocialLoginButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// --- MAIN DASHBOARD SCREEN ---
@Composable
fun DashboardScreen(viewModel: LuxeViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = if (isDarkMode) Color(0x16FFFFFF) else Color(0xD0F1F5F9),
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == DashboardTab.EXPLORE,
                    onClick = { viewModel.setTab(DashboardTab.EXPLORE) },
                    icon = { Icon(Icons.Default.Language, "Explore Deals") },
                    label = { Text("Explore", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == DashboardTab.SEARCH,
                    onClick = { viewModel.setTab(DashboardTab.SEARCH) },
                    icon = { Icon(Icons.Default.Search, "Book Flights") },
                    label = { Text("Book", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == DashboardTab.MY_TRIPS,
                    onClick = { viewModel.setTab(DashboardTab.MY_TRIPS) },
                    icon = { Icon(Icons.Default.LocalMall, "My Trips") },
                    label = { Text("My Trips", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == DashboardTab.SUPPORT,
                    onClick = { viewModel.setTab(DashboardTab.SUPPORT) },
                    icon = { Icon(Icons.Default.ChatBubbleOutline, "AI Concierge") },
                    label = { Text("Concierge", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == DashboardTab.PROFILE,
                    onClick = { viewModel.setTab(DashboardTab.PROFILE) },
                    icon = { Icon(Icons.Default.Person, "Profile Settings") },
                    label = { Text("Profile", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                DashboardTab.EXPLORE -> ExploreTabContent(viewModel = viewModel)
                DashboardTab.SEARCH -> SearchTabContent(viewModel = viewModel)
                DashboardTab.MY_TRIPS -> MyTripsTabContent(viewModel = viewModel)
                DashboardTab.SUPPORT -> SupportTabContent(viewModel = viewModel)
                DashboardTab.PROFILE -> ProfileTabContent(viewModel = viewModel)
            }
        }
    }
}

// --- OPTION C: NOTIFICATION OVERLAYS ---
@Composable
fun NotificationOverlay(viewModel: LuxeViewModel) {
    val alerts by viewModel.activeNotifications.collectAsStateWithLifecycle()
    if (alerts.isNotEmpty()) {
        val banner = alerts.first()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.dismissNotification(banner.id) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = banner.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = banner.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    }
                    IconButton(
                        onClick = { viewModel.dismissNotification(banner.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// --- TAB CONTENT: EXPLORE ---
@Composable
fun ExploreTabContent(viewModel: LuxeViewModel) {
    val selectedCategory by viewModel.selectedExploreCategory.collectAsStateWithLifecycle()
    val recommendations by viewModel.aiSuggestions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAiSuggestionsLoading.collectAsStateWithLifecycle()
    val currency by viewModel.selectedCurrency.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome and Miles counter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AeroLuxe Elite Class",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Good day, traveler",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // VIP Elite Rank badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "GOLD ELITE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Miles Accrued card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("LOYALTY CREDITS", fontSize = 10.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.secondary)
                            Text("64,250 Miles", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            Icons.Default.CardMembership,
                            null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Linear progress bar
                    LinearProgressIndicator(
                        progress = { 0.64f },
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Companion Voucher: Active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Text("35,750 miles to Platinum Elite", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("RECOMMENDED EXCLUSIVES", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Powered by Gemini 3.5 AI dynamic orchestration", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs for explore themes
            val categories = listOf("Romantic", "Adventure", "Eco-Luxe", "Wellness")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { viewModel.fetchExploreSuggestions(category) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Decompressing AI schedules...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }
        } else {
            items(recommendations) { suggestion ->
                AiSuggestionCard(suggestion = suggestion, currencySymbol = currency.takeLast(3).removeSuffix(")")) {
                    // Prepopulate search parameter when clicked
                    val targetCode = if (suggestion.city == "Santorini") "ATH" else if (suggestion.city ==  "Kyoto") "HND" else "DXB"
                    viewModel.setSearchQuery(
                        origin = "SIN",
                        destination = targetCode,
                        cabinClass = "Business",
                        isRound = true,
                        dateMils = System.currentTimeMillis() + (7 * 86400 * 1000),
                        passengers = 1
                    )
                    viewModel.setTab(DashboardTab.SEARCH)
                    viewModel.executeFlightSearch()
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Referal and additional promo banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("REF-LINK AMBASSADOR", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text("Invite your acquaintances", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("They receive 1 complimentary runway service, you gain 15,000 miles onboard.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = { /* Share link mockup */ },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("SHARE", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AiSuggestionCard(suggestion: AiRecommendation, currencySymbol: String, onAction: () -> Unit) {
    // Beautiful Glassmorphism suggestion layout matching Airbnb style
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAction)
    ) {
        Column {
            // Simulated gorgeous graphic card header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00A3FF).copy(alpha = 0.35f),
                                Color(0xFF7C4DFF).copy(alpha = 0.35f)
                            )
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = suggestion.tabTag.uppercase(),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${suggestion.city}, ${suggestion.country}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = suggestion.reason,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESTIMATED PRIVATE TICKET", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            text = "${currencySymbol}${suggestion.price}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("DISCOVER FLIGHTS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- TAB CONTENT: FLIGHT SEARCH & BOOKING ---
@Composable
fun SearchTabContent(viewModel: LuxeViewModel) {
    val step by viewModel.bookingStep.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "BookingFlow"
    ) { currentStep ->
        when (currentStep) {
            BookingStep.SEARCH_INPUT -> FlightSearchForm(viewModel = viewModel)
            BookingStep.FLIGHT_RESULTS -> FlightResultsListView(viewModel = viewModel)
            BookingStep.PASSENGER_DETAILS -> PassengerInputView(viewModel = viewModel)
            BookingStep.SEAT_SELECTION -> InteractiveSeatSelectionView(viewModel = viewModel)
            BookingStep.ADD_ONS -> UpgradesAddonsView(viewModel = viewModel)
            BookingStep.PAYMENT -> SecureCheckoutGateway(viewModel = viewModel)
            BookingStep.CONFIRMATION -> BookingReceiptConfirmationView(viewModel = viewModel)
        }
    }
}

@Composable
fun FlightSearchForm(viewModel: LuxeViewModel) {
    var orig by remember { mutableStateOf("SIN") }
    var dest by remember { mutableStateOf("LHR") }
    var isRound by remember { mutableStateOf(true) }
    var cabin by remember { mutableStateOf("Business") }
    var passengers by remember { mutableStateOf(1) }
    
    val predictOrigin by viewModel.predictOriginResults.collectAsStateWithLifecycle()
    val predictDest by viewModel.predictDestResults.collectAsStateWithLifecycle()
    val trendData by viewModel.fareTrends.collectAsStateWithLifecycle()
    val currency by viewModel.selectedCurrency.collectAsStateWithLifecycle()

    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("TRAVEL RESERVATION CENTER", fontSize = 12.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Text("Find Elite Flights", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Glassmorphic Control widget wrapping form controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicCard(isDark = isDarkMode)
                    .padding(20.dp)
            ) {
                Column {
                    // Flight type toggle tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("One Way" to false, "Round Trip" to true).forEach { (label, value) ->
                            val isSel = isRound == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.08f))
                                    .clickable { isRound = value }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dual input Origin & Destination
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Origin
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = orig,
                                onValueChange = {
                                    orig = it
                                    viewModel.setSearchQuery(it, dest, cabin, isRound, System.currentTimeMillis(), passengers)
                                },
                                label = { Text("Departure") },
                                leadingIcon = { Icon(Icons.Default.FlightTakeoff, null, tint = MaterialTheme.colorScheme.secondary) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                singleLine = true,
                                modifier = Modifier.testTag("departure_input")
                            )
                            predictOrigin.firstOrNull()?.let { prediction ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDarkMode) Color(0x33FFFFFF) else Color(0xE6FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable {
                                            orig = prediction.substringAfter("(").substringBefore(")")
                                            viewModel.setSearchQuery(orig, dest, cabin, isRound, System.currentTimeMillis(), passengers)
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text(prediction, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        // Swap Icon
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    val temp = orig
                                    orig = dest
                                    dest = temp
                                    viewModel.setSearchQuery(orig, dest, cabin, isRound, System.currentTimeMillis(), passengers)
                                }
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.CompareArrows, null, tint = MaterialTheme.colorScheme.secondary)
                        }

                        // Destination
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = dest,
                                onValueChange = {
                                    dest = it
                                    viewModel.setSearchQuery(orig, it, cabin, isRound, System.currentTimeMillis(), passengers)
                                },
                                label = { Text("Destination") },
                                leadingIcon = { Icon(Icons.Default.FlightLand, null, tint = MaterialTheme.colorScheme.secondary) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                singleLine = true,
                                modifier = Modifier.testTag("destination_input")
                            )
                            predictDest.firstOrNull()?.let { prediction ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDarkMode) Color(0x33FFFFFF) else Color(0xE6FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable {
                                            dest = prediction.substringAfter("(").substringBefore(")")
                                            viewModel.setSearchQuery(orig, dest, cabin, isRound, System.currentTimeMillis(), passengers)
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text(prediction, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cabin class and passengers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cabin Class
                        var showCabinMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showCabinMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Class: $cabin", color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onBackground)
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            DropdownMenu(expanded = showCabinMenu, onDismissRequest = { showCabinMenu = false }) {
                                listOf("Economy", "Premium", "Business", "First Class").forEach { cls ->
                                    DropdownMenuItem(text = { Text(cls) }, onClick = {
                                        cabin = cls
                                        showCabinMenu = false
                                        viewModel.setSearchQuery(orig, dest, cls, isRound, System.currentTimeMillis(), passengers)
                                    })
                                }
                            }
                        }

                        // Passengers Increment
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (passengers > 1) { passengers--; viewModel.setSearchQuery(orig, dest, cabin, isRound, System.currentTimeMillis(), passengers) } }) {
                                Icon(Icons.Default.Remove, null, tint = Color.White)
                            }
                            Text("$passengers Traveler${if (passengers > 1) "s" else ""}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onBackground)
                            IconButton(onClick = { if (passengers < 9) { passengers++; viewModel.setSearchQuery(orig, dest, cabin, isRound, System.currentTimeMillis(), passengers) } }) {
                                Icon(Icons.Default.Add, null, tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.executeFlightSearch() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("search_flights_submit")
                    ) {
                        Text("DISCOVER PRIVATE SCHEDULES", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("REAL-TIME FARE ANALYSIS", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("7-Day departure yield and seasonal pricing patterns ($orig to $dest)", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

            Spacer(modifier = Modifier.height(16.dp))

            // Golden Price Trend line Chart on custom Canvas
            FareTrendLineChart(origin = orig, destination = dest, currencySymbol = currency.takeLast(3).removeSuffix(")"))
        }
    }
}

@Composable
fun FareTrendLineChart(origin: String, destination: String, currencySymbol: String) {
    val items: List<Pair<String, Double>> = remember(origin, destination) {
        FlightSearchEngine.getFareTrendData(origin, destination)
    }

    val itemCount = items.size

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .glassmorphicCard(shape = RoundedCornerShape(12.dp), isDark = true)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (items.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val spacing = width / (itemCount - 1)

            val pricesList = items.map { it.second }
            val minPrice = (pricesList.minOrNull() ?: 100.0) * 0.9
            val maxPrice = (pricesList.maxOrNull() ?: 1000.0) * 1.1
            val priceRange = maxPrice - minPrice

            val points: List<Offset> = items.mapIndexed { idx, pair ->
                val price = pair.second
                val x = idx * spacing
                val ratio = if (priceRange != 0.0) ((price - minPrice) / priceRange) else 0.5
                val y = height - (ratio.toFloat() * height)
                Offset(x, y)
            }

            val path = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
            }

            // Fill under path
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkColorsSchemeCustom.GoldColor.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            )

            drawPath(
                path = path,
                color = DarkColorsSchemeCustom.GoldColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw points
            points.forEach { point ->
                drawCircle(
                    color = DarkColorsSchemeCustom.GoldColor,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }

        // Layout day axes labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (item in items) {
                val label = item.first
                val price = item.second
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("${currencySymbol}${price.toInt()}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

// Custom luxury static color palette for helper rendering
object DarkColorsSchemeCustom {
    val GoldColor = Color(0xFFD4AF37)
    val GreenColor = Color(0xFF007A78)
}

// --- VIEW: RESULTS VIEW ---
@Composable
fun FlightResultsListView(viewModel: LuxeViewModel) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val sortBy by viewModel.resultsSortBy.collectAsStateWithLifecycle()
    val orig by viewModel.searchOrigin.collectAsStateWithLifecycle()
    val dest by viewModel.searchDestination.collectAsStateWithLifecycle()
    val cabinClass by viewModel.searchCabinClass.collectAsStateWithLifecycle()
    val currency by viewModel.selectedCurrency.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper action bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.resetBookingFlow() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to search parameters")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$orig to $dest", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(cabinClass, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = { viewModel.executeFlightSearch() }) {
                Icon(Icons.Default.Refresh, "Refresh flights")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sort pill choices
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort by:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            listOf("Price", "Duration", "Eco").forEach { filterType ->
                val isSel = sortBy == filterType
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewModel.setResultsSort(filterType) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        filterType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchResults.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No private jets match this route on selected date.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults) { flight ->
                    FlightItemCard(flight = flight, currencySymbol = currency.takeLast(3).removeSuffix(")")) {
                        viewModel.selectFlight(flight)
                    }
                }
            }
        }
    }
}

@Composable
fun FlightItemCard(flight: Flight, currencySymbol: String, onSelect: () -> Unit) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.US) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Airline logo placeholder and Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Custom decorative circle using airline initials
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            flight.airlineCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00A3FF)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(flight.airlineName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(flight.flightNo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Text(
                    text = "${currencySymbol}${flight.price}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timings row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(formatter.format(Date(flight.departureTime)), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(flight.departureAirportCode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                }

                // Journey indicators
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("${flight.durationMinutes / 60}h ${flight.durationMinutes % 60}m", fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(flight.stopsInfo, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(formatter.format(Date(flight.arrivalTime)), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(flight.arrivalAirportCode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Micro badges footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Eco emblem
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Eco, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("🌱 -${flight.ecoImpactPercentage}% CO2", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }

                // Baggage indicator
                Text(text = flight.baggagePolicy, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                // Seats left label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (flight.seatAvailability <= 3) Color(0xFFFFEBEE)
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${flight.seatAvailability} suites left",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (flight.seatAvailability <= 3) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// --- VIEW: PASSENGERS DETAIL VIEW ---
@Composable
fun PassengerInputView(viewModel: LuxeViewModel) {
    val userSavedCompanionName by viewModel.passengerNameInput.collectAsStateWithLifecycle()
    val emailVal by viewModel.passengerEmailInput.collectAsStateWithLifecycle()
    val passportVal by viewModel.passengerPassportInput.collectAsStateWithLifecycle()
    val flyerVal by viewModel.passengerFlyerInput.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var passport by remember { mutableStateOf("") }
    var flyer by remember { mutableStateOf("") }

    LaunchedEffect(userSavedCompanionName) {
        name = userSavedCompanionName
        email = emailVal
        passport = passportVal
        flyer = flyerVal
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.executeFlightSearch() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to results")
            }
            Text("PASSENGER DOSSIER", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Registered Passenger", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Verify credentials coordinate exactly with passport identifiers for boarding entry clearance.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Passenger Full Name") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("passenger_name_input")
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Contact Address") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = passport,
            onValueChange = { passport = it },
            label = { Text("Passport Number") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = flyer,
            onValueChange = { flyer = it },
            label = { Text("Frequent Flyer Account ID") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )

        Button(
            onClick = { viewModel.applyPassengerDetails(name, email, passport, flyer) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("apply_passenger_button")
        ) {
            Text("CONFIRM PASSENGER & CHOOSE SEATING", fontWeight = FontWeight.Bold)
        }
    }
}

// --- VIEW: DETAILED INTERACTIVE SEAT SELECTION ---
@Composable
fun InteractiveSeatSelectionView(viewModel: LuxeViewModel) {
    val selectedFlight by viewModel.selectedFlight.collectAsStateWithLifecycle()
    val seatSelected by viewModel.selectedSeatCode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.resetBookingFlow() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back block")
            }
            Text("SUITE ARRANGEMENT MAP", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Aircraft: Boeing 777-9 Luxury Suite Cabin Configuration", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(16.dp))

        // Grid indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(DarkColorsSchemeCustom.GoldColor))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Your Suite", fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(DarkColorsSchemeCustom.GreenColor))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Available", fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Occupied", fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrolling Cabin Seat Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✈️ NOSE OF AIRCRAFT", fontSize = 10.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(16.dp))

                // We display 8 executive rows
                for (row in 1..8) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Left block (A, B)
                        listOf("A", "B").forEach { col ->
                            val sCode = "$row$col"
                            val isOccupied = selectedFlight?.seatLayout?.get(sCode) ?: false
                            val isChosen = seatSelected == sCode

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isChosen -> DarkColorsSchemeCustom.GoldColor
                                            isOccupied -> Color.Gray.copy(alpha = 0.4f)
                                            else -> DarkColorsSchemeCustom.GreenColor
                                        }
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .clickable(enabled = !isOccupied) {
                                        viewModel.selectSeat(sCode)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sCode, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Isle gap
                        Spacer(modifier = Modifier.width(24.dp))
                        Text("$row", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(24.dp))

                        // Right block (E, F)
                        listOf("E", "F").forEach { col ->
                            val sCode = "$row$col"
                            val isOccupied = selectedFlight?.seatLayout?.get(sCode) ?: false
                            val isChosen = seatSelected == sCode

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isChosen -> DarkColorsSchemeCustom.GoldColor
                                            isOccupied -> Color.Gray.copy(alpha = 0.4f)
                                            else -> DarkColorsSchemeCustom.GreenColor
                                        }
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .clickable(enabled = !isOccupied) {
                                        viewModel.selectSeat(sCode)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sCode, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected seat display & Confirm button
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SELECTED POSITION", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                    Text("Suite Suite Suite $seatSelected", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Button(
                    onClick = { viewModel.proceedToAddons() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("COORDINATE UPGRADES", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- VIEW: UPGRADES AND ADDONS ---
@Composable
fun UpgradesAddonsView(viewModel: LuxeViewModel) {
    val hasBaggage by viewModel.addonBaggage.collectAsStateWithLifecycle()
    val hasMeal by viewModel.addonMeal.collectAsStateWithLifecycle()
    val hasInsurance by viewModel.addonInsurance.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.resetBookingFlow() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back flow")
            }
            Text("ELITE COMPLIMENTS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Text("Select extra high-end accommodations to enrich your private travel itinerary", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(20.dp))

        // Addon: Baggage
        AddonSelectionCard(
            title = "Extra Checked Luggage Clearance",
            priceText = "+$50.00",
            description = "Injects 1 extra 32kg checked suitcase container allowance (Total 4 pieces enabled).",
            isSelected = hasBaggage,
            icon = Icons.Default.Inventory
        ) {
            viewModel.toggleBaggage()
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Addon: Dining
        AddonSelectionCard(
            title = "Caviar & Lobster Dining Flight Plan",
            priceText = "+$45.00",
            description = "Michelin star certified course featuring authentic chilled Sturgeon Caviar and roasted deep-sea lobster with champagne pairings.",
            isSelected = hasMeal,
            icon = Icons.Default.Restaurant
        ) {
            viewModel.toggleMeal()
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Addon: Insurance
        AddonSelectionCard(
            title = "Elite Executive Insurance",
            priceText = "+$29.00",
            description = "Provides immediate zero-fee cancellations, infinite multi-flight disruptions rescheduling, and luggage priority restitution guarantees.",
            isSelected = hasInsurance,
            icon = Icons.Default.VerifiedUser
        ) {
            viewModel.toggleInsurance()
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.proceedToPayment() },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("proceed_to_payment")
        ) {
            Text("PROCEED TO SECURE CHECKOUT", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddonSelectionCard(
    title: String,
    priceText: String,
    description: String,
    isSelected: Boolean,
    icon: ImageVector,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0x3B00A3FF) else Color(0x13FFFFFF)
        ),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00A3FF) else Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.interactiveIconLightSystem(),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(priceText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), lineHeight = 16.sp)
            }
        }
    }
}

// Helper to determine the theme's icon color
@Composable
fun ColorScheme.interactiveIconLightSystem(): Color {
    return if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
}

// --- VIEW: SECURE MULTI-METHOD PAYMENT GATEWAY ---
@Composable
fun SecureCheckoutGateway(viewModel: LuxeViewModel) {
    val totalCost = viewModel.calculateTotalCost()
    val promoMessage = viewModel.promoStatusMessage.collectAsStateWithLifecycle()
    val currency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val symbol = currency.takeLast(3).removeSuffix(")")
    var codeText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("APPLE") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.resetBookingFlow() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("SECURE GATEWAY CHECKOUT", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total checkout ticket invoice receipt
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOTAL PREMIUM CLEARANCE DUE", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp)
                Text("${symbol}${totalCost}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(14.dp))

                // Promo Coupon Code entry
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { codeText = it },
                        label = { Text("Enter Promo Code") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Button(
                        onClick = { viewModel.applyPromo(codeText) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("APPLY")
                    }
                }

                if (promoMessage.value.isNotEmpty()) {
                    Text(
                        text = promoMessage.value,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("SELECT PAYMENT PLATFORM", fontSize = 14.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        // Payment selections
        listOf(
            "APPLE" to "Apple Pay",
            "GOOGLE" to "Google Pay",
            "VISA" to "Executive Priority Card (**** 4102)"
        ).forEach { (methodId, label) ->
            val isChosen = selectedMethod == methodId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (isChosen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { selectedMethod = methodId }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (methodId == "VISA") Icons.Default.CreditCard else Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                RadioButton(selected = isChosen, onClick = { selectedMethod = methodId })
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.completeBooking(selectedMethod) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_payment_button")
        ) {
            Text("AUTHORISE WIRE TRANSFER", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp)
        }
    }
}

// --- VIEW: COMPLETE RESERVATION CONFIRMATION CARD ---
@Composable
fun BookingReceiptConfirmationView(viewModel: LuxeViewModel) {
    val details by viewModel.latestConfirmedBooking.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Checked Success Emblem
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(DarkColorsSchemeCustom.GreenColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("RESERVATION AUTHORIZED", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("Your elite luxury itinerary is successfully issued.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

        Spacer(modifier = Modifier.height(24.dp))

        // Receipt Card layout
        details?.let { booking ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("BOOKING ID", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text(booking.bookingCode, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ROUTE DETAILS", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text("${booking.departureCity} to ${booking.arrivalCity}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Executive Class Suite ${booking.seatNumber} | Cabin ${booking.cabinClass}", fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Representing barcode with Custom Drawings on Canvas
                    Text("MOCK BOARDING QR / BARCODE", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color.White)
                    ) {
                        // Drawing barcode lines
                        val barWidth = 3f
                        var currentX = 10f
                        val rand = Random(booking.bookingCode.hashCode().toLong())
                        while (currentX < size.width - 10f) {
                            val lineThickness = if (rand.nextBoolean()) barWidth * 2 else barWidth
                            val isBlank = rand.nextBoolean()
                            if (!isBlank) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(currentX, 0f),
                                    size = Size(lineThickness, size.height)
                                )
                            }
                            currentX += lineThickness + 4f
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    // Mock invoice URI load trigger
                    uriHandler.openUri("https://ais-dev-oncdka77x7s4dkykb3lepv-239999801649.asia-east1.run.app")
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            ) {
                Text("DOWNLOAD INVOICE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.resetBookingFlow() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f)
            ) {
                Text("FINISHED", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- TAB CONTENT: MY TRIPS ARCHIVE ENGINE ---
@Composable
fun MyTripsTabContent(viewModel: LuxeViewModel) {
    val bookings by viewModel.allBookings.collectAsStateWithLifecycle()
    var selectedFilterUpcoming by remember { mutableStateOf(true) }

    val filtered = bookings.filter {
        if (selectedFilterUpcoming) it.status == "UPCOMING"
        else it.status == "COMPLETED" || it.status == "CANCELLED"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("MANAGEMENT SUITE", fontSize = 12.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Text("My Travel Archive", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

        Spacer(modifier = Modifier.height(16.dp))

        // Upcoming/Archive switch tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Scheduled Trips" to true, "Completed & Past" to false).forEach { (label, value) ->
                val isSel = selectedFilterUpcoming == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { selectedFilterUpcoming = value }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CardTravel, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No matching trips on this roster.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filtered) { record ->
                    TravelBookingCard(booking = record, onCancel = {
                        viewModel.cancelExistingBooking(record)
                    }, onRescheduleSeat = { newSeat ->
                        viewModel.rescheduleSeat(record, newSeat)
                    })
                }
            }
        }
    }
}

@Composable
fun TravelBookingCard(booking: BookingEntity, onCancel: () -> Unit, onRescheduleSeat: (String) -> Unit) {
    val formatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.US) }
    var showRescheduleDialog by remember { mutableStateOf(false) }
    var inputSuiteText by remember { mutableStateOf(booking.seatNumber) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // High header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(booking.airlineCode, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = booking.flightNo, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }

                // High fidelity Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (booking.status) {
                                "UPCOMING" -> Color(0xFFC8E6C9)
                                "COMPLETED" -> Color(0xFFB3E5FC)
                                else -> Color(0xFFFFCDD2) // Cancelled
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = booking.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (booking.status) {
                            "UPCOMING" -> Color(0xFF1B5E20)
                            "COMPLETED" -> Color(0xFF0277BD)
                            else -> Color(0xFFB71C1C)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dep Air - Arr Air
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(booking.departureCity, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(booking.departureAirportCode, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Icon(Icons.Default.ArrowForward, "to", tint = MaterialTheme.colorScheme.secondary)
                Column(horizontalAlignment = Alignment.End) {
                    Text(booking.arrivalCity, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(booking.arrivalAirportCode, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timings info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("DEPARTURE DATE", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(formatter.format(Date(booking.departureTime)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SUITE SUITE SEAT", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(booking.seatNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }

            // Controls actions display if upcoming
            if (booking.status == "UPCOMING") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showRescheduleDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("RESCHEDULE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onCancel,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL TRIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showRescheduleDialog) {
        AlertDialog(
            onDismissRequest = { showRescheduleDialog = false },
            title = { Text("Reschedule Suite Seating Position") },
            text = {
                Column {
                    Text("Adjust seat identifier code currently saved on boarding register (e.g., 04A, 12F)")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputSuiteText,
                        onValueChange = { inputSuiteText = it },
                        label = { Text("New Suite Seat Number") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRescheduleSeat(inputSuiteText)
                    showRescheduleDialog = false
                }) {
                    Text("CONFIRM")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescheduleDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

// --- TAB CONTENT: SUPPORT CORNER CONCIERGE CHATBOT ---
@Composable
fun SupportTabContent(viewModel: LuxeViewModel) {
    val history by viewModel.chatHistory.collectAsStateWithLifecycle()
    val textVal by viewModel.chatInput.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatbotResponding.collectAsStateWithLifecycle()

    val recordsFaqs = remember {
        listOf(
            "What is the luggage allowance policy?" to "First Class accommodates 3 free checked luggage units (32kg each) plus dual luxury cabin bags. Business includes 2 checked units. All basic flights allow 1 carry-on piece.",
            "Can I apply multi-currency or rescheduling?" to "Yes. AeroLuxe is fully compatible with dynamic currency conversions inside profile settings. reschedule or cancellation is complimentary across elite cabin tiers.",
            "What amenities are provided in lounges?" to "Our global elite partner lounges boast luxury sleep chambers, caviar dining, private work suites with gigabit Wi-Fi, spa therapy, and chauffeured runway pick-ups."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("TRAVEL CONCIERGE", fontSize = 12.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Text("AI Support Center", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

        Spacer(modifier = Modifier.height(16.dp))

        // Dual sub elements: FAQs or Chatbot
        var chatPanelEnabled by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Interactive FAQs" to false, "AI Concierge Chat" to true).forEach { (label, value) ->
                val isSel = chatPanelEnabled == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { chatPanelEnabled = value }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!chatPanelEnabled) {
            // Expandable FAQs
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordsFaqs) { (q, a) ->
                    FaqAccordionItem(question = q, answer = a)
                }
            }
        } else {
            // Chat box dialog scrolling log
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(bottom = 8.dp)
                ) {
                    history.forEach { (msg, isUser) ->
                        LayoutChatBubble(text = msg, isMe = isUser)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (isChatLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Concierge is typing...", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                // Chat Input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textVal,
                        onValueChange = { viewModel.updateChatInput(it) },
                        placeholder = { Text("Inquire about bookings...") },
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.weight(1f).testTag("chat_input_field"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.sendChatMessage() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .testTag("chat_send_button")
                    ) {
                        Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun FaqAccordionItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(question, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(answer, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun LayoutChatBubble(text: String, isMe: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 0.dp,
                        bottomEnd = if (isMe) 0.dp else 16.dp
                    )
                )
                .background(
                    if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

// --- TAB CONTENT: USER PROFILE & ANALYTICAL GRAPH ---
@Composable
fun ProfileTabContent(viewModel: LuxeViewModel) {
    val name by viewModel.userName.collectAsStateWithLifecycle()
    val email by viewModel.userEmail.collectAsStateWithLifecycle()
    val passport by viewModel.userPassport.collectAsStateWithLifecycle()
    val flyer by viewModel.userFlyerNumber.collectAsStateWithLifecycle()

    val currencySelected by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val languageSelected by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val listTravelers by viewModel.savedTravelers.collectAsStateWithLifecycle()

    val isFirebaseAvailable by viewModel.isFirebaseAvailable.collectAsStateWithLifecycle()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsStateWithLifecycle()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsStateWithLifecycle()

    var showCompanionDialog by remember { mutableStateOf(false) }
    var compName by remember { mutableStateOf("") }
    var compEmail by remember { mutableStateOf("") }
    var compPass by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper Header elements
        item {
            Text("ACCOUNT ADMINISTRATION", fontSize = 12.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Text("Traveler Profile", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.height(16.dp))

            // Cloud Firestore Database Console Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isFirebaseAvailable) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CLOUD FIRESTORE DATABASE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isFirebaseAvailable) "Federated Security & Storage" else "Offline Local Environment",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isFirebaseAvailable) Color(0xFF00E676) else Color(0xFFFFD600))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFirebaseAvailable) "ONLINE" else "SANDBOX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFirebaseAvailable) Color(0xFF00E676) else Color(0xFFFFD600)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Sync status: $firestoreSyncStatus",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isFirebaseAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncAllToCloud() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isFirestoreSyncing,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isFirestoreSyncing) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("UPLOAD DB", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { viewModel.syncAllFromCloud() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isFirestoreSyncing,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("DOWNLOAD DB", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Secure transactions and automated real-time backups are disabled because Firebase is not initialized. Please connect a database via Google AI Studio's Secrets panel using FIREBASE_API_KEY parameters to unlock.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Traveler analytic info chart
            Text("TRAVEL STATS ANALYTICS", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileAnalyticsChartWidget()

            Spacer(modifier = Modifier.height(20.dp))

            // Editable Dossier UserCard
            Text("TRAVEL RECORD DOSSIER", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Passport: $passport", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("Corporate Email: $email", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("Frequent Flyer Account: $flyer", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Saved Companions Manager
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("COMPANIONS MANAGER", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showCompanionDialog = true }) {
                    Text("+ ADD NEW")
                }
            }
        }

        if (listTravelers.isEmpty()) {
            item {
                Text("No saved traveling companions.", fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
            }
        } else {
            items(listTravelers) { companion ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(companion.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Passport: ${companion.passportNumber} | FF: ${companion.frequentFlyerNumber}", fontSize = 10.sp)
                        }
                        IconButton(onClick = { viewModel.deleteSavedTraveler(companion) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        // System Settings custom dropdown panel
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("SYSTEM CONFIGURATIONS", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Currency Row Selector
            var showCurrMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCurrMenu = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Primary Currency Type", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currencySelected, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }

                DropdownMenu(expanded = showCurrMenu, onDismissRequest = { showCurrMenu = false }) {
                    listOf("USD ($)", "EUR (€)", "GBP (£)").forEach { curr ->
                        DropdownMenuItem(text = { Text(curr) }, onClick = {
                            viewModel.updateSettings(curr, languageSelected)
                            showCurrMenu = false
                        })
                    }
                }
            }

            // Language Row Selector
            var showLangMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLangMenu = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Select Language System", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(languageSelected, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }

                DropdownMenu(expanded = showLangMenu, onDismissRequest = { showLangMenu = false }) {
                    listOf("English (US)", "French (FR)", "Japanese (JP)", "Arabic (AE)").forEach { lang ->
                        DropdownMenuItem(text = { Text(lang) }, onClick = {
                            viewModel.updateSettings(currencySelected, lang)
                            showLangMenu = false
                        })
                    }
                }
            }

            // Dark styling toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Luxurious Slate Dark Scheme", fontSize = 14.sp)
                Switch(
                    checked = viewModel.isDarkMode.collectAsStateWithLifecycle().value,
                    onCheckedChange = { viewModel.toggleDarkMode() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("DISCONNCT ELITE TRAVEL ACCOUNT", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCompanionDialog) {
        AlertDialog(
            onDismissRequest = { showCompanionDialog = false },
            title = { Text("Add Traveling Companion Card") },
            text = {
                Column {
                    OutlinedTextField(value = compName, onValueChange = { compName = it }, label = { Text("Companion Full Name") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = compEmail, onValueChange = { compEmail = it }, label = { Text("Companion Email Address") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = compPass, onValueChange = { compPass = it }, label = { Text("Companion Passport Number") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (compName.isNotEmpty() && compPass.isNotEmpty()) {
                        viewModel.saveNewTraveler(compName, compEmail, compPass, "")
                        compName = ""
                        compEmail = ""
                        compPass = ""
                        showCompanionDialog = false
                    }
                }) {
                    Text("SAVE REC")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompanionDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun ProfileAnalyticsChartWidget() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Chart
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .drawBehind {
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.15f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )
                        drawArc(
                            color = DarkColorsSchemeCustom.GoldColor,
                            startAngle = -90f,
                            sweepAngle = 260f, // 72%
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("18", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Trips", fontSize = 9.sp)
                }
            }

            // Bullet Legends
            Column(verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(DarkColorsSchemeCustom.GoldColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("First Class Travel Class: 13 Flights", fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(DarkColorsSchemeCustom.GreenColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Business Category: 5 Flights", fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.4f)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Economy Class: 0 Flights", fontSize = 11.sp)
                }
            }
        }
    }
}
