package com.api.ledger.domain.repository

import com.api.ledger.domain.LedgerFailLog
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface LedgerFailLogRepository : ReactiveCrudRepository<LedgerFailLog, Long>
