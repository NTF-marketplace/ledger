package com.api.ledger.service

import com.api.ledger.domain.repository.LedgerRepository
import org.springframework.stereotype.Service

@Service
class LedgerService(
    private val ledgerRepository: LedgerRepository,
) {

    // 돋시성 이슈는 어떻게 해결핲것인지 좀 생각해보자
//    fun
}