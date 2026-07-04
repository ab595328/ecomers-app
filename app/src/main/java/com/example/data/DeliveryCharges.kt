package com.example.data

import kotlin.math.*

data class DeliveryChargeBreakdown(
    val distanceKm: Double,
    val fixedCharge: Double,
    val perKmCharge: Double,
    val totalCharge: Double
)

fun estimateDeliveryDistanceKm(orderId: String): Double {
    val code = orderId.hashCode()
    val absCode = if (code == Int.MIN_VALUE) 0 else kotlin.math.abs(code)
    return 1.0 + (absCode % 71) / 10.0
}

fun calculateDistanceKm(
    fromLatitude: Double,
    fromLongitude: Double,
    toLatitude: Double,
    toLongitude: Double
): Double {
    if (fromLatitude == 0.0 || fromLongitude == 0.0 || toLatitude == 0.0 || toLongitude == 0.0) return 0.0
    val earthRadiusKm = 6371.0088
    val latDelta = Math.toRadians(toLatitude - fromLatitude)
    val lngDelta = Math.toRadians(toLongitude - fromLongitude)
    val a = sin(latDelta / 2).pow(2) +
        cos(Math.toRadians(fromLatitude)) * cos(Math.toRadians(toLatitude)) *
        sin(lngDelta / 2).pow(2)
    return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun isAddressInServiceArea(address: String, config: AppConfig): Boolean {
    val normalized = address.trim().lowercase()
    if (normalized.isBlank()) return false
    val cities = config.serviceCities.map { it.trim().lowercase() }.filter { it.isNotBlank() }
    val pincodes = config.servicePincodes.map { it.filter(Char::isDigit) }.filter { it.isNotBlank() }
    if (cities.isEmpty() && pincodes.isEmpty()) return true
    val cityMatch = cities.isEmpty() || cities.any { city ->
        Regex("""(^|[^a-z0-9])${Regex.escape(city)}([^a-z0-9]|$)""").containsMatchIn(normalized)
    }
    val addressPincodes = Regex("""\b\d{5,6}\b""").findAll(normalized).map { it.value }.toSet()
    val pincodeMatch = pincodes.isEmpty() || addressPincodes.any { it in pincodes }
    return cityMatch && pincodeMatch
}

fun isPincodeInServiceArea(pincode: String, config: AppConfig): Boolean {
    val normalized = pincode.filter(Char::isDigit)
    if (normalized.isBlank()) return false
    val pincodes = config.servicePincodes.map { it.filter(Char::isDigit) }.filter { it.isNotBlank() }
    return pincodes.isEmpty() || normalized in pincodes
}

fun calculateDeliveryCharge(orderAmount: Double, distanceKm: Double): DeliveryChargeBreakdown {
    val isBelow500 = orderAmount < 500.0
    val fixedCharge = if (isBelow500) 80.0 else 150.0
    val perKmCharge = if (isBelow500) 15.0 else 20.0
    val totalCharge = fixedCharge + (distanceKm * perKmCharge)
    return DeliveryChargeBreakdown(distanceKm, fixedCharge, perKmCharge, totalCharge)
}

fun estimateItemAmountFromOrderTotal(orderTotal: Double, distanceKm: Double): Double {
    val below500Charge = 80.0 + (distanceKm * 15.0)
    val below500Base = orderTotal - below500Charge
    if (below500Base >= 0.0 && below500Base < 500.0) return below500Base

    val above500Charge = 150.0 + (distanceKm * 20.0)
    return (orderTotal - above500Charge).coerceAtLeast(0.0)
}
