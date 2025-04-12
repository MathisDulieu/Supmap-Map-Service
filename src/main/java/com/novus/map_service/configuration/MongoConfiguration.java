package com.novus.map_service.configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class MongoConfiguration {

    private final EnvConfiguration envConfiguration;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(envConfiguration.getMongoUri());
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient(), envConfiguration.getDatabaseName()));
    }

}
