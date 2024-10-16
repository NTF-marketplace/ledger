package com.api.ledger

import com.api.ledger.domain.repository.LedgerFailLogRepository
import com.api.ledger.enums.AGGREGATIONS_TYPE
import com.api.ledger.enums.ChainType
import com.api.ledger.kafka.KafkaProducer
import com.api.ledger.kafka.dto.LedgerRequest
import com.api.ledger.service.LedgerService
import com.api.ledger.service.dto.LedgerResponse
import com.api.ledger.service.external.RedisService
import com.api.ledger.service.external.WalletApiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test

@SpringBootTest
@ActiveProfiles("local")
class LedgerTest(
    @Autowired private val walletApiService: WalletApiService,
    @Autowired private val ledgerService: LedgerService,
    @Autowired private val ledgerFailLogRepository: LedgerFailLogRepository,
    @Autowired private val redisService: RedisService,
    @Autowired private val elasticsearchService: ElasticsearchService,
    @Autowired private val kafkaProducer: KafkaProducer,
) {
    @Test
    fun walletApiTest() {

        val request = LedgerRequest(
            orderId = 1,
            address = "0x01b72b4aa3f66f213d62d53e829bc172a6a72867",
            orderAddress = "0x01b72b4aa3f66f213d62d53e829bc172a6a72868",
            nftId = 10L,
            chainType = ChainType.POLYGON_MAINNET,
            price = BigDecimal("1.23")
        )

        ledgerService.ledger(request).block()
        Thread.sleep(36000)
    }

    @Test
    fun redisApiTest() {
        val res = redisService.getNft(nftId = 4L).block()
        println(res.toString())
    }

    @Test
    fun elasticSearch() {
        elasticsearchService.bulkUpdate(1L,BigDecimal(5.3), LocalDateTime.now()).block()
        elasticsearchService.bulkUpdate(2L,BigDecimal(3.3), LocalDateTime.now()).block()
        elasticsearchService.bulkUpdate(4L,BigDecimal(5.3), LocalDateTime.now()).block()
        elasticsearchService.bulkUpdate(3L,BigDecimal(2.8), LocalDateTime.now()).block()
        elasticsearchService.bulkUpdate(9L,BigDecimal(3.8), LocalDateTime.now()).block()
    }

    @Test
    fun asd() {
        val res =elasticsearchService.updateRanking(AGGREGATIONS_TYPE.ONE_HOURS, limit = 50).flatMap {
            elasticsearchService.saveRankings(it)
        }.block()
    }

    @Test
    fun ledgerResponse_kafka() {
        kafkaProducer.sendLedgerResponse(LedgerResponse(nftId = 4L, ledgerPrice = BigDecimal(1.23), System.currentTimeMillis())).block()
        Thread.sleep(10000)
    }

}
