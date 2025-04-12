package com.novus.map_service.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "supmap.properties")
public class EnvConfiguration {
    private String databaseName;
    private String elasticsearchPassword;
    private String elasticsearchUrl;
    private String elasticsearchUsername;
    private String kafkaBootstrapServers;
    private String mongoUri;
}