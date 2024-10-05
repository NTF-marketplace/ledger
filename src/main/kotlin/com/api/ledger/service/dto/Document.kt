package com.api.ledger.service.dto

import com.api.ledger.enums.ChainType
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Document (
    val collectionName: String,
    val chainType: ChainType,
    val ledgerPrice: Double?,
    val lastPrice: Double?
)
