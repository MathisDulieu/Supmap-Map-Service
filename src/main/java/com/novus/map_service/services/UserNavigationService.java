package com.novus.map_service.services;

import com.novus.map_service.utils.LogUtils;
import com.novus.shared_models.common.Kafka.KafkaMessage;
import com.novus.shared_models.common.Log.HttpMethod;
import com.novus.shared_models.common.Log.LogLevel;
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

    public void processUpdateUserNavigationPreferences(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();

        try {
            String preferredTransportMode = request.get("preferredTransportMode");
            int proximityAlertDistance = Integer.parseInt(request.get("proximityAlertDistance"));

            log.info("Updating navigation preferences for user {}: transport mode = {}, alert distance = {} meters",
                    authenticatedUser.getId(), preferredTransportMode, proximityAlertDistance);

            // Update navigation preferences logic here
            // You might want to update user preferences in a user repository

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "UPDATE_USER_NAVIGATION_PREFERENCES_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' updated their navigation preferences", authenticatedUser.getId()),
                    HttpMethod.PUT,
                    "/map/preferences",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "UPDATE_USER_NAVIGATION_PREFERENCES_ERROR",
                    "Error processing update user navigation preferences request",
                    HttpMethod.PUT, "/map/preferences", authenticatedUser);
        }
    }

    public void processGetNearbyUsers(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();

        try {
            log.info("Finding nearby users for user: {}", authenticatedUser.getId());

            // Get nearby users logic here
            // This would typically involve spatial queries or proximity calculations
            // List<UserLocationDto> nearbyUsers = mapUtils.buildGetNearbyUsersUsersResponse(latitude, longitude);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "GET_NEARBY_USERS_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' requested nearby users", authenticatedUser.getId()),
                    HttpMethod.GET,
                    "/map/users/nearby",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "GET_NEARBY_USERS_ERROR",
                    "Error processing get nearby users request",
                    HttpMethod.GET, "/map/users/nearby", authenticatedUser);
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