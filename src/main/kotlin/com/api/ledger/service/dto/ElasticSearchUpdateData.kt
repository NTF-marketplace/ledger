package com.api.ledger.service.dto

import com.api.ledger.enums.ChainType
import java.math.BigDecimal

data class ElasticSearchUpdateData(
    val id: Long,
    val chainType: ChainType,
    val collectionName: String,
    val lastPrice: BigDecimal?,
    val collectionLogo: String?,
)
