package com.api.ledger.ledger.kafka.dto

import com.api.ledger.ledger.enums.ChainType
import java.math.BigDecimal

data class LedgerRequest(
    val nftId: Long,
    val address: String,
    val price: BigDecimal,
    val chainType: ChainType,
    val orderAddress: String
)

