package com.api.ledger.service.dto

import com.api.ledger.enums.ChainType
import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDateTime

data class ElasticSearchUpdateData(
    val id: Long,
    val chainType: ChainType,
    val collectionName: String,
    val collectionLogo: String?,
    val lastPrice: BigDecimal?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") val ledgerTime: LocalDateTime?,
    val ledgerPrice: BigDecimal?,
)
