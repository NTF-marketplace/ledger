package com.api.ledger.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wallet")
data class WalletApiProperties(
    val uri: String,
)
