package com.api.ledger.config

import com.api.ledger.enums.AGGREGATIONS_TYPE
import com.api.ledger.service.external.ElasticsearchService
import org.springframework.batch.core.DefaultJobKeyGenerator
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import reactor.core.publisher.Mono
import javax.sql.DataSource

@Configuration
@EnableBatchProcessing
class BatchConfig(
    @Qualifier("batchDataSource") private val dataSource: DataSource,
    @Qualifier("batchTransactionManager") private val transactionManager: PlatformTransactionManager,
    private val elasticsearchService: ElasticsearchService,
): DefaultBatchConfiguration() {

    override fun getDataSource(): DataSource = dataSource
    override fun getTransactionManager(): PlatformTransactionManager = transactionManager

    @Bean
    override fun jobRepository(): JobRepository {
        val jobRepositoryFactoryBean = JobRepositoryFactoryBean()
        jobRepositoryFactoryBean.setDataSource(dataSource)
        jobRepositoryFactoryBean.transactionManager = transactionManager
        jobRepositoryFactoryBean.setDatabaseType("POSTGRES")
        jobRepositoryFactoryBean.setJobKeyGenerator(DefaultJobKeyGenerator())
        val incrementerFactory = DefaultDataFieldMaxValueIncrementerFactory(dataSource)
        jobRepositoryFactoryBean.setIncrementerFactory(incrementerFactory)
        jobRepositoryFactoryBean.afterPropertiesSet()
        return jobRepositoryFactoryBean.getObject()
    }

    @Bean
    override fun jobExplorer(): JobExplorer {
        val jobExplorerFactoryBean = JobExplorerFactoryBean()
        jobExplorerFactoryBean.setDataSource(dataSource)
        jobExplorerFactoryBean.transactionManager = transactionManager
        jobExplorerFactoryBean.setTablePrefix("BATCH_")
        jobExplorerFactoryBean.afterPropertiesSet()
        return jobExplorerFactoryBean.getObject()
    }

    @Bean
    override fun jobLauncher(): JobLauncher {
        val taskExecutorLauncher = TaskExecutorJobLauncher()
        taskExecutorLauncher.setJobRepository(jobRepository())
        taskExecutorLauncher.setTaskExecutor(SimpleAsyncTaskExecutor())
        taskExecutorLauncher.afterPropertiesSet()
        return taskExecutorLauncher
    }

    @Bean
    fun jobLauncherApplicationRunner(): JobLauncherApplicationRunner {
        return JobLauncherApplicationRunner(jobLauncher(), jobExplorer(),jobRepository())
    }

    @Bean
    fun rankingUpdateJob(): Job {
        return JobBuilder("rankingUpdateJob", jobRepository())
            .start(rankingUpdateStep())
            .build()
    }

    @Bean
    fun rankingUpdateStep(): Step {
        return StepBuilder("rankingUpdateStep", jobRepository())
            .tasklet(
                { _, _ ->
                    runBatch()
                    RepeatStatus.FINISHED
                },
                transactionManager
            )
            .build()
    }


    private fun runBatch() {
        println("Running batch job...")
        val oneHour = elasticsearchService.updateRanking(AGGREGATIONS_TYPE.ONE_HOURS, 30)
            .flatMap { elasticsearchService.saveRankings(it) }

        val sixHours = elasticsearchService.updateRanking(AGGREGATIONS_TYPE.SIX_HOURS, 30)
            .flatMap { elasticsearchService.saveRankings(it) }

        val oneDay = elasticsearchService.updateRanking(AGGREGATIONS_TYPE.ONE_DAY, 30)
            .flatMap { elasticsearchService.saveRankings(it) }

        val sevenDays = elasticsearchService.updateRanking(AGGREGATIONS_TYPE.SEVEN_DAY, 30)
            .flatMap { elasticsearchService.saveRankings(it) }

        Mono.`when`(oneHour, sixHours, oneDay, sevenDays)
            .doOnSuccess { println("Batch job completed successfully.") }
            .doOnError { error -> println("Batch job failed with error: $error") }
            .subscribe()
    }
}