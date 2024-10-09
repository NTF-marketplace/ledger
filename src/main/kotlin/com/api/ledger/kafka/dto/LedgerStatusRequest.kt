package com.api.ledger.kafka.dto

import com.api.ledger.enums.OrderStatusType
import java.math.BigDecimal

data class LedgerStatusRequest(
    val orderId: Long,
    val status: OrderStatusType,
    val ledgerPrice: BigDecimal?,
)
