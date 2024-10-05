package com.api.ledger.service.external

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.BulkResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ElasticsearchService(
    private val client: ElasticsearchClient,
    private val redisService: RedisService,
) {

    fun bulkUpdate(nftId: Long, lastPrice: BigDecimal): Mono<BulkResponse> {
        val roundedPrice = lastPrice.setScale(2, RoundingMode.HALF_UP)
        return redisService.transformData(nftId)
            .map { data ->
                data.copy(lastPrice = roundedPrice)
            }
            .flatMap { updatedData ->
                Mono.fromCallable {
                    val bulkRequest = BulkRequest.Builder()
                    bulkRequest.operations { op ->
                        op.index { idx ->
                            idx.index("nfts")
                                .id(updatedData.id.toString())
                                .document(updatedData)
                        }
                    }
                    client.bulk(bulkRequest.build())
                }
            }
    }
}