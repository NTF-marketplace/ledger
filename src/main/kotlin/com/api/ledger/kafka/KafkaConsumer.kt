package com.api.ledger.kafka

import com.api.ledger.kafka.dto.LedgerRequest
import com.api.ledger.service.LedgerService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class KafkaConsumer(
    private val ledgerService: LedgerService,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)

    @KafkaListener(
        topics = ["ledger-topic"],
        groupId = "ledger-group",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun consumeLedgerTopic(
        @Payload payload: LinkedHashMap<String, Any>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long,
        acknowledgment: Acknowledgment,
    ) {
        val ledgerRequest = convertToLedgerRequest(payload)
        logger.info(
            "Received LedgerRequest: $ledgerRequest from topic: $topic, partition: $partition, offset: $offset, timestamp: $timestamp",
        )

        ledgerService.ledger(ledgerRequest)
            .doOnSuccess {
                acknowledgment.acknowledge()
            }
            .doOnError {
                acknowledgment.acknowledge()
            }
            .subscribe()
    }

    private fun convertToLedgerRequest(payload: LinkedHashMap<String, Any>): LedgerRequest {
        return objectMapper.convertValue(payload, LedgerRequest::class.java)
    }
}