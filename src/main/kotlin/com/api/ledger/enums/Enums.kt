package com.api.ledger.enums

import java.io.Serializable

enum class TokenType { MATIC, ETH, BTC }

enum class ContractType{
    ERC1155,
    ERC721
}
enum class ChainType {
    ETHEREUM_MAINNET,
    LINEA_MAINNET,
    LINEA_SEPOLIA,
    POLYGON_MAINNET,
    ETHEREUM_HOLESKY,
    ETHEREUM_SEPOLIA,
    POLYGON_AMOY,

}

enum class OrderStatusType: Serializable { PENDING,FAILED,CANCELD,COMPLETED }

enum class AGGREGATIONS_TYPE { ONE_HOURS, SIX_HOURS, ONE_DAY, SEVEN_DAY }