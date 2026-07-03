package com.example.data

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
