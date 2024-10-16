package com.api.ledger.service.dto

import java.math.BigDecimal

data class LedgerResponse(
    val nftId: Long,
    val ledgerPrice: BigDecimal,
    val ledgerTime: Long,
)
