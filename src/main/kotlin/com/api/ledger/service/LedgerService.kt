package com.api.ledger.service

import com.api.ledger.domain.Ledger
import com.api.ledger.domain.LedgerFailLog
import com.api.ledger.domain.repository.LedgerFailLogRepository
import com.api.ledger.domain.repository.LedgerRepository
import com.api.ledger.enums.OrderStatusType
import com.api.ledger.exception.BadRequestException
import com.api.ledger.exception.InternalServerException
import com.api.ledger.exception.UnexpectedStatusCodeException
import com.api.ledger.kafka.KafkaProducer
import com.api.ledger.kafka.dto.LedgerRequest
import com.api.ledger.kafka.dto.LedgerStatusRequest
import com.api.ledger.service.dto.LedgerResponse
import com.api.ledger.service.dto.TransferRequest
import com.api.ledger.service.external.WalletApiService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.math.BigDecimal

@Service
class LedgerService(
    private val ledgerRepository: LedgerRepository,
    private val walletApiService: WalletApiService,
    private val ledgerFailLogRepository: LedgerFailLogRepository,
    private val kafkaProducer: KafkaProducer,
) {

    private val logger = LoggerFactory.getLogger(LedgerService::class.java)

    fun ledger(request: LedgerRequest): Mono<Void> {
        return ledgerRepository.existsByOrderId(request.orderId)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(IllegalStateException("already ledger"))
                } else {
                    walletApiService.transfer(
                        TransferRequest(
                            fromAddress = request.orderAddress,
                            toAddress = request.address,
                            chainType = request.chainType,
                            amount = request.price,
                            nftId = request.nftId
                        )
                    )
                        .flatMap { responseEntity ->
                            handleTransferResponse(responseEntity, request)
                        }
                        .retryWhen(
                            Retry.max(3)
                                .filter { it is InternalServerException }
                                .doBeforeRetry {
                                    logger.info("재시도 중: ${it.failure().message}")
                                }
                        )
                }
            }
            .onErrorResume { error ->
                logger.error("최종 에러 발생", error)
                saveFailLog(request, error.message)
                    .then(Mono.empty())
            }
    }

    private fun handleTransferResponse(responseEntity: ResponseEntity<*>, request: LedgerRequest): Mono<Void> {
        return when (responseEntity.statusCode) {
            HttpStatus.OK -> saveLedgerLog(request)
                .doOnSuccess { logger.info("Ledger 로그 저장 성공") }
                .doOnError { logger.error("Ledger 로그 저장 실패", it) }

            HttpStatus.BAD_REQUEST -> {
                logger.warn("Bad Request 발생")
                saveFailLog(request, "Bad Request: ${responseEntity.body}")
                    .then(Mono.error(BadRequestException("Bad Request: ${responseEntity.body}")))
            }

            HttpStatus.INTERNAL_SERVER_ERROR -> {
                logger.error("Internal Server Error 발생")
                saveFailLog(request, "Internal Server Error: ${responseEntity.body}")
                    .then(Mono.error(InternalServerException("Internal Server Error: ${responseEntity.body}")))
            }

            else -> {
                logger.error("예상치 못한 상태 코드: ${responseEntity.statusCode}")
                saveFailLog(request, "Unexpected status code: ${responseEntity.statusCode}")
                    .then(Mono.error(UnexpectedStatusCodeException("Unexpected status code: ${responseEntity.statusCode}")))
            }
        }
    }

    private fun saveLedgerLog(request: LedgerRequest): Mono<Void> {
        logger.info("saveLedgerLog 함수 시작: $request")
        return ledgerRepository.save(
            Ledger(
                nftId = request.nftId,
                saleAddress = request.address,
                orderAddress = request.orderAddress,
                createdAt = System.currentTimeMillis(),
                ledgerPrice = request.price,
                chainType = request.chainType,
                orderId = request.orderId
            )
        )
            .flatMap {
                logger.info("Ledger 저장 완료, 상태 전송 시작")
                sendLedgerStatus(request.orderId, OrderStatusType.COMPLETED,it.ledgerPrice)
                    .then(
                        if (OrderStatusType.COMPLETED == OrderStatusType.COMPLETED) {
                            sendLedger(request.nftId, it.ledgerPrice, it.createdAt ?: System.currentTimeMillis())
                        } else {
                            Mono.empty()
                        }
                    )
            }
            .doOnSuccess { logger.info("Elasticsearch 업데이트 완료") }
            .doOnError { logger.error("Ledger 저장, 상태 전송 또는 Elasticsearch 업데이트 실패", it) }
            .then()
    }

    //여기로 오게끔
    private fun saveFailLog(request: LedgerRequest, message: String?): Mono<Void> {
        logger.info("saveFailLog 함수 시작: $request, 메시지: $message")
        return ledgerFailLogRepository.save(
            LedgerFailLog(
                orderId = request.orderId,
                message = message
            )
        )
            .flatMap {
                logger.info("실패 로그 저장 완료, 상태 전송 시작")
                sendLedgerStatus(request.orderId, OrderStatusType.FAILED,null)
            }
            .doOnSuccess { logger.info("실패 로그 저장 및 상태 전송 완료") }
            .doOnError { logger.error("실패 로그 저장 또는 상태 전송 실패", it) }
    }

    private fun sendLedger(nftId: Long,ledgerPrice: BigDecimal,ledgerTime:Long):Mono<Void> {
        return kafkaProducer.sendLedgerResponse(LedgerResponse(nftId,ledgerPrice,ledgerTime))
            .doOnSuccess { logger.info("Ledger 상태 전송 완료") }
            .doOnError { logger.error("Ledger 상태 전송 실패", it) }

    }

    private fun sendLedgerStatus(orderId: Long, status: OrderStatusType,ledgerPrice: BigDecimal?): Mono<Void> {
        logger.info("sendLedgerStatus 함수 시작: orderId=$orderId, status=$status")
        return kafkaProducer.sendLedgerStatus(
            LedgerStatusRequest(orderId, status, ledgerPrice)
        )
            .doOnSuccess { logger.info("Ledger 상태 전송 완료") }
            .doOnError { logger.error("Ledger 상태 전송 실패", it) }
    }
}