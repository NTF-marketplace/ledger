package com.api.ledger.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
class BatchDataSourceConfig {

    @Bean
    @Qualifier("batchDataSource")
    fun batchDataSource(): DataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.driverClassName = "org.postgresql.Driver"
        hikariConfig.jdbcUrl = "jdbc:postgresql://localhost:5437/ledger"
        hikariConfig.username = "ledger"
        hikariConfig.password = "ledger"
        return HikariDataSource(hikariConfig)
    }

    @Bean
    @Qualifier("batchTransactionManager")
    fun batchTransactionManager(): PlatformTransactionManager {
        return DataSourceTransactionManager(batchDataSource())
    }
}