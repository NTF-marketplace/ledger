package com.api.ledger.service.dto

import com.api.ledger.enums.AGGREGATIONS_TYPE
import com.api.ledger.enums.ChainType

data class CollectionRanking(
        val collectionName: String,
        val chainType: ChainType,
        val totalPrice: Double? = 0.0,
        val lowPrice: Double? = 0.0,
        val highPrice: Double?= 0.0,
        val timeRange: AGGREGATIONS_TYPE
    )
