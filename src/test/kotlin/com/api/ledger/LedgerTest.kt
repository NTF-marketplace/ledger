package com.api.ledger

import com.api.ledger.enums.ChainType
import com.api.ledger.service.dto.TransferRequest
import com.api.ledger.service.external.WalletApiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import kotlin.test.Test

@SpringBootTest
class LedgerTest(
    @Autowired private val walletApiService: WalletApiService,
) {
    @Test
    fun walletApiTest() {
        val reuqest =
            TransferRequest(
                toAddress = "0x01b72b4aa3f66f213d62d53e829bc172a6a72867",
                fromAddress = "0x01b82b4aa3f66f213d62d53e829bc172a6a72867",
                nftId = 4,
                chainType = ChainType.POLYGON_MAINNET,
                amount = BigDecimal(1.23),
            )
        walletApiService.transfer(request = reuqest).block()
    }
}
