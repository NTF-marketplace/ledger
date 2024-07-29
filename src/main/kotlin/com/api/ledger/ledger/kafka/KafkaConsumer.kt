package com.api.ledger.ledger.kafka

import com.api.ledger.ledger.enums.ChainType
import com.api.ledger.ledger.kafka.dto.LedgerRequest
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class KafkaConsumer {

    private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)

    @KafkaListener(
        topics = ["ledger-topic"],
        groupId = "ledger-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeLedgerTopic(
        @Payload payload: LinkedHashMap<String, Any>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long
    ) {
        try {
            val ledgerRequest = convertToLedgerRequest(payload)
            logger.info("Received LedgerRequest: $ledgerRequest from topic: $topic, partition: $partition, offset: $offset, timestamp: $timestamp")
            TODO()
        } catch (ex: Exception) {
            logger.error("Error converting payload to LedgerRequest", ex)
        }
    }

    private fun convertToLedgerRequest(payload: LinkedHashMap<String, Any>): LedgerRequest {
        val nftId = (payload["nftId"] as Number).toLong()
        val address = payload["address"] as String
        val price = BigDecimal.valueOf(payload["price"] as Double)
        val chainType = ChainType.valueOf(payload["chainType"] as String)
        val orderAddress = payload["orderAddress"] as String

        return LedgerRequest(nftId, address, price, chainType, orderAddress)
    }
}