package com.api.ledger.service.external

import com.api.ledger.kafka.dto.LedgerRequest
import com.api.ledger.properties.WalletApiProperties
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class WalletApiService(
    private val walletApiProperties: WalletApiProperties,
) {

    private val webClient = WebClient.builder()
        .baseUrl(walletApiProperties.uri)
        .build()

    fun transfer(request: LedgerRequest): Mono<ResponseEntity<String>> {
        return webClient.post()
            .uri { uriBuilder ->
                uriBuilder.path("/v1/transfer").build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .toEntity(String::class.java)
    }

}