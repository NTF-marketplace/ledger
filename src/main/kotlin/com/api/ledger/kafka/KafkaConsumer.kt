package com.api.ledger.kafka

import com.api.ledger.enums.ChainType
import com.api.ledger.kafka.dto.LedgerRequest
import com.api.ledger.service.LedgerService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Service
class KafkaConsumer(private val ledgerService: LedgerService) {

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
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long,
        acknowledgment: Acknowledgment
    ) {
        val ledgerRequest = convertToLedgerRequest(payload)
        logger.info("Received LedgerRequest: $ledgerRequest from topic: $topic, partition: $partition, offset: $offset, timestamp: $timestamp")

        processLedgerRequest(ledgerRequest)
            .doOnSuccess {
                acknowledgment.acknowledge()
            }
            .onErrorResume {
                ledgerService.orderFailureAndMoveToNext(acknowledgment)
            }
            .subscribe()
    }

    private fun convertToLedgerRequest(payload: LinkedHashMap<String, Any>): LedgerRequest {
        val nftId = (payload["nftId"] as Number).toLong()
        val address = payload["address"] as String
        val price = BigDecimal.valueOf(payload["price"] as Double)
        val chainType = ChainType.valueOf(payload["chainType"] as String)
        val orderAddress = payload["orderAddress"] as String

        return LedgerRequest(nftId, address, price, chainType, orderAddress)
    }

    fun processLedgerRequest(ledgerRequest: LedgerRequest): Mono<Void> {
        return ledgerService.ledger(ledgerRequest)
    }
}