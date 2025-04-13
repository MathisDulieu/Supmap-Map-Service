package com.novus.map_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novus.map_service.services.*;
import com.novus.shared_models.common.Kafka.KafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Consumer {

    private final ObjectMapper objectMapper;
    private final AdminDashboardService adminDashboardService;
    private final AlertService alertService;
    private final LocationService locationService;
    private final RouteService routeService;
    private final UserNavigationService userNavigationService;
    private final SharingService sharingService;

    @KafkaListener(topics = "map-service", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAuthenticationEvents(
            @Payload String messageJson,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("JSON message received from map-service topic [key: {}, partition: {}, offset: {}]", key, partition, offset);

            KafkaMessage kafkaMessage = objectMapper.readValue(messageJson, KafkaMessage.class);

            processMessage(key, kafkaMessage);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    private void processMessage(String operationKey, KafkaMessage kafkaMessage) {
        log.info("Processing operation: {}", operationKey);

        switch (operationKey) {
            case "getMapAdminDashboardData":
                adminDashboardService.processGetMapAdminDashboardData(kafkaMessage);
                break;
            case "saveNewAlert":
                alertService.processSaveNewAlert(kafkaMessage);
                break;
            case "getAllAlertsByPosition":
                alertService.processGetAllAlertsByPosition(kafkaMessage);
                break;
            case "getAllAlertsByRoute":
                alertService.processGetAllAlertsByRoute(kafkaMessage);
                break;
            case "validateUserAlert":
                alertService.processValidateUserAlert(kafkaMessage);
                break;
            case "invalidateUserAlert":
                alertService.processInvalidateUserAlert(kafkaMessage);
                break;
            case "getUserFavoriteLocations":
                locationService.processGetUserFavoriteLocations(kafkaMessage);
                break;
            case "saveNewUserFavoriteLocation":
                locationService.processSaveNewUserFavoriteLocation(kafkaMessage);
                break;
            case "deleteUserFavoriteLocation":
                locationService.processDeleteUserFavoriteLocation(kafkaMessage);
                break;
            case "updateUserFavoriteLocation":
                locationService.processUpdateUserFavoriteLocation(kafkaMessage);
                break;
            case "saveUserRoute":
                routeService.processSaveUserRoute(kafkaMessage);
                break;
            case "getUserRouteHistory":
                routeService.processGetUserRouteHistory(kafkaMessage);
                break;
            case "saveNewRouteRecalculation":
                routeService.processSaveNewRouteRecalculation(kafkaMessage);
                break;
            case "updateUserNavigationPreferences":
                userNavigationService.processUpdateUserNavigationPreferences(kafkaMessage);
                break;
            case "getNearbyUsers":
                userNavigationService.processGetNearbyUsers(kafkaMessage);
                break;
            case "shareLocation":
                sharingService.processShareLocation(kafkaMessage);
                break;
            case "shareRoute":
                sharingService.processShareRoute(kafkaMessage);
                break;
            default:
                log.warn("Unknown operation: {}", operationKey);
                break;
        }
    }
}