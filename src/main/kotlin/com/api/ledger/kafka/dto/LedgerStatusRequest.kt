package com.api.ledger.kafka.dto

import com.api.ledger.enums.OrderStatusType

data class LedgerStatusRequest(
    val orderId: Long,
    val status: OrderStatusType,
)
