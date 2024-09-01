package com.api.ledger

import com.api.ledger.domain.repository.LedgerFailLogRepository
import com.api.ledger.enums.ChainType
import com.api.ledger.kafka.dto.LedgerRequest
import com.api.ledger.service.LedgerService
import com.api.ledger.service.dto.TransferRequest
import com.api.ledger.service.external.RedisService
import com.api.ledger.service.external.WalletApiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import kotlin.test.Test

@SpringBootTest
class LedgerTest(
    @Autowired private val walletApiService: WalletApiService,
    @Autowired private val ledgerService: LedgerService,
    @Autowired private val ledgerFailLogRepository: LedgerFailLogRepository,
    @Autowired private val redisService: RedisService,
) {
    @Test
    fun walletApiTest() {

        val request = LedgerRequest(
            orderId = 1,
            address = "0x01b72b4aa3f66f213d62d53e829bc172a6a72867",
            orderAddress = "0x01b82b4aa3f66f213d62d53e829bc172a6a72867",
            nftId = 4L,
            chainType = ChainType.POLYGON_MAINNET,
            price = BigDecimal("1.23")
        )

        ledgerService.ledger(request).block()
    }

    @Test
    fun redisApiTest() {
        val res = redisService.getNft(nftId = 4L).block()
        println(res.toString())
    }
}
