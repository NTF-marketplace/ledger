package com.api.ledger.enums

enum class TokenType { MATIC, ETH, BTC }

enum class ChainType {
    ETHEREUM_MAINNET,
    LINEA_MAINNET,
    LINEA_SEPOLIA,
    POLYGON_MAINNET,
    ETHEREUM_HOLESKY,
    ETHEREUM_SEPOLIA,
    POLYGON_AMOY,
}

enum class OrderStatusType { PENDING,FAILED,CANCELD,COMPLETED }