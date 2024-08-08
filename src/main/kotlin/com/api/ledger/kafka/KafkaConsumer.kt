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
import reactor.core.publisher.Mono

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

        processLedgerRequest(ledgerRequest)
            .doOnSuccess {
                acknowledgment.acknowledge() // 메시지 수동 확인
            }.onErrorResume {
                ledgerService.orderFailureAndMoveToNext(acknowledgment) // 에러 발생 시 처리
            }.subscribe()
    }

    private fun convertToLedgerRequest(payload: LinkedHashMap<String, Any>): LedgerRequest =
        objectMapper.convertValue(payload, LedgerRequest::class.java)

    fun processLedgerRequest(ledgerRequest: LedgerRequest): Mono<Void> = ledgerService.ledger(ledgerRequest)
}
