package com.api.ledger.kafka

import com.api.ledger.kafka.dto.LedgerStatusRequest
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class KafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String,Any>,
) {
    private val logger = LoggerFactory.getLogger(KafkaProducer::class.java)
    fun sendLedgerStatus(request: LedgerStatusRequest): Mono<Void> {
        return Mono.create { sink ->
            val future = kafkaTemplate.send("ledgerStatus-topic", request)
            future.whenComplete { result, ex ->
                if(ex== null) {
                    logger.info("Sent ledger request successfully: ${result?.recordMetadata}")
                    sink.success()
                } else {
                    logger.error("Faild to send", ex)
                    sink.error(ex)
                }
            }
        }
    }
}