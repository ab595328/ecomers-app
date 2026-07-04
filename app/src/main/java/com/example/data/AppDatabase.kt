package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.first
import com.google.android.gms.tasks.Task
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

// --- Entity: User ---
@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val password: String,
    val isPlusMember: Boolean = false,
    val savedAddress: String = "",
    val savedAddressLat: Double = 0.0,
    val savedAddressLng: Double = 0.0,
    val savedCards: String = "",
    val deviceCount: Int = 1,
    val notificationsEnabled: Boolean = true,
    val selectedLanguage: String = "English",
    val privacyAccepted: Boolean = true,
    val phone: String = "",
    val gender: String = "",
    val role: String = "User", // "User" or "Seller"
    val shopName: String = "",
    val shopAddress: String = "",
    val shopAddressLat: Double = 0.0,
    val shopAddressLng: Double = 0.0,
    val editRequestPending: Boolean = false,
    val requestedName: String = "",
    val requestedShopName: String = "",
    val requestedShopAddress: String = "",
    val requestedShopAddressLat: Double = 0.0,
    val requestedShopAddressLng: Double = 0.0,
    val isSellerVerified: Boolean = false,
    val isSellerVerificationPending: Boolean = false,
    val sellerMobile: String = "",
    val sellerAadhaar: String = "",
    val sellerShopPhoto: String = "",
    val sellerOwnerPhoto: String = "",
    val sellerBankAccount: String = "",
    val sellerPanCard: String = "",
    val sellerGstNumber: String = "",
    
    // --- Delivery Partner Properties ---
    val deliveryMobile: String = "",
    val deliveryAadhaar: String = "",
    val deliveryPhoto: String = "",
    val deliveryAddress: String = "",
    val deliveryAddressLat: Double = 0.0,
    val deliveryAddressLng: Double = 0.0,
    val deliveryBankAccount: String = "",
    val deliveryEmergencyContact: String = "",
    val deliveryVehicleType: String = "",
    val deliveryVehicleNumber: String = "",
    val isDeliveryPartnerVerified: Boolean = false
)

// --- Entity: Product ---
@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: Int,
    val name: String,
    val price: Double,
    val originalPrice: Double,
    val rating: Float,
    val category: String,
    val imageUrlName: String, // name of local drawable or asset
    val description: String,
    val isFeatured: Boolean = false,
    val sellerEmail: String = "", // tags who listed the product
    val extraImages: String = "", // comma-separated secondary images
    val stockQuantity: Int = 0
)

// --- Entity: CartItem ---
@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val productId: Int,
    val quantity: Int = 1
)

// --- Entity: WishlistItem ---
@Entity(tableName = "wishlist_items")
data class WishlistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val productId: Int
)

// --- Entity: Order ---
@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val orderId: String,
    val email: String,
    val orderDate: Long,
    val totalAmount: Double,
    val itemsAmount: Double = 0.0,
    val deliveryDistanceKm: Double = 0.0,
    val deliveryFixedCharge: Double = 0.0,
    val deliveryPerKmCharge: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val status: String, // "Processing", "Shipped", "Delivered", "Cancelled"
    val itemsSummary: String,
    val deliveryAddress: String = "",
    val deliveryAddressLat: Double = 0.0,
    val deliveryAddressLng: Double = 0.0,
    val couponApplied: String = "",
    
    // --- Delivery Tracking Properties ---
    val deliveryPartnerEmail: String = "",
    val deliveryStatus: String = "", // "On the Way", "Delivery Take More Time", "Delivered"
    val sellerConfirmed: Boolean = false,
    val sellerRejectRequested: Boolean = false,
    val sellerChangeDeliveryBoyRequested: Boolean = false,
    val paymentMode: String = "COD",
    val paymentTransactionId: String = "",
    val productQuantities: String = ""
)

// --- Dynamic Config Data Classes (Firestore-only, not Room entities) ---

data class Banner(
    val id: String = "",
    val label: String = "",
    val title: String = "",
    val description: String = "",
    val targetCategory: String = "",
    val gradientStart: String = "#1B5E20",
    val gradientEnd: String = "#A5D6A7",
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

data class Coupon(
    val code: String = "",
    val discountPercent: Int = 0,
    val description: String = "",
    val isActive: Boolean = true,
    val minOrderAmount: Double = 0.0,
    val maxDiscount: Double = 999999.0
)

data class Shortcut(
    val title: String = "",
    val iconName: String = "flash_on",
    val colorHex: String = "#FF5722",
    val action: String = "",
    val toastMessage: String = "",
    val sortOrder: Int = 0
)

data class AppConfig(
    val categories: List<String> = listOf("All", "Electronics", "Fresh Products", "Fashion", "Home & Kitchen"),
    val languages: List<String> = listOf("English", "Español", "Français", "Deutsch", "Hindi", "Bengali"),
    val flashSaleEndTime: Long = 0L,
    val serviceCities: List<String> = emptyList(),
    val servicePincodes: List<String> = emptyList(),
    val payoutDelayHours: Int = 24
)

// --- DAO Interface ---
@Dao
interface AppDao {
    // User Queries
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUser(email: String): User?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    // Product Queries
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProduct(productId: Int)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    // Cart Queries
    @Query("SELECT * FROM cart_items WHERE email = :email")
    fun getCartItems(email: String): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItem)

    @Query("UPDATE cart_items SET quantity = :qty WHERE id = :itemId")
    suspend fun updateCartQuantity(itemId: Int, qty: Int)

    @Query("DELETE FROM cart_items WHERE id = :itemId")
    suspend fun deleteCartItem(itemId: Int)

    @Query("DELETE FROM cart_items WHERE email = :email")
    suspend fun clearCart(email: String)

    // Wishlist Queries
    @Query("SELECT * FROM wishlist_items WHERE email = :email")
    fun getWishlistItems(email: String): Flow<List<WishlistItem>>

    @Query("SELECT * FROM wishlist_items WHERE email = :email AND productId = :prodId LIMIT 1")
    suspend fun getWishlistItem(email: String, prodId: Int): WishlistItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistItem)

    @Query("DELETE FROM wishlist_items WHERE email = :email AND productId = :prodId")
    suspend fun deleteWishlistItem(email: String, prodId: Int)

    @Query("DELETE FROM wishlist_items WHERE email = :email")
    suspend fun clearWishlist(email: String)

    // Order Queries
    @Query("SELECT * FROM orders WHERE email = :email ORDER BY orderDate DESC")
    fun getOrders(email: String): Flow<List<Order>>

    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    @Query("DELETE FROM orders WHERE orderId = :orderId")
    suspend fun deleteOrder(orderId: String)
}

// --- App Database ---
@Database(
    entities = [User::class, Product::class, CartItem::class, WishlistItem::class, Order::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zyl_vor_bazaar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Helper Extension for Tasks ---
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

// --- Repository ---
class AppRepository(private val appDao: AppDao) {

    // Private Mappers
    private fun mapToUser(map: Map<String, Any?>): User {
        return User(
            email = map["email"] as? String ?: "",
            name = map["name"] as? String ?: "",
            password = map["password"] as? String ?: "",
            isPlusMember = map["isPlusMember"] as? Boolean ?: false,
            savedAddress = map["savedAddress"] as? String ?: "",
            savedAddressLat = (map["savedAddressLat"] as? Number)?.toDouble() ?: 0.0,
            savedAddressLng = (map["savedAddressLng"] as? Number)?.toDouble() ?: 0.0,
            savedCards = map["savedCards"] as? String ?: "",
            deviceCount = (map["deviceCount"] as? Number)?.toInt() ?: 1,
            notificationsEnabled = map["notificationsEnabled"] as? Boolean ?: true,
            selectedLanguage = map["selectedLanguage"] as? String ?: "English",
            privacyAccepted = map["privacyAccepted"] as? Boolean ?: true,
            phone = map["phone"] as? String ?: "",
            gender = map["gender"] as? String ?: "",
            role = map["role"] as? String ?: "User",
            shopName = map["shopName"] as? String ?: "",
            shopAddress = map["shopAddress"] as? String ?: "",
            shopAddressLat = (map["shopAddressLat"] as? Number)?.toDouble() ?: 0.0,
            shopAddressLng = (map["shopAddressLng"] as? Number)?.toDouble() ?: 0.0,
            editRequestPending = map["editRequestPending"] as? Boolean ?: false,
            requestedName = map["requestedName"] as? String ?: "",
            requestedShopName = map["requestedShopName"] as? String ?: "",
            requestedShopAddress = map["requestedShopAddress"] as? String ?: "",
            requestedShopAddressLat = (map["requestedShopAddressLat"] as? Number)?.toDouble() ?: 0.0,
            requestedShopAddressLng = (map["requestedShopAddressLng"] as? Number)?.toDouble() ?: 0.0,
            isSellerVerified = map["isSellerVerified"] as? Boolean ?: false,
            isSellerVerificationPending = map["isSellerVerificationPending"] as? Boolean ?: false,
            sellerMobile = map["sellerMobile"] as? String ?: "",
            sellerAadhaar = map["sellerAadhaar"] as? String ?: "",
            sellerShopPhoto = map["sellerShopPhoto"] as? String ?: "",
            sellerOwnerPhoto = map["sellerOwnerPhoto"] as? String ?: "",
            sellerBankAccount = map["sellerBankAccount"] as? String ?: "",
            sellerPanCard = map["sellerPanCard"] as? String ?: "",
            sellerGstNumber = map["sellerGstNumber"] as? String ?: "",
            deliveryMobile = map["deliveryMobile"] as? String ?: "",
            deliveryAadhaar = map["deliveryAadhaar"] as? String ?: "",
            deliveryPhoto = map["deliveryPhoto"] as? String ?: "",
            deliveryAddress = map["deliveryAddress"] as? String ?: "",
            deliveryAddressLat = (map["deliveryAddressLat"] as? Number)?.toDouble() ?: 0.0,
            deliveryAddressLng = (map["deliveryAddressLng"] as? Number)?.toDouble() ?: 0.0,
            deliveryBankAccount = map["deliveryBankAccount"] as? String ?: "",
            deliveryEmergencyContact = map["deliveryEmergencyContact"] as? String ?: "",
            deliveryVehicleType = map["deliveryVehicleType"] as? String ?: "",
            deliveryVehicleNumber = map["deliveryVehicleNumber"] as? String ?: "",
            isDeliveryPartnerVerified = map["isDeliveryPartnerVerified"] as? Boolean ?: false
        )
    }

    private fun userToMap(user: User): Map<String, Any> {
        return mapOf(
            "email" to user.email,
            "name" to user.name,
            "password" to user.password,
            "isPlusMember" to user.isPlusMember,
            "savedAddress" to user.savedAddress,
            "savedAddressLat" to user.savedAddressLat,
            "savedAddressLng" to user.savedAddressLng,
            "savedCards" to user.savedCards,
            "deviceCount" to user.deviceCount,
            "notificationsEnabled" to user.notificationsEnabled,
            "selectedLanguage" to user.selectedLanguage,
            "privacyAccepted" to user.privacyAccepted,
            "phone" to user.phone,
            "gender" to user.gender,
            "role" to user.role,
            "shopName" to user.shopName,
            "shopAddress" to user.shopAddress,
            "shopAddressLat" to user.shopAddressLat,
            "shopAddressLng" to user.shopAddressLng,
            "editRequestPending" to user.editRequestPending,
            "requestedName" to user.requestedName,
            "requestedShopName" to user.requestedShopName,
            "requestedShopAddress" to user.requestedShopAddress,
            "requestedShopAddressLat" to user.requestedShopAddressLat,
            "requestedShopAddressLng" to user.requestedShopAddressLng,
            "isSellerVerified" to user.isSellerVerified,
            "isSellerVerificationPending" to user.isSellerVerificationPending,
            "sellerMobile" to user.sellerMobile,
            "sellerAadhaar" to user.sellerAadhaar,
            "sellerShopPhoto" to user.sellerShopPhoto,
            "sellerOwnerPhoto" to user.sellerOwnerPhoto,
            "sellerBankAccount" to user.sellerBankAccount,
            "sellerPanCard" to user.sellerPanCard,
            "sellerGstNumber" to user.sellerGstNumber,
            "deliveryMobile" to user.deliveryMobile,
            "deliveryAadhaar" to user.deliveryAadhaar,
            "deliveryPhoto" to user.deliveryPhoto,
            "deliveryAddress" to user.deliveryAddress,
            "deliveryAddressLat" to user.deliveryAddressLat,
            "deliveryAddressLng" to user.deliveryAddressLng,
            "deliveryBankAccount" to user.deliveryBankAccount,
            "deliveryEmergencyContact" to user.deliveryEmergencyContact,
            "deliveryVehicleType" to user.deliveryVehicleType,
            "deliveryVehicleNumber" to user.deliveryVehicleNumber,
            "isDeliveryPartnerVerified" to user.isDeliveryPartnerVerified
        )
    }

    private fun mapToProduct(map: Map<String, Any?>): Product {
        return Product(
            id = (map["id"] as? Number)?.toInt() ?: 0,
            name = map["name"] as? String ?: "",
            price = (map["price"] as? Number)?.toDouble() ?: 0.0,
            originalPrice = (map["originalPrice"] as? Number)?.toDouble() ?: 0.0,
            rating = (map["rating"] as? Number)?.toFloat() ?: 0.0f,
            category = map["category"] as? String ?: "",
            imageUrlName = map["imageUrlName"] as? String ?: "",
            description = map["description"] as? String ?: "",
            isFeatured = map["isFeatured"] as? Boolean ?: false,
            sellerEmail = map["sellerEmail"] as? String ?: "",
            extraImages = map["extraImages"] as? String ?: "",
            stockQuantity = (map["stockQuantity"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 100
        )
    }

    private fun productToMap(p: Product): Map<String, Any> {
        return mapOf(
            "id" to p.id,
            "name" to p.name,
            "price" to p.price,
            "originalPrice" to p.originalPrice,
            "rating" to p.rating,
            "category" to p.category,
            "imageUrlName" to p.imageUrlName,
            "description" to p.description,
            "isFeatured" to p.isFeatured,
            "sellerEmail" to p.sellerEmail,
            "extraImages" to p.extraImages,
            "stockQuantity" to p.stockQuantity.coerceAtLeast(0)
        )
    }

    private fun mapToOrder(map: Map<String, Any?>): Order {
        return Order(
            orderId = map["orderId"] as? String ?: "",
            email = map["email"] as? String ?: "",
            orderDate = (map["orderDate"] as? Number)?.toLong() ?: 0L,
            totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            itemsAmount = (map["itemsAmount"] as? Number)?.toDouble() ?: 0.0,
            deliveryDistanceKm = (map["deliveryDistanceKm"] as? Number)?.toDouble() ?: 0.0,
            deliveryFixedCharge = (map["deliveryFixedCharge"] as? Number)?.toDouble() ?: 0.0,
            deliveryPerKmCharge = (map["deliveryPerKmCharge"] as? Number)?.toDouble() ?: 0.0,
            deliveryCharge = (map["deliveryCharge"] as? Number)?.toDouble() ?: 0.0,
            status = map["status"] as? String ?: "",
            itemsSummary = map["itemsSummary"] as? String ?: "",
            deliveryAddress = map["deliveryAddress"] as? String ?: "",
            deliveryAddressLat = (map["deliveryAddressLat"] as? Number)?.toDouble() ?: 0.0,
            deliveryAddressLng = (map["deliveryAddressLng"] as? Number)?.toDouble() ?: 0.0,
            couponApplied = map["couponApplied"] as? String ?: "",
            deliveryPartnerEmail = map["deliveryPartnerEmail"] as? String ?: "",
            deliveryStatus = map["deliveryStatus"] as? String ?: "",
            sellerConfirmed = map["sellerConfirmed"] as? Boolean ?: false,
            sellerRejectRequested = map["sellerRejectRequested"] as? Boolean ?: false,
            sellerChangeDeliveryBoyRequested = map["sellerChangeDeliveryBoyRequested"] as? Boolean ?: false,
            paymentMode = map["paymentMode"] as? String ?: "COD",
            paymentTransactionId = map["paymentTransactionId"] as? String ?: "",
            productQuantities = map["productQuantities"] as? String ?: ""
        )
    }

    private fun orderToMap(o: Order): Map<String, Any> {
        return mapOf(
            "orderId" to o.orderId,
            "email" to o.email,
            "orderDate" to o.orderDate,
            "totalAmount" to o.totalAmount,
            "itemsAmount" to o.itemsAmount,
            "deliveryDistanceKm" to o.deliveryDistanceKm,
            "deliveryFixedCharge" to o.deliveryFixedCharge,
            "deliveryPerKmCharge" to o.deliveryPerKmCharge,
            "deliveryCharge" to o.deliveryCharge,
            "status" to o.status,
            "itemsSummary" to o.itemsSummary,
            "deliveryAddress" to o.deliveryAddress,
            "deliveryAddressLat" to o.deliveryAddressLat,
            "deliveryAddressLng" to o.deliveryAddressLng,
            "couponApplied" to o.couponApplied,
            "deliveryPartnerEmail" to o.deliveryPartnerEmail,
            "deliveryStatus" to o.deliveryStatus,
            "sellerConfirmed" to o.sellerConfirmed,
            "sellerRejectRequested" to o.sellerRejectRequested,
            "sellerChangeDeliveryBoyRequested" to o.sellerChangeDeliveryBoyRequested,
            "paymentMode" to o.paymentMode,
            "paymentTransactionId" to o.paymentTransactionId,
            "productQuantities" to o.productQuantities
        )
    }

    private fun mapToCartItem(map: Map<String, Any?>): CartItem {
        return CartItem(
            id = (map["id"] as? Number)?.toInt() ?: 0,
            email = map["email"] as? String ?: "",
            productId = (map["productId"] as? Number)?.toInt() ?: 0,
            quantity = (map["quantity"] as? Number)?.toInt() ?: 1
        )
    }

    private fun cartItemToMap(c: CartItem): Map<String, Any> {
        return mapOf(
            "id" to c.id,
            "email" to c.email,
            "productId" to c.productId,
            "quantity" to c.quantity
        )
    }

    private fun mapToWishlistItem(map: Map<String, Any?>): WishlistItem {
        return WishlistItem(
            id = (map["id"] as? Number)?.toInt() ?: 0,
            email = map["email"] as? String ?: "",
            productId = (map["productId"] as? Number)?.toInt() ?: 0
        )
    }

    private fun wishlistItemToMap(w: WishlistItem): Map<String, Any> {
        return mapOf(
            "id" to w.id,
            "email" to w.email,
            "productId" to w.productId
        )
    }

    // Repository Operations directly pointing to Firestore

    suspend fun getUser(email: String): User? {
        // 1. Try to fetch remotely first under a 3-second timeout
        val remoteUser = try {
            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(email)
                    .get()
                    .awaitTask()
                if (doc.exists()) mapToUser(doc.data ?: emptyMap()) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (remoteUser != null) {
            // Cache locally
            try {
                appDao.insertUser(remoteUser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return remoteUser
        }

        // 2. Fall back to local database
        return try {
            appDao.getUser(email)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = FirebaseFirestore.getInstance()
            .collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { mapToUser(it) }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertUser(user: User) {
        // 1. Write locally first to ensure offline-first works instantly
        try {
            appDao.insertUser(user)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Write to Firebase Firestore in the background with a 3-second timeout
        try {
            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.email)
                    .set(userToMap(user))
                    .awaitTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateUser(user: User) = insertUser(user)

    fun getAllProducts(): Flow<List<Product>> = callbackFlow {
        val listener = FirebaseFirestore.getInstance()
            .collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { mapToProduct(it) }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertProducts(products: List<Product>) {
        try {
            appDao.insertProducts(products)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertProduct(product: Product) {
        try {
            appDao.insertProduct(product)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            FirebaseFirestore.getInstance()
                .collection("products")
                .document(product.id.toString())
                .set(productToMap(product))
                .awaitTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteProduct(productId: Int) {
        FirebaseFirestore.getInstance()
            .collection("products")
            .document(productId.toString())
            .delete()
            .awaitTask()
        appDao.deleteProduct(productId)
    }

    suspend fun getProductCount(): Int {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("products")
                .get()
                .awaitTask()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }

    fun getCartItems(email: String): Flow<List<CartItem>> = appDao.getCartItems(email)

    suspend fun syncCartFromFirestore(email: String) {
        try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                FirebaseFirestore.getInstance()
                    .collection("cart_items")
                    .whereEqualTo("email", email)
                    .get()
                    .awaitTask()
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { mapToCartItem(it) }
                }
                appDao.clearCart(email)
                items.forEach { appDao.insertCartItem(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertCartItem(item: CartItem) {
        try {
            appDao.insertCartItem(item)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                val db = FirebaseFirestore.getInstance()
                val existing = db.collection("cart_items")
                    .whereEqualTo("email", item.email)
                    .whereEqualTo("productId", item.productId)
                    .get()
                    .awaitTask()

                if (!existing.isEmpty) {
                    val docId = existing.documents.first().id
                    val currentQty = (existing.documents.first().data?.get("quantity") as? Number)?.toInt() ?: 1
                    db.collection("cart_items")
                        .document(docId)
                        .update("quantity", currentQty + item.quantity)
                        .awaitTask()
                } else {
                    val finalId = if (item.id == 0) (Math.random() * 10000000).toInt() else item.id
                    val finalItem = item.copy(id = finalId)
                    db.collection("cart_items")
                        .document(finalId.toString())
                        .set(cartItemToMap(finalItem))
                        .awaitTask()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateCartQuantity(itemId: Int, qty: Int) {
        try {
            appDao.updateCartQuantity(itemId, qty)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                FirebaseFirestore.getInstance()
                    .collection("cart_items")
                    .document(itemId.toString())
                    .update("quantity", qty)
                    .awaitTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteCartItem(itemId: Int) {
        try {
            appDao.deleteCartItem(itemId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                FirebaseFirestore.getInstance()
                    .collection("cart_items")
                    .document(itemId.toString())
                    .delete()
                    .awaitTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearCart(email: String) {
        try {
            appDao.clearCart(email)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            kotlinx.coroutines.withTimeoutOrNull(4000L) {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("cart_items")
                    .whereEqualTo("email", email)
                    .get()
                    .awaitTask()
                snapshot.documents.forEach { doc ->
                    db.collection("cart_items").document(doc.id).delete().awaitTask()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getWishlistItems(email: String): Flow<List<WishlistItem>> = appDao.getWishlistItems(email)

    suspend fun getWishlistItem(email: String, prodId: Int): WishlistItem? {
        return try {
            appDao.getWishlistItem(email, prodId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncWishlistFromFirestore(email: String) {
        try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                FirebaseFirestore.getInstance()
                    .collection("wishlist_items")
                    .whereEqualTo("email", email)
                    .get()
                    .awaitTask()
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { mapToWishlistItem(it) }
                }
                appDao.clearWishlist(email)
                items.forEach { appDao.insertWishlistItem(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertWishlistItem(item: WishlistItem) {
        try {
            appDao.insertWishlistItem(item)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                val db = FirebaseFirestore.getInstance()
                val existing = db.collection("wishlist_items")
                    .whereEqualTo("email", item.email)
                    .whereEqualTo("productId", item.productId)
                    .get()
                    .awaitTask()

                if (existing.isEmpty) {
                    val finalId = if (item.id == 0) (Math.random() * 10000000).toInt() else item.id
                    val finalItem = item.copy(id = finalId)
                    db.collection("wishlist_items")
                        .document(finalId.toString())
                        .set(wishlistItemToMap(finalItem))
                        .awaitTask()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteWishlistItem(email: String, prodId: Int) {
        try {
            appDao.deleteWishlistItem(email, prodId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("wishlist_items")
                    .whereEqualTo("email", email)
                    .whereEqualTo("productId", prodId)
                    .get()
                    .awaitTask()
                snapshot.documents.forEach { doc ->
                    db.collection("wishlist_items").document(doc.id).delete().awaitTask()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getOrders(email: String): Flow<List<Order>> = appDao.getOrders(email)

    fun getAllOrders(): Flow<List<Order>> = appDao.getAllOrders()

    suspend fun upsertOrderLocal(order: Order) {
        try {
            appDao.insertOrder(order)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertOrder(order: Order) {
        upsertOrderLocal(order)
        try {
            FirebaseFirestore.getInstance()
                .collection("orders")
                .document(order.orderId)
                .set(orderToMap(order))
                .awaitTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateOrder(order: Order) = insertOrder(order)

    suspend fun deleteOrder(orderId: String) {
        try {
            appDao.deleteOrder(orderId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            FirebaseFirestore.getInstance()
                .collection("orders")
                .document(orderId)
                .delete()
                .awaitTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Dynamic Config Repository Functions ---

    fun getAllBanners(): Flow<List<Banner>> = callbackFlow {
        val listener = FirebaseFirestore.getInstance()
            .collection("banners")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            Banner(
                                id = data["id"] as? String ?: doc.id,
                                label = data["label"] as? String ?: "",
                                title = data["title"] as? String ?: "",
                                description = data["description"] as? String ?: "",
                                targetCategory = data["targetCategory"] as? String ?: "",
                                gradientStart = data["gradientStart"] as? String ?: "#1B5E20",
                                gradientEnd = data["gradientEnd"] as? String ?: "#A5D6A7",
                                sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
                                isActive = data["isActive"] as? Boolean ?: true
                            )
                        }
                    }.filter { it.isActive }.sortedBy { it.sortOrder }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAllCoupons(): Flow<List<Coupon>> = callbackFlow {
        val listener = FirebaseFirestore.getInstance()
            .collection("coupons")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            Coupon(
                                code = data["code"] as? String ?: doc.id,
                                discountPercent = (data["discountPercent"] as? Number)?.toInt() ?: 0,
                                description = data["description"] as? String ?: "",
                                isActive = data["isActive"] as? Boolean ?: true,
                                minOrderAmount = (data["minOrderAmount"] as? Number)?.toDouble() ?: 0.0,
                                maxDiscount = (data["maxDiscount"] as? Number)?.toDouble() ?: 999999.0
                            )
                        }
                    }.filter { it.isActive }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAllShortcuts(): Flow<List<Shortcut>> = callbackFlow {
        val listener = FirebaseFirestore.getInstance()
            .collection("shortcuts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            Shortcut(
                                title = data["title"] as? String ?: "",
                                iconName = data["iconName"] as? String ?: "flash_on",
                                colorHex = data["colorHex"] as? String ?: "#FF5722",
                                action = data["action"] as? String ?: "",
                                toastMessage = data["toastMessage"] as? String ?: "",
                                sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0
                            )
                        }
                    }.sortedBy { it.sortOrder }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    fun getAppConfig(): Flow<AppConfig> = callbackFlow {
        val listener = FirebaseFirestore.getInstance()
            .collection("app_config")
            .document("main")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap()
                    val config = AppConfig(
                        categories = (data["categories"] as? List<String>) ?: listOf("All", "Electronics", "Fresh Products", "Fashion", "Home & Kitchen"),
                        languages = (data["languages"] as? List<String>) ?: listOf("English", "Español", "Français", "Deutsch", "Hindi", "Bengali"),
                        flashSaleEndTime = (data["flashSaleEndTime"] as? Number)?.toLong() ?: 0L,
                        serviceCities = (data["serviceCities"] as? List<String>) ?: emptyList(),
                        servicePincodes = (data["servicePincodes"] as? List<String>) ?: emptyList(),
                        payoutDelayHours = (data["payoutDelayHours"] as? Number)?.toInt() ?: 24
                    )
                    trySend(config)
                } else {
                    trySend(AppConfig())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun seedBanners() {
        try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("banners").get().awaitTask()
            if (snapshot.isEmpty) {
                val defaults = listOf(
                    mapOf(
                        "id" to "banner_1", "label" to "LAUNCH SPECIAL",
                        "title" to "50% OFF ON SMART ASSISTANTS",
                        "description" to "Futuristic wellness tech with AMOLED screens & rapid charge.",
                        "targetCategory" to "Electronics",
                        "gradientStart" to "#1B5E20", "gradientEnd" to "#A5D6A7",
                        "sortOrder" to 0, "isActive" to true
                    ),
                    mapOf(
                        "id" to "banner_2", "label" to "WEEKEND HARVEST",
                        "title" to "30% OFF ALL ORGANIC PRODUCTS",
                        "description" to "Crispy, hand-picked pesticide-free seasonal green food.",
                        "targetCategory" to "Fresh Products",
                        "gradientStart" to "#2E7D32", "gradientEnd" to "#81C784",
                        "sortOrder" to 1, "isActive" to true
                    ),
                    mapOf(
                        "id" to "banner_3", "label" to "STYLE INSPIRATION",
                        "title" to "BUY 1 GET 1 ON EMERALD SNEAKERS",
                        "description" to "Elevate your step with lightweight, vulcanized rubber running soles.",
                        "targetCategory" to "Fashion",
                        "gradientStart" to "#8E24AA", "gradientEnd" to "#CE93D8",
                        "sortOrder" to 2, "isActive" to true
                    )
                )
                defaults.forEach { banner ->
                    db.collection("banners").document(banner["id"] as String).set(banner).awaitTask()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun seedCoupons() {
        try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("coupons").get().awaitTask()
            if (snapshot.isEmpty) {
                val defaults = listOf(
                    mapOf(
                        "code" to "BAZAAR20", "discountPercent" to 20,
                        "description" to "20% OFF on all products!",
                        "isActive" to true, "minOrderAmount" to 0, "maxDiscount" to 999999
                    )
                )
                defaults.forEach { coupon ->
                    db.collection("coupons").document(coupon["code"] as String).set(coupon).awaitTask()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun seedShortcuts() {
        try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("shortcuts").get().awaitTask()
            if (snapshot.isEmpty) {
                val defaults = listOf(
                    mapOf("title" to "Flash Deals", "iconName" to "flash_on", "colorHex" to "#FF5722", "action" to "category:All", "toastMessage" to "Showing lightning deals & offers!", "sortOrder" to 0),
                    mapOf("title" to "Smart Tech", "iconName" to "devices", "colorHex" to "#2196F3", "action" to "category:Electronics", "toastMessage" to "", "sortOrder" to 1),
                    mapOf("title" to "Fresh Produce", "iconName" to "local_florist", "colorHex" to "#4CAF50", "action" to "category:Fresh Products", "toastMessage" to "", "sortOrder" to 2),
                    mapOf("title" to "Wardrobe", "iconName" to "shopping_bag", "colorHex" to "#E91E63", "action" to "category:Fashion", "toastMessage" to "", "sortOrder" to 3),
                    mapOf("title" to "VIP Coupons", "iconName" to "confirmation_number", "colorHex" to "#9C27B0", "action" to "show_coupon", "toastMessage" to "", "sortOrder" to 4)
                )
                defaults.forEachIndexed { index, shortcut ->
                    db.collection("shortcuts").document("shortcut_$index").set(shortcut).awaitTask()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun seedAppConfig() {
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("app_config").document("main").get().awaitTask()
            if (!doc.exists()) {
                val config = mapOf(
                    "categories" to listOf("All", "Electronics", "Fresh Products", "Fashion", "Home & Kitchen"),
                    "languages" to listOf("English", "Español", "Français", "Deutsch", "Hindi", "Bengali"),
                    "flashSaleEndTime" to (System.currentTimeMillis() + 10800000L), // 3 hours from now
                    "serviceCities" to emptyList<String>(),
                    "servicePincodes" to emptyList<String>(),
                    "payoutDelayHours" to 24
                )
                db.collection("app_config").document("main").set(config).awaitTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
