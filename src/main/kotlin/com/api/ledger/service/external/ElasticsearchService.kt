package com.api.ledger.service.external

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.BulkResponse
import com.api.ledger.enums.AGGREGATIONS_TYPE
import com.api.ledger.service.dto.CollectionRanking
import com.api.ledger.service.dto.Document
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class ElasticsearchService(
    private val client: ElasticsearchClient,
    private val redisService: RedisService,
) {

    fun saveRankings(rankingsByTimeRange: MutableMap<AGGREGATIONS_TYPE, List<CollectionRanking>>): Mono<Void> {
        return Flux.fromIterable(rankingsByTimeRange.entries)
            .flatMap { (type, rankings) ->
                Flux.fromIterable(rankings)
                    .flatMap { ranking ->
                        Mono.fromCallable {
                            val response = client.index { indexRequest ->
                                indexRequest.index("rankings")
                                    .id("${ranking.collectionName}")
                                    .document(ranking)
                            }

                            if (response.result().name != "CREATED" && response.result().name != "UPDATED") {
                                println("Failed to save ranking for ${ranking.collectionName} in ${type.name} range")
                            }
                        }
                    }
            }
            .then()
    }

    fun bulkUpdate(nftId: Long, price: BigDecimal, ledgerTime: LocalDateTime): Mono<BulkResponse> {
        val roundedPrice = price.setScale(2, RoundingMode.HALF_UP)
        return redisService.transformData(nftId)
            .map { data ->
                data.copy(ledgerPrice = roundedPrice, ledgerTime = ledgerTime)
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

    fun updateRanking(type: AGGREGATIONS_TYPE, limit: Int = 10): Mono<MutableMap<AGGREGATIONS_TYPE, List<CollectionRanking>>> {
        return Mono.defer {
            val startTime = toInstant(type)
            val formattedStartTime = DateTimeFormatter.ISO_INSTANT.format(startTime)

            val rangeQuery = RangeQuery.of { r ->
                r.date { d ->
                    d.field("ledgerTime")
                        .gte(formattedStartTime)
                }
            }

            Mono.fromCallable {
                val searchResponse = client.search({ search ->
                    search.index("nfts")
                        .size(limit)
                        .query { q -> q.range(rangeQuery) }
                }, Document::class.java)

                val documents = searchResponse.hits().hits().mapNotNull { it.source() }
                val rankings = groupAndAggregateResults(documents, type, limit)
                mutableMapOf(type to rankings)
            }
        }
    }


    private fun groupAndAggregateResults(
        documents: List<Document>,
        type: AGGREGATIONS_TYPE,
        limit: Int
    ): List<CollectionRanking> {
        val groupedByCollection = documents.groupBy { it.collectionName to it.chainType }

        return groupedByCollection.map { (key, docs) ->
            val (collectionName, chainType) = key
            val totalPrice = docs.sumOf { it.ledgerPrice ?: 0.0 }
            val lowPrice = docs.mapNotNull { it.lastPrice }
                .minOrNull() ?: 0.0
            val highPrice = docs.mapNotNull { it.lastPrice }
                .maxOrNull() ?: 0.0

            CollectionRanking(
                collectionName = collectionName,
                chainType = chainType,
                totalPrice = totalPrice,
                lowPrice = lowPrice,
                highPrice = highPrice,
                timeRange = type
            )
        }.sortedByDescending { it.totalPrice }.take(limit)
    }


    private fun toInstant(aggregationType: AGGREGATIONS_TYPE) : Instant {
        val now = Instant.now()
        return when (aggregationType) {
            AGGREGATIONS_TYPE.ONE_HOURS -> now.minus(1, ChronoUnit.HOURS)
            AGGREGATIONS_TYPE.SIX_HOURS -> now.minus(6, ChronoUnit.HOURS)
            AGGREGATIONS_TYPE.ONE_DAY -> now.minus(1, ChronoUnit.DAYS)
            AGGREGATIONS_TYPE.SEVEN_DAY -> now.minus(7, ChronoUnit.DAYS)
        }
    }
}