package com.api.ledger.ledger.domain

import com.api.ledger.ledger.enums.TokenType
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
    val tokenType: TokenType,
)