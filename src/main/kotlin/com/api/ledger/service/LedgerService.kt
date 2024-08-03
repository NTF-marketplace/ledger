package com.api.ledger.service

import com.api.ledger.domain.Ledger
import com.api.ledger.domain.repository.LedgerRepository
import com.api.ledger.kafka.dto.LedgerRequest
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
) {

    // 돋시성 이슈는 어떻게 해결핲것인지 좀 생각해보자
    // TODO("각각 다른 커스텀에러만들기")
    fun ledger(request: LedgerRequest): Mono<Void> {
        return walletApiService.transfer(request)
            .flatMap { responseEntity ->
                when (responseEntity.statusCode) {
                    HttpStatus.OK -> handleSuccess(request)
                    HttpStatus.BAD_REQUEST -> {
                        sendFailedNotification(request, "Bad Request: ${responseEntity.body}")
                            .then(Mono.error(BadRequestException("Bad Request: ${responseEntity.body}")))
                    }
                    HttpStatus.INTERNAL_SERVER_ERROR -> {
                        sendFailedNotification(request, "Internal Server Error: ${responseEntity.body}")
                            .then(Mono.error(InternalServerException("Internal Server Error: ${responseEntity.body}")))
                    }
                    else -> {
                        sendFailedNotification(request, "Unexpected status code: ${responseEntity.statusCode}")
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
                sendFailedNotification(request, error.message)
                    .then(Mono.empty())
            }
    }

    fun orderFailureAndMoveToNext(acknowledgment: Acknowledgment): Mono<Void> {
        acknowledgment.acknowledge()
        return Mono.empty()
    }

    private fun handleSuccess(request: LedgerRequest): Mono<Void> {
        return saveLedgerLog(request)
            .then()
    }

    //TODO("체결로그 저장 후 상태값 바꾸기")
    private fun saveLedgerLog(request: LedgerRequest): Mono<Void> {

        return Mono.empty()
    }


    // TODO("실패로그 저장")
    private fun sendFailedNotification(request: LedgerRequest, message: String?): Mono<Void> {
        // 실패로그 저장
        return Mono.empty()
    }

}

class BadRequestException(message: String) : RuntimeException(message)
class InternalServerException(message: String) : RuntimeException(message)
class UnexpectedStatusCodeException(message: String) : RuntimeException(message)