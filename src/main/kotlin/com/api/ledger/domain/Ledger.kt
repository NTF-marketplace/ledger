package com.api.ledger.domain

import com.api.ledger.enums.ChainType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("ledger")
data class Ledger(
    @Id val id: Long? = null,
    val nftId: Long,
    val saleAddress: String,
    val orderAddress: String,
    val createdAt: Long? = System.currentTimeMillis(),
    val ledgerPrice: BigDecimal,
    val chainType: ChainType,
    val orderId: Long
)
