package com.novus.map_service.services;

import com.novus.map_service.configuration.DateConfiguration;
import com.novus.map_service.dao.UserDaoUtils;
import com.novus.map_service.utils.LogUtils;
import com.novus.shared_models.common.Kafka.KafkaMessage;
import com.novus.shared_models.common.Log.HttpMethod;
import com.novus.shared_models.common.Log.LogLevel;
import com.novus.shared_models.common.User.NavigationPreferences;
import com.novus.shared_models.common.User.TransportMode;
import com.novus.shared_models.common.User.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNavigationService {

    private final LogUtils logUtils;
    private final UserDaoUtils userDaoUtils;
    private final DateConfiguration dateConfiguration;

    public void processUpdateUserNavigationPreferences(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();
        log.info("Starting to process update user navigation preferences request for user: {}", authenticatedUser.getId());

        try {
            String preferredTransportMode = request.get("preferredTransportMode");
            int proximityAlertDistance = Integer.parseInt(request.get("proximityAlertDistance"));
            boolean avoidTolls = Boolean.parseBoolean(request.get("avoidTolls"));
            boolean avoidHighways = Boolean.parseBoolean(request.get("avoidHighways"));
            boolean avoidTraffic = Boolean.parseBoolean(request.get("avoidTraffic"));
            boolean showUsers = Boolean.parseBoolean(request.get("showUsers"));

            NavigationPreferences navigationPreferences = authenticatedUser.getNavigationPreferences();
            navigationPreferences.setPreferredTransportMode(TransportMode.valueOf(preferredTransportMode));
            navigationPreferences.setProximityAlertDistance(proximityAlertDistance);
            navigationPreferences.setAvoidTolls(avoidTolls);
            navigationPreferences.setAvoidHighways(avoidHighways);
            navigationPreferences.setAvoidTraffic(avoidTraffic);
            navigationPreferences.setShowUsers(showUsers);
            authenticatedUser.setNavigationPreferences(navigationPreferences);

            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());
            authenticatedUser.setUpdatedAt(dateConfiguration.newDate());

            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "UPDATE_USER_NAVIGATION_PREFERENCES_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' updated their navigation preferences", authenticatedUser.getId()),
                    HttpMethod.PUT,
                    "/private/map/navigation-preferences",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Navigation preferences successfully updated for user: {}", authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing update user navigation preferences request: {}", e.getMessage());
            logError(e, kafkaMessage, "UPDATE_USER_NAVIGATION_PREFERENCES_ERROR",
                    "Error processing update user navigation preferences request",
                    HttpMethod.PUT, "/private/map/navigation-preferences", authenticatedUser);
        }
    }

    public void processGetNearbyUsers(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        log.info("Starting to process get nearby users request for user: {}", authenticatedUser.getId());

        try {
            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());

            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "GET_NEARBY_USERS_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' requested nearby users", authenticatedUser.getId()),
                    HttpMethod.GET,
                    "/private/map/nearby-users",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Nearby users successfully retrieved for user: {}", authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing get nearby users request: {}", e.getMessage());
            logError(e, kafkaMessage, "GET_NEARBY_USERS_ERROR",
                    "Error processing get nearby users request",
                    HttpMethod.GET, "/private/map/nearby-users", authenticatedUser);
        }
    }

    private void logError(Exception e, KafkaMessage kafkaMessage, String errorCode,
                          String message, HttpMethod httpMethod, String endpoint, User user) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        logUtils.buildAndSaveLog(
                LogLevel.ERROR,
                errorCode,
                kafkaMessage.getIpAddress(),
                message + ": " + e.getMessage(),
                httpMethod,
                endpoint,
                "map-service",
                stackTrace,
                user != null ? user.getId() : null
        );
        throw new RuntimeException(message + ": " + e.getMessage(), e);
    }
}