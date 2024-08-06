package com.api.ledger.service.dto

import com.api.ledger.enums.ChainType
import java.math.BigDecimal

data class TransferRequest(
    val fromAddress: String,
    val toAddress: String,
    val chainType: ChainType,
    val amount: BigDecimal,
    val nftId: Long,
)
