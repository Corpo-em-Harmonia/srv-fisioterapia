package com.thalia.fisioterapia.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Sem este bean os @Transactional do projeto não faziam nada (o Spring Boot não cria gerenciador
 * de transação para o Mongo): em converterLeadParaPaciente, um erro no meio deixava paciente
 * criado sem avaliação. Transações exigem replica set — o Atlas já é; para um Mongo local
 * standalone, desligue com app.mongo.transactions=false.
 */
@Configuration
@ConditionalOnProperty(name = "app.mongo.transactions", havingValue = "true", matchIfMissing = true)
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
