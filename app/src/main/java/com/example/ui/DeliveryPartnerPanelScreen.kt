package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Order
import com.example.data.User
import com.example.data.calculateDeliveryCharge
import com.example.data.estimateDeliveryDistanceKm
import com.example.data.estimateItemAmountFromOrderTotal
import com.example.ui.theme.*
import com.example.viewmodel.BazaarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnerPanelScreen(viewModel: BazaarViewModel, onLogout: () -> Unit) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var activeTab by remember { mutableStateOf("Dashboard") } // "Dashboard", "Order Requests", "Active Orders"

    // Filter orders specifically for this delivery boy
    val myOrders = allOrders.filter { it.deliveryPartnerEmail == currentUser?.email }
    
    val todayDelivered = myOrders.count { it.deliveryStatus == "Delivered" || it.status == "Delivered" }
    val todayPending = myOrders.count {
        (it.deliveryStatus == "Shipping Ready" || it.deliveryStatus == "On the Way" || it.deliveryStatus == "Delivery Take More Time") &&
            it.status != "Delivered" &&
            it.status != "Cancelled"
    }
    val todayCanceled = myOrders.count { it.status == "Cancelled" }

    // Order Requests: seller accepted orders remain open to all delivery partners for 24 hours.
    val availableRequests = allOrders.filter { 
        it.deliveryPartnerEmail.isBlank() && 
        it.status != "Cancelled" && 
        it.status != "Delivered" &&
        (it.status == "Shipping Ready" || it.status == "Ready for Delivery" || it.status == "Accepted" || it.sellerConfirmed) &&
        (System.currentTimeMillis() - it.orderDate) <= 24 * 60 * 60 * 1000
    }

    // Active accepted orders
    val activeAcceptedOrders = myOrders.filter { 
        it.status != "Delivered" && 
        it.status != "Cancelled" && 
        it.deliveryStatus != "Delivered" 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SoftGrey)
                        ) {
                            if (!currentUser?.deliveryPhoto.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentUser?.deliveryPhoto,
                                    contentDescription = "Partner Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Avatar Placeholder",
                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                    tint = MutedText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ZYL VOR DELIVERY",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = RichBlack
                            )
                            Text(
                                text = currentUser?.name ?: "Delivery Partner",
                                fontSize = 11.sp,
                                color = MutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onLogout()
                            Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("delivery_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = AccentRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CustomWhite)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SoftGrey.copy(alpha = 0.4f))
        ) {
            // Profile & Vehicle summary strip
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (currentUser?.deliveryVehicleType) {
                                "Bike" -> Icons.Default.TwoWheeler
                                "Scooter" -> Icons.Default.Moped
                                "Cycle" -> Icons.Default.PedalBike
                                else -> Icons.Default.DirectionsCar
                            },
                            contentDescription = "Vehicle",
                            tint = DarkGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Vehicle: ${currentUser?.deliveryVehicleType ?: "Bike"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RichBlack
                            )
                            if (currentUser?.deliveryVehicleType != "Cycle" && !currentUser?.deliveryVehicleNumber.isNullOrBlank()) {
                                Text(
                                    text = "Plate: ${currentUser?.deliveryVehicleNumber}",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (currentUser?.isDeliveryPartnerVerified == true) LightGreenSecondary.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (currentUser?.isDeliveryPartnerVerified == true) "Verified Partner ✓" else "Pending Approval ⏳",
                            color = if (currentUser?.isDeliveryPartnerVerified == true) DarkGreenPrimary else AccentRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Tab Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Dashboard", "Order Requests", "Active Orders", "Wallet", "Profile").forEach { tab ->
                    val selected = activeTab == tab
                    val countText = when (tab) {
                        "Order Requests" -> " (${availableRequests.size})"
                        "Active Orders" -> " (${activeAcceptedOrders.size})"
                        else -> ""
                    }
                    Button(
                        onClick = { activeTab = tab },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) DarkGreenPrimary else CustomWhite,
                            contentColor = if (selected) CustomWhite else RichBlack
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .padding(horizontal = 4.dp)
                            .testTag("tab_${tab.lowercase().replace(" ", "_")}"),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$tab$countText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Content
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { targetTab ->
                when (targetTab) {
                    "Dashboard" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Statistics Cards Grid
                            Text(
                                text = "Today's Deliveries Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RichBlack
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Delivered Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp),
                                    colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Delivered", fontSize = 11.sp, color = MutedText)
                                        Text("$todayDelivered", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                    }
                                }

                                // Pending Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp),
                                    colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Pending, contentDescription = "", tint = Color(0xFFFBC02D), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Pending", fontSize = 11.sp, color = MutedText)
                                        Text("$todayPending", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                    }
                                }

                                // Cancelled Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp),
                                    colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = "", tint = AccentRed, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Canceled", fontSize = 11.sp, color = MutedText)
                                        Text("$todayCanceled", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RichBlack)
                                    }
                                }
                            }

                            // Emergency contact and guidelines
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Delivery Partner Guidelines", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RichBlack)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("• Always verify items with the merchant before scanning and starting the journey.\n• Call customers politely if you face issues locating the residential address.\n• Update delivery status instantly to keep buyers and sellers in sync.", fontSize = 11.sp, color = MutedText, lineHeight = 16.sp)
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = SoftGrey)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Your Emergency Contact", fontSize = 11.sp, color = MutedText)
                                            Text(currentUser?.deliveryEmergencyContact ?: "N/A", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RichBlack)
                                        }
                                        Icon(Icons.Default.Phone, contentDescription = "Emergency Phone", tint = AccentRed, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    "Order Requests" -> {
                        if (availableRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Inbox, contentDescription = "", tint = MutedText, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No New Order Requests", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RichBlack)
                                    Text("Orders listed by sellers ready for dispatch will appear here.", fontSize = 11.sp, color = MutedText, textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(availableRequests) { order ->
                                    val customer = allUsers.find { it.email == order.email }
                                    val seller = allUsers.find { it.role == "Seller" } // Fallback or first seller in DB

                                    OrderRequestCard(
                                        order = order,
                                        customer = customer,
                                        seller = seller,
                                        onAccept = {
                                            viewModel.acceptOrder(order.orderId, currentUser?.email ?: "")
                                            Toast.makeText(context, "Order accepted successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        onReject = {
                                            viewModel.rejectOrderRequestByDeliveryPartner(order.orderId)
                                            Toast.makeText(context, "Order request declined.", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "Active Orders" -> {
                        if (activeAcceptedOrders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = "", tint = MutedText, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Active Shipments", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RichBlack)
                                    Text("Go to 'Order Requests' to accept and process new shipments.", fontSize = 11.sp, color = MutedText, textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(activeAcceptedOrders) { order ->
                                    val customer = allUsers.find { it.email == order.email }
                                    val seller = allUsers.find { it.role == "Seller" }

                                    ActiveOrderControlCard(
                                        order = order,
                                        customer = customer,
                                        seller = seller,
                                        onUpdateStatus = { newStatus ->
                                            viewModel.updateDeliveryStatus(order.orderId, newStatus)
                                            Toast.makeText(context, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "Wallet" -> {
                        val withdrawRequests by viewModel.withdrawRequests.collectAsState()
                        val deliveredOrders = myOrders.filter { it.deliveryStatus == "Delivered" || it.status == "Delivered" }
                        
                        fun calculateOrderEarning(order: com.example.data.Order): Double {
                            val distance = estimateDeliveryDistanceKm(order.orderId)
                            val itemAmount = order.itemsAmount.takeIf { it > 0.0 }
                                ?: estimateItemAmountFromOrderTotal(order.totalAmount, distance)
                            return order.deliveryCharge.takeIf { it > 0.0 }
                                ?: calculateDeliveryCharge(itemAmount, distance).totalCharge
                        }
                        
                        val totalEarnings = deliveredOrders.sumOf { calculateOrderEarning(it) }
                        
                        val approvedWithdrawals = withdrawRequests.filter { it.deliveryPartnerEmail == currentUser?.email && it.status == "Approved" }.sumOf { it.amount }
                        val pendingWithdrawals = withdrawRequests.filter { it.deliveryPartnerEmail == currentUser?.email && it.status == "Pending" }.sumOf { it.amount }
                        val totalWithdrawn = approvedWithdrawals + pendingWithdrawals
                        
                        val availableBalance = (totalEarnings - totalWithdrawn).coerceAtLeast(0.0)

                        var withdrawAmount by remember { mutableStateOf("") }
                        var bankDetails by remember { mutableStateOf("") }
                        var amountError by remember { mutableStateOf<String?>(null) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkGreenPrimary),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Zyl Vor Pay Wallet (INR)", color = CustomWhite.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("₹${String.format("%.2f", availableBalance)}", color = CustomWhite, fontSize = 32.sp, fontWeight = FontWeight.Black)
                                    Text("Available Balance to Withdraw", color = CustomWhite.copy(alpha = 0.8f), fontSize = 11.sp)
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider(color = CustomWhite.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total Lifetime Earnings", color = CustomWhite.copy(alpha = 0.7f), fontSize = 10.sp)
                                            Text("₹${String.format("%.2f", totalEarnings)}", color = CustomWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Pending / Withdrawn", color = CustomWhite.copy(alpha = 0.7f), fontSize = 10.sp)
                                            Text("₹${String.format("%.2f", totalWithdrawn)}", color = CustomWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SoftGrey)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Apply for Withdraw Request",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = RichBlack
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    OutlinedTextField(
                                        value = withdrawAmount,
                                        onValueChange = {
                                            withdrawAmount = it
                                            amountError = null
                                        },
                                        label = { Text("Amount (₹)") },
                                        placeholder = { Text("e.g. 500.00") },
                                        isError = amountError != null,
                                        modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                    if (amountError != null) {
                                        Text(amountError!!, color = AccentRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    OutlinedTextField(
                                        value = bankDetails,
                                        onValueChange = { bankDetails = it },
                                        label = { Text("Bank Transfer Details") },
                                        placeholder = { Text("Account Name, No, IFSC, Bank...") },
                                        modifier = Modifier.fillMaxWidth().testTag("withdraw_bank_input"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.height(14.dp))
                                    
                                    Button(
                                        onClick = {
                                            val amt = withdrawAmount.toDoubleOrNull()
                                            if (amt == null || amt <= 0) {
                                                amountError = "Please enter a valid positive amount"
                                            } else if (amt > availableBalance) {
                                                amountError = "Withdrawal exceeds available balance (₹${String.format("%.2f", availableBalance)})"
                                            } else if (bankDetails.isBlank()) {
                                                Toast.makeText(context, "Please enter your bank transfer details", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.applyWithdrawal(currentUser?.email ?: "", amt, bankDetails)
                                                Toast.makeText(context, "Withdraw request submitted successfully!", Toast.LENGTH_SHORT).show()
                                                withdrawAmount = ""
                                                bankDetails = ""
                                                amountError = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("submit_withdraw_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Submit", tint = CustomWhite, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Submit Withdraw Request", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CustomWhite)
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LightGreenSecondary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DarkGreenPrimary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Earning Model Info", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkGreenPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "• Below ₹500: ₹80 fixed + ₹15 per KM\n• ₹500 and above: ₹150 fixed + ₹20 per KM",
                                        fontSize = 11.sp,
                                        color = RichBlack
                                    )
                                }
                            }

                            Text(
                                text = "Earnings History by Product",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RichBlack
                            )
                            
                            if (deliveredOrders.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CustomWhite)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = "", tint = MutedText, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("No completed earnings yet", fontSize = 11.sp, color = MutedText)
                                    }
                                }
                            } else {
                                deliveredOrders.forEach { order ->
                                    val earn = calculateOrderEarning(order)
                                    val dist = order.deliveryDistanceKm.takeIf { it > 0.0 } ?: estimateDeliveryDistanceKm(order.orderId)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Order #${order.orderId.takeLast(7)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = RichBlack
                                                )
                                                Text(
                                                    text = order.itemsSummary,
                                                    fontSize = 11.sp,
                                                    color = MutedText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val dateString = android.text.format.DateFormat.format("MMM dd, yyyy", order.orderDate).toString()
                                                Text(
                                                    text = "Delivered: $dateString  •  Distance: ${String.format("%.1f", dist)} KM",
                                                    fontSize = 10.sp,
                                                    color = DarkGreenPrimary
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "+₹${String.format("%.2f", earn)}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkGreenPrimary,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Earnings Received",
                                                    fontSize = 9.sp,
                                                    color = MutedText
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val myWithdrawals = withdrawRequests.filter { it.deliveryPartnerEmail == currentUser?.email }
                            if (myWithdrawals.isNotEmpty()) {
                                Text(
                                    text = "Your Withdrawal Requests",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = RichBlack
                                )
                                myWithdrawals.forEach { req ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, SoftGrey)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("ID: ${req.id}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RichBlack)
                                                    Text(
                                                        text = "Requested: " + android.text.format.DateFormat.format("MMM dd, yyyy HH:mm", req.requestDate).toString(),
                                                        fontSize = 10.sp,
                                                        color = MutedText
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (req.status == "Approved") LightGreenSecondary else Color(0xFFFFF3E0),
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = req.status,
                                                        color = if (req.status == "Approved") DarkGreenPrimary else Color(0xFFE65100),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Amount: ₹${String.format("%.2f", req.amount)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkGreenPrimary)
                                            Text("Bank Info: ${req.bankDetails}", fontSize = 10.sp, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                            if (req.status == "Pending") {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.approveWithdrawal(req.id)
                                                        Toast.makeText(context, "Admin Simulation: Withdrawal request approved!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary.copy(alpha = 0.9f)),
                                                    modifier = Modifier.fillMaxWidth().height(32.dp).testTag("approve_withdraw_${req.id}"),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Approve Withdrawal Instantly (Simulated Admin)", fontSize = 11.sp, color = CustomWhite)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Profile" -> {
                        var editName by remember { mutableStateOf(currentUser?.name ?: "") }
                        var editPhone by remember { mutableStateOf(currentUser?.deliveryMobile ?: "") }
                        var editVehicleType by remember { mutableStateOf(currentUser?.deliveryVehicleType ?: "Bike") }
                        var editVehicleNumber by remember { mutableStateOf(currentUser?.deliveryVehicleNumber ?: "") }
                        var editEmergencyContact by remember { mutableStateOf(currentUser?.deliveryEmergencyContact ?: "") }
                        var editAddress by remember { mutableStateOf(currentUser?.deliveryAddress ?: "") }
                        var editAddressLat by remember { mutableStateOf(currentUser?.deliveryAddressLat ?: 0.0) }
                        var editAddressLng by remember { mutableStateOf(currentUser?.deliveryAddressLng ?: 0.0) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isVerified = currentUser?.isDeliveryPartnerVerified == true
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isVerified) LightGreenSecondary.copy(alpha = 0.3f) else Color(0xFFFFF3E0)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isVerified) DarkGreenPrimary else Color(0xFFFFB74D))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isVerified) Icons.Default.Verified else Icons.Default.Pending,
                                            contentDescription = "",
                                            tint = if (isVerified) DarkGreenPrimary else Color(0xFFE65100),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (isVerified) "VERIFIED PARTNER" else "PROFILE PENDING APPROVAL",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    color = if (isVerified) DarkGreenPrimary else Color(0xFFE65100)
                                                )
                                                if (isVerified) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(DarkGreenPrimary, shape = CircleShape)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("PRO", color = CustomWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (isVerified) "Your background check is complete. Happy delivering!" else "Your registration details are under review. Our team will verify shortly.",
                                                fontSize = 11.sp,
                                                color = RichBlack.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    if (!isVerified) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                viewModel.verifyDeliveryPartner(currentUser?.email ?: "", true)
                                                Toast.makeText(context, "Admin Simulation: Profile verified successfully!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("verify_profile_bypass_button"),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Verify My Profile Instantly (Simulated Admin)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CustomWhite)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                viewModel.verifyDeliveryPartner(currentUser?.email ?: "", false)
                                                Toast.makeText(context, "Admin Simulation: Profile reset to Pending!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("reset_profile_bypass_button"),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Reset to Pending (Simulated Admin)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CustomWhite)
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SoftGrey)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Current Registered Details", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RichBlack)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    val details = listOf(
                                        Icons.Default.Person to ("Name" to (currentUser?.name ?: "N/A")),
                                        Icons.Default.Email to ("Email" to (currentUser?.email ?: "N/A")),
                                        Icons.Default.Phone to ("Delivery Mobile" to (currentUser?.deliveryMobile ?: "N/A")),
                                        Icons.Default.TwoWheeler to ("Vehicle Type" to (currentUser?.deliveryVehicleType ?: "N/A")),
                                        Icons.Default.Badge to ("Plate Number" to (currentUser?.deliveryVehicleNumber ?: "N/A")),
                                        Icons.Default.ContactPhone to ("Emergency Contact" to (currentUser?.deliveryEmergencyContact ?: "N/A")),
                                        Icons.Default.Home to ("Home Address" to (currentUser?.deliveryAddress ?: "N/A"))
                                    )

                                    details.forEach { (icon, pair) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(icon, contentDescription = "", tint = DarkGreenPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(pair.first, fontSize = 10.sp, color = MutedText)
                                                Text(pair.second, fontSize = 12.sp, color = RichBlack, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CustomWhite),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SoftGrey)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Apply For Profile Change Request", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RichBlack)
                                    Text("Update any of your details and submit a new verification request.", fontSize = 11.sp, color = MutedText)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Full Name") },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name"),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = editPhone,
                                        onValueChange = { editPhone = it },
                                        label = { Text("Mobile Contact") },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_phone"),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    var expandedVehicleDropdown by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = editVehicleType,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Vehicle Type") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            trailingIcon = {
                                                IconButton(onClick = { expandedVehicleDropdown = !expandedVehicleDropdown }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "")
                                                }
                                            }
                                        )
                                        DropdownMenu(
                                            expanded = expandedVehicleDropdown,
                                            onDismissRequest = { expandedVehicleDropdown = false },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            listOf("Bike", "Scooter", "Cycle", "Car").forEach { vehicle ->
                                                DropdownMenuItem(
                                                    text = { Text(vehicle) },
                                                    onClick = {
                                                        editVehicleType = vehicle
                                                        expandedVehicleDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = editVehicleNumber,
                                        onValueChange = { editVehicleNumber = it },
                                        label = { Text("Vehicle Plate Number") },
                                        placeholder = { Text("e.g. DL-4S-BC-1234") },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_vehicle_num"),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = editEmergencyContact,
                                        onValueChange = { editEmergencyContact = it },
                                        label = { Text("Emergency Contact Number") },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_emergency"),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    AddressSuggestionField(
                                        value = editAddress,
                                        onValueChange = { editAddress = it },
                                        onAddressSelected = {
                                            editAddress = it.address
                                            editAddressLat = it.latitude
                                            editAddressLng = it.longitude
                                        },
                                        label = "Residential Address",
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "edit_profile_address"
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (editName.isBlank() || editPhone.isBlank()) {
                                                Toast.makeText(context, "Name and Phone cannot be blank!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.updateDeliveryPartnerProfile(
                                                    name = editName,
                                                    phone = editPhone,
                                                    vehicleType = editVehicleType,
                                                    vehicleNumber = editVehicleNumber,
                                                    emergencyContact = editEmergencyContact,
                                                    address = editAddress,
                                                    addressLat = editAddressLat,
                                                    addressLng = editAddressLng
                                                )
                                                Toast.makeText(context, "Profile change request applied successfully!", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("submit_profile_changes")
                                    ) {
                                        Text("Submit Profile Change Request", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CustomWhite)
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

@Composable
fun OrderRequestCard(
    order: Order,
    customer: User?,
    seller: User?,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val fallbackDistanceKm = estimateDeliveryDistanceKm(order.orderId)
    val deliveryDistanceKm = order.deliveryDistanceKm.takeIf { it > 0.0 } ?: fallbackDistanceKm
    val itemAmount = order.itemsAmount.takeIf { it > 0.0 }
        ?: estimateItemAmountFromOrderTotal(order.totalAmount, fallbackDistanceKm)
    val deliveryCharge = if (order.deliveryCharge > 0.0) {
        com.example.data.DeliveryChargeBreakdown(
            distanceKm = deliveryDistanceKm,
            fixedCharge = order.deliveryFixedCharge,
            perKmCharge = order.deliveryPerKmCharge,
            totalCharge = order.deliveryCharge
        )
    } else {
        calculateDeliveryCharge(itemAmount, deliveryDistanceKm)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CustomWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SoftGrey)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 24 Hour Countdown Timer
            val timeLeftMs = (order.orderDate + 24 * 60 * 60 * 1000) - System.currentTimeMillis()
            val hoursLeft = (timeLeftMs / (1000 * 60 * 60)).coerceAtLeast(0)
            val minutesLeft = ((timeLeftMs % (1000 * 60 * 60)) / (1000 * 60)).coerceAtLeast(0)
            val timeLeftText = if (timeLeftMs > 0) "${hoursLeft}h ${minutesLeft}m remaining" else "Expired"

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = "", tint = AccentRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Must accept within 24 hours",
                        fontSize = 11.sp,
                        color = AccentRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = timeLeftText,
                    fontSize = 11.sp,
                    color = if (timeLeftMs < 2 * 60 * 60 * 1000) AccentRed else DarkGreenPrimary,
                    fontWeight = FontWeight.Black
                )
            }
            Divider(color = SoftGrey)
            Spacer(modifier = Modifier.height(8.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: #${order.orderId.takeLast(7)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = RichBlack
                )
                Box(
                    modifier = Modifier
                        .background(LightGreenSecondary, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Earn ₹${String.format("%.2f", deliveryCharge.totalCharge)}",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreenPrimary,
                        fontSize = 11.sp
                    )
                }
            }
            Text(
                text = "Delivery ${String.format("%.1f", deliveryDistanceKm)} km: fixed ₹${String.format("%.0f", deliveryCharge.fixedCharge)} + ₹${String.format("%.0f", deliveryCharge.perKmCharge)}/km",
                fontSize = 10.sp,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = SoftGrey)
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Customer Section
            Text("CUSTOMER DETAILS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGreenPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = customer?.name ?: "Customer Name (Mandatory)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RichBlack
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (!customer?.phone.isNullOrBlank()) customer.phone else customer?.sellerMobile?.ifBlank { "9876543210" } ?: "9876543210",
                    fontSize = 11.sp,
                    color = RichBlack
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = order.deliveryAddress.ifBlank { customer?.savedAddress?.ifBlank { "Green Valley Residential, Delhi" } ?: "Green Valley Residential, Delhi" },
                    fontSize = 11.sp,
                    color = MutedText,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Shop Details Section
            Text("MERCHANT / SHOP DETAILS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFF57C00))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = seller?.shopName?.ifBlank { "Green Bazaar Organic Mart" } ?: "Green Bazaar Organic Mart",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RichBlack
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Owner: ${seller?.name?.ifBlank { "Vijay Kumar" } ?: "Vijay Kumar"}",
                    fontSize = 11.sp,
                    color = RichBlack
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = seller?.sellerMobile?.ifBlank { "9812345678" } ?: "9812345678",
                    fontSize = 11.sp,
                    color = RichBlack
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = seller?.shopAddress?.ifBlank { "Main Bazaar Sector-4, New Delhi" } ?: "Main Bazaar Sector-4, New Delhi",
                    fontSize = 11.sp,
                    color = MutedText,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Payment Mode: ${order.paymentMode}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = RichBlack
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                    border = BorderStroke(1.dp, AccentRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("reject_request_${order.orderId}")
                ) {
                    Text("Decline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp)
                        .testTag("accept_request_${order.orderId}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "", tint = CustomWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept Order", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CustomWhite)
                }
            }
        }
    }
}

@Composable
fun ActiveOrderControlCard(
    order: Order,
    customer: User?,
    seller: User?,
    onUpdateStatus: (String) -> Unit
) {
    val fallbackDistanceKm = estimateDeliveryDistanceKm(order.orderId)
    val deliveryDistanceKm = order.deliveryDistanceKm.takeIf { it > 0.0 } ?: fallbackDistanceKm
    val itemAmount = order.itemsAmount.takeIf { it > 0.0 }
        ?: estimateItemAmountFromOrderTotal(order.totalAmount, fallbackDistanceKm)
    val deliveryCharge = if (order.deliveryCharge > 0.0) {
        com.example.data.DeliveryChargeBreakdown(
            distanceKm = deliveryDistanceKm,
            fixedCharge = order.deliveryFixedCharge,
            perKmCharge = order.deliveryPerKmCharge,
            totalCharge = order.deliveryCharge
        )
    } else {
        calculateDeliveryCharge(itemAmount, deliveryDistanceKm)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CustomWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkGreenPrimary)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTIVE ORDER #${order.orderId.takeLast(7)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = RichBlack
                    )
                    Text(
                        text = "Current Status: ${order.deliveryStatus.ifBlank { "Shipping Ready" }}",
                        color = DarkGreenPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(LightGreenSecondary.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Earn ₹${String.format("%.2f", deliveryCharge.totalCharge)}",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreenPrimary,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = "Delivery ${String.format("%.1f", deliveryDistanceKm)} km: fixed ₹${String.format("%.0f", deliveryCharge.fixedCharge)} + ₹${String.format("%.0f", deliveryCharge.perKmCharge)}/km",
                color = MutedText,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = SoftGrey)
            Spacer(modifier = Modifier.height(10.dp))

            // Customer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Customer: ${customer?.name ?: "Customer Name"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RichBlack
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Phone: ${if (!customer?.phone.isNullOrBlank()) customer.phone else customer?.sellerMobile?.ifBlank { "9876543210" } ?: "9876543210"}",
                    fontSize = 11.sp,
                    color = RichBlack
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delivery Address: ${order.deliveryAddress.ifBlank { customer?.savedAddress?.ifBlank { "Green Valley Residential, Delhi" } ?: "Green Valley Residential, Delhi" }}",
                    fontSize = 11.sp,
                    color = MutedText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shop
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Merchant: ${seller?.shopName?.ifBlank { "Green Bazaar Mart" } ?: "Green Bazaar Mart"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RichBlack
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = "", tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Merchant Phone: ${seller?.sellerMobile?.ifBlank { "9812345678" } ?: "9812345678"}",
                    fontSize = 11.sp,
                    color = RichBlack
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = SoftGrey)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "UPDATE LIVE DELIVERY STATUS",
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                color = RichBlack,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Live status updater row of buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // On the Way
                    Button(
                        onClick = { onUpdateStatus("On the Way") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (order.deliveryStatus == "On the Way") DarkGreenPrimary else SoftGrey
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("status_on_the_way_${order.orderId}"),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "On the Way", 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            color = if (order.deliveryStatus == "On the Way") CustomWhite else RichBlack
                        )
                    }

                    // Delivery Take More Time
                    Button(
                        onClick = { onUpdateStatus("Delivery Take More Time") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (order.deliveryStatus == "Delivery Take More Time") Color(0xFFF57C00) else SoftGrey
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(36.dp)
                            .testTag("status_delay_${order.orderId}"),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Take More Time", 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            color = if (order.deliveryStatus == "Delivery Take More Time") CustomWhite else RichBlack
                        )
                    }
                }

                // Delivered Button (Delivering final status)
                Button(
                    onClick = { onUpdateStatus("Delivered") },
                    enabled = order.deliveryStatus == "On the Way" || order.deliveryStatus == "Delivery Take More Time",
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("status_delivered_${order.orderId}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "", tint = CustomWhite, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark Order as Delivered", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CustomWhite)
                }
            }
        }
    }
}
