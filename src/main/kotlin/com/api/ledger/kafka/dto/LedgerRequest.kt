package com.api.ledger.kafka.dto

import com.api.ledger.enums.ChainType
import java.math.BigDecimal

data class LedgerRequest(
    val orderId: Long,
    val nftId: Long,
    val address: String,
    val price: BigDecimal,
    val chainType: ChainType,
    val orderAddress: String,
)
