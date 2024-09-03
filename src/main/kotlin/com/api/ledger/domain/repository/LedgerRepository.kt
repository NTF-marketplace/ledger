package com.api.ledger.domain.repository

import com.api.ledger.domain.Ledger
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface LedgerRepository : ReactiveCrudRepository<Ledger, Long> {
    fun existsByNftId(nftId: Long): Mono<Boolean>
}
