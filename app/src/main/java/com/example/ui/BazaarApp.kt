package com.example.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.RazorpayResult
import com.example.data.CartItem
import com.example.data.Order
import com.example.data.Product
import com.example.data.Banner
import com.example.data.Coupon
import com.example.data.Shortcut
import com.example.data.calculateDeliveryCharge
import com.example.data.estimateDeliveryDistanceKm
import com.example.data.estimateItemAmountFromOrderTotal
import com.example.ui.theme.*
import com.example.viewmodel.AuthState
import com.example.viewmodel.BazaarViewModel
import com.razorpay.Checkout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

private const val HELP_WHATSAPP_NUMBER = "919101785067"
private const val HELP_DISPLAY_MOBILE = "9101785067"
private const val HELP_EMAIL = "zylvorbazaar@gmail.com"

// Simple sealed hierarchy for Screens
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object Seller : Screen("seller")
    object DeliveryPartner : Screen("delivery_partner")
}

// Simple Bottom Bar Tabs
enum class BottomTab(val title: String, val iconFilled: ImageVector, val iconOutlined: ImageVector) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Categories("Categories", Icons.Filled.GridView, Icons.Outlined.GridView),
    Cart("Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    Wishlist("Wishlist", Icons.Filled.Favorite, Icons.Outlined.Favorite),
    Orders("My Orders", Icons.Filled.Assignment, Icons.Outlined.Assignment),
    Profile("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun BazaarApp(activity: MainActivity? = null, viewModel: BazaarViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val splashCompleted by viewModel.splashCompleted.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // Screen navigation flow
    LaunchedEffect(splashCompleted) {
        if (splashCompleted) {
            if (authState is AuthState.Success) {
                val user = (authState as AuthState.Success).user
                currentScreen = when (user.role) {
                    "Seller" -> Screen.Seller
                    "DeliveryPartner" -> Screen.DeliveryPartner
                    else -> Screen.Main
                }
            } else {
                currentScreen = Screen.Login
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success && splashCompleted) {
            val user = (authState as AuthState.Success).user
            currentScreen = when (user.role) {
                "Seller" -> Screen.Seller
                "DeliveryPartner" -> Screen.DeliveryPartner
                else -> Screen.Main
            }
        }
    }

    val newlyRegisteredUser by viewModel.newlyRegisteredUser.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (currentScreen) {
                is Screen.Splash -> SplashScreen(onSplashFinished = { viewModel.completeSplash() })
                is Screen.Login -> LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = { currentScreen = Screen.Register }
                )
                is Screen.Register -> RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = { currentScreen = Screen.Login }
                )
                is Screen.Main -> MainScreen(viewModel = viewModel, activity = activity, onLogout = {
                    viewModel.logout()
                    currentScreen = Screen.Login
                })
                is Screen.Seller -> SellerPanelScreen(viewModel = viewModel, onLogout = {
                    viewModel.logout()
                    currentScreen = Screen.Login
                })
                is Screen.DeliveryPartner -> DeliveryPartnerPanelScreen(viewModel = viewModel, onLogout = {
                    viewModel.logout()
                    currentScreen = Screen.Login
                })
            }

            newlyRegisteredUser?.let { user ->
                RegistrationWelcomeOverlay(
                    name = user.name,
                    role = user.role,
                    onDismiss = { viewModel.clearNewlyRegistered() }
                )
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN (Animated)
// ==========================================
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var animateLogo by remember { mutableStateOf(false) }
    var slideDetails by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateLogo = true
        delay(600)
        slideDetails = true
        delay(2200)
        onSplashFinished()
    }

    val scaleScale by animateTransition(target = if (animateLogo) 1.1f else 0.4f, duration = 1200)
    val alphaAnim by animateTransition(target = if (slideDetails) 1.0f else 0.0f, duration = 1000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(CustomWhite, LightGreenSecondary, CustomWhite)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated App Logo wrapper (Generated app bag icon style)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scaleScale)
                    .shadow(12.dp, shape = CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(MintAccent, DarkGreenPrimary)
                        ),
                        shape = CircleShape
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon_1782139846344),
                    contentDescription = "ZYL VOR BAZAAR Logo",
                    modifier = Modifier.fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Text Typography App Title
            Text(
                text = "ZYL VOR BAZAAR",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkGreenPrimary,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "The Premium Green Marketplace",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MutedText
                ),
                modifier = Modifier.alpha(0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated core features slider
            Column(
                modifier = Modifier
                    .alpha(alphaAnim)
                    .offset(y = if (slideDetails) 0.dp else 40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "• Fast Delivery •",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = RichBlack
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Fresh Products •",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreenPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 24/7 Support •",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = RichBlack
                    )
                )

                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = DarkGreenPrimary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

// Wrapper for simple animations
@Composable
fun animateTransition(target: Float, duration: Int): State<Float> {
    return animateFloatAsState(
        targetValue = target,
        animationSpec = tween(duration, easing = FastOutSlowInEasing),
        label = ""
    )
}

// ==========================================
// 2. LOGIN SCREEN
// ==========================================
@Composable
fun DeliveryLoginHero(
    modifier: Modifier = Modifier,
    role: String,
    badgeText: String
) {
    val imageRes = when (role) {
        "Seller" -> R.drawable.img_seller_login_hero
        "DeliveryPartner" -> R.drawable.img_delivery_login_hero
        else -> R.drawable.img_user_login_hero
    }
    val contentDesc = when (role) {
        "Seller" -> "Seller managing catalog"
        "DeliveryPartner" -> "Fast grocery delivery partner"
        else -> "Customer shopping fresh groceries"
    }
    val iconVector = when (role) {
        "Seller" -> Icons.Default.Storefront
        "DeliveryPartner" -> Icons.Default.DeliveryDining
        else -> Icons.Default.LocalMall
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(178.dp)
            .shadow(10.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LightGreenSecondary)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = contentDesc,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)),
                            startY = 80f
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
                    .background(CustomWhite.copy(alpha = 0.94f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = DarkGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = badgeText,
                    color = RichBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: BazaarViewModel,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("User") } // "User" or "Seller"
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetAuthState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomWhite)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DeliveryLoginHero(
            role = selectedRole,
            badgeText = when (selectedRole) {
                "Seller" -> "Manage your store catalog"
                "DeliveryPartner" -> "Fresh delivery in minutes"
                else -> "Explore premium green marketplace"
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to ZYL VOR BAZAAR",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = RichBlack
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Log in to check customized delivery perks",
            style = MaterialTheme.typography.bodyMedium.copy(color = MutedText),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Role Selector Widget for Login
        Text(
            text = "Select Account Type",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MutedText,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftGrey, shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("User", "Seller", "DeliveryPartner").forEach { roleOption ->
                val active = selectedRole == roleOption
                val displayRoleName = if (roleOption == "DeliveryPartner") "Delivery Partner" else roleOption
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedRole = roleOption }
                        .testTag("login_role_select_${roleOption.lowercase()}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) DarkGreenPrimary else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayRoleName,
                            fontWeight = FontWeight.Bold,
                            color = if (active) CustomWhite else MutedText,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = DarkGreenPrimary) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = DarkGreenPrimary) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { viewModel.login(email, password, selectedRole) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("login_button"),
            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = CustomWhite, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Login Securely",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CustomWhite
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Don't have an account? ", color = MutedText)
            TextButton(onClick = onNavigateToRegister) {
                Text(
                    text = "Register Here",
                    color = DarkGreenPrimary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

// ==========================================
// 3. REGISTER SCREEN
// ==========================================
@Composable
fun RegisterScreen(
    viewModel: BazaarViewModel,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("User") } // "User" or "Seller"
    var shopName by remember { mutableStateOf("") }
    var shopAddress by remember { mutableStateOf("") }
    var shopAddressLat by remember { mutableStateOf(0.0) }
    var shopAddressLng by remember { mutableStateOf(0.0) }
    
    // Step-by-Step wizard for Seller Verification
    var currentStep by remember { mutableStateOf(1) }
    var sellerMobile by remember { mutableStateOf("") }
    var sellerOtpInput by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isMobileVerified by remember { mutableStateOf(false) }
    var showOtpError by remember { mutableStateOf(false) }
    var generatedOtp by remember { mutableStateOf("") }
    var otpTimer by remember { mutableStateOf(0) }
    var showSmsBanner by remember { mutableStateOf(false) }
    var smsMessage by remember { mutableStateOf("") }

    LaunchedEffect(otpTimer) {
        if (otpTimer > 0) {
            delay(1000L)
            otpTimer -= 1
        }
    }

    LaunchedEffect(showSmsBanner) {
        if (showSmsBanner) {
            delay(8000L)
            showSmsBanner = false
        }
    }
    
    var sellerAadhaar by remember { mutableStateOf("") }
    var sellerShopPhoto by remember { mutableStateOf("") }
    var sellerOwnerPhoto by remember { mutableStateOf("") }
    var bankAccountNum by remember { mutableStateOf("") }
    var bankIfsc by remember { mutableStateOf("") }
    var sellerPan by remember { mutableStateOf("") }
    var sellerGst by remember { mutableStateOf("") }
    
    // --- Delivery Partner Registration State ---
    var deliveryEmergencyContact by remember { mutableStateOf("") }
    var deliveryVehicleType by remember { mutableStateOf("Bike") } // "Bike", "Cycle", "Scooter"
    var deliveryVehicleNumber by remember { mutableStateOf("") }

    // Dialog state for mock shop photo upload
    var showShopPhotoDialog by remember { mutableStateOf(false) }
    var showOwnerPhotoDialog by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    val shopPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadImageToFirebaseStorage(uri, "shops/${email.ifBlank { "unknown" }}_shop.jpg") { url ->
                sellerShopPhoto = url
                showShopPhotoDialog = false
                Toast.makeText(context, "Shop photo selected successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val ownerPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadImageToFirebaseStorage(uri, "owners/${email.ifBlank { "unknown" }}_owner.jpg") { url ->
                sellerOwnerPhoto = url
                showOwnerPhotoDialog = false
                Toast.makeText(context, "Owner photo selected successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetAuthState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomWhite)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DeliveryLoginHero(role = selectedRole, badgeText = "Join fast local delivery")

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = RichBlack
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Sign up to join our premium green network",
            style = MaterialTheme.typography.bodyMedium.copy(color = MutedText)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Role Selector Widget
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Account Type",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            if (currentStep == 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = AccentRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Locked in Step 2",
                        fontSize = 10.sp,
                        color = AccentRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftGrey, shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("User", "Seller", "DeliveryPartner").forEach { roleOption ->
                val active = selectedRole == roleOption
                val displayRoleName = if (roleOption == "DeliveryPartner") "Delivery Partner" else roleOption
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = currentStep == 1) { selectedRole = roleOption }
                        .testTag("role_select_${roleOption.lowercase()}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) DarkGreenPrimary else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayRoleName,
                            fontWeight = FontWeight.Bold,
                            color = if (active) CustomWhite else MutedText.copy(alpha = if (currentStep == 2) 0.5f else 1f),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (selectedRole == "User" || (selectedRole == "Seller" && currentStep == 1) || (selectedRole == "DeliveryPartner" && currentStep == 1)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = DarkGreenPrimary) },
                modifier = Modifier.fillMaxWidth().testTag("name_registration"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = DarkGreenPrimary) },
                modifier = Modifier.fillMaxWidth().testTag("email_registration"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp)
            )

            if (selectedRole == "Seller") {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = "Shop Name", tint = DarkGreenPrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("shop_name_registration"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))
                AddressSuggestionField(
                    value = shopAddress,
                    onValueChange = { shopAddress = it },
                    onAddressSelected = {
                        shopAddress = it.address
                        shopAddressLat = it.latitude
                        shopAddressLng = it.longitude
                    },
                    label = "Full Shop Address",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "shop_address_registration"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = DarkGreenPrimary) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("password_registration"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.OfflinePin, contentDescription = "Confirm", tint = DarkGreenPrimary) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("confirm_password_registration"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedRole == "Seller") {
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() || shopName.isBlank() || shopAddress.isBlank()) {
                            Toast.makeText(context, "Please fill in all Step 1 fields.", Toast.LENGTH_SHORT).show()
                        } else if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                        } else {
                            currentStep = 2
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("next_verification_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Next: Seller Verification (Step 2 of 2)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CustomWhite
                        )
                    )
                }
            } else if (selectedRole == "DeliveryPartner") {
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                            Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                        } else if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                        } else {
                            currentStep = 2
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("next_delivery_verification_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Next: Delivery Verification (Step 2 of 2)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CustomWhite
                        )
                    )
                }
            } else {
                Button(
                    onClick = {
                        viewModel.register(name, email, password, confirmPassword, selectedRole)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_registration_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = CustomWhite, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Sign Up Securely",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CustomWhite
                            )
                        )
                    }
                }
            }
        } else {
            if (selectedRole == "Seller") {
                // STEP 2: SELLER VERIFICATION (Mandatory Documents & OTP)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightGreenSecondary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "STEP 2: MANDATORY SELLER VERIFICATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DarkGreenPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To maintain our network integrity, please upload/complete the following documents.",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Mobile Number & OTP Verification
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1. Mobile Number Verification", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RichBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sellerMobile,
                                onValueChange = { if (it.all { char -> char.isDigit() }) sellerMobile = it },
                                label = { Text("Mobile Number (10 digits)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                enabled = !isMobileVerified,
                                shape = RoundedCornerShape(8.dp)
                            )
                            
                            if (!isMobileVerified) {
                                Button(
                                    onClick = {
                                        if (sellerMobile.length == 10) {
                                            if (otpTimer == 0) {
                                                val randomOtp = (100000..999999).random().toString()
                                                generatedOtp = randomOtp
                                                sellerOtpInput = ""
                                                isOtpSent = true
                                                otpTimer = 30
                                                smsMessage = "[BAZAAR] Use $randomOtp to verify your Green Seller Account. Do not share this OTP."
                                                showSmsBanner = true
                                                Toast.makeText(context, "Verification SMS sent to +91 $sellerMobile", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Please enter a valid 10-digit mobile number.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = otpTimer == 0,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (otpTimer > 0) Color.Gray else DarkGreenPrimary,
                                        disabledContainerColor = Color.Gray
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text(
                                        text = if (otpTimer > 0) "Resend in ${otpTimer}s" else if (isOtpSent) "Resend" else "Send OTP",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (isOtpSent && !isMobileVerified) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = sellerOtpInput,
                                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) sellerOtpInput = it },
                                    label = { Text("6-Digit OTP") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                
                                Button(
                                    onClick = {
                                        if (sellerOtpInput == generatedOtp) {
                                            isMobileVerified = true
                                            isOtpSent = false
                                            showOtpError = false
                                            Toast.makeText(context, "Mobile Verified!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showOtpError = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text("Verify", fontSize = 11.sp)
                                }
                            }
                            if (showOtpError) {
                                Text("Invalid OTP. Hint: Check the messages notification at the top of your screen or wait for the timer to resend.", color = AccentRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }

                        if (isMobileVerified) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mobile verified successfully!", color = DarkGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Aadhaar Card
                OutlinedTextField(
                    value = sellerAadhaar,
                    onValueChange = { if (it.length <= 12 && it.all { char -> char.isDigit() }) sellerAadhaar = it },
                    label = { Text("2. Aadhaar Card Number (12 digits)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3 & 4. Direct Upload Mocks for Shop Photo and Owner Photo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Shop Photo Box
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { shopPhotoLauncher.launch("image/*") },
                        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (sellerShopPhoto.isNotBlank()) {
                                AsyncImage(
                                    model = sellerShopPhoto,
                                    contentDescription = "Shop Photo",
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Shop Uploaded ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                            } else {
                                Icon(Icons.Default.Storefront, contentDescription = "", tint = MutedText, modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Shop Photo (Req)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                Text("Tap to Select Photo", fontSize = 9.sp, color = MutedText)
                            }
                        }
                    }

                    // Owner Photo Box
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { ownerPhotoLauncher.launch("image/*") },
                        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (sellerOwnerPhoto.isNotBlank()) {
                                AsyncImage(
                                    model = sellerOwnerPhoto,
                                    contentDescription = "Owner Photo",
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Selfie Uploaded ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                            } else {
                                Icon(Icons.Default.Person, contentDescription = "", tint = MutedText, modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Owner Selfie (Req)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                Text("Tap to Select Selfie", fontSize = 9.sp, color = MutedText)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Bank Account Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("5. Bank Account Details (Mandatory)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RichBlack)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedTextField(
                            value = bankAccountNum,
                            onValueChange = { if (it.all { char -> char.isDigit() }) bankAccountNum = it },
                            label = { Text("Bank Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedTextField(
                            value = bankIfsc,
                            onValueChange = { bankIfsc = it.uppercase() },
                            label = { Text("Bank IFSC Code (e.g. SBIN0001234)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Section
                Text(
                    text = "Optional Details (Not Mandatory)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sellerPan,
                        onValueChange = { sellerPan = it.uppercase() },
                        label = { Text("PAN Card") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = sellerGst,
                        onValueChange = { sellerGst = it.uppercase() },
                        label = { Text("GST Number") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f).height(54.dp),
                        border = BorderStroke(1.dp, DarkGreenPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (!isMobileVerified) {
                                Toast.makeText(context, "Please complete mobile OTP verification first.", Toast.LENGTH_LONG).show()
                            } else if (sellerAadhaar.length != 12) {
                                Toast.makeText(context, "Please enter a valid 12-digit Aadhaar Card number.", Toast.LENGTH_LONG).show()
                            } else if (sellerShopPhoto.isBlank()) {
                                Toast.makeText(context, "Please select/upload a shop photo.", Toast.LENGTH_LONG).show()
                            } else if (sellerOwnerPhoto.isBlank()) {
                                Toast.makeText(context, "Please select/upload an owner photo/selfie.", Toast.LENGTH_LONG).show()
                            } else if (bankAccountNum.isBlank() || bankIfsc.isBlank()) {
                                Toast.makeText(context, "Please enter bank account details.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.register(
                                    name = name,
                                    email = email,
                                    pword = password,
                                    confPword = confirmPassword,
                                    role = selectedRole,
                                    shopName = shopName,
                                    shopAddress = shopAddress,
                                    shopAddressLat = shopAddressLat,
                                    shopAddressLng = shopAddressLng,
                                    sellerMobile = sellerMobile,
                                    sellerAadhaar = sellerAadhaar,
                                    sellerShopPhoto = sellerShopPhoto,
                                    sellerOwnerPhoto = sellerOwnerPhoto,
                                    sellerBankAccount = "$bankAccountNum ($bankIfsc)",
                                    sellerPanCard = sellerPan,
                                    sellerGstNumber = sellerGst
                                )
                            }
                        },
                        modifier = Modifier.weight(2.1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = CustomWhite, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Register & Submit Profile", fontWeight = FontWeight.Bold, color = CustomWhite)
                        }
                    }
                }
            } else if (selectedRole == "DeliveryPartner") {
                // STEP 2: ZYL VOR DELIVERY PARTNER REGISTRATION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightGreenSecondary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "STEP 2: MANDATORY DELIVERY PARTNER REGISTRATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DarkGreenPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Welcome to ZYL VOR DELIVERY PARTNER. Please complete your registration details.",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Mobile Number & OTP Verification
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1. Mobile Number Verification", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RichBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sellerMobile,
                                onValueChange = { if (it.all { char -> char.isDigit() }) sellerMobile = it },
                                label = { Text("Mobile Number (10 digits)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                enabled = !isMobileVerified,
                                shape = RoundedCornerShape(8.dp)
                            )
                            
                            if (!isMobileVerified) {
                                Button(
                                    onClick = {
                                        if (sellerMobile.length == 10) {
                                            if (otpTimer == 0) {
                                                val randomOtp = (100000..999999).random().toString()
                                                generatedOtp = randomOtp
                                                sellerOtpInput = ""
                                                isOtpSent = true
                                                otpTimer = 30
                                                smsMessage = "[ZYL VOR] Use $randomOtp to verify your Delivery Partner Account."
                                                showSmsBanner = true
                                                Toast.makeText(context, "Verification SMS sent to +91 $sellerMobile", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Please enter a valid 10-digit mobile number.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = otpTimer == 0,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (otpTimer > 0) Color.Gray else DarkGreenPrimary,
                                        disabledContainerColor = Color.Gray
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text(
                                        text = if (otpTimer > 0) "Resend in ${otpTimer}s" else if (isOtpSent) "Resend" else "Send OTP",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (isOtpSent && !isMobileVerified) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = sellerOtpInput,
                                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) sellerOtpInput = it },
                                    label = { Text("6-Digit OTP") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                
                                Button(
                                    onClick = {
                                        if (sellerOtpInput == generatedOtp) {
                                            isMobileVerified = true
                                            isOtpSent = false
                                            showOtpError = false
                                            Toast.makeText(context, "Mobile Verified!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showOtpError = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text("Verify", fontSize = 11.sp)
                                }
                            }
                            if (showOtpError) {
                                Text("Invalid OTP. Hint: Check the messages notification at the top of your screen or wait for the timer to resend.", color = AccentRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }

                        if (isMobileVerified) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mobile verified successfully!", color = DarkGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Aadhaar Card
                OutlinedTextField(
                    value = sellerAadhaar,
                    onValueChange = { if (it.length <= 12 && it.all { char -> char.isDigit() }) sellerAadhaar = it },
                    label = { Text("Aadhaar Card Number (12 digits)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Passport Size Photo / Selfie
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (sellerOwnerPhoto.isNotBlank()) {
                            AsyncImage(
                                model = sellerOwnerPhoto,
                                contentDescription = "Selfie Photo",
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Selfie Captured ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "", tint = MutedText, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { ownerPhotoLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Take Passport Size Photo / Selfie", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Full Residential Address
                AddressSuggestionField(
                    value = shopAddress,
                    onValueChange = { shopAddress = it },
                    onAddressSelected = {
                        shopAddress = it.address
                        shopAddressLat = it.latitude
                        shopAddressLng = it.longitude
                    },
                    label = "Full Residential Address",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Bank Account Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("5. Bank Account Details", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RichBlack)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = bankAccountNum,
                            onValueChange = { if (it.all { char -> char.isDigit() }) bankAccountNum = it },
                            label = { Text("Bank Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = bankIfsc,
                            onValueChange = { bankIfsc = it.uppercase() },
                            label = { Text("Bank IFSC Code") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Emergency Contact Number
                OutlinedTextField(
                    value = deliveryEmergencyContact,
                    onValueChange = { if (it.all { char -> char.isDigit() }) deliveryEmergencyContact = it },
                    label = { Text("Emergency Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 7. Vehicle Type & Number
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("7. Vehicle Information", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RichBlack)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Vehicle Type", fontSize = 11.sp, color = MutedText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Bike", "Cycle", "Scooter").forEach { vehicle ->
                                val selected = deliveryVehicleType == vehicle
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { deliveryVehicleType = vehicle },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) DarkGreenPrimary else SoftGrey
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = vehicle,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) CustomWhite else MutedText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                        if (deliveryVehicleType != "Cycle") {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = deliveryVehicleNumber,
                                onValueChange = { deliveryVehicleNumber = it.uppercase() },
                                label = { Text("Vehicle Plate Number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f).height(54.dp),
                        border = BorderStroke(1.dp, DarkGreenPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (!isMobileVerified) {
                                Toast.makeText(context, "Please complete mobile OTP verification first.", Toast.LENGTH_LONG).show()
                            } else if (sellerAadhaar.length != 12) {
                                Toast.makeText(context, "Please enter a valid 12-digit Aadhaar Card number.", Toast.LENGTH_LONG).show()
                            } else if (sellerOwnerPhoto.isBlank()) {
                                Toast.makeText(context, "Please take a selfie/passport photo.", Toast.LENGTH_LONG).show()
                            } else if (shopAddress.isBlank()) {
                                Toast.makeText(context, "Please enter your address.", Toast.LENGTH_LONG).show()
                            } else if (bankAccountNum.isBlank() || bankIfsc.isBlank()) {
                                Toast.makeText(context, "Please enter bank account details.", Toast.LENGTH_LONG).show()
                            } else if (deliveryEmergencyContact.isBlank()) {
                                Toast.makeText(context, "Please enter emergency contact number.", Toast.LENGTH_LONG).show()
                            } else if (deliveryVehicleType != "Cycle" && deliveryVehicleNumber.isBlank()) {
                                Toast.makeText(context, "Please enter vehicle plate number.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.register(
                                    name = name,
                                    email = email,
                                    pword = password,
                                    confPword = confirmPassword,
                                    role = selectedRole,
                                    shopName = "Zyl Vor Delivery",
                                    shopAddress = shopAddress,
                                    shopAddressLat = shopAddressLat,
                                    shopAddressLng = shopAddressLng,
                                    sellerMobile = sellerMobile,
                                    sellerAadhaar = sellerAadhaar,
                                    sellerShopPhoto = "",
                                    sellerOwnerPhoto = sellerOwnerPhoto,
                                    sellerBankAccount = "$bankAccountNum ($bankIfsc)",
                                    deliveryMobile = sellerMobile,
                                    deliveryAadhaar = sellerAadhaar,
                                    deliveryPhoto = sellerOwnerPhoto,
                                    deliveryAddress = shopAddress,
                                    deliveryAddressLat = shopAddressLat,
                                    deliveryAddressLng = shopAddressLng,
                                    deliveryBankAccount = "$bankAccountNum ($bankIfsc)",
                                    deliveryEmergencyContact = deliveryEmergencyContact,
                                    deliveryVehicleType = deliveryVehicleType,
                                    deliveryVehicleNumber = deliveryVehicleNumber
                                )
                            }
                        },
                        modifier = Modifier.weight(2.1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = CustomWhite, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Register Partner", fontWeight = FontWeight.Bold, color = CustomWhite)
                        }
                    }
                }
            }
        }



        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Already have an account? ", color = MutedText)
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Login",
                    color = DarkGreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

        // Floating SMS Notification Banner
        AnimatedVisibility(
            visible = showSmsBanner,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .zIndex(99f)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF333333), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "SMS",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Messages • BAZAAR", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("now", color = Color.Gray, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = smsMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. MAIN SCREEN (With Navigation Tabs)
// ==========================================
@Composable
fun MainScreen(
    viewModel: BazaarViewModel,
    activity: MainActivity? = null,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BottomTab.Home) }
    val user by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    var showProductDetail by remember { mutableStateOf<Product?>(null) }
    var directBuyProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(activity) {
        activity?.razorpayResult?.collect { result ->
            when (result) {
                is com.example.RazorpayResult.Success -> {
                    val pending = viewModel.pendingCheckout.value
                    if (pending != null) {
                        viewModel.checkout(
                            totalAmount = pending.totalAmount,
                            summary = pending.summary,
                            customOrderId = pending.orderId,
                            deliveryAddress = pending.address,
                            couponApplied = pending.coupon,
                            deliveryAddressLat = pending.addressLat,
                            deliveryAddressLng = pending.addressLng,
                            itemsAmount = pending.itemsAmount,
                            deliveryDistanceKm = pending.deliveryDistanceKm,
                            deliveryFixedCharge = pending.deliveryFixedCharge,
                            deliveryPerKmCharge = pending.deliveryPerKmCharge,
                            deliveryCharge = pending.deliveryCharge,
                            paymentMode = "Online Payment",
                            paymentTransactionId = result.paymentId,
                            clearCartAfterCheckout = pending.clearCartAfterCheckout
                        )
                        viewModel.notifyCheckoutSuccess()
                        viewModel.clearPendingCheckout()
                    }
                }
                is com.example.RazorpayResult.Error -> {
                    val msg = if (result.code == 0 || result.description.contains("cancel", ignoreCase = true)) {
                        "Payment Cancelled"
                    } else {
                        "Payment Failed. Please try again."
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearPendingCheckout()
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .shadow(16.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                border = BorderStroke(1.5.dp, SoftGrey)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0.dp)
                ) {
                    BottomTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                // If detail is open, we can close it when backing to generic tabs
                                showProductDetail = null
                                selectedTab = tab
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.iconFilled else tab.iconOutlined,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) DarkGreenPrimary else MutedText
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) DarkGreenPrimary else MutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkGreenPrimary,
                                indicatorColor = LightGreenSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = showProductDetail to selectedTab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        initialOffsetX = { it / 5 }
                    )) togetherWith (fadeOut(animationSpec = tween(160)) + slideOutHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        targetOffsetX = { -it / 8 }
                    ))
                },
                label = "main_content_transition"
            ) { (detailProduct, tab) ->
                if (detailProduct != null) {
                    ProductDetailPane(
                        product = detailProduct,
                        onBack = { showProductDetail = null },
                        viewModel = viewModel
                    )
                } else {
                    when (tab) {
                        BottomTab.Home -> HomeScreen(
                            viewModel = viewModel,
                            onProductSelect = { showProductDetail = it },
                            onProductBuy = { directBuyProduct = it }
                        )
                        BottomTab.Categories -> CategoriesScreen(
                            viewModel = viewModel,
                            onProductSelect = { showProductDetail = it },
                            onProductBuy = { directBuyProduct = it }
                        )
                        BottomTab.Cart -> CartScreen(viewModel = viewModel, activity = activity, onBackToShopping = { selectedTab = BottomTab.Home })
                        BottomTab.Wishlist -> WishlistScreen(
                            viewModel = viewModel,
                            onProductSelect = { showProductDetail = it },
                            onProductBuy = { directBuyProduct = it }
                        )
                        BottomTab.Orders -> OrdersScreen(viewModel = viewModel)
                        BottomTab.Profile -> ProfileScreen(viewModel = viewModel, onLogout = onLogout)
                    }
                }
            }

            directBuyProduct?.let { product ->
                DirectBuyCheckoutDialog(
                    product = product,
                    viewModel = viewModel,
                    activity = activity,
                    onDismiss = { directBuyProduct = null }
                )
            }
        }
    }
}

// ==========================================
// A. HOME SCREEN (Flipkart & Amazon vibe)
// ==========================================

private fun extractPincode(address: String): String {
    return Regex("\\b\\d{5,6}\\b")
        .findAll(address)
        .lastOrNull()
        ?.value
        .orEmpty()
}

/** Fetches the current location and returns "City, PostalCode" or a fallback string. */
@SuppressLint("MissingPermission")
private suspend fun getLocationText(context: android.content.Context): String {
    val selection = getCurrentAddressSelection(context)
    return if (selection.address.isNotBlank()) selection.address else "Location unavailable"
}

/** Fetches the current location with reverse-geocoded address and coordinates. */
@SuppressLint("MissingPermission")
private suspend fun getCurrentAddressSelection(context: android.content.Context): AddressSelection {
    return try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) {} }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) {} }
        }
        if (location == null) return AddressSelection("Current location")
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            val geocoder = Geocoder(context, Locale.getDefault())
            val addrs = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addrs
                ?.firstOrNull()
                ?.getAddressLine(0)
                ?.takeIf { it.isNotBlank() }
                ?: "Current location"
            AddressSelection(address, location.latitude, location.longitude)
        }
    } catch (e: Exception) {
        AddressSelection("Location unavailable")
    }
}

data class AddressSelection(
    val address: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@Composable
fun FlashSaleCountdownText(flashSaleEndTime: Long) {
    var secondsLeft by remember(flashSaleEndTime) {
        val diff = (flashSaleEndTime - System.currentTimeMillis()) / 1000
        mutableStateOf(if (diff > 0) diff.toLong() else 10745L)
    }

    LaunchedEffect(flashSaleEndTime) {
        while (true) {
            delay(1000)
            secondsLeft = if (secondsLeft > 0) secondsLeft - 1 else 10800L
        }
    }

    val hours = secondsLeft / 3600
    val minutes = (secondsLeft % 3600) / 60
    val seconds = secondsLeft % 60
    Text(
        text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
        color = Color(0xFFE65100),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun HomeBannerCarousel(
    bannersList: List<Banner>,
    onCategorySelect: (String) -> Unit
) {
    var bannerIndex by remember { mutableStateOf(0) }

    LaunchedEffect(bannersList) {
        while (true) {
            delay(4500)
            bannerIndex = if (bannersList.isNotEmpty()) {
                (bannerIndex + 1) % bannersList.size
            } else {
                (bannerIndex + 1) % 3
            }
        }
    }

    val bannersToShow = remember(bannersList) {
        if (bannersList.isNotEmpty()) {
            bannersList
        } else {
            listOf(
                Banner(id = "banner_1", label = "LAUNCH SPECIAL", title = "50% OFF ON SMART ASSISTANTS", description = "Futuristic wellness tech with AMOLED screens & rapid charge.", targetCategory = "Electronics", gradientStart = "#1B5E20", gradientEnd = "#A5D6A7"),
                Banner(id = "banner_2", label = "WEEKEND HARVEST", title = "30% OFF ALL ORGANIC PRODUCTS", description = "Crispy, hand-picked pesticide-free seasonal green food.", targetCategory = "Fresh Products", gradientStart = "#2E7D32", gradientEnd = "#81C784"),
                Banner(id = "banner_3", label = "STYLE INSPIRATION", title = "BUY 1 GET 1 ON EMERALD SNEAKERS", description = "Elevate your step with lightweight, vulcanized rubber running soles.", targetCategory = "Fashion", gradientStart = "#8E24AA", gradientEnd = "#CE93D8")
            )
        }
    }
    val currentBannerIndex = if (bannersToShow.isNotEmpty()) bannerIndex % bannersToShow.size else 0
    val currentBanner = bannersToShow.getOrNull(currentBannerIndex) ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(8.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (currentBanner.targetCategory.isNotEmpty()) {
                    onCategorySelect(currentBanner.targetCategory)
                }
            }
    ) {
        val colors = try {
            listOf(
                Color(android.graphics.Color.parseColor(currentBanner.gradientStart)),
                Color(android.graphics.Color.parseColor(currentBanner.gradientEnd))
            )
        } catch (e: Exception) {
            listOf(DarkGreenPrimary, MintAccent)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(colors))
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = 180.dp, y = (-50).dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.7f)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .background(AccentGold, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = currentBanner.label,
                    color = RichBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentBanner.title,
                color = CustomWhite,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentBanner.description,
                color = CustomWhite.copy(alpha = 0.85f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bannersToShow.forEachIndexed { index, _ ->
                val active = index == currentBannerIndex
                Box(
                    modifier = Modifier
                        .size(if (active) 16.dp else 8.dp, 8.dp)
                        .background(
                            color = if (active) AccentGold else CustomWhite.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun AddressSuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    onAddressSelected: (AddressSelection) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    maxLines: Int = 3
) {
    val context = LocalContext.current
    var suggestions by remember { mutableStateOf<List<AddressSelection>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (value.trim().length < 3) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        isSearching = true
        delay(350L)
        suggestions = withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault())
                    .getFromLocationName(value.trim(), 5)
                    ?.mapNotNull { addr ->
                        val line = addr.getAddressLine(0)
                        if (line.isNullOrBlank()) {
                            null
                        } else {
                            AddressSelection(line, addr.latitude, addr.longitude)
                        }
                    }
                    ?.distinctBy { it.address }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        isSearching = false
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                onAddressSelected(AddressSelection(it))
            },
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = label, tint = DarkGreenPrimary) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = DarkGreenPrimary)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            singleLine = false,
            maxLines = maxLines,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DarkGreenPrimary)
        )

        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                border = BorderStroke(1.dp, SoftGrey),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    suggestions.take(4).forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(suggestion.address)
                                    onAddressSelected(suggestion)
                                    suggestions = emptyList()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = DarkGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = suggestion.address,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RichBlack,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Lat ${String.format("%.5f", suggestion.latitude)}, Lng ${String.format("%.5f", suggestion.longitude)}",
                                    fontSize = 10.sp,
                                    color = MutedText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: BazaarViewModel,
    onProductSelect: (Product) -> Unit,
    onProductBuy: (Product) -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val products by viewModel.filteredProducts.collectAsState(initial = emptyList())
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val cartList by viewModel.currentCart.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ── Live location ─────────────────────────────────────
    var locationText by remember { mutableStateOf("Fetching location...") }
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            coroutineScope.launch { locationText = getLocationText(context) }
        } else {
            locationText = "Location off"
        }
    }
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            locationText = getLocationText(context)
        } else {
            locationPermLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    val categories by viewModel.categories.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val bannersList by viewModel.banners.collectAsState()
    val shortcutsList by viewModel.shortcuts.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomWhite)
    ) {
        // App top identity header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(DarkGreenPrimary, shape = CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Bag",
                            tint = CustomWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ZYL VOR BAZAAR",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = DarkGreenPrimary
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = locationText,
                                fontSize = 11.sp,
                                color = RichBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(LightGreenSecondary, shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Standard Care",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreenPrimary
                    )
                }
            }
        }

        // Search Bar Area
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search electronic gadgets, fresh food, fashion...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = DarkGreenPrimary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkGreenPrimary,
                    unfocusedBorderColor = SoftGrey,
                    focusedContainerColor = SoftGrey,
                    unfocusedContainerColor = SoftGrey
                ),
                singleLine = true
            )
        }

        // --- QUICK CIRCULAR SERVICE SHORTCUTS (Flipkart style!) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (shortcutsList.isNotEmpty()) {
                    shortcutsList.forEach { shortcut ->
                        val icon = when (shortcut.iconName) {
                            "flash_on" -> Icons.Default.FlashOn
                            "devices" -> Icons.Default.Devices
                            "local_florist" -> Icons.Default.LocalFlorist
                            "shopping_bag" -> Icons.Default.ShoppingBag
                            "confirmation_number" -> Icons.Default.ConfirmationNumber
                            else -> Icons.Default.HelpOutline
                        }
                        val color = try {
                            Color(android.graphics.Color.parseColor(shortcut.colorHex))
                        } catch (e: Exception) {
                            Color(0xFF9C27B0)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (shortcut.action.startsWith("category:")) {
                                        val cat = shortcut.action.substringAfter("category:")
                                        viewModel.selectCategory(cat)
                                        if (shortcut.toastMessage.isNotEmpty()) {
                                            Toast.makeText(context, shortcut.toastMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    } else if (shortcut.action == "show_coupon") {
                                        val firstCoupon = viewModel.coupons.value.firstOrNull()
                                        val couponMsg = if (firstCoupon != null) {
                                            "🎟️ Copy Coupon Code: ${firstCoupon.code} for ${firstCoupon.discountPercent}% OFF at Checkout!"
                                        } else {
                                            "🎟️ Copy Coupon Code: BAZAAR20 for 20% OFF at Checkout!"
                                        }
                                        Toast.makeText(context, couponMsg, Toast.LENGTH_LONG).show()
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(color.copy(alpha = 0.15f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = shortcut.title,
                                    tint = color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = shortcut.title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RichBlack,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val shortcuts = listOf(
                        Triple("Flash Deals", Icons.Default.FlashOn, Color(0xFFFF5722)),
                        Triple("Smart Tech", Icons.Default.Devices, Color(0xFF2196F3)),
                        Triple("Fresh Produce", Icons.Default.LocalFlorist, Color(0xFF4CAF50)),
                        Triple("Wardrobe", Icons.Default.ShoppingBag, Color(0xFFE91E63)),
                        Triple("VIP Coupons", Icons.Default.ConfirmationNumber, Color(0xFF9C27B0))
                    )

                    shortcuts.forEach { (title, icon, color) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    when (title) {
                                        "Flash Deals" -> {
                                            viewModel.selectCategory("All")
                                            Toast.makeText(context, "Showing lightning deals & offers!", Toast.LENGTH_SHORT).show()
                                        }
                                        "Smart Tech" -> viewModel.selectCategory("Electronics")
                                        "Fresh Produce" -> viewModel.selectCategory("Fresh Products")
                                        "Wardrobe" -> viewModel.selectCategory("Fashion")
                                        "VIP Coupons" -> {
                                            Toast.makeText(context, "🎟️ Copy Coupon Code: BAZAAR20 for 20% OFF at Checkout!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(color.copy(alpha = 0.15f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RichBlack,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Premium Swipable / Rotating Carousel Billboard Hero Banner
        item {
            HomeBannerCarousel(
                bannersList = bannersList,
                onCategorySelect = { viewModel.selectCategory(it) }
            )
        }

        // --- FLASH SALE WITH COUNTDOWN (Flipkart style!) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, SoftGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OfflineBolt,
                                contentDescription = "",
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lightning Flash Deals",
                                fontWeight = FontWeight.Black,
                                color = RichBlack,
                                fontSize = 14.sp
                            )
                        }

                        // Countdown Timer Box
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFFFFE0B2), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            FlashSaleCountdownText(appConfig.flashSaleEndTime)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal scrolling flash sale items
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        val flashItems = products.take(4)
                        items(flashItems, key = { it.id }) { prod ->
                            val flashPhoto = prod.primaryPhotoUrl()
                            val flashPhotoModel = remember(flashPhoto) {
                                resolveProductImageModel(context, flashPhoto)
                            }
                            Card(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clickable { onProductSelect(prod) },
                                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                border = BorderStroke(1.dp, SoftGrey),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .background(SoftGrey, shape = RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (flashPhotoModel != null) {
                                            AsyncImage(
                                                model = flashPhotoModel,
                                                contentDescription = prod.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                placeholder = painterResource(id = R.drawable.img_hero_banner_1782139859933),
                                                error = painterResource(id = R.drawable.img_hero_banner_1782139859933)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = when (prod.category) {
                                                    "Electronics" -> Icons.Default.Devices
                                                    "Fresh Products" -> Icons.Default.LocalFlorist
                                                    "Fashion" -> Icons.Default.ShoppingBag
                                                    else -> Icons.Default.Kitchen
                                                },
                                                contentDescription = "",
                                                tint = DarkGreenPrimary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        // Save Badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .background(AccentRed, RoundedCornerShape(bottomEnd = 8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "SAVINGS",
                                                color = CustomWhite,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = prod.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = RichBlack
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "₹${prod.price}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = DarkGreenPrimary
                                        )
                                        Text(
                                            text = "₹${prod.originalPrice}",
                                            textDecoration = TextDecoration.LineThrough,
                                            fontSize = 9.sp,
                                            color = MutedText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Category Row
        item {
            Text(
                text = "Shop by Categories",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = RichBlack
                ),
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCat == cat
                    val bg = if (isSelected) DarkGreenPrimary else SoftGrey
                    val fg = if (isSelected) CustomWhite else RichBlack

                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.selectCategory(cat) }
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = bg
                    ) {
                        Text(
                            text = cat,
                            color = fg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // Headline Featured Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deals & Featured Products",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = RichBlack
                    )
                )
                TextButton(onClick = { viewModel.selectCategory("All") }) {
                    Text(text = "View All", color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Products Catalog list (2-Column dynamic style)
        if (products.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "Empty",
                        tint = MutedText,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No matching products found.",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Group in pairs to mimic grid cleanly inside LazyColumn
            val pairs = remember(products) { products.chunked(2) }
            val cartByProductId = remember(cartList) { cartList.associateBy { it.productId } }
            items(
                items = pairs,
                key = { pair -> pair.joinToString("_") { it.id.toString() } }
            ) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { prod ->
                        Box(modifier = Modifier.weight(1f)) {
                            val cartItem = cartByProductId[prod.id]
                            ProductItemCard(
                                product = prod,
                                onCallSelect = { onProductSelect(prod) },
                                onCallAddToCart = {
                                    viewModel.addToCart(prod)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("✅ ${prod.name} added to cart!")
                                    }
                                },
                                onCallBuyNow = { onProductBuy(prod) },
                                onCallWishlistToggle = { viewModel.toggleWishlist(prod) },
                                isWishlisted = viewModel.isWishlisted(prod.id),
                                cartQuantity = cartItem?.quantity ?: 0,
                                onIncrement = { cartItem?.let { viewModel.increaseCartQty(it) } ?: viewModel.addToCart(prod) },
                                onDecrement = { cartItem?.let { viewModel.decreaseCartQty(it) } }
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Bottom Brand trust footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightGreenSecondary)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Verified Quality",
                    tint = DarkGreenPrimary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ZYL VOR BAZAAR TRUST ASSURED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = RichBlack
                )
                Text(
                    text = "• 100% Quality checked • Fresh replacement guidelines \n• Advanced Buyer security protection",
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = MutedText,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "© 2026 ZYL VOR BAZAAR Inc.",
                    fontSize = 10.sp,
                    color = MutedText
                )
            }
        }
    } // end LazyColumn

    // Snackbar for add-to-cart flash
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    ) { data ->
        Snackbar(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            containerColor = DarkGreenPrimary,
            contentColor = CustomWhite,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = CustomWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(data.visuals.message, color = CustomWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
    } // end Box
}

// 2-Column Product Card
fun Product.primaryPhotoUrl(): String {
    return photoEntries().firstOrNull().orEmpty()
}

fun Product.photoEntries(): List<String> {
    return (listOf(imageUrlName) + extraImages.split(","))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

fun resolveProductImageModel(context: android.content.Context, imageRef: String): Any? {
    val trimmed = imageRef.trim()
    if (trimmed.isBlank()) return null
    if (
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("content://", ignoreCase = true) ||
        trimmed.startsWith("file://", ignoreCase = true)
    ) {
        return trimmed
    }
    val resourceName = trimmed.substringBeforeLast(".")
    val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    return resourceId.takeIf { it != 0 } ?: trimmed
}

@Composable
fun ProductItemCard(
    product: Product,
    onCallSelect: () -> Unit,
    onCallAddToCart: () -> Unit,
    onCallBuyNow: () -> Unit,
    onCallWishlistToggle: () -> Unit,
    isWishlisted: Boolean,
    cartQuantity: Int = 0,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {}
) {
    val context = LocalContext.current
    val primaryPhotoUrl = product.primaryPhotoUrl()
    val primaryPhotoModel = remember(primaryPhotoUrl) {
        resolveProductImageModel(context, primaryPhotoUrl)
    }
    val isInStock = product.stock > 0
    val stockLabel = if (product.stock >= 999) "In stock" else "Stock: ${product.stock}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCallSelect() }
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
            .testTag("product_item_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = CustomWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(SoftGrey)
        ) {
            // Display actual product photo if available, otherwise use a category visual.
            if (primaryPhotoModel != null) {
                AsyncImage(
                    model = primaryPhotoModel,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.img_hero_banner_1782139859933),
                    error = painterResource(id = R.drawable.img_hero_banner_1782139859933)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (product.category) {
                            "Electronics" -> Icons.Default.Devices
                            "Fresh Products" -> Icons.Default.LocalFlorist
                            "Fashion" -> Icons.Default.ShoppingBag
                            else -> Icons.Default.Kitchen
                        },
                        contentDescription = "Visual",
                        tint = DarkGreenPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            // Wishlist Button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = onCallWishlistToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .background(CustomWhite.copy(alpha = 0.9f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isWishlisted) AccentRed else MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Rating Badge overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = AccentGold,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = product.rating.toString(),
                    color = CustomWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = product.category,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkGreenPrimary
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = RichBlack
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Pricing Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${product.price}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = RichBlack
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "₹${product.originalPrice}",
                    color = MutedText,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.LineThrough
                )
            }

            Text(
                text = if (isInStock) stockLabel else "Out of stock",
                color = if (isInStock) DarkGreenPrimary else AccentRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cart control: +/- row if in cart, else Add to Cart button
            if (cartQuantity > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(DarkGreenPrimary, shape = RoundedCornerShape(6.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = CustomWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "$cartQuantity",
                        color = CustomWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = onIncrement,
                        enabled = cartQuantity < product.stock,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = if (cartQuantity < product.stock) CustomWhite else CustomWhite.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onCallAddToCart,
                    enabled = isInStock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGreenPrimary,
                        disabledContainerColor = MutedText.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Cart",
                        tint = CustomWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isInStock) "Add to Cart" else "Out of Stock",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CustomWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onCallBuyNow,
                enabled = isInStock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("product_buy_now_${product.id}"),
                border = BorderStroke(1.dp, DarkGreenPrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DarkGreenPrimary,
                    disabledContentColor = MutedText
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OfflineBolt,
                    contentDescription = "Buy Now",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isInStock) "Buy Now" else "Unavailable",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// ==========================================
// B. CATEGORIES SCREEN
// ==========================================
@Composable
fun CategoriesScreen(
    viewModel: BazaarViewModel,
    onProductSelect: (Product) -> Unit,
    onProductBuy: (Product) -> Unit
) {
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val products by viewModel.filteredProducts.collectAsState(initial = emptyList())
    val cartList by viewModel.currentCart.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val categoriesList by viewModel.categories.collectAsState()
    val sections = categoriesList.filter { it != "All" }.map { name ->
        val icon = when (name) {
            "Electronics" -> Icons.Default.Devices
            "Fresh Products" -> Icons.Default.LocalFlorist
            "Fashion" -> Icons.Default.ShoppingBag
            "Home & Kitchen" -> Icons.Default.Kitchen
            else -> Icons.Default.Category
        }
        Pair(name, icon)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomWhite)
    ) {
        Text(
            text = "Explore Categories",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = RichBlack
            ),
            modifier = Modifier.padding(16.dp)
        )

        // Horizontal Category Icons
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sections) { (name, icon) ->
                val isSelected = selectedCat == name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.selectCategory(name) }
                        .width(72.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(2.dp, CircleShape)
                            .background(
                                color = if (isSelected) DarkGreenPrimary else LightGreenSecondary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = if (isSelected) CustomWhite else DarkGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = name,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) DarkGreenPrimary else RichBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Divider(color = SoftGrey, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

        // Grid for category products
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCat Catalog items",
                fontWeight = FontWeight.Bold,
                color = RichBlack
            )
            TextButton(onClick = { viewModel.selectCategory("All") }) {
                Text("Reset Filter", color = DarkGreenPrimary)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (products.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = "", tint = MutedText, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No products in $selectedCat yet.", color = MutedText)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { prod ->
                        val cartItem = cartList.find { it.productId == prod.id }
                        ProductItemCard(
                            product = prod,
                            onCallSelect = { onProductSelect(prod) },
                            onCallAddToCart = {
                                viewModel.addToCart(prod)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("✅ ${prod.name} added to cart!")
                                }
                            },
                            onCallBuyNow = { onProductBuy(prod) },
                            onCallWishlistToggle = { viewModel.toggleWishlist(prod) },
                            isWishlisted = viewModel.isWishlisted(prod.id),
                            cartQuantity = cartItem?.quantity ?: 0,
                            onIncrement = { cartItem?.let { viewModel.increaseCartQty(it) } ?: viewModel.addToCart(prod) },
                            onDecrement = { cartItem?.let { viewModel.decreaseCartQty(it) } }
                        )
                    }
                }
            }
        }
    }

    // Snackbar for add-to-cart flash in Categories
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
    ) { data ->
        Snackbar(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            containerColor = DarkGreenPrimary,
            contentColor = CustomWhite,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = CustomWhite, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(data.visuals.message, color = CustomWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// C. CART SCREEN
// ==========================================
@Composable
fun CartScreen(
    viewModel: BazaarViewModel,
    activity: MainActivity? = null,
    onBackToShopping: () -> Unit
) {
    val cartList by viewModel.currentCart.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val serviceAreas by viewModel.serviceAreas.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Checkout dialog states
    var showCartCheckoutDialog by remember { mutableStateOf(false) }
    var cartCheckoutStep by remember { mutableStateOf(1) } // 1: Address, 2: Payment/Coupon, 3: Success
    var cartTempAddress by remember { mutableStateOf("") }
    var cartTempAddressLat by remember { mutableStateOf(0.0) }
    var cartTempAddressLng by remember { mutableStateOf(0.0) }
    var isFetchingCartLocation by remember { mutableStateOf(false) }
    var cartCouponText by remember { mutableStateOf("") }
    var cartIsCouponApplied by remember { mutableStateOf(false) }
    var generatedCartOrderId by remember { mutableStateOf("ZVB-" + (System.currentTimeMillis() % 100000000).toString()) }
    var estimatedDeliveryDate by remember { mutableStateOf("") }

    // Synchronize default values when user completes database syncing
    LaunchedEffect(user) {
        if (cartTempAddress.isEmpty() && !user?.savedAddress.isNullOrEmpty()) {
            cartTempAddress = user!!.savedAddress
            cartTempAddressLat = user!!.savedAddressLat
            cartTempAddressLng = user!!.savedAddressLng
        }
    }

    LaunchedEffect(showCartCheckoutDialog) {
        if (showCartCheckoutDialog) {
            estimatedDeliveryDate = "In 2-3 Days (Standard)"
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.checkoutSuccessEvent.collect {
            cartCheckoutStep = 3
            showCartCheckoutDialog = true
        }
    }

    // Compute prices
    val computedItems = cartList.mapNotNull { cartItem ->
        val product = allProducts.find { it.id == cartItem.productId }
        if (product != null) Pair(cartItem, product) else null
    }
    val stockIssue = computedItems.firstOrNull { (cartItem, product) ->
        product.stock <= 0 || cartItem.quantity > product.stock
    }

    val totalSubtotal = computedItems.sumOf { it.first.quantity * it.second.price }

    val appliedCoupon = if (cartIsCouponApplied) {
        viewModel.validateCoupon(cartCouponText)
    } else {
        null
    }
    val discountPercent = appliedCoupon?.discountPercent ?: 0
    val cartDiscountAmount = totalSubtotal * (discountPercent / 100.0)
    val cartPayableBeforeDelivery = (totalSubtotal - cartDiscountAmount).coerceAtLeast(0.0)
    val cartDeliveryDistanceKm = estimateDeliveryDistanceKm(generatedCartOrderId)
    val cartDeliveryCharge = calculateDeliveryCharge(cartPayableBeforeDelivery, cartDeliveryDistanceKm)
    val cartFinalTotal = cartPayableBeforeDelivery + cartDeliveryCharge.totalCharge
    val cartPinCode = extractPincode(cartTempAddress)
    val isCartAddressDeliverable = cartPinCode.isNotBlank() && serviceAreas.any {
        it.pinCode.filter { ch -> ch.isDigit() } == cartPinCode
    }
    val cartLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            coroutineScope.launch {
                isFetchingCartLocation = true
                val selection = getCurrentAddressSelection(context)
                cartTempAddress = selection.address
                cartTempAddressLat = selection.latitude
                cartTempAddressLng = selection.longitude
                isFetchingCartLocation = false
            }
        } else {
            Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomWhite)
    ) {
        Text(
            text = "Shopping Cart",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = RichBlack
            ),
            modifier = Modifier.padding(16.dp)
        )

        if (computedItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveShoppingCart,
                    contentDescription = "Empty",
                    tint = MutedText,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your Cart is empty!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RichBlack)
                )
                Text(
                    text = "Add some fresh or premium gear to proceed",
                    color = MutedText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )
                Button(
                    onClick = onBackToShopping,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Search Bazaar Now", color = CustomWhite)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(computedItems, key = { (cartItem, product) -> "${cartItem.id}_${product.id}" }) { (cartItem, product) ->
                    CartItemRow(
                        cartItem = cartItem,
                        product = product,
                        onIncrease = { viewModel.increaseCartQty(cartItem) },
                        onDecrease = { viewModel.decreaseCartQty(cartItem) },
                        onRemove = { viewModel.removeCartItem(cartItem) }
                    )
                }
            }

            // Pricing details card & Checkout Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGrey),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = MutedText)
                        Text("₹${String.format("%.2f", totalSubtotal)}", fontWeight = FontWeight.Bold, color = RichBlack)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivery Charge (${String.format("%.1f", cartDeliveryDistanceKm)} km)", color = MutedText)
                        Text("₹${String.format("%.2f", cartDeliveryCharge.totalCharge)}", fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                    }
                    Text(
                        text = "Fixed ₹${String.format("%.0f", cartDeliveryCharge.fixedCharge)} + ₹${String.format("%.0f", cartDeliveryCharge.perKmCharge)}/km",
                        color = MutedText,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = Color.LightGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = RichBlack)
                        Text("₹${String.format("%.2f", cartFinalTotal)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge, color = DarkGreenPrimary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (stockIssue != null) {
                                val product = stockIssue.second
                                Toast.makeText(context, "${product.name} has only ${product.stock.coerceAtLeast(0)} in stock.", Toast.LENGTH_SHORT).show()
                            } else {
                                showCartCheckoutDialog = true
                                cartCheckoutStep = 1
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("checkout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = "Checkout", tint = CustomWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Proceed to Checkout", fontWeight = FontWeight.Black, color = CustomWhite)
                    }
                }
            }
        }
    }

    // --- Cart Multi-Step Checkout Dialog ---
    if (showCartCheckoutDialog) {
        Dialog(onDismissRequest = { showCartCheckoutDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bazaar Cart Checkout",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkGreenPrimary
                        )
                        IconButton(onClick = { showCartCheckoutDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = RichBlack)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("1. Shipping", "2. Payment", "3. Complete").forEachIndexed { index, sName ->
                            val active = index + 1 == cartCheckoutStep
                            val finished = index + 1 < cartCheckoutStep
                            val color = if (active || finished) DarkGreenPrimary else MutedText
                            Text(
                                text = sName,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Black else FontWeight.Normal,
                                color = color
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = SoftGrey)
                    Spacer(modifier = Modifier.height(16.dp))

                    when (cartCheckoutStep) {
                        1 -> {
                            Text("Step 1: Confirm Shipping Address", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RichBlack)
                            Spacer(modifier = Modifier.height(10.dp))
                            AddressSuggestionField(
                                value = cartTempAddress,
                                onValueChange = { cartTempAddress = it },
                                onAddressSelected = {
                                    cartTempAddress = it.address
                                    cartTempAddressLat = it.latitude
                                    cartTempAddressLng = it.longitude
                                },
                                label = "Delivery Address Details",
                                modifier = Modifier
                                    .fillMaxWidth(),
                                testTag = "cart_checkout_address"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fine || coarse) {
                                        coroutineScope.launch {
                                            isFetchingCartLocation = true
                                            val selection = getCurrentAddressSelection(context)
                                            cartTempAddress = selection.address
                                            cartTempAddressLat = selection.latitude
                                            cartTempAddressLng = selection.longitude
                                            isFetchingCartLocation = false
                                        }
                                    } else {
                                        cartLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, DarkGreenPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isFetchingCartLocation) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DarkGreenPrimary)
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Current Location", modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use Current Location", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val cartServiceText = when {
                                serviceAreas.isEmpty() -> "Delivery not available: admin has not added service pincodes."
                                cartPinCode.isBlank() -> "Select an address with pincode to check delivery."
                                isCartAddressDeliverable -> "Delivery available for pincode $cartPinCode."
                                else -> "Delivery Not Available for pincode $cartPinCode."
                            }
                            Text(
                                text = cartServiceText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCartAddressDeliverable) DarkGreenPrimary else AccentRed
                            )
                            if (serviceAreas.isNotEmpty()) {
                                Text(
                                    text = "Admin service pincodes: ${serviceAreas.joinToString { it.pinCode }}",
                                    fontSize = 10.sp,
                                    color = MutedText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    if (cartTempAddress.isBlank()) {
                                        Toast.makeText(context, "Address cannot be empty", Toast.LENGTH_SHORT).show()
                                    } else if (cartTempAddressLat == 0.0 && cartTempAddressLng == 0.0) {
                                        Toast.makeText(context, "Please select an address suggestion or use current location for lat/long.", Toast.LENGTH_SHORT).show()
                                    } else if (!isCartAddressDeliverable) {
                                        Toast.makeText(context, "Delivery Not Available for this pincode.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateAddress(cartTempAddress, cartTempAddressLat, cartTempAddressLng)
                                        cartCheckoutStep = 2
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Proceed to Payment & Coupon", color = CustomWhite)
                            }
                        }
                        2 -> {
                            Text("Step 2: Applied Coupons & Payment", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RichBlack)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Coupons card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LightGreenSecondary.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, LightGreenSecondary)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    val firstCoupon = viewModel.coupons.value.firstOrNull()
                                    val hintText = if (firstCoupon != null) {
                                        "Apply Coupon Code (Try: ${firstCoupon.code} for ${firstCoupon.discountPercent}% Off!)"
                                    } else {
                                        "Apply Coupon Code (Try: BAZAAR20 for 20% Off!)"
                                    }
                                    Text(hintText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = cartCouponText,
                                            onValueChange = { cartCouponText = it },
                                            placeholder = { Text("Enter Code") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DarkGreenPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                val coupon = viewModel.validateCoupon(cartCouponText)
                                                if (coupon != null) {
                                                    cartIsCouponApplied = true
                                                    Toast.makeText(context, "Coupon ${coupon.code} Applied! ${coupon.discountPercent}% discount!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    cartIsCouponApplied = false
                                                    Toast.makeText(context, "Invalid Code", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(46.dp)
                                        ) {
                                            Text("Apply", fontSize = 11.sp, color = CustomWhite)
                                        }
                                    }

                                    if (cartIsCouponApplied && appliedCoupon != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("✅ Coupon ${appliedCoupon.code} Activated! (${appliedCoupon.discountPercent}% OFF)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Pricing summary card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SoftGrey),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cart Subtotal:", fontSize = 11.sp, color = MutedText)
                                        Text("₹${String.format("%.2f", totalSubtotal)}", fontSize = 11.sp, color = RichBlack)
                                    }
                                    if (cartIsCouponApplied && appliedCoupon != null) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Discount Saved (${appliedCoupon.discountPercent}%):", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                            Text("-₹${String.format("%.2f", cartDiscountAmount)}", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Delivery (${String.format("%.1f", cartDeliveryDistanceKm)} km):", fontSize = 11.sp, color = MutedText)
                                        Text("+₹${String.format("%.2f", cartDeliveryCharge.totalCharge)}", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "Fixed ₹${String.format("%.0f", cartDeliveryCharge.fixedCharge)} + ₹${String.format("%.0f", cartDeliveryCharge.perKmCharge)}/km",
                                        fontSize = 10.sp,
                                        color = MutedText
                                    )
                                    Divider(color = Color.LightGray, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Final Payable:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                        Text("₹${String.format("%.2f", cartFinalTotal)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = DarkGreenPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Razorpay Payment Button
                            Button(
                                onClick = {
                                    if (stockIssue != null) {
                                        val product = stockIssue.second
                                        Toast.makeText(context, "${product.name} has only ${product.stock.coerceAtLeast(0)} in stock.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!isCartAddressDeliverable) {
                                        Toast.makeText(context, "Delivery Not Available for this pincode.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val act = activity ?: (context as? Activity)
                                    if (act != null) {
                                        val summary = computedItems.joinToString(", ") { "${it.second.name} x${it.first.quantity}" }
                                        // Set pending checkout info so Razorpay callback can retrieve it on success
                                        viewModel.setPendingCheckout(
                                            com.example.viewmodel.PendingCheckout(
                                                totalAmount = cartFinalTotal,
                                                summary = summary,
                                                orderId = generatedCartOrderId,
                                                address = cartTempAddress,
                                                coupon = if (cartIsCouponApplied) cartCouponText else "",
                                                addressLat = cartTempAddressLat,
                                                addressLng = cartTempAddressLng,
                                                itemsAmount = cartPayableBeforeDelivery,
                                                deliveryDistanceKm = cartDeliveryDistanceKm,
                                                deliveryFixedCharge = cartDeliveryCharge.fixedCharge,
                                                deliveryPerKmCharge = cartDeliveryCharge.perKmCharge,
                                                deliveryCharge = cartDeliveryCharge.totalCharge
                                            )
                                        )
                                        try {
                                            Checkout.preload(act.applicationContext)
                                            val checkout = Checkout()
                                            checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
                                            val options = JSONObject().apply {
                                                put("name", "ZYL VOR BAZAAR")
                                                put("description", summary.take(255))
                                                put("currency", "INR")
                                                put("amount", (cartFinalTotal * 100).toInt())
                                                put("prefill", JSONObject().apply {
                                                    put("email", user?.email ?: "")
                                                    put("contact", "")
                                                })
                                                put("theme", JSONObject().apply {
                                                    put("color", "#1B5E20")
                                                })
                                            }
                                            checkout.open(act, options)
                                        } catch (e: Exception) {
                                            viewModel.clearPendingCheckout()
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Could not open payment: ${e.message}")
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Payment unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CustomWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pay Securely ₹${String.format("%.2f", cartFinalTotal)}", color = CustomWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                        3 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(Color(0xFFE8F5E9), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "",
                                        tint = DarkGreenPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Order Placed Successfully!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = DarkGreenPrimary
                                    )
                                )
                                Text("All products have been registered for packing.", fontSize = 11.sp, color = MutedText)

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SoftGrey),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Order ID reference:", fontSize = 11.sp, color = MutedText)
                                            Text(generatedCartOrderId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Est. Shipment Delivery:", fontSize = 11.sp, color = MutedText)
                                            Text(estimatedDeliveryDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Shipping Destination:", fontSize = 11.sp, color = MutedText)
                                            Text(cartTempAddress, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RichBlack, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        showCartCheckoutDialog = false
                                        cartCheckoutStep = 1
                                        onBackToShopping()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Done & Back to Bazaar", color = CustomWhite)
                                }
                            }
                        }
                    }
                }
            }
        }
    } // end Column

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    ) { data ->
        Snackbar(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            containerColor = AccentRed,
            contentColor = CustomWhite,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(data.visuals.message, color = CustomWhite, fontWeight = FontWeight.Bold)
        }
    }
    } // end Box
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    product: Product,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val primaryPhotoUrl = product.primaryPhotoUrl()
    val primaryPhotoModel = remember(primaryPhotoUrl) {
        resolveProductImageModel(context, primaryPhotoUrl)
    }
    val canIncrease = cartItem.quantity < product.stock

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CustomWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Representative product leading icon/image
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightGreenSecondary),
                contentAlignment = Alignment.Center
            ) {
                if (primaryPhotoModel != null) {
                    AsyncImage(
                        model = primaryPhotoModel,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.img_hero_banner_1782139859933),
                        error = painterResource(id = R.drawable.img_hero_banner_1782139859933)
                    )
                } else {
                    Icon(
                        imageVector = when(product.category) {
                            "Electronics" -> Icons.Default.Devices
                            "Fresh Products" -> Icons.Default.LocalFlorist
                            "Fashion" -> Icons.Default.ShoppingBag
                            else -> Icons.Default.Kitchen
                        },
                        contentDescription = "",
                        tint = DarkGreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    color = RichBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Category: ${product.category}",
                    fontSize = 11.sp,
                    color = MutedText
                )
                Text(
                    text = "₹${product.price} each",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreenPrimary
                )
                Text(
                    text = if (product.stock > 0) {
                        if (product.stock >= 999) "In stock" else "Available: ${product.stock}"
                    } else {
                        "Out of stock"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (product.stock > 0) DarkGreenPrimary else AccentRed
                )
            }

            // Quantity adjusters
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(SoftGrey, shape = RoundedCornerShape(14.dp))
            ) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Minus", tint = DarkGreenPrimary, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = cartItem.quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = RichBlack
                )
                IconButton(
                    onClick = onIncrease,
                    enabled = canIncrease,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Plus",
                        tint = if (canIncrease) DarkGreenPrimary else MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
            }
        }
    }
}

// ==========================================
// D. WISHLIST SCREEN
// ==========================================
@Composable
fun WishlistScreen(
    viewModel: BazaarViewModel,
    onProductSelect: (Product) -> Unit,
    onProductBuy: (Product) -> Unit
) {
    val wishlist by viewModel.currentWishlist.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    val favedProducts = wishlist.mapNotNull { wish ->
        allProducts.find { it.id == wish.productId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomWhite)
    ) {
        Text(
            text = "My Wishlist",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = RichBlack
            ),
            modifier = Modifier.padding(16.dp)
        )

        if (favedProducts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "",
                    tint = MutedText,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No wishlist items yet.",
                    color = MutedText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap heart on product items to save them here",
                    fontSize = 12.sp,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favedProducts, key = { it.id }) { prod ->
                    ProductItemCard(
                        product = prod,
                        onCallSelect = { onProductSelect(prod) },
                        onCallAddToCart = { viewModel.addToCart(prod) },
                        onCallBuyNow = { onProductBuy(prod) },
                        onCallWishlistToggle = { viewModel.toggleWishlist(prod) },
                        isWishlisted = true
                    )
                }
            }
        }
    }
}

// ==========================================
// E. MY ORDERS SCREEN
// ==========================================
@Composable
fun OrdersScreen(viewModel: BazaarViewModel) {
    val context = LocalContext.current
    val orderList by viewModel.currentOrders.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    var selectedOrderForDetail by remember { mutableStateOf<Order?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomWhite)
    ) {
        Text(
            text = "My Orders",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = RichBlack
            ),
            modifier = Modifier.padding(16.dp)
        )

        if (orderList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Empty",
                    tint = MutedText,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You have placed no orders yet.",
                    color = MutedText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Purchase items from cart to track shipping states",
                    fontSize = 12.sp,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orderList, key = { it.orderId }) { order ->
                    OrderItemCard(
                        order = order,
                        allProducts = allProducts,
                        onClick = { selectedOrderForDetail = order }
                    )
                }
            }
        }
    }

    // --- Flipkart/Amazon/Meesho Style Full Order Details Screen ---
    if (selectedOrderForDetail != null) {
        val order = selectedOrderForDetail!!
        val orderLines = remember(order, allProducts) { buildOrderProductLines(order, allProducts) }
        val displayStatus = orderDisplayStatus(order)
        val placedOn = formatOrderDate(order.orderDate)
        Dialog(onDismissRequest = { selectedOrderForDetail = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedOrderForDetail = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RichBlack)
                            }
                            Text(
                                text = "Order Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = RichBlack
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (displayStatus.uppercase()) {
                                        "DELIVERED", "SUCCESS" -> DarkGreenPrimary.copy(alpha = 0.1f)
                                        "CANCELLED" -> AccentRed.copy(alpha = 0.1f)
                                        else -> LightGreenSecondary
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = displayStatus,
                                fontWeight = FontWeight.Bold,
                                color = when (displayStatus.uppercase()) {
                                    "DELIVERED", "SUCCESS" -> DarkGreenPrimary
                                    "CANCELLED" -> AccentRed
                                    else -> DarkGreenPrimary
                                },
                                fontSize = 11.sp
                            )
                        }
                    }

                    Divider(color = SoftGrey, modifier = Modifier.padding(vertical = 8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Order Metadata Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Order ID", fontSize = 11.sp, color = MutedText)
                                Text(order.orderId, fontWeight = FontWeight.Bold, color = RichBlack, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Placed On", fontSize = 11.sp, color = MutedText)
                                Text(placedOn, fontWeight = FontWeight.Bold, color = RichBlack, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tracking Progress Tracker
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SoftGrey),
                            border = BorderStroke(1.dp, SoftGrey.copy(alpha = 0.8f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Delivery Status Tracker", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                                Spacer(modifier = Modifier.height(10.dp))

                                val isCancelled = displayStatus.equals("Cancelled", ignoreCase = true)
                                val isAccepted = displayStatus.equals("Accepted", ignoreCase = true) || order.sellerConfirmed
                                val isShippingReady = displayStatus.equals("Shipping Ready", ignoreCase = true) || displayStatus.equals("Shipped", ignoreCase = true) || displayStatus.equals("Ready to Deliver", ignoreCase = true)
                                val isOnTheWay = displayStatus.equals("On the Way", ignoreCase = true)
                                val isDelivered = displayStatus.equals("Delivered", ignoreCase = true) || displayStatus.equals("Success", ignoreCase = true)

                                val progressValue = when {
                                    isCancelled -> 0.0f
                                    isDelivered -> 1.0f
                                    isOnTheWay -> 0.75f
                                    isShippingReady -> 0.5f
                                    isAccepted -> 0.25f
                                    else -> 0.1f
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Confirmed", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (!isCancelled) DarkGreenPrimary else MutedText)
                                    Text("Shipping Ready", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isShippingReady || isOnTheWay || isDelivered) DarkGreenPrimary else MutedText)
                                    Text("On the Way", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isOnTheWay || isDelivered) DarkGreenPrimary else MutedText)
                                    Text("Delivered", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDelivered) DarkGreenPrimary else MutedText)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = progressValue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isCancelled) AccentRed else DarkGreenPrimary,
                                    trackColor = SoftGrey.copy(alpha = 0.5f)
                                )

                                if (isCancelled) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("This order was cancelled by seller or customer.", color = AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isDelivered) "Package delivered safely to destination." else "Estimated delivery: Within 2 days.",
                                        fontSize = 11.sp,
                                        color = RichBlack,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Products in This Order", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RichBlack)
                        Spacer(modifier = Modifier.height(8.dp))

                        orderLines.forEach { line ->
                            val matchingProduct = line.product
                            val itemTotal = (matchingProduct?.price ?: 0.0) * line.quantity
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                border = BorderStroke(1.dp, SoftGrey),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Image
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(LightGreenSecondary.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val photoUrl = matchingProduct?.primaryPhotoUrl().orEmpty()
                                        val photoModel = remember(photoUrl) {
                                            resolveProductImageModel(context, photoUrl)
                                        }
                                        if (photoModel != null) {
                                            AsyncImage(
                                                model = photoModel,
                                                contentDescription = matchingProduct?.name ?: line.name,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop,
                                                placeholder = painterResource(id = R.drawable.img_hero_banner_1782139859933),
                                                error = painterResource(id = R.drawable.img_hero_banner_1782139859933)
                                            )
                                        } else {
                                            Icon(Icons.Default.Inventory2, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = matchingProduct?.name ?: line.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = RichBlack,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Qty: ${line.quantity} | Category: ${matchingProduct?.category ?: "Bazaar Premium"}",
                                            fontSize = 10.sp,
                                            color = MutedText
                                        )
                                        Text(
                                            text = matchingProduct?.description?.ifBlank { "No description available" } ?: line.rawText,
                                            fontSize = 10.sp,
                                            color = MutedText,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (matchingProduct != null) "₹${String.format("%.2f", matchingProduct.price)} each" else "Price included",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = DarkGreenPrimary
                                            )
                                            Text(
                                                text = if (matchingProduct != null) "Line total ₹${String.format("%.2f", itemTotal)}" else displayStatus,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RichBlack
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Status: $displayStatus",
                                            fontSize = 10.sp,
                                            color = if (displayStatus.equals("Cancelled", ignoreCase = true)) AccentRed else DarkGreenPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Shipping & Contact Address
                        Text("Shipping Address Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RichBlack)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CustomWhite),
                            border = BorderStroke(1.dp, SoftGrey)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Address", tint = DarkGreenPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = order.deliveryAddress.ifBlank { "Home / Default Registered Address" },
                                        fontSize = 12.sp,
                                        color = RichBlack,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Delivery status: ${order.deliveryStatus.ifBlank { displayStatus }}", fontSize = 10.sp, color = MutedText)
                                    if (order.deliveryAddressLat != 0.0 || order.deliveryAddressLng != 0.0) {
                                        Text(
                                            "Coordinates: ${String.format("%.5f", order.deliveryAddressLat)}, ${String.format("%.5f", order.deliveryAddressLng)}",
                                            fontSize = 10.sp,
                                            color = MutedText
                                        )
                                    }
                                    if (order.deliveryPartnerEmail.isNotBlank()) {
                                        Text("Delivery partner: ${order.deliveryPartnerEmail}", fontSize = 10.sp, color = MutedText)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Pricing and savings break down
                        Text("Payment & Billing Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RichBlack)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CustomWhite),
                            border = BorderStroke(1.dp, SoftGrey)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val deliveryDistanceKm = estimateDeliveryDistanceKm(order.orderId)
                                val itemAmount = order.itemsAmount.takeIf { it > 0.0 }
                                    ?: estimateItemAmountFromOrderTotal(order.totalAmount, deliveryDistanceKm)
                                val deliveryCharge = if (order.deliveryCharge > 0.0) {
                                    com.example.data.DeliveryChargeBreakdown(
                                        distanceKm = order.deliveryDistanceKm.takeIf { it > 0.0 } ?: deliveryDistanceKm,
                                        fixedCharge = order.deliveryFixedCharge,
                                        perKmCharge = order.deliveryPerKmCharge,
                                        totalCharge = order.deliveryCharge
                                    )
                                } else {
                                    calculateDeliveryCharge(itemAmount, deliveryDistanceKm)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Items amount", fontSize = 11.sp, color = MutedText)
                                    Text("₹${String.format("%.2f", itemAmount)}", fontSize = 11.sp, color = RichBlack, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Payment mode", fontSize = 11.sp, color = MutedText)
                                    Text(order.paymentMode, fontSize = 11.sp, color = RichBlack, fontWeight = FontWeight.Bold)
                                }
                                if (order.couponApplied.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Coupon applied (${order.couponApplied})", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                        Text("20% Off Saved!", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Delivery (${String.format("%.1f", deliveryDistanceKm)} km)", fontSize = 11.sp, color = MutedText)
                                    Text("₹${String.format("%.2f", deliveryCharge.totalCharge)}", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Fixed ₹${String.format("%.0f", deliveryCharge.fixedCharge)} + ₹${String.format("%.0f", deliveryCharge.perKmCharge)}/km",
                                    fontSize = 10.sp,
                                    color = MutedText
                                )
                                Divider(color = SoftGrey, modifier = Modifier.padding(vertical = 8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Amount Paid", fontSize = 12.sp, fontWeight = FontWeight.Black, color = RichBlack)
                                    Text("₹${String.format("%.2f", order.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = DarkGreenPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Customer Care / Help section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "Invoice document downloaded!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invoice", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { openHelpWhatsApp(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary)
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = "", tint = CustomWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 11.sp, color = CustomWhite)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { openHelpEmail(context) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary),
                            border = BorderStroke(1.dp, DarkGreenPrimary)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email $HELP_EMAIL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class OrderProductLine(
    val rawText: String,
    val name: String,
    val quantity: Int,
    val product: Product?
)

fun buildOrderProductLines(order: Order, products: List<Product>): List<OrderProductLine> {
    return order.itemsSummary
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { raw ->
            val qty = Regex("""(?:x|Qty:\s*)\s*(\d+)""", RegexOption.IGNORE_CASE)
                .find(raw)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: 1
            val cleanName = raw
                .substringBefore(" x")
                .substringBefore(" Qty:")
                .substringBefore(" (")
                .trim()
                .ifBlank { raw }
            val product = products.find {
                it.name.equals(cleanName, ignoreCase = true) ||
                    cleanName.contains(it.name, ignoreCase = true) ||
                    it.name.contains(cleanName, ignoreCase = true)
            }
            OrderProductLine(rawText = raw, name = cleanName, quantity = qty, product = product)
        }
}

fun orderDisplayStatus(order: Order): String {
    return when {
        order.status.equals("Cancelled", ignoreCase = true) -> "Cancelled"
        order.deliveryStatus.isNotBlank() -> order.deliveryStatus
        order.status.isNotBlank() -> order.status
        else -> "Processing"
    }
}

fun formatOrderDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Date unavailable"
    return java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US)
        .format(java.util.Date(timestamp))
}

@Composable
fun OrderItemCard(
    order: Order,
    allProducts: List<Product>,
    onClick: () -> Unit
) {
    val displayStatus = orderDisplayStatus(order)
    val orderLines = remember(order, allProducts) { buildOrderProductLines(order, allProducts) }
    val progress = orderProgress(displayStatus, order.sellerConfirmed)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing)),
        colors = CardDefaults.cardColors(containerColor = CustomWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftGrey)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderId,
                    fontWeight = FontWeight.Black,
                    color = DarkGreenPrimary
                )
                Box(
                    modifier = Modifier
                        .background(
                            if (displayStatus.equals("Cancelled", ignoreCase = true)) AccentRed.copy(alpha = 0.1f) else LightGreenSecondary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = displayStatus,
                        fontWeight = FontWeight.Bold,
                        color = if (displayStatus.equals("Cancelled", ignoreCase = true)) AccentRed else DarkGreenPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Items: ${orderLines.joinToString { "${it.name} x${it.quantity}" }}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RichBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Paid: ₹${String.format("%.2f", order.totalAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = RichBlack
                )
                Text(
                    text = formatOrderDate(order.orderDate),
                    fontSize = 12.sp,
                    color = MutedText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Logistics progress bar UI
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Processing", fontSize = 10.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                    Text("In Transit", fontSize = 10.sp, color = if (progress >= 0.5f) DarkGreenPrimary else MutedText)
                    Text("Delivered", fontSize = 10.sp, color = if (progress >= 1f) DarkGreenPrimary else MutedText)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (displayStatus.equals("Cancelled", ignoreCase = true)) AccentRed else DarkGreenPrimary,
                    trackColor = SoftGrey
                )
            }
        }
    }
}

fun orderProgress(status: String, sellerConfirmed: Boolean): Float {
    return when {
        status.equals("Cancelled", ignoreCase = true) -> 0f
        status.equals("Delivered", ignoreCase = true) || status.equals("Success", ignoreCase = true) -> 1f
        status.equals("On the Way", ignoreCase = true) -> 0.75f
        status.equals("Shipping Ready", ignoreCase = true) ||
            status.equals("Ready to Deliver", ignoreCase = true) ||
            status.equals("Shipped", ignoreCase = true) -> 0.5f
        status.equals("Accepted", ignoreCase = true) || sellerConfirmed -> 0.25f
        else -> 0.1f
    }
}

// ==========================================
// F. PROFILE SCREEN (Highly structured, settings)
// ==========================================
@Composable
fun ProfileScreen(
    viewModel: BazaarViewModel,
    onLogout: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val orders by viewModel.currentOrders.collectAsState()
    val context = LocalContext.current
    val onlinePaymentOrders = orders.filter { !it.paymentMode.equals("COD", ignoreCase = true) }

    // Dialog state controllers
    var showEditProfile by remember { mutableStateOf(false) }
    var showAddresses by remember { mutableStateOf(false) }
    var showCards by remember { mutableStateOf(false) }
    var showPaymentHistory by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showHelpCenter by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomWhite)
    ) {
        // User identity leading info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightGreenSecondary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar display built neatly
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(DarkGreenPrimary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = CustomWhite
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user?.name ?: "ZVB User",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RichBlack
                        )
                    )

                    Text(
                        text = user?.email ?: "user@zylvorbazaar.com",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MutedText
                        )
                    )

                    if (user != null && (!user!!.phone.isNullOrBlank() || !user!!.gender.isNullOrBlank())) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!user!!.phone.isNullOrBlank()) {
                                Text(
                                    text = "📞 ${user!!.phone}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                                )
                            }
                            if (!user!!.gender.isNullOrBlank()) {
                                Text(
                                    text = "👤 ${user!!.gender}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Headings: Profile customization options
        item {
            Text(
                text = "ACCOUNT PREFERENCES",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MutedText
                ),
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
            )
        }

        // Row: Edit Profile dialog trigger
        item {
            ProfileInteractiveRow(
                icon = Icons.Default.Edit,
                title = "Edit Profile Info",
                description = "Change full name & account identity fields",
                onClick = { showEditProfile = true }
            )
        }

        // Row: Saved Credit Cards dialog trigger
        item {
            ProfileInteractiveRow(
                icon = Icons.Default.Payment,
                title = "Saved Credit / Debit & Gift Cards",
                description = user?.savedCards ?: "No saved standard bank cards",
                onClick = { showCards = true }
            )
        }

        item {
            ProfileInteractiveRow(
                icon = Icons.Default.History,
                title = "Payment History",
                description = if (onlinePaymentOrders.isEmpty()) {
                    "No online payments completed yet"
                } else {
                    "${onlinePaymentOrders.size} online payment record(s)"
                },
                onClick = { showPaymentHistory = true }
            )
        }

        // Row: Saved Addresses dialog trigger
        item {
            ProfileInteractiveRow(
                icon = Icons.Default.HomeWork,
                title = "Saved Addresses >",
                description = user?.savedAddress ?: "No registered home delivery coordinates",
                onClick = { showAddresses = true }
            )
        }

        // Settings headings
        item {
            Text(
                text = "GLOBAL ENVIRONMENT",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MutedText
                ),
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
            )
        }

        // Row: Select Language dialog trigger
        item {
            ProfileInteractiveRow(
                icon = Icons.Default.Language,
                title = "Select Language >",
                description = "Currently set to: ${user?.selectedLanguage ?: "English"}",
                onClick = { showLanguage = true }
            )
        }

        // Row: Notification settings switcher inline widget
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                border = BorderStroke(1.dp, SoftGrey),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "", tint = DarkGreenPrimary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Notification Settings >", fontWeight = FontWeight.Bold, color = RichBlack)
                            Text(
                                text = if (user?.notificationsEnabled == true) "Real-time alerts active" else "System silent state",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                    }

                    Switch(
                        checked = user?.notificationsEnabled ?: true,
                        onCheckedChange = { viewModel.toggleNotifications(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CustomWhite,
                            checkedTrackColor = DarkGreenPrimary,
                            uncheckedThumbColor = CustomWhite,
                            uncheckedTrackColor = MutedText
                        )
                    )
                }
            }
        }

        // Row: Privacy Center dialog trigger
        item {
            ProfileInteractiveRow(
                icon = Icons.Default.Security,
                title = "Privacy Center >",
                description = if (user?.privacyAccepted == true) "Privacy terms accepted for this account" else "Review and accept privacy terms",
                onClick = { showPrivacy = true }
            )
        }

        item {
            ProfileInteractiveRow(
                icon = Icons.Default.SupportAgent,
                title = "Help Center",
                description = "WhatsApp $HELP_DISPLAY_MOBILE or email $HELP_EMAIL",
                onClick = { showHelpCenter = true }
            )
        }

        // Logout
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(50.dp)
                    .testTag("logout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = CustomWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out securely", color = CustomWhite)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- Sub-Dialog Popup: Edit Profile Info ---
    if (showEditProfile) {
        var tempName by remember(showEditProfile, user?.email) { mutableStateOf(user?.name ?: "") }
        var tempPhone by remember(showEditProfile, user?.email) { mutableStateOf(user?.phone ?: "") }
        var tempGender by remember(showEditProfile, user?.email) { mutableStateOf(user?.gender?.ifBlank { "Male" } ?: "Male") }

        Dialog(onDismissRequest = { showEditProfile = false }) {
            Surface(
                modifier = Modifier.shadow(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Edit Profile Info", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RichBlack)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkGreenPrimary,
                            unfocusedBorderColor = MutedText,
                            focusedTextColor = RichBlack,
                            unfocusedTextColor = RichBlack
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = user?.email ?: "",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = SoftGrey,
                            disabledTextColor = MutedText,
                            disabledLabelColor = MutedText
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkGreenPrimary,
                            unfocusedBorderColor = MutedText,
                            focusedTextColor = RichBlack,
                            unfocusedTextColor = RichBlack
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Gender selection
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                        Text("Gender", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = RichBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val genders = listOf("Male", "Female", "Other")
                            genders.forEach { gender ->
                                val isSelected = tempGender == gender
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) DarkGreenPrimary else LightGreenSecondary,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { tempGender = gender }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = gender,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CustomWhite else DarkGreenPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditProfile = false }) { 
                            Text("Cancel", color = MutedText) 
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (tempName.isBlank()) {
                                    Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateProfile(tempName, tempPhone, tempGender)
                                    showEditProfile = false
                                    Toast.makeText(context, "Identity Profile Updated", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary)
                        ) {
                            Text("Save changes", color = CustomWhite)
                        }
                    }
                }
            }
        }
    }

    // --- Sub-Dialog Popup: Saved Credit / Debit Cards ---
    if (showCards) {
        var cardTemp by remember(showCards, user?.savedCards) { mutableStateOf(user?.savedCards ?: "") }
        Dialog(onDismissRequest = { showCards = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                modifier = Modifier.shadow(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Credit / Debit & Gift Cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Add your payment tokens securely. This metadata is saved locally inside Room storage.", fontSize = 12.sp, color = MutedText)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = cardTemp,
                        onValueChange = { cardTemp = it },
                        placeholder = { Text("e.g. *1111 (Emerald Gold Premium)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = {
                                viewModel.updateCards("")
                                cardTemp = ""
                                Toast.makeText(context, "Saved payment method removed", Toast.LENGTH_SHORT).show()
                            },
                            enabled = cardTemp.isNotBlank()
                        ) { Text("Remove", color = AccentRed) }
                        Spacer(modifier = Modifier.width(10.dp))
                        TextButton(onClick = { showCards = false }) { Text("Close") }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                viewModel.updateCards(cardTemp)
                                showCards = false
                                Toast.makeText(context, "Payment Method Configured", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary)
                        ) {
                            Text("Update Payment")
                        }
                    }
                }
            }
        }
    }

    if (showPaymentHistory) {
        val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
        Dialog(onDismissRequest = { showPaymentHistory = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                modifier = Modifier.shadow(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text("Payment History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RichBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Only completed online payments are shown here.", fontSize = 12.sp, color = MutedText)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (onlinePaymentOrders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No online payment history yet.", color = MutedText, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(onlinePaymentOrders) { order ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SoftGrey),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(order.orderId, fontWeight = FontWeight.Black, color = RichBlack, fontSize = 12.sp)
                                            Text("₹${String.format("%.2f", order.totalAmount)}", fontWeight = FontWeight.Black, color = DarkGreenPrimary, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(order.itemsSummary, color = MutedText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Paid on ${dateFormatter.format(java.util.Date(order.orderDate))}", color = RichBlack, fontSize = 11.sp)
                                        Text("Mode: ${order.paymentMode}", color = MutedText, fontSize = 11.sp)
                                        if (order.paymentTransactionId.isNotBlank()) {
                                            Text("Txn: ${order.paymentTransactionId}", color = MutedText, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPaymentHistory = false }) {
                            Text("Close", color = DarkGreenPrimary)
                        }
                    }
                }
            }
        }
    }

    // --- Sub-Dialog Popup: Saved Addresses ---
    if (showAddresses) {
        var addrTemp by remember(showAddresses, user?.savedAddress) { mutableStateOf(user?.savedAddress ?: "") }
        var addrLat by remember(showAddresses, user?.savedAddressLat) { mutableStateOf(user?.savedAddressLat ?: 0.0) }
        var addrLng by remember(showAddresses, user?.savedAddressLng) { mutableStateOf(user?.savedAddressLng ?: 0.0) }
        Dialog(onDismissRequest = { showAddresses = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                modifier = Modifier.shadow(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Registered Address Coordinates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    AddressSuggestionField(
                        value = addrTemp,
                        onValueChange = { addrTemp = it },
                        onAddressSelected = {
                            addrTemp = it.address
                            addrLat = it.latitude
                            addrLng = it.longitude
                        },
                        label = "Saved Delivery Address",
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = {
                                viewModel.updateAddress("", 0.0, 0.0)
                                addrTemp = ""
                                addrLat = 0.0
                                addrLng = 0.0
                                Toast.makeText(context, "Delivery address removed", Toast.LENGTH_SHORT).show()
                            },
                            enabled = addrTemp.isNotBlank()
                        ) { Text("Remove", color = AccentRed) }
                        Spacer(modifier = Modifier.width(10.dp))
                        TextButton(onClick = { showAddresses = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                viewModel.updateAddress(addrTemp, addrLat, addrLng)
                                showAddresses = false
                                Toast.makeText(context, "Delivery Address Saved", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary)
                        ) {
                            Text("Set Destination")
                        }
                    }
                }
            }
        }
    }

    // --- Sub-Dialog Popup: Language Selection ---
    if (showLanguage) {
        val list = languages.ifEmpty { listOf("English", "Español", "Français", "Deutsch", "Hindi", "Bengali") }
        Dialog(onDismissRequest = { showLanguage = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                modifier = Modifier.shadow(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Select App Language", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    list.forEach { lang ->
                        val isSelected = user?.selectedLanguage == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage(lang)
                                    showLanguage = false
                                    Toast.makeText(context, "Language switched to $lang", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = lang, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "", tint = DarkGreenPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Sub-Dialog Popup: Privacy Center ---
    if (showPrivacy) {
        var accepted by remember(showPrivacy, user?.privacyAccepted) { mutableStateOf(user?.privacyAccepted ?: false) }
        Dialog(onDismissRequest = { showPrivacy = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                modifier = Modifier.shadow(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = "", tint = DarkGreenPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Bazaar Privacy Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "1. Data Storage: Your account credentials, password hashes, cart listings, and purchase receipts are stored securely in local Android SQLite (Room persistence library).\n\n" +
                               "2. Analytics: No unsolicited telemetry logs are transmitted. We honor Google Android security guidelines. All coordinates remain system local.\n\n" +
                               "3. Cookies & Cache: Temporary catalog visual memory resides locally in cache folders.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = RichBlack
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { accepted = !accepted }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = accepted,
                            onCheckedChange = { accepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = DarkGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I accept the privacy terms for this account.",
                            fontSize = 13.sp,
                            color = RichBlack,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.updatePrivacyAccepted(accepted)
                            showPrivacy = false
                            Toast.makeText(
                                context,
                                if (accepted) "Privacy preferences accepted" else "Privacy acceptance removed",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary)
                    ) {
                        Text(if (accepted) "Save Privacy Preference" else "Save Without Acceptance")
                    }
                }
            }
        }
    }

    if (showHelpCenter) {
        Dialog(onDismissRequest = { showHelpCenter = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                modifier = Modifier.shadow(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "", tint = DarkGreenPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Help Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = RichBlack)
                    }
                    Text(
                        text = "Contact ZYL VOR BAZAAR support directly.",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                    Button(
                        onClick = { openHelpWhatsApp(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "", tint = CustomWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp $HELP_DISPLAY_MOBILE", color = CustomWhite, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { openHelpEmail(context) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, DarkGreenPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(HELP_EMAIL, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showHelpCenter = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close", color = MutedText)
                    }
                }
            }
        }
    }
}

fun openHelpWhatsApp(context: android.content.Context) {
    val message = Uri.encode("Hello ZYL VOR BAZAAR support, I need help with my account/order.")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$HELP_WHATSAPP_NUMBER?text=$message"))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp is unavailable on this device", Toast.LENGTH_SHORT).show()
    }
}

fun openHelpEmail(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$HELP_EMAIL")
        putExtra(Intent.EXTRA_SUBJECT, "ZYL VOR BAZAAR Help Request")
        putExtra(Intent.EXTRA_TEXT, "Hello ZYL VOR BAZAAR support,\n\nI need help with ")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

// Utility to verify nullability checks for boolean structures
fun isSelectedAndTrue(status: Boolean?): Boolean {
    return status == true
}

@Composable
fun ProfileInteractiveRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    accentColor: Color = DarkGreenPrimary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CustomWhite),
        border = BorderStroke(1.dp, SoftGrey),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = RichBlack
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "",
                tint = MutedText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun DirectBuyCheckoutDialog(
    product: Product,
    viewModel: BazaarViewModel,
    activity: MainActivity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val serviceAreas by viewModel.serviceAreas.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var step by remember(product.id) { mutableStateOf(1) }
    var address by remember(product.id, user?.savedAddress) { mutableStateOf(user?.savedAddress ?: "") }
    var addressLat by remember(product.id, user?.savedAddressLat) { mutableStateOf(user?.savedAddressLat ?: 0.0) }
    var addressLng by remember(product.id, user?.savedAddressLng) { mutableStateOf(user?.savedAddressLng ?: 0.0) }
    var isFetchingLocation by remember(product.id) { mutableStateOf(false) }
    var couponText by remember(product.id) { mutableStateOf("") }
    var couponApplied by remember(product.id) { mutableStateOf(false) }
    val orderId = remember(product.id) { "ZVB-" + java.util.UUID.randomUUID().toString().uppercase().take(8) }
    val appliedCoupon = if (couponApplied) viewModel.validateCoupon(couponText) else null
    val discountAmount = product.price * ((appliedCoupon?.discountPercent ?: 0) / 100.0)
    val payableBeforeDelivery = (product.price - discountAmount).coerceAtLeast(0.0)
    val deliveryDistanceKm = estimateDeliveryDistanceKm(orderId)
    val deliveryCharge = calculateDeliveryCharge(payableBeforeDelivery, deliveryDistanceKm)
    val finalAmount = payableBeforeDelivery + deliveryCharge.totalCharge
    val pinCode = extractPincode(address)
    val isAddressDeliverable = pinCode.isNotBlank() && serviceAreas.any {
        it.pinCode.filter { ch -> ch.isDigit() } == pinCode
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            coroutineScope.launch {
                isFetchingLocation = true
                val selection = getCurrentAddressSelection(context)
                address = selection.address
                addressLat = selection.latitude
                addressLng = selection.longitude
                isFetchingLocation = false
            }
        } else {
            Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.checkoutSuccessEvent.collect {
            step = 3
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = CustomWhite,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Buy Now", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = DarkGreenPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = RichBlack)
                    }
                }

                Text(product.name, fontSize = 12.sp, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(14.dp))

                when (step) {
                    1 -> {
                        Text("Confirm Delivery Address", fontWeight = FontWeight.Bold, color = RichBlack)
                        Spacer(modifier = Modifier.height(10.dp))
                        AddressSuggestionField(
                            value = address,
                            onValueChange = { address = it },
                            onAddressSelected = {
                                address = it.address
                                addressLat = it.latitude
                                addressLng = it.longitude
                            },
                            label = "Delivery Address",
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (fine || coarse) {
                                    coroutineScope.launch {
                                        isFetchingLocation = true
                                        val selection = getCurrentAddressSelection(context)
                                        address = selection.address
                                        addressLat = selection.latitude
                                        addressLng = selection.longitude
                                        isFetchingLocation = false
                                    }
                                } else {
                                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, DarkGreenPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isFetchingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DarkGreenPrimary)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = "Current Location", modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use Current Location", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val serviceText = when {
                            serviceAreas.isEmpty() -> "Delivery not available: admin has not added service pincodes."
                            pinCode.isBlank() -> "Select an address with pincode to check delivery."
                            isAddressDeliverable -> "Delivery available for pincode $pinCode."
                            else -> "Delivery Not Available for pincode $pinCode."
                        }
                        Text(
                            text = serviceText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAddressDeliverable) DarkGreenPrimary else AccentRed
                        )
                        if (serviceAreas.isNotEmpty()) {
                            Text(
                                text = "Admin service pincodes: ${serviceAreas.joinToString { it.pinCode }}",
                                fontSize = 10.sp,
                                color = MutedText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (address.isBlank()) {
                                    Toast.makeText(context, "Address cannot be empty", Toast.LENGTH_SHORT).show()
                                } else if (addressLat == 0.0 && addressLng == 0.0) {
                                    Toast.makeText(context, "Please select an address suggestion or use current location for lat/long.", Toast.LENGTH_SHORT).show()
                                } else if (!isAddressDeliverable) {
                                    Toast.makeText(context, "Delivery Not Available for this pincode.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateAddress(address, addressLat, addressLng)
                                    step = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Continue to Payment", color = CustomWhite)
                        }
                    }
                    2 -> {
                        Text("Payment", fontWeight = FontWeight.Bold, color = RichBlack)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = couponText,
                                onValueChange = { couponText = it },
                                placeholder = { Text("Coupon Code") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DarkGreenPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    val coupon = viewModel.validateCoupon(couponText)
                                    couponApplied = coupon != null
                                    Toast.makeText(
                                        context,
                                        if (coupon != null) "Coupon ${coupon.code} applied" else "Invalid Coupon Code",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text("Apply", fontSize = 11.sp, color = CustomWhite)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SoftGrey), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Item price", fontSize = 12.sp, color = MutedText)
                                    Text("₹${String.format("%.2f", product.price)}", fontSize = 12.sp, color = RichBlack)
                                }
                                if (appliedCoupon != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${appliedCoupon.code} discount", fontSize = 12.sp, color = DarkGreenPrimary)
                                        Text("-₹${String.format("%.2f", discountAmount)}", fontSize = 12.sp, color = DarkGreenPrimary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Delivery (${String.format("%.1f", deliveryDistanceKm)} km)", fontSize = 12.sp, color = MutedText)
                                    Text("+₹${String.format("%.2f", deliveryCharge.totalCharge)}", fontSize = 12.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Fixed ₹${String.format("%.0f", deliveryCharge.fixedCharge)} + ₹${String.format("%.0f", deliveryCharge.perKmCharge)}/km",
                                    fontSize = 10.sp,
                                    color = MutedText
                                )
                                Divider(color = Color.LightGray, modifier = Modifier.padding(vertical = 6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total", fontWeight = FontWeight.Bold, color = RichBlack)
                                    Text("₹${String.format("%.2f", finalAmount)}", fontWeight = FontWeight.Black, color = DarkGreenPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (!isAddressDeliverable) {
                                    Toast.makeText(context, "Delivery Not Available for this pincode.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val act = activity ?: (context as? Activity)
                                if (act == null) {
                                    Toast.makeText(context, "Payment unavailable", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val summary = "${product.name} x1"
                                viewModel.setPendingCheckout(
                                    com.example.viewmodel.PendingCheckout(
                                        totalAmount = finalAmount,
                                        summary = summary,
                                        orderId = orderId,
                                        address = address,
                                        coupon = if (couponApplied) couponText else "",
                                        addressLat = addressLat,
                                        addressLng = addressLng,
                                        itemsAmount = payableBeforeDelivery,
                                        deliveryDistanceKm = deliveryDistanceKm,
                                        deliveryFixedCharge = deliveryCharge.fixedCharge,
                                        deliveryPerKmCharge = deliveryCharge.perKmCharge,
                                        deliveryCharge = deliveryCharge.totalCharge,
                                        clearCartAfterCheckout = false
                                    )
                                )
                                try {
                                    Checkout.preload(act.applicationContext)
                                    val checkout = Checkout()
                                    checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
                                    val options = JSONObject().apply {
                                        put("name", "ZYL VOR BAZAAR")
                                        put("description", summary.take(255))
                                        put("currency", "INR")
                                        put("amount", (finalAmount * 100).toInt())
                                        put("prefill", JSONObject().apply {
                                            put("email", user?.email ?: "")
                                            put("contact", user?.phone ?: "")
                                        })
                                        put("theme", JSONObject().apply { put("color", "#1B5E20") })
                                    }
                                    checkout.open(act, options)
                                } catch (e: Exception) {
                                    viewModel.clearPendingCheckout()
                                    Toast.makeText(context, "Could not open payment: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CustomWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay Securely ₹${String.format("%.2f", finalAmount)}", color = CustomWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                    3 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DarkGreenPrimary, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Order Placed Successfully!", fontWeight = FontWeight.Black, color = DarkGreenPrimary)
                            Text(orderId, fontSize = 12.sp, color = MutedText)
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Continue Shopping", color = CustomWhite)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// G. PRODUCT DETAIL PANE (Premium Expanded details)
// ==========================================
@Composable
fun ProductDetailPane(
    product: Product,
    onBack: () -> Unit,
    viewModel: BazaarViewModel
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    var isFaved = viewModel.isWishlisted(product.id)
    val user by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val serviceAreas by viewModel.serviceAreas.collectAsState()
    val seller = allUsers.find { it.email.equals(product.sellerEmail, ignoreCase = true) }
    val sellerDisplayName = seller?.shopName?.ifBlank { seller.name }?.ifBlank { "ZVB Certified Seller" } ?: "ZVB Certified Seller"
    val isInStock = product.stock > 0
    val stockLabel = if (product.stock >= 999) "In Stock (ZVB Assured)" else "Stock Available: ${product.stock}"

    var showCheckoutDialog by remember { mutableStateOf(false) }
    var checkoutStep by remember { mutableStateOf(1) } // 1: Delivery Address confirmation, 2: Secure Payment & Coupon, 3: Success info
    var tempAddress by remember { mutableStateOf(user?.savedAddress ?: "") }
    var tempAddressLat by remember { mutableStateOf(user?.savedAddressLat ?: 0.0) }
    var tempAddressLng by remember { mutableStateOf(user?.savedAddressLng ?: 0.0) }
    var isFetchingDirectLocation by remember { mutableStateOf(false) }
    var tempCard by remember { mutableStateOf(user?.savedCards ?: "") }
    var directOrderId by remember { mutableStateOf("") }
    var directDeliveryDate by remember { mutableStateOf("") }
    val carouselImages = remember(product.imageUrlName, product.extraImages) { product.photoEntries() }
    val displayCount = if (carouselImages.isNotEmpty()) carouselImages.size else 3
    val pagerState = rememberPagerState(pageCount = { displayCount })
    val coroutineScope = rememberCoroutineScope()

    // Pincode validation state
    var pincodeText by remember { mutableStateOf("") }
    var pincodeStatus by remember { mutableStateOf("") }

    // Accordion expand state
    var specsExpanded by remember { mutableStateOf(false) }

    // Promo Code state in direct buy dialog
    var couponText by remember { mutableStateOf("") }
    var isCouponApplied by remember { mutableStateOf(false) }

    val appliedCoupon = if (isCouponApplied) {
        viewModel.validateCoupon(couponText)
    } else {
        null
    }
    val discountPercent = appliedCoupon?.discountPercent ?: 0
    val discountAmount = product.price * (discountPercent / 100.0)
    val payableBeforeDelivery = (product.price - discountAmount).coerceAtLeast(0.0)
    val deliveryDistanceKm = estimateDeliveryDistanceKm(directOrderId)
    val deliveryCharge = calculateDeliveryCharge(payableBeforeDelivery, deliveryDistanceKm)
    val finalTotalAmount = payableBeforeDelivery + deliveryCharge.totalCharge
    val directPinCode = extractPincode(tempAddress)
    val isDirectAddressDeliverable = directPinCode.isNotBlank() && serviceAreas.any {
        it.pinCode.filter { ch -> ch.isDigit() } == directPinCode
    }
    val directLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            coroutineScope.launch {
                isFetchingDirectLocation = true
                val selection = getCurrentAddressSelection(context)
                tempAddress = selection.address
                tempAddressLat = selection.latitude
                tempAddressLng = selection.longitude
                isFetchingDirectLocation = false
            }
        } else {
            Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    // Synchronize default values when user completes database syncing
    LaunchedEffect(user) {
        if (tempAddress.isEmpty() && !user?.savedAddress.isNullOrEmpty()) {
            tempAddress = user!!.savedAddress
            tempAddressLat = user!!.savedAddressLat
            tempAddressLng = user!!.savedAddressLng
        }
        if (tempCard.isEmpty() && !user?.savedCards.isNullOrEmpty()) {
            tempCard = user!!.savedCards
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.checkoutSuccessEvent.collect {
            checkoutStep = 3
            showCheckoutDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomWhite)
            .verticalScroll(rememberScrollState())
    ) {
        // Upper action navigation bar & image visual gallery
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .background(SoftGrey)
        ) {
            // Sliding HorizontalPager feature
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (carouselImages.isNotEmpty() && page < carouselImages.size) {
                        val currentImg = carouselImages[page]
                        val currentModel = remember(currentImg) {
                            resolveProductImageModel(context, currentImg)
                        }
                        if (currentModel != null) {
                            AsyncImage(
                                model = currentModel,
                                contentDescription = product.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.img_hero_banner_1782139859933),
                                error = painterResource(id = R.drawable.img_hero_banner_1782139859933)
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = when (page % 3) {
                                        0 -> when (product.category) {
                                            "Electronics" -> Icons.Default.Devices
                                            "Fresh Products" -> Icons.Default.LocalFlorist
                                            "Fashion" -> Icons.Default.ShoppingBag
                                            else -> Icons.Default.Kitchen
                                        }
                                        1 -> Icons.Default.Inventory
                                        else -> Icons.Default.WorkspacePremium
                                    },
                                    contentDescription = "",
                                    tint = DarkGreenPrimary,
                                    modifier = Modifier.size(90.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when (page % 3) {
                                        0 -> "Primary Product View"
                                        1 -> "Secure Boxed Packaging"
                                        else -> "Quality Inspection Certified"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedText
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = when (page) {
                                    0 -> when (product.category) {
                                        "Electronics" -> Icons.Default.Devices
                                        "Fresh Products" -> Icons.Default.LocalFlorist
                                        "Fashion" -> Icons.Default.ShoppingBag
                                        else -> Icons.Default.Kitchen
                                    }
                                    1 -> Icons.Default.Inventory
                                    else -> Icons.Default.WorkspacePremium
                                },
                                contentDescription = "",
                                tint = DarkGreenPrimary,
                                modifier = Modifier.size(90.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (page) {
                                    0 -> "Primary Product View"
                                    1 -> "Secure Boxed Packaging"
                                    else -> "Quality Inspection Certified"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedText
                            )
                        }
                    }
                }
            }

            // Top Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(CustomWhite.copy(alpha = 0.9f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RichBlack)
                }

                IconButton(
                    onClick = { viewModel.toggleWishlist(product) },
                    modifier = Modifier
                        .background(CustomWhite.copy(alpha = 0.9f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isFaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Fave",
                        tint = if (isFaved) AccentRed else RichBlack
                    )
                }
            }

            // Carousel Dots / Thumbnail selector at bottom of image box like Flipkart
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(displayCount) { index ->
                    val active = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = if (active) DarkGreenPrimary else CustomWhite.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (active) CustomWhite else RichBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Descriptive metadata block
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Category tag & Stock Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(LightGreenSecondary, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = product.category.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreenPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (isInStock) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isInStock) stockLabel else "Out of Stock",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isInStock) Color(0xFF2E7D32) else AccentRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = RichBlack
                )
            )

            // Rating Stars & Flipkart-style breakdown metrics popover
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .background(SoftGrey.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF388E3C), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${product.rating}",
                                color = CustomWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.Star, contentDescription = "", tint = AccentGold, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "2,482 verified customer reviews",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RichBlack
                        )
                        Text(
                            text = "98% Positive Buyer Feedback • 4.8 Seller score",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = SoftGrey)
                Spacer(modifier = Modifier.height(8.dp))

                // Breakdown list
                listOf(
                    Pair("5 Star", 0.84f),
                    Pair("4 Star", 0.12f),
                    Pair("3 Star", 0.04f)
                ).forEach { (label, fraction) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp), color = MutedText)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .background(SoftGrey, shape = CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .background(DarkGreenPrimary, shape = CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${(fraction * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = RichBlack)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Pricing details block
            Text(
                text = "SPECIAL DISCOUNT PRICE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "₹${product.price}",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = DarkGreenPrimary
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Original price: ₹${product.originalPrice}",
                        color = MutedText,
                        fontSize = 13.sp,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = "Save ₹${String.format("%.2f", product.originalPrice - product.price)} instantly",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentRed
                    )
                }
            }

            Divider(color = SoftGrey, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

            // --- SELLER INFORMATION ---
            Card(
                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                border = BorderStroke(1.dp, SoftGrey),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(LightGreenSecondary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sold by: $sellerDisplayName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RichBlack
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "4.9 ★ Rated Seller", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "• ZVB Certified", fontSize = 11.sp, color = MutedText)
                        }
                    }
                    Icon(Icons.Default.Verified, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- INTERACTIVE PINCODE DELIVERY ESTIMATOR ---
            Card(
                colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, SoftGrey),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Delivery Speed & Availability",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = RichBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pincodeText,
                            onValueChange = { pincodeText = it },
                            placeholder = { Text("Enter 5-digit Pincode", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkGreenPrimary,
                                unfocusedBorderColor = SoftGrey,
                                focusedContainerColor = CustomWhite,
                                unfocusedContainerColor = CustomWhite
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val normalizedPin = pincodeText.filter { it.isDigit() }
                                if (normalizedPin.length >= 5) {
                                    pincodeStatus = if (viewModel.isPincodeDeliverable(normalizedPin)) {
                                        "Delivery available for pincode $normalizedPin."
                                    } else {
                                        "Delivery Not Available for pincode $normalizedPin."
                                    }
                                } else {
                                    pincodeStatus = "Invalid pincode. Enter at least 5 digits."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Verify", color = CustomWhite, fontSize = 12.sp)
                        }
                    }

                    if (pincodeStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pincodeStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pincodeStatus.startsWith("Delivery available")) DarkGreenPrimary else AccentRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expandable Technical Specs Accordion
            Card(
                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                border = BorderStroke(1.dp, SoftGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { specsExpanded = !specsExpanded },
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsSuggest, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Technical Specifications",
                                fontWeight = FontWeight.Bold,
                                color = RichBlack,
                                fontSize = 13.sp
                            )
                        }
                        Icon(
                            imageVector = if (specsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "",
                            tint = MutedText
                        )
                    }

                    if (specsExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = SoftGrey)
                        Spacer(modifier = Modifier.height(8.dp))

                        val specData = listOf(
                            Pair("Manufacturer", "Zyl Vor Global Group Ltd."),
                            Pair("Product Material", "Premium Bio-Sourced Eco Polymers"),
                            Pair("Net Weight", "340 grams (Eco-lightweight)"),
                            Pair("Certified Carbon Score", "A+ Certified Carbon Neutral"),
                            Pair("Warranty Duration", "1-Year Full Care Comprehensive Warranty")
                        )

                        specData.forEach { (key, valString) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = key, fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                                Text(text = valString, fontSize = 11.sp, color = RichBlack, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Technical details description overview
            Text(
                text = "Overview & Specifications",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = RichBlack
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    color = RichBlack.copy(alpha = 0.85f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Delivery Assurance card
            Card(
                colors = CardDefaults.cardColors(containerColor = LightGreenSecondary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Moped, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Guaranteed Emerald Rapid Delivery", fontWeight = FontWeight.Bold, color = RichBlack, fontSize = 13.sp)
                        Text("Order within 2 hours to receive it tomorrow morning.", fontSize = 11.sp, color = MutedText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- CUSTOMER REVIEW COMMENTS LIST ---
            Text(
                text = "Verified Buyer Reviews",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = RichBlack
            )
            Spacer(modifier = Modifier.height(10.dp))

            listOf(
                Triple("Sophia Miller", "Incredible premium feel. Spotless zero carbon packaging and arrived exactly on time! Highly recommend.", "5.0 ★"),
                Triple("Liam Jackson", "Fabulous fit and finish. Built like a luxury item and works exactly as described.", "4.8 ★")
            ).forEach { (author, review, ratingVal) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = author, fontSize = 11.sp, fontWeight = FontWeight.Black, color = RichBlack)
                            Box(
                                modifier = Modifier
                                    .background(DarkGreenPrimary, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = ratingVal, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CustomWhite)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "\"$review\"", fontSize = 11.sp, color = RichBlack, lineHeight = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom action buttons deck
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add to Cart with mini scale-up indicator
                Button(
                    onClick = {
                        viewModel.addToCart(product)
                        Toast.makeText(context, "Added ${product.name} to Cart!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = isInStock,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_to_cart_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightGreenSecondary,
                        disabledContainerColor = MutedText.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isInStock) "Add to Cart" else "Out of Stock",
                            color = DarkGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Buy Now Direct Purchase
                Button(
                    onClick = {
                        directOrderId = "ZVB-" + java.util.UUID.randomUUID().toString().uppercase().take(8)
                        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                        directDeliveryDate = formatter.format(System.currentTimeMillis() + 86400000) // tomorrow!
                        showCheckoutDialog = true
                        checkoutStep = 1
                    },
                    enabled = isInStock,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("buy_now_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGreenPrimary,
                        disabledContainerColor = MutedText.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OfflineBolt, contentDescription = "", tint = CustomWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isInStock) "Buy Now" else "Unavailable", color = CustomWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- Sub-Dialog Popup: Direct Purchase Checkout Flow ---
    if (showCheckoutDialog) {
        Dialog(onDismissRequest = { showCheckoutDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header Status Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Secure Checkout Direct Buy",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkGreenPrimary
                        )
                        IconButton(onClick = { showCheckoutDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = RichBlack)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step Wizard Tracker Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("1. Shipping", "2. Payment", "3. Complete").forEachIndexed { index, stepName ->
                            val active = index + 1 == checkoutStep
                            val finished = index + 1 < checkoutStep
                            val color = if (active || finished) DarkGreenPrimary else MutedText
                            Text(
                                text = stepName,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Black else FontWeight.Normal,
                                color = color
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = SoftGrey)
                    Spacer(modifier = Modifier.height(16.dp))

                    when (checkoutStep) {
                        1 -> {
                            // Shipping details block
                            Text("Step 1: Confirm Delivery Address", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RichBlack)
                            Spacer(modifier = Modifier.height(10.dp))
                            AddressSuggestionField(
                                value = tempAddress,
                                onValueChange = { tempAddress = it },
                                onAddressSelected = {
                                    tempAddress = it.address
                                    tempAddressLat = it.latitude
                                    tempAddressLng = it.longitude
                                },
                                label = "Delivery Address",
                                modifier = Modifier
                                    .fillMaxWidth(),
                                testTag = "checkout_address_input"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fine || coarse) {
                                        coroutineScope.launch {
                                            isFetchingDirectLocation = true
                                            val selection = getCurrentAddressSelection(context)
                                            tempAddress = selection.address
                                            tempAddressLat = selection.latitude
                                            tempAddressLng = selection.longitude
                                            isFetchingDirectLocation = false
                                        }
                                    } else {
                                        directLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, DarkGreenPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isFetchingDirectLocation) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DarkGreenPrimary)
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Current Location", modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use Current Location", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val directServiceText = when {
                                serviceAreas.isEmpty() -> "Delivery not available: admin has not added service pincodes."
                                directPinCode.isBlank() -> "Select an address with pincode to check delivery."
                                isDirectAddressDeliverable -> "Delivery available for pincode $directPinCode."
                                else -> "Delivery Not Available for pincode $directPinCode."
                            }
                            Text(
                                text = directServiceText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDirectAddressDeliverable) DarkGreenPrimary else AccentRed
                            )
                            if (serviceAreas.isNotEmpty()) {
                                Text(
                                    text = "Admin service pincodes: ${serviceAreas.joinToString { it.pinCode }}",
                                    fontSize = 10.sp,
                                    color = MutedText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    if (tempAddress.isBlank()) {
                                        Toast.makeText(context, "Address cannot be empty", Toast.LENGTH_SHORT).show()
                                    } else if (tempAddressLat == 0.0 && tempAddressLng == 0.0) {
                                        Toast.makeText(context, "Please select an address suggestion or use current location for lat/long.", Toast.LENGTH_SHORT).show()
                                    } else if (!isDirectAddressDeliverable) {
                                        Toast.makeText(context, "Delivery Not Available for this pincode.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateAddress(tempAddress, tempAddressLat, tempAddressLng)
                                        checkoutStep = 2
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Proceed to Payment & Coupon", color = CustomWhite)
                            }
                        }
                        2 -> {
                            // Payment confirmation block with COUPON input box
                            Text("Step 2: Enter Promo Coupon & Payment Info", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RichBlack)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Promo Code section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LightGreenSecondary.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, LightGreenSecondary)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    val firstCoupon = viewModel.coupons.value.firstOrNull()
                                    val hintText = if (firstCoupon != null) {
                                        "Apply Promo Coupon (Try: ${firstCoupon.code} for ${firstCoupon.discountPercent}% off!)"
                                    } else {
                                        "Apply Promo Coupon (Try: BAZAAR20 for 20% off!)"
                                    }
                                    Text(hintText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = couponText,
                                            onValueChange = { couponText = it },
                                            placeholder = { Text("Coupon Code") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DarkGreenPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                val coupon = viewModel.validateCoupon(couponText)
                                                if (coupon != null) {
                                                    isCouponApplied = true
                                                    Toast.makeText(context, "Coupon ${coupon.code} Applied successfully! ${coupon.discountPercent}% OFF!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    isCouponApplied = false
                                                    Toast.makeText(context, "Invalid Coupon Code", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(46.dp)
                                        ) {
                                            Text("Apply", fontSize = 11.sp, color = CustomWhite)
                                        }
                                    }

                                    if (isCouponApplied && appliedCoupon != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("✅ Promo Applied: ${appliedCoupon.discountPercent}% Discount Subtracted!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Order billing summary
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SoftGrey),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Item price:", fontSize = 11.sp, color = MutedText)
                                        Text("₹${product.price}", fontSize = 11.sp, color = RichBlack)
                                    }
                                    if (isCouponApplied && appliedCoupon != null) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${appliedCoupon.code} Discount (${appliedCoupon.discountPercent}%):", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                            Text("-₹${String.format("%.2f", discountAmount)}", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Delivery (${String.format("%.1f", deliveryDistanceKm)} km):", fontSize = 11.sp, color = MutedText)
                                        Text("+₹${String.format("%.2f", deliveryCharge.totalCharge)}", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "Fixed ₹${String.format("%.0f", deliveryCharge.fixedCharge)} + ₹${String.format("%.0f", deliveryCharge.perKmCharge)}/km",
                                        fontSize = 10.sp,
                                        color = MutedText
                                    )
                                    Divider(color = Color.LightGray, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total to Pay:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                        Text("₹${String.format("%.2f", finalTotalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = DarkGreenPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (!isInStock) {
                                        Toast.makeText(context, "${product.name} is out of stock.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!isDirectAddressDeliverable) {
                                        Toast.makeText(context, "Delivery Not Available for this pincode.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val act = activity ?: (context as? Activity)
                                    if (act != null) {
                                        val summary = "${product.name} x1"
                                        // Set pending checkout info so Razorpay callback can retrieve it on success
                                        viewModel.setPendingCheckout(
                                            com.example.viewmodel.PendingCheckout(
                                                totalAmount = finalTotalAmount,
                                                summary = summary,
                                                orderId = directOrderId,
                                                address = tempAddress,
                                                coupon = if (isCouponApplied) couponText else "",
                                                addressLat = tempAddressLat,
                                                addressLng = tempAddressLng,
                                                itemsAmount = payableBeforeDelivery,
                                                deliveryDistanceKm = deliveryDistanceKm,
                                                deliveryFixedCharge = deliveryCharge.fixedCharge,
                                                deliveryPerKmCharge = deliveryCharge.perKmCharge,
                                                deliveryCharge = deliveryCharge.totalCharge,
                                                clearCartAfterCheckout = false
                                            )
                                        )
                                        try {
                                            Checkout.preload(act.applicationContext)
                                            val checkout = Checkout()
                                            checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
                                            val options = JSONObject().apply {
                                                put("name", "ZYL VOR BAZAAR")
                                                put("description", summary.take(255))
                                                put("currency", "INR")
                                                put("amount", (finalTotalAmount * 100).toInt())
                                                put("prefill", JSONObject().apply {
                                                    put("email", user?.email ?: "")
                                                    put("contact", "")
                                                })
                                                put("theme", JSONObject().apply {
                                                    put("color", "#1B5E20")
                                                })
                                            }
                                            checkout.open(act, options)
                                        } catch (e: Exception) {
                                            viewModel.clearPendingCheckout()
                                            Toast.makeText(context, "Could not open payment: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Payment unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CustomWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pay Securely ₹${String.format("%.2f", finalTotalAmount)}", color = CustomWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                        3 -> {
                            // Success Info Screen with Scale Animations
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(Color(0xFFE8F5E9), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "",
                                        tint = DarkGreenPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Order Placed Successfully!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = DarkGreenPrimary
                                    )
                                )
                                Text(
                                    text = "Your order is registered on ZVB Safe Network.",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SoftGrey),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Unique Order ID:", fontSize = 11.sp, color = MutedText)
                                            Text(directOrderId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Item Purchased:", fontSize = 11.sp, color = MutedText)
                                            Text(product.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RichBlack, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Estimate Delivery:", fontSize = 11.sp, color = MutedText)
                                            Text(directDeliveryDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Address Info:", fontSize = 11.sp, color = MutedText)
                                            Text(tempAddress, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RichBlack, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        showCheckoutDialog = false
                                        checkoutStep = 1
                                        onBack() // Collapse details pane back
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Continue Shopping", color = CustomWhite)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(16.dp)
            .background(Color.LightGray)
    )
}

@Composable
fun DeviceRow(
    name: String,
    status: String,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "",
                    tint = if (canRemove) MutedText else DarkGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RichBlack
                    )
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        color = if (canRemove) MutedText else DarkGreenPrimary
                    )
                }
            }
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Device",
                        tint = AccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Text(
                    text = "Current",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreenPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

// ==========================================
// 5. SELLER PANEL SCREEN (Meesho Style)
// ==========================================
enum class SellerTab(val title: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Default.Dashboard),
    Products("My Inventory", Icons.Default.Inventory2),
    Profile("Shop Profile", Icons.Default.Storefront)
}

@Composable
fun SellerPanelScreen(
    viewModel: BazaarViewModel,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val context = LocalContext.current

    var activeTab by remember { mutableStateOf(SellerTab.Dashboard) }
    var selectedStatusFilter by remember { mutableStateOf("Pending") }
    var showAddProductDialog by remember { mutableStateOf(false) }

    // Form states for Add Product
    var newProdName by remember { mutableStateOf("") }
    var newProdPrice by remember { mutableStateOf("") }
    var newProdOrigPrice by remember { mutableStateOf("") }
    var newProdStock by remember { mutableStateOf("") }
    var newProdCat by remember { mutableStateOf("Electronics") }
    var newProdDesc by remember { mutableStateOf("") }
    var imageList by remember { mutableStateOf(listOf<String>()) }
    var showProductImageSelectorDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    val productPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val imageId = UUID.randomUUID().toString().take(6)
            viewModel.uploadProductImageToCloudinary(uri, currentUser?.email ?: "unknown", imageId) { url ->
                imageList = imageList + url
                showProductImageSelectorDialog = false
                Toast.makeText(context, "Product image uploaded successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Form states for Shop Edit Request
    var editName by remember { mutableStateOf(currentUser?.name ?: "") }
    var editShopName by remember { mutableStateOf(currentUser?.shopName ?: "") }
    var editShopAddress by remember { mutableStateOf(currentUser?.shopAddress ?: "") }
    var editShopAddressLat by remember { mutableStateOf(currentUser?.shopAddressLat ?: 0.0) }
    var editShopAddressLng by remember { mutableStateOf(currentUser?.shopAddressLng ?: 0.0) }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 4.dp,
                color = CustomWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentUser?.shopName ?: "ZYL VOR BAZAAR Seller Hub",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            color = DarkGreenPrimary
                        )
                        Text(
                            text = "Seller Hub • Secure Room Network",
                            fontSize = 11.sp,
                            color = MutedText,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .background(SoftGrey, shape = RoundedCornerShape(8.dp))
                            .size(40.dp)
                            .testTag("seller_logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = AccentRed)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = CustomWhite,
                tonalElevation = 8.dp
            ) {
                SellerTab.values().forEach { tab ->
                    val active = activeTab == tab
                    NavigationBarItem(
                        selected = active,
                        onClick = { activeTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkGreenPrimary,
                            selectedTextColor = DarkGreenPrimary,
                            indicatorColor = LightGreenSecondary.copy(alpha = 0.5f),
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        ),
                        modifier = Modifier.testTag("seller_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        containerColor = SoftGrey
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                SellerTab.Dashboard -> {
                    // Compute metrics
                    val isVerified = currentUser?.isSellerVerified == true
                    
                    val sellerEmail = currentUser?.email.orEmpty()
                    val sellerProductsForCurrentSeller = allProducts.filter {
                        sellerEmail.isNotBlank() && it.sellerEmail.equals(sellerEmail, ignoreCase = true)
                    }
                    val sellerProductNames = sellerProductsForCurrentSeller.map { it.name }
                    
                    val sellerOrders = if (isVerified) {
                        allOrders.mapNotNull { order ->
                            val itemsList = order.itemsSummary.split(", ")
                            val sellerItems = itemsList.filter { itemStr ->
                                val parts = itemStr.split(" x")
                                val productName = parts.getOrNull(0)?.trim() ?: ""
                                sellerProductNames.any { it.equals(productName, ignoreCase = true) }
                            }
                            if (sellerItems.isNotEmpty()) {
                                val sellerSummary = sellerItems.joinToString(", ")
                                val subtotal = sellerItems.sumOf { itemStr ->
                                    val parts = itemStr.split(" x")
                                    val productName = parts.getOrNull(0)?.trim() ?: ""
                                    val quantity = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
                                    val product = sellerProductsForCurrentSeller.find { it.name.equals(productName, ignoreCase = true) }
                                    (product?.price ?: 0.0) * quantity
                                }
                                order.copy(
                                    itemsSummary = sellerSummary,
                                    totalAmount = subtotal
                                )
                            } else {
                                null
                            }
                        }
                    } else {
                        emptyList()
                    }
                    val pendingCount = sellerOrders.count { 
                        it.status == "Processing" || it.status == "Pending" || it.status.isBlank() || it.status == "Seller Reject Requested"
                    }
                    val readyCount = sellerOrders.count { 
                        it.status == "Accepted" || it.status == "Shipped" || it.status == "Ready to Deliver" || it.status == "Shipping Ready" || it.status == "On the Way" || it.status == "Delivery Take More Time" || it.status == "Ready for Delivery" || it.status == "Delivery Accepted"
                    }
                    val successCount = sellerOrders.count { it.status == "Delivered" }
                    val cancelledCount = sellerOrders.count { it.status == "Cancelled" }
                    val filteredOrders = when (selectedStatusFilter) {
                        "Pending" -> sellerOrders.filter { 
                            it.status == "Processing" || it.status == "Pending" || it.status.isBlank() || it.status == "Seller Reject Requested"
                        }
                        "Ready to Deliver" -> sellerOrders.filter { 
                            it.status == "Accepted" || it.status == "Shipped" || it.status == "Ready to Deliver" || it.status == "Shipping Ready" || it.status == "On the Way" || it.status == "Delivery Take More Time" || it.status == "Ready for Delivery" || it.status == "Delivery Accepted"
                        }
                        "Delivered" -> sellerOrders.filter { it.status == "Delivered" }
                        "Cancelled" -> sellerOrders.filter { it.status == "Cancelled" }
                        else -> sellerOrders
                    }
 
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 0. Verification Status Notification Card
                        if (!isVerified) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                    border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.PendingActions,
                                                contentDescription = "Pending",
                                                tint = Color(0xFFE65100),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Profile Verification Pending",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFFE65100)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Your seller verification profile is under admin review. While your profile is pending, you can add products to your catalog, but you cannot receive any customer orders.",
                                            fontSize = 12.sp,
                                            color = RichBlack,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Metric Header Section
                        item {
                            Column {
                                Text(
                                    text = "Order Management Pipeline",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = RichBlack
                                )
                                Text(
                                    text = "Tap a card below to filter the customer orders list.",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                        }

                        // Grid-style metric cards (2x2 layout) with Selection feedback
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Pending Orders Card
                                    val isPendingSelected = selectedStatusFilter == "Pending"
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedStatusFilter = "Pending" },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isPendingSelected) Color(0xFFFFF3E0) else CustomWhite
                                        ),
                                        border = BorderStroke(
                                            width = if (isPendingSelected) 2.dp else 1.dp,
                                            color = if (isPendingSelected) Color(0xFFF57C00) else SoftGrey
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.PendingActions,
                                                    contentDescription = "Pending Orders",
                                                    tint = Color(0xFFF57C00),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Pending",
                                                    fontSize = 11.sp,
                                                    color = if (isPendingSelected) Color(0xFFE65100) else MutedText,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "$pendingCount",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isPendingSelected) Color(0xFFE65100) else RichBlack
                                            )
                                        }
                                    }

                                    // Ready To Deliver (Shipped) Orders Card
                                    val isReadySelected = selectedStatusFilter == "Ready to Deliver"
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedStatusFilter = "Ready to Deliver" },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isReadySelected) Color(0xFFE3F2FD) else CustomWhite
                                        ),
                                        border = BorderStroke(
                                            width = if (isReadySelected) 2.dp else 1.dp,
                                            color = if (isReadySelected) Color(0xFF1976D2) else SoftGrey
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalShipping,
                                                    contentDescription = "Ready to Deliver",
                                                    tint = Color(0xFF1976D2),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Ready to Deliver",
                                                    fontSize = 11.sp,
                                                    color = if (isReadySelected) Color(0xFF0D47A1) else MutedText,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "$readyCount",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isReadySelected) Color(0xFF0D47A1) else RichBlack
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Order Delivered Card
                                    val isDeliveredSelected = selectedStatusFilter == "Delivered"
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedStatusFilter = "Delivered" },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDeliveredSelected) Color(0xFFE8F5E9) else CustomWhite
                                        ),
                                        border = BorderStroke(
                                            width = if (isDeliveredSelected) 2.dp else 1.dp,
                                            color = if (isDeliveredSelected) DarkGreenPrimary else SoftGrey
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Delivered Items",
                                                    tint = DarkGreenPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Delivered",
                                                    fontSize = 11.sp,
                                                    color = if (isDeliveredSelected) DarkGreenPrimary else MutedText,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "$successCount",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isDeliveredSelected) DarkGreenPrimary else RichBlack
                                            )
                                        }
                                    }

                                    // Canceled Orders Card
                                    val isCancelledSelected = selectedStatusFilter == "Cancelled"
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedStatusFilter = "Cancelled" },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCancelledSelected) Color(0xFFFFEBEE) else CustomWhite
                                        ),
                                        border = BorderStroke(
                                            width = if (isCancelledSelected) 2.dp else 1.dp,
                                            color = if (isCancelledSelected) AccentRed else SoftGrey
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Cancel,
                                                    contentDescription = "Canceled Orders",
                                                    tint = AccentRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Cancelled",
                                                    fontSize = 11.sp,
                                                    color = if (isCancelledSelected) AccentRed else MutedText,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "$cancelledCount",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isCancelledSelected) AccentRed else RichBlack
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Orders manager title
                        item {
                            Text(
                                text = "Active Customer Orders - $selectedStatusFilter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = RichBlack,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (filteredOrders.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CustomWhite)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Inbox, contentDescription = "", tint = MutedText, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (!isVerified) "Verification pending. Orders will appear here once verified." else "No $selectedStatusFilter orders found in the system.",
                                            color = MutedText,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredOrders) { order ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("seller_order_item_${order.orderId}"),
                                    colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("ID: ${order.orderId}", fontWeight = FontWeight.Black, color = DarkGreenPrimary, fontSize = 13.sp)
                                            
                                            // Dynamic Status Chip
                                            val statusColor = when (order.status) {
                                                "Delivered" -> DarkGreenPrimary
                                                "Cancelled" -> AccentRed
                                                "Accepted", "Shipping Ready", "Ready for Delivery", "Delivery Accepted", "Shipped", "Ready to Deliver", "On the Way" -> Color(0xFF1976D2)
                                                else -> Color(0xFFF57C00)
                                            }
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = order.status.ifBlank { "PENDING" }.uppercase(),
                                                    color = statusColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        val customer = allUsers.find { it.email.equals(order.email, ignoreCase = true) }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Product: ${order.itemsSummary}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                        Text("Amount: ₹${order.totalAmount}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = DarkGreenPrimary)
                                        
                                        if (order.couponApplied.isNotBlank()) {
                                            Text("Coupon Applied: ${order.couponApplied}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGreenPrimary)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // Buyer Details Section
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "CUSTOMER & PAYMENT INFO",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkGreenPrimary,
                                                    letterSpacing = 1.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Person, contentDescription = "", tint = MutedText, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Name: ${customer?.name ?: "Valued Customer"} (${order.email})",
                                                        fontSize = 11.sp,
                                                        color = RichBlack,
                                                        fontWeight = FontWeight.Medium
                                                     )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Phone, contentDescription = "", tint = MutedText, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Phone: ${customer?.phone?.ifBlank { "Not Provided" } ?: "Not Provided"}",
                                                        fontSize = 11.sp,
                                                        color = RichBlack
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.Top) {
                                                    Icon(Icons.Default.Home, contentDescription = "", tint = MutedText, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                                     Spacer(modifier = Modifier.width(6.dp))
                                                     Text(
                                                         text = "Delivery Address: ${order.deliveryAddress.ifBlank { customer?.savedAddress?.ifBlank { "Default Registered Address" } ?: "Default Registered Address" }}",
                                                         fontSize = 11.sp,
                                                         color = RichBlack
                                                     )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CreditCard, contentDescription = "", tint = MutedText, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Payment Mode: " + if (customer != null && customer.savedCards.isNotBlank()) {
                                                            "Credit/Debit Card (ending in ${customer.savedCards.takeLast(4)})"
                                                        } else {
                                                            "Secure Digital Wallet Payment"
                                                        },
                                                        fontSize = 11.sp,
                                                        color = RichBlack
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider(color = SoftGrey)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Status controls
                                        Text("Action Control Status:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedText)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (order.status != "Delivered" && order.status != "Cancelled") {
                                                if (order.status == "Processing" || order.status == "Pending" || order.status.isBlank()) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.confirmOrderReady(order.orderId)
                                                            Toast.makeText(context, "Order ${order.orderId} accepted. Shipping ready and sent to delivery partners.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Text("Accept Order", fontSize = 10.sp, color = CustomWhite)
                                                    }
                                                }

                                                if (order.status == "Shipping Ready" && order.deliveryPartnerEmail.isBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFFFF3E0), shape = RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Waiting for Delivery Partner acceptance...",
                                                            color = Color(0xFFE65100),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                if (order.status == "Delivery Accepted" || order.deliveryPartnerEmail.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(LightGreenSecondary, shape = RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Delivery partner assigned. Customer sees Shipping Ready until pickup starts.",
                                                            color = DarkGreenPrimary,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.updateOrderStatus(order.orderId, "Cancelled")
                                                        Toast.makeText(context, "Order ${order.orderId} Cancelled", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp),
                                                    border = BorderStroke(1.dp, AccentRed)
                                                ) {
                                                    Text("Cancel", fontSize = 10.sp)
                                                }
                                            } else {
                                                Text(
                                                    text = "Order finalized. No modifications allowed.",
                                                    fontSize = 11.sp,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    color = MutedText
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SellerTab.Products -> {
                    // Filter products owned by this seller
                    val sellerEmail = currentUser?.email.orEmpty()
                    val sellerProducts = allProducts.filter {
                        sellerEmail.isNotBlank() && it.sellerEmail.equals(sellerEmail, ignoreCase = true)
                    }

                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { showAddProductDialog = true },
                                containerColor = DarkGreenPrimary,
                                contentColor = CustomWhite,
                                modifier = Modifier.testTag("add_product_fab")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Product")
                            }
                        },
                        containerColor = SoftGrey
                    ) { innerPadding ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Your Listed Products (${sellerProducts.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = RichBlack
                                    )
                                    Text(
                                        text = "ZYL VOR BAZAAR Fast Listing",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkGreenPrimary
                                    )
                                }
                            }

                            if (sellerProducts.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CustomWhite)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Default.Inventory, contentDescription = "", tint = MutedText, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("No products listed yet.", color = MutedText, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = { showAddProductDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary)
                                            ) {
                                                Text("List First Product", color = CustomWhite)
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(sellerProducts, key = { it.id }) { prod ->
                                    val sellerProductPhoto = prod.primaryPhotoUrl()
                                    val sellerProductPhotoModel = remember(sellerProductPhoto) {
                                        resolveProductImageModel(context, sellerProductPhoto)
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CustomWhite)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Image placeholder
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(LightGreenSecondary.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (sellerProductPhotoModel != null) {
                                                    AsyncImage(
                                                        model = sellerProductPhotoModel,
                                                        contentDescription = prod.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop,
                                                        placeholder = painterResource(id = R.drawable.img_hero_banner_1782139859933),
                                                        error = painterResource(id = R.drawable.img_hero_banner_1782139859933)
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Inventory2, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(28.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RichBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(prod.category, fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("₹${prod.price}", fontWeight = FontWeight.Black, color = DarkGreenPrimary, fontSize = 13.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("₹${prod.originalPrice}", textDecoration = TextDecoration.LineThrough, fontSize = 11.sp, color = MutedText)
                                                }
                                                Text(
                                                    text = if (prod.stock > 0) {
                                                        if (prod.stock >= 999) "Stock: Available" else "Stock: ${prod.stock}"
                                                    } else {
                                                        "Out of stock"
                                                    },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (prod.stock > 0) DarkGreenPrimary else AccentRed
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = { editingProduct = prod },
                                                        modifier = Modifier.height(34.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, DarkGreenPrimary),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.deleteProduct(prod)
                                                            Toast.makeText(context, "Product deleted.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.height(34.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, AccentRed),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SellerTab.Profile -> {
                    val isVerified = currentUser?.isSellerVerified == true
                    val sellerEmail = currentUser?.email.orEmpty()
                    val sellerProductsForCurrentSeller = allProducts.filter {
                        sellerEmail.isNotBlank() && it.sellerEmail.equals(sellerEmail, ignoreCase = true)
                    }
                    val sellerProductNames = sellerProductsForCurrentSeller.map { it.name }
                    val sellerOrders = if (isVerified) {
                        allOrders.mapNotNull { order ->
                            val itemsList = order.itemsSummary.split(", ")
                            val sellerItems = itemsList.filter { itemStr ->
                                val parts = itemStr.split(" x")
                                val productName = parts.getOrNull(0)?.trim() ?: ""
                                sellerProductNames.any { it.equals(productName, ignoreCase = true) }
                            }
                            if (sellerItems.isNotEmpty()) {
                                val sellerSummary = sellerItems.joinToString(", ")
                                val subtotal = sellerItems.sumOf { itemStr ->
                                    val parts = itemStr.split(" x")
                                    val productName = parts.getOrNull(0)?.trim() ?: ""
                                    val quantity = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
                                    val product = sellerProductsForCurrentSeller.find { it.name.equals(productName, ignoreCase = true) }
                                    (product?.price ?: 0.0) * quantity
                                }
                                order.copy(
                                    itemsSummary = sellerSummary,
                                    totalAmount = subtotal
                                )
                            } else {
                                null
                            }
                        }
                    } else {
                        emptyList()
                    }

                    val deliveredSellerOrders = sellerOrders.filter { it.status.equals("Delivered", ignoreCase = true) }
                    val walletBalance = deliveredSellerOrders.sumOf { it.totalAmount }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Earnings Wallet Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkGreenPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Seller Earnings Wallet (INR)", color = CustomWhite.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("₹${String.format("%.2f", walletBalance)}", color = CustomWhite, fontSize = 28.sp, fontWeight = FontWeight.Black)
                                Text("Total credited from delivered customer orders", color = CustomWhite.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                        }

                        // Current Profile Header
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CustomWhite)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(LightGreenSecondary, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (currentUser?.shopName ?: "M").take(1).uppercase(),
                                            fontWeight = FontWeight.Black,
                                            color = DarkGreenPrimary,
                                            fontSize = 20.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(currentUser?.shopName ?: "N/A", fontWeight = FontWeight.Black, fontSize = 16.sp, color = RichBlack)
                                        if (currentUser?.isSellerVerified == true) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Verified, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("ZYL VOR BAZAAR VERIFIED SELLER", fontSize = 11.sp, color = DarkGreenPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.PendingActions, contentDescription = "", tint = Color(0xFFF57C00), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Profile Verification Pending", fontSize = 11.sp, color = Color(0xFFF57C00), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = SoftGrey)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Seller Name:", fontSize = 12.sp, color = MutedText)
                                    Text(currentUser?.name ?: "N/A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Shop Address:", fontSize = 12.sp, color = MutedText)
                                    Text(currentUser?.shopAddress ?: "N/A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RichBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Email Address:", fontSize = 12.sp, color = MutedText)
                                    Text(currentUser?.email ?: "N/A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                }
                            }
                        }

                        // Edit Request Form Widget
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CustomWhite)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Apply for Profile Edit Request",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = RichBlack
                                )
                                Text(
                                    text = "Submit request to change Shop Name, Owner Name or Shop Address.",
                                    fontSize = 11.sp,
                                    color = MutedText,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = editShopName,
                                    onValueChange = { editShopName = it },
                                    label = { Text("New Shop Name") },
                                    modifier = Modifier.fillMaxWidth().testTag("profile_edit_shopname"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("New Owner Full Name") },
                                    modifier = Modifier.fillMaxWidth().testTag("profile_edit_ownername"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                AddressSuggestionField(
                                    value = editShopAddress,
                                    onValueChange = { editShopAddress = it },
                                    onAddressSelected = {
                                        editShopAddress = it.address
                                        editShopAddressLat = it.latitude
                                        editShopAddressLng = it.longitude
                                    },
                                    label = "New Shop Address",
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "profile_edit_shopaddress",
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (editShopName.isNotBlank() && editName.isNotBlank() && editShopAddress.isNotBlank()) {
                                            viewModel.applyShopEditRequest(
                                                editName,
                                                editShopName,
                                                editShopAddress,
                                                editShopAddressLat,
                                                editShopAddressLng
                                            )
                                            Toast.makeText(context, "Edit request submitted securely to Room database!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Please enter all fields.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Submit Edit Request", color = CustomWhite)
                                }

                                if (currentUser?.editRequestPending == true) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = LightGreenSecondary.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, LightGreenSecondary)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("⏳ Pending Verification Status", fontWeight = FontWeight.Bold, color = DarkGreenPrimary, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Owner: ${currentUser?.requestedName}", fontSize = 11.sp, color = RichBlack)
                                            Text("Shop: ${currentUser?.requestedShopName}", fontSize = 11.sp, color = RichBlack)
                                            Text("Address: ${currentUser?.requestedShopAddress}", fontSize = 11.sp, color = RichBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.approveProfileEditRequest(currentUser?.email ?: "")
                                                    Toast.makeText(context, "Admin Bypass: Changes approved instantly!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Approve Instantly (Simulated Admin)", fontSize = 11.sp, color = CustomWhite)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Product dialog form
    if (showAddProductDialog) {
        Dialog(onDismissRequest = { showAddProductDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "List New Product",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkGreenPrimary
                        )
                        IconButton(onClick = { showAddProductDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = RichBlack)
                        }
                    }

                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = { newProdName = it },
                        label = { Text("Product Title Name") },
                        modifier = Modifier.fillMaxWidth().testTag("add_product_title"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newProdPrice,
                            onValueChange = { newProdPrice = it },
                            label = { Text("Offer Price ($)") },
                            modifier = Modifier.weight(1f).testTag("add_product_price"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = newProdOrigPrice,
                            onValueChange = { newProdOrigPrice = it },
                            label = { Text("Original Price ($)") },
                            modifier = Modifier.weight(1f).testTag("add_product_orig_price"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    OutlinedTextField(
                        value = newProdStock,
                        onValueChange = { newProdStock = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Available Stock") },
                        modifier = Modifier.fillMaxWidth().testTag("add_product_stock"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Simple Category text input or dropdown
                    OutlinedTextField(
                        value = newProdCat,
                        onValueChange = { newProdCat = it },
                        label = { Text("Category (e.g. Electronics, Fashion)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_product_category"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newProdDesc,
                        onValueChange = { newProdDesc = it },
                        label = { Text("Product Description details") },
                        modifier = Modifier.fillMaxWidth().testTag("add_product_desc"),
                        maxLines = 4,
                        singleLine = false
                    )

                    // Multiple Images Adding Option (Direct Mock Upload)
                    Text(
                        text = "Product Gallery (Direct Mock Upload)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = RichBlack,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (imageList.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clickable { productPhotoLauncher.launch("image/*") },
                            colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, SoftGrey),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "", tint = MutedText, modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No Images Uploaded Yet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                Text("Tap to select actual photos from your gallery", fontSize = 10.sp, color = MutedText)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            imageList.forEachIndexed { index, imgUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = "Uploaded Product Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            val newList = imageList.toMutableList()
                                            newList.removeAt(index)
                                            imageList = newList
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(AccentRed.copy(alpha = 0.8f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = CustomWhite, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            productPhotoLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreenPrimary),
                        border = BorderStroke(1.dp, DarkGreenPrimary),
                        modifier = Modifier.align(Alignment.Start),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Photo", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val price = newProdPrice.toDoubleOrNull()
                            val origPrice = newProdOrigPrice.toDoubleOrNull()
                            val stock = newProdStock.toIntOrNull()
                            val extraImagesStr = imageList.joinToString(",")

                            if (newProdName.isNotBlank() && price != null && origPrice != null && stock != null && newProdCat.isNotBlank() && imageList.isNotEmpty()) {
                                viewModel.addProduct(
                                    newProdName,
                                    price,
                                    origPrice,
                                    newProdCat,
                                    newProdDesc,
                                    currentUser?.email ?: "",
                                    stock,
                                    extraImagesStr
                                )
                                showAddProductDialog = false
                                // Reset form
                                newProdName = ""
                                newProdPrice = ""
                                newProdOrigPrice = ""
                                newProdStock = ""
                                newProdDesc = ""
                                imageList = emptyList()
                                Toast.makeText(context, "Product listed successfully on the Bazaar!", Toast.LENGTH_SHORT).show()
                            } else if (imageList.isEmpty()) {
                                Toast.makeText(context, "Please upload at least one product photo.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter valid entries, prices and stock.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Publish Product", color = CustomWhite)
                    }
                }
            }
        }
    }

    editingProduct?.let { productToEdit ->
        var editProdName by remember(productToEdit.id) { mutableStateOf(productToEdit.name) }
        var editProdPrice by remember(productToEdit.id) { mutableStateOf(productToEdit.price.toString()) }
        var editProdOrigPrice by remember(productToEdit.id) { mutableStateOf(productToEdit.originalPrice.toString()) }
        var editProdStock by remember(productToEdit.id) { mutableStateOf(productToEdit.stock.toString()) }
        var editProdCat by remember(productToEdit.id) { mutableStateOf(productToEdit.category) }
        var editProdDesc by remember(productToEdit.id) { mutableStateOf(productToEdit.description) }

        Dialog(onDismissRequest = { editingProduct = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = CustomWhite,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit Product",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkGreenPrimary
                        )
                        IconButton(onClick = { editingProduct = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = RichBlack)
                        }
                    }

                    OutlinedTextField(
                        value = editProdName,
                        onValueChange = { editProdName = it },
                        label = { Text("Product Title Name") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_product_title"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editProdPrice,
                            onValueChange = { editProdPrice = it },
                            label = { Text("Offer Price ($)") },
                            modifier = Modifier.weight(1f).testTag("edit_product_price"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = editProdOrigPrice,
                            onValueChange = { editProdOrigPrice = it },
                            label = { Text("Original Price ($)") },
                            modifier = Modifier.weight(1f).testTag("edit_product_orig_price"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    OutlinedTextField(
                        value = editProdStock,
                        onValueChange = { editProdStock = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Available Stock") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_product_stock"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = editProdCat,
                        onValueChange = { editProdCat = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_product_category"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editProdDesc,
                        onValueChange = { editProdDesc = it },
                        label = { Text("Product Description details") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_product_desc"),
                        maxLines = 4,
                        singleLine = false
                    )

                    Button(
                        onClick = {
                            val price = editProdPrice.toDoubleOrNull()
                            val originalPrice = editProdOrigPrice.toDoubleOrNull()
                            val stock = editProdStock.toIntOrNull()

                            if (editProdName.isNotBlank() && price != null && originalPrice != null && stock != null && editProdCat.isNotBlank()) {
                                viewModel.updateProduct(
                                    productToEdit.copy(
                                        name = editProdName.trim(),
                                        price = price,
                                        originalPrice = originalPrice,
                                        category = editProdCat.trim(),
                                        description = editProdDesc.trim(),
                                        stock = stock
                                    )
                                )
                                editingProduct = null
                                Toast.makeText(context, "Product updated.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter valid product details.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = CustomWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Product", color = CustomWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun RegistrationWelcomeOverlay(
    name: String,
    role: String,
    onDismiss: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "Sparks")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val sparks = remember {
        List(70) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 320f + 80f
            val color = when (Random.nextInt(4)) {
                0 -> Color(0xFFFFD700) // Gold
                1 -> Color(0xFFFF6F00) // Deep Orange
                2 -> Color(0xFF4CAF50) // Emerald Green Accent
                else -> Color(0xFF00E5FF) // Cyan Sparkle
            }
            val size = Random.nextFloat() * 7f + 3f
            Triple(angle, speed, Pair(color, size))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size / 2f
            sparks.forEach { (angle, speed, style) ->
                val (color, baseSize) = style
                val radius = speed * progress
                val x = center.width + cos(angle) * radius
                val y = center.height + sin(angle) * radius
                val alpha = (1f - progress).coerceIn(0f, 1f)
                val currentSize = baseSize * (1f - progress * 0.4f)
                drawCircle(
                    color = color,
                    radius = currentSize,
                    center = androidx.compose.ui.geometry.Offset(x, y),
                    alpha = alpha
                )
            }
        }

        Card(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CustomWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(LightGreenSecondary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Sparks",
                        tint = DarkGreenPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Welcome to Bazaar!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = RichBlack,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Hi $name, your new $role profile has been created! Explore the sustainable, eco-friendly green network platform built for premium buyers and certified local merchants.",
                    fontSize = 13.sp,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Get Started", color = CustomWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
