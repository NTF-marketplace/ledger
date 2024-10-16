package com.api.ledger.kafka

import com.api.ledger.kafka.dto.LedgerStatusRequest
import com.api.ledger.service.dto.LedgerResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord

@Service
class KafkaProducer(
    private val kafkaSender: KafkaSender<String, Any>,
) {
    private val logger = LoggerFactory.getLogger(KafkaProducer::class.java)

    fun sendLedgerStatus(request: LedgerStatusRequest): Mono<Void> {
        val record = SenderRecord.create(
            "ledgerStatus-topic",
            null,
            null,
            request.orderId.toString(),
            request as Any,
            null
        )

        return kafkaSender.send(Mono.just(record))
            .next()
            .doOnSuccess {
                logger.info("request successfully: ${it.recordMetadata()}")
            }
            .doOnError {
                logger.error("Failed to request", it)
            }
            .then()
    }

    fun sendLedgerResponse(response: LedgerResponse): Mono<Void> {
        val record = SenderRecord.create(
            "ledgerResponse-topic",
            null,
            null,
            response.nftId.toString(),
            response as Any,
            null
        )

        return kafkaSender.send(Mono.just(record))
            .next()
            .doOnSuccess {
                logger.info("request successfully: ${it.recordMetadata()}")
            }
            .doOnError {
                logger.error("Failed to request", it)
            }
            .then()
    }

}