package com.example.viewmodel

import android.app.Application
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// --- Coroutine Task Extension ---
// Uses suspendCancellableCoroutine so coroutine timeouts/cancellations work properly.
// Without this, Firebase callbacks could hang forever if the network drops mid-call.
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (!cont.isActive) return@addOnCompleteListener
        if (task.isSuccessful) {
            cont.resume(task.result)
        } else {
            cont.resumeWithException(task.exception ?: Exception("Firebase Task failed"))
        }
    }
}

// --- Mapping Extensions for Firestore ---
fun User.toMap(): Map<String, Any> {
    return mapOf(
        "email" to email,
        "name" to name,
        "password" to password,
        "isPlusMember" to isPlusMember,
        "savedAddress" to savedAddress,
        "savedAddressLat" to savedAddressLat,
        "savedAddressLng" to savedAddressLng,
        "savedCards" to savedCards,
        "deviceCount" to deviceCount,
        "notificationsEnabled" to notificationsEnabled,
        "selectedLanguage" to selectedLanguage,
        "privacyAccepted" to privacyAccepted,
        "phone" to phone,
        "gender" to gender,
        "role" to role,
        "shopName" to shopName,
        "shopAddress" to shopAddress,
        "shopAddressLat" to shopAddressLat,
        "shopAddressLng" to shopAddressLng,
        "editRequestPending" to editRequestPending,
        "requestedName" to requestedName,
        "requestedShopName" to requestedShopName,
        "requestedShopAddress" to requestedShopAddress,
        "requestedShopAddressLat" to requestedShopAddressLat,
        "requestedShopAddressLng" to requestedShopAddressLng,
        "isSellerVerified" to isSellerVerified,
        "isSellerVerificationPending" to isSellerVerificationPending,
        "sellerMobile" to sellerMobile,
        "sellerAadhaar" to sellerAadhaar,
        "sellerShopPhoto" to sellerShopPhoto,
        "sellerOwnerPhoto" to sellerOwnerPhoto,
        "sellerBankAccount" to sellerBankAccount,
        "sellerPanCard" to sellerPanCard,
        "sellerGstNumber" to sellerGstNumber,
        
        // --- Delivery Partner Mapping ---
        "deliveryMobile" to deliveryMobile,
        "deliveryAadhaar" to deliveryAadhaar,
        "deliveryPhoto" to deliveryPhoto,
        "deliveryAddress" to deliveryAddress,
        "deliveryAddressLat" to deliveryAddressLat,
        "deliveryAddressLng" to deliveryAddressLng,
        "deliveryBankAccount" to deliveryBankAccount,
        "deliveryEmergencyContact" to deliveryEmergencyContact,
        "deliveryVehicleType" to deliveryVehicleType,
        "deliveryVehicleNumber" to deliveryVehicleNumber,
        "isDeliveryPartnerVerified" to isDeliveryPartnerVerified
    )
}

fun Map<String, Any?>.toUser(): User {
    return User(
        email = this["email"] as? String ?: "",
        name = this["name"] as? String ?: "",
        password = this["password"] as? String ?: "",
        isPlusMember = this["isPlusMember"] as? Boolean ?: false,
        savedAddress = this["savedAddress"] as? String ?: "",
        savedAddressLat = (this["savedAddressLat"] as? Number)?.toDouble() ?: 0.0,
        savedAddressLng = (this["savedAddressLng"] as? Number)?.toDouble() ?: 0.0,
        savedCards = this["savedCards"] as? String ?: "",
        deviceCount = (this["deviceCount"] as? Number)?.toInt() ?: 1,
        notificationsEnabled = this["notificationsEnabled"] as? Boolean ?: true,
        selectedLanguage = this["selectedLanguage"] as? String ?: "English",
        privacyAccepted = this["privacyAccepted"] as? Boolean ?: true,
        phone = this["phone"] as? String ?: "",
        gender = this["gender"] as? String ?: "",
        role = this["role"] as? String ?: "User",
        shopName = this["shopName"] as? String ?: "",
        shopAddress = this["shopAddress"] as? String ?: "",
        shopAddressLat = (this["shopAddressLat"] as? Number)?.toDouble() ?: 0.0,
        shopAddressLng = (this["shopAddressLng"] as? Number)?.toDouble() ?: 0.0,
        editRequestPending = this["editRequestPending"] as? Boolean ?: false,
        requestedName = this["requestedName"] as? String ?: "",
        requestedShopName = this["requestedShopName"] as? String ?: "",
        requestedShopAddress = this["requestedShopAddress"] as? String ?: "",
        requestedShopAddressLat = (this["requestedShopAddressLat"] as? Number)?.toDouble() ?: 0.0,
        requestedShopAddressLng = (this["requestedShopAddressLng"] as? Number)?.toDouble() ?: 0.0,
        isSellerVerified = this["isSellerVerified"] as? Boolean ?: false,
        isSellerVerificationPending = this["isSellerVerificationPending"] as? Boolean ?: false,
        sellerMobile = this["sellerMobile"] as? String ?: "",
        sellerAadhaar = this["sellerAadhaar"] as? String ?: "",
        sellerShopPhoto = this["sellerShopPhoto"] as? String ?: "",
        sellerOwnerPhoto = this["sellerOwnerPhoto"] as? String ?: "",
        sellerBankAccount = this["sellerBankAccount"] as? String ?: "",
        sellerPanCard = this["sellerPanCard"] as? String ?: "",
        sellerGstNumber = this["sellerGstNumber"] as? String ?: "",
        
        // --- Delivery Partner Mapping ---
        deliveryMobile = this["deliveryMobile"] as? String ?: "",
        deliveryAadhaar = this["deliveryAadhaar"] as? String ?: "",
        deliveryPhoto = this["deliveryPhoto"] as? String ?: "",
        deliveryAddress = this["deliveryAddress"] as? String ?: "",
        deliveryAddressLat = (this["deliveryAddressLat"] as? Number)?.toDouble() ?: 0.0,
        deliveryAddressLng = (this["deliveryAddressLng"] as? Number)?.toDouble() ?: 0.0,
        deliveryBankAccount = this["deliveryBankAccount"] as? String ?: "",
        deliveryEmergencyContact = this["deliveryEmergencyContact"] as? String ?: "",
        deliveryVehicleType = this["deliveryVehicleType"] as? String ?: "",
        deliveryVehicleNumber = this["deliveryVehicleNumber"] as? String ?: "",
        isDeliveryPartnerVerified = this["isDeliveryPartnerVerified"] as? Boolean ?: false
    )
}

fun Product.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "name" to name,
        "price" to price,
        "originalPrice" to originalPrice,
        "rating" to rating,
        "category" to category,
        "imageUrlName" to imageUrlName,
        "description" to description,
        "isFeatured" to isFeatured,
        "sellerEmail" to sellerEmail,
        "extraImages" to extraImages
    )
}

fun Map<String, Any?>.toProduct(): Product {
    return Product(
        id = (this["id"] as? Number)?.toInt() ?: 0,
        name = this["name"] as? String ?: "",
        price = (this["price"] as? Number)?.toDouble() ?: 0.0,
        originalPrice = (this["originalPrice"] as? Number)?.toDouble() ?: 0.0,
        rating = (this["rating"] as? Number)?.toFloat() ?: 0.0f,
        category = this["category"] as? String ?: "",
        imageUrlName = this["imageUrlName"] as? String ?: "",
        description = this["description"] as? String ?: "",
        isFeatured = this["isFeatured"] as? Boolean ?: false,
        sellerEmail = this["sellerEmail"] as? String ?: "",
        extraImages = this["extraImages"] as? String ?: ""
    )
}

fun Order.toMap(): Map<String, Any> {
    return mapOf(
        "orderId" to orderId,
        "email" to email,
        "orderDate" to orderDate,
        "totalAmount" to totalAmount,
        "itemsAmount" to itemsAmount,
        "deliveryDistanceKm" to deliveryDistanceKm,
        "deliveryFixedCharge" to deliveryFixedCharge,
        "deliveryPerKmCharge" to deliveryPerKmCharge,
        "deliveryCharge" to deliveryCharge,
        "status" to status,
        "itemsSummary" to itemsSummary,
        "deliveryAddress" to deliveryAddress,
        "deliveryAddressLat" to deliveryAddressLat,
        "deliveryAddressLng" to deliveryAddressLng,
        "couponApplied" to couponApplied,
        
        "deliveryPartnerEmail" to deliveryPartnerEmail,
        "deliveryStatus" to deliveryStatus,
        "sellerConfirmed" to sellerConfirmed,
        "sellerRejectRequested" to sellerRejectRequested,
        "sellerChangeDeliveryBoyRequested" to sellerChangeDeliveryBoyRequested,
        "paymentMode" to paymentMode,
        "paymentTransactionId" to paymentTransactionId
    )
}

fun Map<String, Any?>.toOrder(): Order {
    return Order(
        orderId = this["orderId"] as? String ?: "",
        email = this["email"] as? String ?: "",
        orderDate = (this["orderDate"] as? Number)?.toLong() ?: 0L,
        totalAmount = (this["totalAmount"] as? Number)?.toDouble() ?: 0.0,
        itemsAmount = (this["itemsAmount"] as? Number)?.toDouble() ?: 0.0,
        deliveryDistanceKm = (this["deliveryDistanceKm"] as? Number)?.toDouble() ?: 0.0,
        deliveryFixedCharge = (this["deliveryFixedCharge"] as? Number)?.toDouble() ?: 0.0,
        deliveryPerKmCharge = (this["deliveryPerKmCharge"] as? Number)?.toDouble() ?: 0.0,
        deliveryCharge = (this["deliveryCharge"] as? Number)?.toDouble() ?: 0.0,
        status = this["status"] as? String ?: "",
        itemsSummary = this["itemsSummary"] as? String ?: "",
        deliveryAddress = this["deliveryAddress"] as? String ?: "",
        deliveryAddressLat = (this["deliveryAddressLat"] as? Number)?.toDouble() ?: 0.0,
        deliveryAddressLng = (this["deliveryAddressLng"] as? Number)?.toDouble() ?: 0.0,
        couponApplied = this["couponApplied"] as? String ?: "",
        
        deliveryPartnerEmail = this["deliveryPartnerEmail"] as? String ?: "",
        deliveryStatus = this["deliveryStatus"] as? String ?: "",
        sellerConfirmed = this["sellerConfirmed"] as? Boolean ?: false,
        sellerRejectRequested = this["sellerRejectRequested"] as? Boolean ?: false,
        sellerChangeDeliveryBoyRequested = this["sellerChangeDeliveryBoyRequested"] as? Boolean ?: false,
        paymentMode = this["paymentMode"] as? String ?: "COD",
        paymentTransactionId = this["paymentTransactionId"] as? String ?: ""
    )
}

data class PendingCheckout(
    val totalAmount: Double,
    val summary: String,
    val orderId: String,
    val address: String,
    val coupon: String,
    val addressLat: Double = 0.0,
    val addressLng: Double = 0.0,
    val itemsAmount: Double = 0.0,
    val deliveryDistanceKm: Double = 0.0,
    val deliveryFixedCharge: Double = 0.0,
    val deliveryPerKmCharge: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val clearCartAfterCheckout: Boolean = true
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class BazaarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.appDao())

    // --- State: Pending Checkout & Success Event ---
    private val _pendingCheckout = MutableStateFlow<PendingCheckout?>(null)
    val pendingCheckout: StateFlow<PendingCheckout?> = _pendingCheckout.asStateFlow()

    private val _checkoutSuccessEvent = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val checkoutSuccessEvent: SharedFlow<Boolean> = _checkoutSuccessEvent.asSharedFlow()

    fun setPendingCheckout(info: PendingCheckout) {
        _pendingCheckout.value = info
    }

    fun clearPendingCheckout() {
        _pendingCheckout.value = null
    }

    fun notifyCheckoutSuccess() {
        viewModelScope.launch {
            _checkoutSuccessEvent.emit(true)
        }
    }

    // --- State: Auth ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // --- State: Withdrawal Requests ---
    private val _withdrawRequests = MutableStateFlow<List<WithdrawRequest>>(emptyList())
    val withdrawRequests: StateFlow<List<WithdrawRequest>> = _withdrawRequests.asStateFlow()

    init {
        loadWithdrawRequests()
    }

    private fun loadWithdrawRequests() {
        val prefs = getApplication<Application>().getSharedPreferences("delivery_wallet_prefs", android.content.Context.MODE_PRIVATE)
        val raw = prefs.getString("withdrawals", "") ?: ""
        if (raw.isNotEmpty()) {
            try {
                val list = raw.split("||").filter { it.isNotBlank() }.map { chunk ->
                    val parts = chunk.split(";;")
                    WithdrawRequest(
                        id = parts[0],
                        deliveryPartnerEmail = parts[1],
                        amount = parts[2].toDouble(),
                        bankDetails = parts[3],
                        status = parts[4],
                        requestDate = parts[5].toLong()
                    )
                }
                _withdrawRequests.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveWithdrawRequests(list: List<WithdrawRequest>) {
        _withdrawRequests.value = list
        val prefs = getApplication<Application>().getSharedPreferences("delivery_wallet_prefs", android.content.Context.MODE_PRIVATE)
        val raw = list.joinToString("||") { req ->
            "${req.id};;${req.deliveryPartnerEmail};;${req.amount};;${req.bankDetails};;${req.status};;${req.requestDate}"
        }
        prefs.edit().putString("withdrawals", raw).apply()
    }

    fun applyWithdrawal(email: String, amount: Double, bankDetails: String) {
        val req = WithdrawRequest(
            id = "WD-" + System.currentTimeMillis().toString().takeLast(6),
            deliveryPartnerEmail = email,
            amount = amount,
            bankDetails = bankDetails,
            status = "Pending",
            requestDate = System.currentTimeMillis()
        )
        val currentList = _withdrawRequests.value.toMutableList()
        currentList.add(0, req)
        saveWithdrawRequests(currentList)
    }

    fun approveWithdrawal(requestId: String) {
        val currentList = _withdrawRequests.value.map {
            if (it.id == requestId) it.copy(status = "Approved") else it
        }
        saveWithdrawRequests(currentList)
    }

    private val _newlyRegisteredUser = MutableStateFlow<User?>(null)
    val newlyRegisteredUser: StateFlow<User?> = _newlyRegisteredUser.asStateFlow()

    fun clearNewlyRegistered() {
        _newlyRegisteredUser.value = null
    }

    // --- State: Products ---
    val allProducts: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Filtered Products
    val filteredProducts: Flow<List<Product>> = combine(
        allProducts,
        _searchQuery,
        _selectedCategory
    ) { products, query, cat ->
        products.filter { prod ->
            val matchQuery = prod.name.contains(query, ignoreCase = true) || 
                             prod.description.contains(query, ignoreCase = true)
            val matchCat = cat == "All" || prod.category.equals(cat, ignoreCase = true)
            matchQuery && matchCat
        }
    }

    // --- State: Cart & Wishlist ---
    private val _currentCart = MutableStateFlow<List<CartItem>>(emptyList())
    val currentCart: StateFlow<List<CartItem>> = _currentCart.asStateFlow()

    private val _currentWishlist = MutableStateFlow<List<WishlistItem>>(emptyList())
    val currentWishlist: StateFlow<List<WishlistItem>> = _currentWishlist.asStateFlow()

    // --- State: Orders ---
    private val _currentOrders = MutableStateFlow<List<Order>>(emptyList())
    val currentOrders: StateFlow<List<Order>> = _currentOrders.asStateFlow()

    val allOrders: StateFlow<List<Order>> = repository.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State: Dynamic Config ---
    val banners: StateFlow<List<Banner>> = repository.getAllBanners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coupons: StateFlow<List<Coupon>> = repository.getAllCoupons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortcuts: StateFlow<List<Shortcut>> = repository.getAllShortcuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appConfig: StateFlow<AppConfig> = repository.getAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    // Dynamic categories derived from appConfig
    val categories: StateFlow<List<String>> = appConfig
        .map { it.categories }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Electronics", "Fresh Products", "Fashion", "Home & Kitchen"))

    // Dynamic languages derived from appConfig
    val languages: StateFlow<List<String>> = appConfig
        .map { it.languages }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("English", "Español", "Français", "Deutsch", "Hindi", "Bengali"))

    // --- State: Splash ---
    private val _splashCompleted = MutableStateFlow(false)
    val splashCompleted: StateFlow<Boolean> = _splashCompleted.asStateFlow()

    init {
        seedInitialData()
        observeCartAndWishlist()
        setupFirestoreSync()
        syncLocalDataToFirestore()
        checkPersistedSession()
        observeUserSessionChanges()
    }

    private fun checkPersistedSession() {
        val prefs = getApplication<Application>().getSharedPreferences("bazaar_prefs", android.content.Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("logged_in_email", null)
        val emailToCheck = savedEmail ?: FirebaseAuth.getInstance().currentUser?.email
        
        if (!emailToCheck.isNullOrBlank()) {
            viewModelScope.launch {
                val user = repository.getUser(emailToCheck)
                if (user != null) {
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                }
            }
        }
    }

    private fun observeUserSessionChanges() {
        viewModelScope.launch {
            _currentUser.collect { user ->
                val prefs = getApplication<Application>().getSharedPreferences("bazaar_prefs", android.content.Context.MODE_PRIVATE)
                if (user != null) {
                    prefs.edit().putString("logged_in_email", user.email).apply()
                } else {
                    prefs.edit().remove("logged_in_email").apply()
                }
            }
        }
    }

    private var lastSyncedEmail: String? = null

    private fun observeCartAndWishlist() {
        viewModelScope.launch {
            _currentUser.collectLatest { user ->
                if (user != null) {
                    if (lastSyncedEmail != user.email) {
                        lastSyncedEmail = user.email
                        repository.syncCartFromFirestore(user.email)
                        repository.syncWishlistFromFirestore(user.email)
                    }
                } else {
                    lastSyncedEmail = null
                }
            }
        }

        viewModelScope.launch {
            _currentUser.collectLatest { user ->
                if (user != null) {
                    // Collect cart locally from Room DB
                    repository.getCartItems(user.email).collectLatest { cart ->
                        _currentCart.value = cart
                    }
                } else {
                    _currentCart.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            _currentUser.collectLatest { user ->
                if (user != null) {
                    // Collect wishlist locally from Room DB
                    repository.getWishlistItems(user.email).collectLatest { wish ->
                        _currentWishlist.value = wish
                    }
                } else {
                    _currentWishlist.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            _currentUser.collectLatest { user ->
                if (user != null) {
                    // Collect orders
                    repository.getOrders(user.email).collectLatest { orders ->
                        _currentOrders.value = orders
                    }
                } else {
                    _currentOrders.value = emptyList()
                }
            }
        }
    }

    private fun setupFirestoreSync() {
        try {
            val db = FirebaseFirestore.getInstance()
            
            // Sync all products from Firestore to Room
            db.collection("products").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val prods = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toProduct()
                    }
                    if (prods.isNotEmpty()) {
                        viewModelScope.launch {
                            repository.insertProducts(prods)
                        }
                    }
                }
            }

            // Sync all users from Firestore to Room for real-time verification and syncing
            db.collection("users").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val userList = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toUser()
                    }
                    viewModelScope.launch {
                        userList.forEach { repository.insertUser(it) }
                    }
                }
            }
            
            // Sync all orders from Firestore to Room
            db.collection("orders").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orderList = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toOrder()
                    }
                    viewModelScope.launch {
                        orderList.forEach { order ->
                            repository.upsertOrderLocal(order)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun syncLocalDataToFirestore() {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                
                // Push local users to Firestore
                repository.getAllUsers().first().forEach { user ->
                    db.collection("users").document(user.email).set(user.toMap())
                }
                
                // Push local products to Firestore
                repository.getAllProducts().first().forEach { prod ->
                    db.collection("products").document(prod.id.toString()).set(prod.toMap())
                }
                
                // Push local orders to Firestore
                repository.getAllOrders().first().forEach { order ->
                    db.collection("orders").document(order.orderId).set(order.toMap())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun completeSplash() {
        _splashCompleted.value = true
    }

    // Validate a coupon code against dynamic coupons from Firestore
    fun validateCoupon(code: String): Coupon? {
        return coupons.value.find { it.code.equals(code.trim(), ignoreCase = true) && it.isActive }
    }

    fun wipeDummyData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. Delete all products from Firestore
                val productsSnap = db.collection("products").get().awaitTask()
                productsSnap.documents.forEach { doc ->
                    db.collection("products").document(doc.id).delete().awaitTask()
                }

                // 2. Delete all users from Firestore
                val usersSnap = db.collection("users").get().awaitTask()
                usersSnap.documents.forEach { doc ->
                    db.collection("users").document(doc.id).delete().awaitTask()
                }

                // 3. Delete all orders from Firestore
                val ordersSnap = db.collection("orders").get().awaitTask()
                ordersSnap.documents.forEach { doc ->
                    db.collection("orders").document(doc.id).delete().awaitTask()
                }

                // 4. Delete all cart items from Firestore
                val cartSnap = db.collection("cart_items").get().awaitTask()
                cartSnap.documents.forEach { doc ->
                    db.collection("cart_items").document(doc.id).delete().awaitTask()
                }

                // 5. Delete all wishlist items from Firestore
                val wishlistSnap = db.collection("wishlist_items").get().awaitTask()
                wishlistSnap.documents.forEach { doc ->
                    db.collection("wishlist_items").document(doc.id).delete().awaitTask()
                }

                // 6. Delete config templates to allow clean re-seed
                val bannersSnap = db.collection("banners").get().awaitTask()
                bannersSnap.documents.forEach { doc ->
                    db.collection("banners").document(doc.id).delete().awaitTask()
                }

                val couponsSnap = db.collection("coupons").get().awaitTask()
                couponsSnap.documents.forEach { doc ->
                    db.collection("coupons").document(doc.id).delete().awaitTask()
                }

                val shortcutsSnap = db.collection("shortcuts").get().awaitTask()
                shortcutsSnap.documents.forEach { doc ->
                    db.collection("shortcuts").document(doc.id).delete().awaitTask()
                }

                db.collection("app_config").document("main").delete().awaitTask()

                // Clear local Room database tables
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }

                // Re-seed only configurations, not products
                seedInitialData()

                // Logout current user since user account is deleted
                FirebaseAuth.getInstance().signOut()
                _currentUser.value = null
                _authState.value = AuthState.Idle

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun seedInitialData() {
        viewModelScope.launch {
            // Seed dynamic config collections only (do not seed any dummy products)
            repository.seedBanners()
            repository.seedCoupons()
            repository.seedShortcuts()
            repository.seedAppConfig()
        }
    }



    // --- Search & Filter ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }

    // --- Actions: Authentication ---
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun login(email: String, pword: String, role: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (email.isBlank() || pword.isBlank()) {
                _authState.value = AuthState.Error("Please enter email and password.")
                return@launch
            }

            try {
                // Try to login with Firebase within a 4-second timeout to prevent hanging forever
                val firebaseSuccess = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    try {
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pword).awaitTask()
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }

                if (firebaseSuccess == true) {
                    // Fetch user from Firestore within 4 seconds
                    val doc = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                        try {
                            FirebaseFirestore.getInstance().collection("users")
                                .document(email)
                                .get()
                                .awaitTask()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }

                    val user = doc?.data?.toUser()
                    if (user != null) {
                        if (user.role != role) {
                            _authState.value = AuthState.Error("Incorrect account type selected. This email is registered as a ${user.role}.")
                            return@launch
                        }

                        // Save local copy
                        repository.insertUser(user)
                        _currentUser.value = user
                        _authState.value = AuthState.Success(user)
                        return@launch
                    }
                }

                // If firebase auth was unsuccessful or timed out, fall back to offline local database
                val localUser = repository.getUser(email)
                if (localUser != null && localUser.password == pword) {
                    if (localUser.role != role) {
                        _authState.value = AuthState.Error("Incorrect account type selected. This email is registered as a ${localUser.role}.")
                    } else {
                        _currentUser.value = localUser
                        _authState.value = AuthState.Success(localUser)
                    }
                } else {
                    _authState.value = AuthState.Error("Authentication failed. Check your credentials or internet connection.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful fallback to offline local database if offline / network error
                val localUser = repository.getUser(email)
                if (localUser != null && localUser.password == pword) {
                    if (localUser.role != role) {
                        _authState.value = AuthState.Error("Incorrect account type selected. This email is registered as a ${localUser.role}.")
                    } else {
                        _currentUser.value = localUser
                        _authState.value = AuthState.Success(localUser)
                    }
                } else {
                    val errorMsg = e.localizedMessage ?: "Authentication failed. Check your credentials or internet connection."
                    _authState.value = AuthState.Error(errorMsg)
                }
            }
        }
    }

    fun register(
        name: String,
        email: String,
        pword: String,
        confPword: String,
        role: String = "User",
        shopName: String = "",
        shopAddress: String = "",
        shopAddressLat: Double = 0.0,
        shopAddressLng: Double = 0.0,
        sellerMobile: String = "",
        sellerAadhaar: String = "",
        sellerShopPhoto: String = "",
        sellerOwnerPhoto: String = "",
        sellerBankAccount: String = "",
        sellerPanCard: String = "",
        sellerGstNumber: String = "",
        
        // --- Delivery Partner optional params ---
        deliveryMobile: String = "",
        deliveryAadhaar: String = "",
        deliveryPhoto: String = "",
        deliveryAddress: String = "",
        deliveryAddressLat: Double = 0.0,
        deliveryAddressLng: Double = 0.0,
        deliveryBankAccount: String = "",
        deliveryEmergencyContact: String = "",
        deliveryVehicleType: String = "",
        deliveryVehicleNumber: String = ""
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (name.isBlank() || email.isBlank() || pword.isBlank()) {
                _authState.value = AuthState.Error("All fields are mandatory.")
                return@launch
            }
            if (pword != confPword) {
                _authState.value = AuthState.Error("Passwords do not match.")
                return@launch
            }

            val newUser = User(
                email = email,
                name = name,
                password = pword,
                savedAddress = if (role == "Seller") shopAddress else if (role == "DeliveryPartner") deliveryAddress else "ffff123 Green Bazaar Lane, Eco City, 54002",
                savedAddressLat = if (role == "Seller") shopAddressLat else if (role == "DeliveryPartner") deliveryAddressLat else 0.0,
                savedAddressLng = if (role == "Seller") shopAddressLng else if (role == "DeliveryPartner") deliveryAddressLng else 0.0,
                savedCards = "*4242 (Visa Classic)",
                role = role,
                shopName = shopName,
                shopAddress = shopAddress,
                shopAddressLat = shopAddressLat,
                shopAddressLng = shopAddressLng,
                isSellerVerified = false,
                isSellerVerificationPending = if (role == "Seller") true else false,
                sellerMobile = sellerMobile,
                sellerAadhaar = sellerAadhaar,
                sellerShopPhoto = sellerShopPhoto,
                sellerOwnerPhoto = sellerOwnerPhoto,
                sellerBankAccount = sellerBankAccount,
                sellerPanCard = sellerPanCard,
                sellerGstNumber = sellerGstNumber,
                
                deliveryMobile = deliveryMobile,
                deliveryAadhaar = deliveryAadhaar,
                deliveryPhoto = deliveryPhoto,
                deliveryAddress = deliveryAddress,
                deliveryAddressLat = deliveryAddressLat,
                deliveryAddressLng = deliveryAddressLng,
                deliveryBankAccount = deliveryBankAccount,
                deliveryEmergencyContact = deliveryEmergencyContact,
                deliveryVehicleType = deliveryVehicleType,
                deliveryVehicleNumber = deliveryVehicleNumber,
                isDeliveryPartnerVerified = false
            )

            try {
                // Try to register in Firebase Auth and Firestore with a 4-second timeout to prevent hanging forever
                val firebaseSuccess = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    try {
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pword).awaitTask()
                        FirebaseFirestore.getInstance().collection("users")
                            .document(email)
                            .set(newUser.toMap())
                            .awaitTask()
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }

                // Regardless of Firebase timing out or failing (offline), register user in local Room database
                repository.insertUser(newUser)
                _newlyRegisteredUser.value = newUser
                _currentUser.value = newUser
                _authState.value = AuthState.Success(newUser)

                if (firebaseSuccess == true) {
                    // Successfully registered to both Firebase and Room!
                } else {
                    // Successfully registered to local database (offline fallback mode)!
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful fallback to offline local database registration
                try {
                    repository.insertUser(newUser)
                    _newlyRegisteredUser.value = newUser
                    _currentUser.value = newUser
                    _authState.value = AuthState.Success(newUser)
                } catch (localEx: Exception) {
                    val errorMsg = e.localizedMessage ?: "Registration failed. Please check network connection."
                    _authState.value = AuthState.Error(errorMsg)
                }
            }
        }
    }

    private fun saveUser(user: User) {
        viewModelScope.launch {
            repository.insertUser(user)
            if (_currentUser.value?.email == user.email) {
                _currentUser.value = user
            }
            try {
                FirebaseFirestore.getInstance().collection("users")
                    .document(user.email)
                    .set(user.toMap())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    // --- Profile & Account Settings Actions ---
    fun updateProfile(name: String, phone: String, gender: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            name = name.trim(),
            phone = phone.trim(),
            gender = gender
        )
        saveUser(updated)
    }

    fun togglePlusMembership() {
        val current = _currentUser.value ?: return
        val updated = current.copy(isPlusMember = !current.isPlusMember)
        saveUser(updated)
    }

    fun updateAddress(address: String, latitude: Double = 0.0, longitude: Double = 0.0) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            savedAddress = address.trim(),
            savedAddressLat = latitude,
            savedAddressLng = longitude
        )
        saveUser(updated)
    }

    fun updateCards(card: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(savedCards = card.trim())
        saveUser(updated)
    }

    fun updateLanguage(lang: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(selectedLanguage = lang)
        saveUser(updated)
    }

    fun toggleNotifications(enabled: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(notificationsEnabled = enabled)
        saveUser(updated)
    }

    fun updatePrivacyAccepted(accepted: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(privacyAccepted = accepted)
        saveUser(updated)
    }

    fun addDevice() {
        val current = _currentUser.value ?: return
        val updated = current.copy(deviceCount = current.deviceCount + 1)
        saveUser(updated)
    }

    fun removeDevice() {
        val current = _currentUser.value ?: return
        if (current.deviceCount <= 1) return
        val updated = current.copy(deviceCount = current.deviceCount - 1)
        saveUser(updated)
    }

    fun verifyDeliveryPartner(email: String, verified: Boolean) {
        viewModelScope.launch {
            val user = repository.getUser(email) ?: return@launch
            val updated = user.copy(isDeliveryPartnerVerified = verified)
            saveUser(updated)
        }
    }

    fun updateDeliveryPartnerProfile(
        name: String,
        phone: String,
        vehicleType: String,
        vehicleNumber: String,
        emergencyContact: String,
        address: String,
        addressLat: Double = 0.0,
        addressLng: Double = 0.0
    ) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            name = name,
            deliveryMobile = phone,
            deliveryVehicleType = vehicleType,
            deliveryVehicleNumber = vehicleNumber,
            deliveryEmergencyContact = emergencyContact,
            deliveryAddress = address,
            deliveryAddressLat = addressLat,
            deliveryAddressLng = addressLng
        )
        saveUser(updated)
    }

    // --- Actions: Wishlist ---
    fun toggleWishlist(product: Product) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val currentWishItem = repository.getWishlistItem(user.email, product.id)
            if (currentWishItem != null) {
                repository.deleteWishlistItem(user.email, product.id)
            } else {
                repository.insertWishlistItem(WishlistItem(email = user.email, productId = product.id))
            }
        }
    }

    fun isWishlisted(prodId: Int): Boolean {
        return _currentWishlist.value.any { it.productId == prodId }
    }

    // --- Actions: Cart ---
    fun addToCart(product: Product) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val existing = _currentCart.value.find { it.productId == product.id }
            if (existing != null) {
                repository.updateCartQuantity(existing.id, existing.quantity + 1)
            } else {
                repository.insertCartItem(CartItem(email = user.email, productId = product.id))
            }
        }
    }

    fun increaseCartQty(item: CartItem) {
        viewModelScope.launch {
            repository.updateCartQuantity(item.id, item.quantity + 1)
        }
    }

    fun decreaseCartQty(item: CartItem) {
        viewModelScope.launch {
            if (item.quantity > 1) {
                repository.updateCartQuantity(item.id, item.quantity - 1)
            } else {
                repository.deleteCartItem(item.id)
            }
        }
    }

    fun removeCartItem(item: CartItem) {
        viewModelScope.launch {
            repository.deleteCartItem(item.id)
        }
    }

    // --- Actions: Purchase / Clear Cart ---
    fun checkout(
        totalAmount: Double,
        summary: String,
        customOrderId: String = "",
        deliveryAddress: String = "",
        couponApplied: String = "",
        deliveryAddressLat: Double = 0.0,
        deliveryAddressLng: Double = 0.0,
        itemsAmount: Double = 0.0,
        deliveryDistanceKm: Double = 0.0,
        deliveryFixedCharge: Double = 0.0,
        deliveryPerKmCharge: Double = 0.0,
        deliveryCharge: Double = 0.0,
        paymentMode: String = "COD",
        paymentTransactionId: String = "",
        clearCartAfterCheckout: Boolean = true
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val orderId = if (customOrderId.isNotBlank()) customOrderId else "ZVB-" + UUID.randomUUID().toString().uppercase().take(8)
            val newOrder = Order(
                orderId = orderId,
                email = user.email,
                orderDate = System.currentTimeMillis(),
                totalAmount = totalAmount,
                itemsAmount = itemsAmount,
                deliveryDistanceKm = deliveryDistanceKm,
                deliveryFixedCharge = deliveryFixedCharge,
                deliveryPerKmCharge = deliveryPerKmCharge,
                deliveryCharge = deliveryCharge,
                status = "Processing",
                itemsSummary = summary,
                deliveryAddress = deliveryAddress,
                deliveryAddressLat = deliveryAddressLat,
                deliveryAddressLng = deliveryAddressLng,
                couponApplied = couponApplied,
                paymentMode = paymentMode,
                paymentTransactionId = paymentTransactionId
            )
            repository.insertOrder(newOrder)
            if (clearCartAfterCheckout) {
                repository.clearCart(user.email)
            }
            try {
                FirebaseFirestore.getInstance().collection("orders")
                    .document(orderId)
                    .set(newOrder.toMap())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Actions: Seller Panel Support ---
    fun addProduct(
        name: String,
        price: Double,
        originalPrice: Double,
        category: String,
        description: String,
        sellerEmail: String,
        extraImages: String = ""
    ) {
        viewModelScope.launch {
            val id = (System.currentTimeMillis() % 10000000).toInt()
            val imageUrls = extraImages
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val primaryImage = imageUrls.firstOrNull().orEmpty()
            val secondaryImages = imageUrls.drop(1).joinToString(",")
            val newProd = Product(
                id = id,
                name = name,
                price = price,
                originalPrice = originalPrice,
                rating = 4.5f + (Math.random() * 0.5).toFloat(),
                category = category,
                imageUrlName = primaryImage,
                description = description,
                isFeatured = false,
                sellerEmail = sellerEmail,
                extraImages = secondaryImages
            )
            repository.insertProduct(newProd)
            try {
                FirebaseFirestore.getInstance().collection("products")
                    .document(id.toString())
                    .set(newProd.toMap())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val orderList = allOrders.value
            val found = orderList.find { it.orderId == orderId } ?: return@launch
            val updated = found.copy(status = newStatus)
            repository.updateOrder(updated)
            try {
                FirebaseFirestore.getInstance().collection("orders")
                    .document(orderId)
                    .update("status", newStatus)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun applyShopEditRequest(
        requestedName: String,
        requestedShopName: String,
        requestedShopAddress: String,
        requestedShopAddressLat: Double = 0.0,
        requestedShopAddressLng: Double = 0.0
    ) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            editRequestPending = true,
            requestedName = requestedName,
            requestedShopName = requestedShopName,
            requestedShopAddress = requestedShopAddress,
            requestedShopAddressLat = requestedShopAddressLat,
            requestedShopAddressLng = requestedShopAddressLng
        )
        saveUser(updated)
    }

    fun approveProfileEditRequest(userEmail: String) {
        viewModelScope.launch {
            val user = repository.getUser(userEmail) ?: return@launch
            if (user.editRequestPending) {
                val updated = user.copy(
                    name = user.requestedName.ifBlank { user.name },
                    shopName = user.requestedShopName.ifBlank { user.shopName },
                    shopAddress = user.requestedShopAddress.ifBlank { user.shopAddress },
                    shopAddressLat = if (user.requestedShopAddress.isNotBlank()) user.requestedShopAddressLat else user.shopAddressLat,
                    shopAddressLng = if (user.requestedShopAddress.isNotBlank()) user.requestedShopAddressLng else user.shopAddressLng,
                    editRequestPending = false,
                    requestedName = "",
                    requestedShopName = "",
                    requestedShopAddress = "",
                    requestedShopAddressLat = 0.0,
                    requestedShopAddressLng = 0.0
                )
                saveUser(updated)
            }
        }
    }

    fun acceptOrder(orderId: String, deliveryPartnerEmail: String) {
        viewModelScope.launch {
            val orderList = allOrders.value
            val found = orderList.find { it.orderId == orderId }
            val requestAgeMs = System.currentTimeMillis() - (found?.orderDate ?: 0L)
            if (found != null && found.deliveryPartnerEmail.isBlank() && requestAgeMs <= 24 * 60 * 60 * 1000L) {
                val updated = found.copy(
                    deliveryPartnerEmail = deliveryPartnerEmail,
                    status = "Delivery Accepted",
                    deliveryStatus = "Shipping Ready"
                )
                repository.updateOrder(updated)
                try {
                    FirebaseFirestore.getInstance().collection("orders")
                        .document(orderId)
                        .set(updated.toMap())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun rejectOrderRequestByDeliveryPartner(orderId: String) {
        viewModelScope.launch {
            val orderList = allOrders.value
            val found = orderList.find { it.orderId == orderId }
            if (found != null) {
                val updated = found.copy(
                    deliveryPartnerEmail = "",
                    deliveryStatus = "",
                    status = if (found.sellerConfirmed) "Shipping Ready" else "Processing"
                )
                repository.updateOrder(updated)
                try {
                    FirebaseFirestore.getInstance().collection("orders")
                        .document(orderId)
                        .set(updated.toMap())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateDeliveryStatus(orderId: String, newDeliveryStatus: String) {
        viewModelScope.launch {
            val orderList = allOrders.value
            val found = orderList.find { it.orderId == orderId }
            if (found != null) {
                val mainStatus = when (newDeliveryStatus) {
                    "Delivered" -> "Delivered"
                    "On the Way" -> "On the Way"
                    "Delivery Take More Time" -> "Delivery Take More Time"
                    else -> found.status
                }
                val updated = found.copy(
                    deliveryStatus = newDeliveryStatus,
                    status = mainStatus
                )
                repository.updateOrder(updated)
                try {
                    FirebaseFirestore.getInstance().collection("orders")
                        .document(orderId)
                        .set(updated.toMap())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun confirmOrderReady(orderId: String) {
        viewModelScope.launch {
            val orderList = allOrders.value
            val found = orderList.find { it.orderId == orderId }
            if (found != null) {
                val updated = found.copy(sellerConfirmed = true, status = "Shipping Ready", deliveryStatus = "")
                repository.updateOrder(updated)
                try {
                    FirebaseFirestore.getInstance().collection("orders")
                        .document(orderId)
                        .set(updated.toMap())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun requestRejectOrderBySeller(orderId: String) {
        viewModelScope.launch {
            val orderList = allOrders.value
            val found = orderList.find { it.orderId == orderId }
            if (found != null) {
                val updated = found.copy(sellerRejectRequested = true, status = "Seller Reject Requested")
                repository.updateOrder(updated)
                try {
                    FirebaseFirestore.getInstance().collection("orders")
                        .document(orderId)
                        .set(updated.toMap())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun requestChangeDeliveryBoy(orderId: String) {
        viewModelScope.launch {
            val orderList = allOrders.value
            val found = orderList.find { it.orderId == orderId }
            if (found != null) {
                val updated = found.copy(
                    sellerChangeDeliveryBoyRequested = true,
                    deliveryPartnerEmail = "", // reset delivery partner assignment so someone else accepts
                    deliveryStatus = "",
                    status = "Shipping Ready"
                )
                repository.updateOrder(updated)
                try {
                    FirebaseFirestore.getInstance().collection("orders")
                        .document(orderId)
                        .set(updated.toMap())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
            try {
                FirebaseFirestore.getInstance().collection("orders")
                    .document(orderId)
                    .delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun uploadImageToFirebaseStorage(uri: Uri, path: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(path)
                storageRef.putFile(uri).awaitTask()
                val downloadUrl = storageRef.downloadUrl.awaitTask().toString()
                onSuccess(downloadUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful fallback: return the content Uri string as the image URL
                // Coil will render this content Uri perfectly on screen
                onSuccess(uri.toString())
            }
        }
    }

    fun uploadProductImageToCloudinary(uri: Uri, sellerEmail: String, imageId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
                val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET
                val folder = BuildConfig.CLOUDINARY_FOLDER

                if (cloudName.isBlank() || uploadPreset.isBlank()) {
                    throw IllegalStateException("Cloudinary cloud name or upload preset is missing.")
                }

                val downloadUrl = withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val resolver = app.contentResolver
                    val mimeType = resolver.getType(uri) ?: "image/jpeg"
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
                    val safeSeller = sellerEmail.ifBlank { "unknown" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    val publicId = "${safeSeller}_$imageId"
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Unable to read selected image.")

                    val fileBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    val multipart = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "product_$imageId.$extension", fileBody)
                        .addFormDataPart("upload_preset", uploadPreset)
                        .addFormDataPart("public_id", publicId)
                        .apply {
                            if (folder.isNotBlank()) addFormDataPart("folder", folder)
                        }
                        .build()

                    val request = Request.Builder()
                        .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                        .post(multipart)
                        .build()

                    OkHttpClient().newCall(request).execute().use { response ->
                        val raw = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            throw IllegalStateException("Cloudinary upload failed: $raw")
                        }
                        JSONObject(raw).optString("secure_url").ifBlank {
                            JSONObject(raw).optString("url")
                        }.ifBlank {
                            throw IllegalStateException("Cloudinary did not return an image URL.")
                        }
                    }
                }

                onSuccess(downloadUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                onSuccess(uri.toString())
            }
        }
    }
}

data class WithdrawRequest(
    val id: String,
    val deliveryPartnerEmail: String,
    val amount: Double,
    val bankDetails: String,
    val status: String, // "Pending", "Approved"
    val requestDate: Long
)
