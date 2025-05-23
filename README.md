# Map-Service

## Description
Real-time navigation service for SUPMAP that handles route optimization, traffic alerts, incident reporting,
user location sharing, and community-driven validation of road conditions. Supports favorite location management,
navigation preferences, and route history tracking with comprehensive metrics collection for performance monitoring.

## Features
- Route calculation with optimization based on real-time traffic conditions
- Automatic route recalculation when traffic incidents occur
- Traffic incidents management (accidents, traffic jams, roadblocks, etc.)
- Support for route preferences (avoid tolls, fastest route, etc.)
- Real-time traffic prediction based on historical data

## Tech Stack
- Java 21
- Spring Boot 3.4.4
- MongoDB for geographical and traffic data storage
- Kafka for real-time event streaming
- Spring Security for secured endpoints

## Dependencies
- shared-models: Common data models for the SUPMAP ecosystem
- database-utils: Database utility functions
- Spring Boot starters (web, data-mongodb, security)
- Lombok for reducing boilerplate code
- Kafka for event-driven communication between services

## Configuration
The service can be configured via environment variables or application properties:

```yaml
supmap:
  properties:
    database-name: map_service_db
    elasticsearch-password: your-password
    elasticsearch-url: http://elasticsearch:9200
    elasticsearch-username: elastic
    kafka-bootstrap-servers: kafka:9092
    mongo-uri: mongodb://user:password@mongodb:27017/map_service_db