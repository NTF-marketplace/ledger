package com.api.ledger.config

import org.springframework.kafka.support.serializer.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ser.std.StringSerializer
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.MessageListenerContainer

@Configuration
@EnableKafka
class KafkaConfig {

    private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        return KafkaAdmin(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers))
    }

    @Bean
    fun ledgerTopic(): NewTopic = TopicBuilder.name("ledger-topic")
        .partitions(4)
        .replicas(1)
        .build()

    @Bean
    fun ledgerStatusTopic(): NewTopic = TopicBuilder.name("ledgerStatus-topic")
        .partitions(4)
        .replicas(1)
        .build()


    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "ledger-group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java.name,
            JsonDeserializer.TRUSTED_PACKAGES to "*",
            JsonDeserializer.VALUE_DEFAULT_TYPE to Any::class.java.name
        )
        return DefaultKafkaConsumerFactory(props, StringDeserializer(), JsonDeserializer(Any::class.java, false))
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory()
        factory.setConcurrency(4)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setCommonErrorHandler(object : CommonErrorHandler {
            override fun handleRemaining(thrownException: Exception, records: List<org.apache.kafka.clients.consumer.ConsumerRecord<*, *>>, consumer: org.apache.kafka.clients.consumer.Consumer<*, *>, container: MessageListenerContainer) {
                logger.error("Error in consumer: ${thrownException.message}", thrownException)
                logger.error("Problematic records: $records")
            }
        })
        return factory
    }

}