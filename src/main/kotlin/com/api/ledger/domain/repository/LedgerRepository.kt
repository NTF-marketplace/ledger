package com.api.ledger.domain.repository

import com.api.ledger.domain.Ledger
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface LedgerRepository : ReactiveCrudRepository<Ledger, Long>
