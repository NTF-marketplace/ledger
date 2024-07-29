package com.api.ledger.ledger.domain.repository

import com.api.ledger.ledger.domain.Ledger
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface LedgerRepository : ReactiveCrudRepository<Ledger,Long> {
}