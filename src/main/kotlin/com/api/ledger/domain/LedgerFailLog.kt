package com.api.ledger.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("ledger_fail_log")
data class LedgerFailLog(
    @Id val id: Long? = null,
    val orderId: Long,
    val createdAt: Long? = System.currentTimeMillis(),
    val message: String?,
)
