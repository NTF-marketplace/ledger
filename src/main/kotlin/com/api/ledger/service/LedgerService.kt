package com.api.ledger.service

import com.api.ledger.domain.Ledger
import com.api.ledger.domain.LedgerFailLog
import com.api.ledger.domain.repository.LedgerFailLogRepository
import com.api.ledger.domain.repository.LedgerRepository
import com.api.ledger.enums.OrderStatusType
import com.api.ledger.kafka.KafkaProducer
import com.api.ledger.kafka.dto.LedgerRequest
import com.api.ledger.kafka.dto.LedgerStatusRequest
import com.api.ledger.service.dto.TransferRequest
import com.api.ledger.service.external.WalletApiService
import org.springframework.http.HttpStatus
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import kotlin.time.Duration

@Service
class LedgerService(
    private val ledgerRepository: LedgerRepository,
    private val walletApiService: WalletApiService,
    private val ledgerFailLogRepository: LedgerFailLogRepository,
    private val kafkaProducer: KafkaProducer,
) {

    // 돋시성 이슈는 어떻게 해결핲것인지 좀 생각해보자
    // transfer -> ledger -> 상태변경
    // 그럼 candel 이 있을 이유가 굳이 있긴한다?
    // TODO("각각 다른 커스텀에러만들기")
    fun ledger(request: LedgerRequest): Mono<Void> {
        return walletApiService.transfer(
            TransferRequest(
                fromAddress = request.orderAddress,
                toAddress = request.address,
                chainType = request.chainType,
                amount = request.price,
                nftId = request.nftId
            )
        )
            .flatMap { responseEntity ->
                when (responseEntity.statusCode) {
                    HttpStatus.OK -> saveLedgerLog(request)
                    HttpStatus.BAD_REQUEST -> {
                        saveFailLog(request, "Bad Request: ${responseEntity.body}")
                            .then(Mono.error(BadRequestException("Bad Request: ${responseEntity.body}")))
                    }
                    HttpStatus.INTERNAL_SERVER_ERROR -> {
                        saveFailLog(request, "Internal Server Error: ${responseEntity.body}")
                            .then(Mono.error(InternalServerException("Internal Server Error: ${responseEntity.body}")))
                    }
                    else -> {
                        saveFailLog(request, "Unexpected status code: ${responseEntity.statusCode}")
                            .then(Mono.error(UnexpectedStatusCodeException("Unexpected status code: ${responseEntity.statusCode}")))
                    }
                }
            }
            .retryWhen(
                Retry.max(3)
                    .filter { it is InternalServerException }
                    .doBeforeRetry {
                        println("Retrying due to error: ${it.failure().message}")
                    }
            )
            .onErrorResume { error ->
                saveFailLog(request, error.message)
                    .then(Mono.empty())
            }
    }

    fun orderFailureAndMoveToNext(acknowledgment: Acknowledgment): Mono<Void> {
        acknowledgment.acknowledge()
        return Mono.empty()
    }


    private fun saveLedgerLog(request: LedgerRequest): Mono<Void> {
        return ledgerRepository.save(
            Ledger(
                nftId = request.nftId,
                saleAddress = request.address,
                orderAddress = request.orderAddress,
                createdAt = System.currentTimeMillis(),
                ledgerPrice = request.price,
                chainType = request.chainType
            )
        ).flatMap {
            sendLedgerStatus(request.orderId, OrderStatusType.COMPLETED)
        }
    }

    private fun saveFailLog(request: LedgerRequest, message: String?): Mono<Void> {
        return ledgerFailLogRepository.save(
            LedgerFailLog(
                orderId = request.orderId,
                message = message
            )
        ).flatMap {
            sendLedgerStatus(request.orderId, OrderStatusType.FAILED)
        }
    }

    private fun sendLedgerStatus(orderId: Long, status: OrderStatusType): Mono<Void> {
        return kafkaProducer.sendLedgerStatus(
            LedgerStatusRequest(orderId, status)
        )
    }

}

class BadRequestException(message: String) : RuntimeException(message)
class InternalServerException(message: String) : RuntimeException(message)
class UnexpectedStatusCodeException(message: String) : RuntimeException(message)