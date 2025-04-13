package com.novus.map_service.services;

import com.novus.map_service.configuration.DateConfiguration;
import com.novus.map_service.dao.UserDaoUtils;
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
public class SharingService {

    private final LogUtils logUtils;
    private final UserDaoUtils userDaoUtils;
    private final DateConfiguration dateConfiguration;

    public void processShareLocation(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();
        log.info("Starting to process share location request for user: {}", authenticatedUser.getId());

        try {
            String qrCodeUrl = request.get("qrCodeUrl");

            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());
            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SHARE_LOCATION_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' shared their location via QR code. QR code URL: %s",
                            authenticatedUser.getId(), qrCodeUrl),
                    HttpMethod.POST,
                    "/private/map/location/share",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Location successfully shared via QR code for user: {}", authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing share location request: {}", e.getMessage());
            logError(e, kafkaMessage, "SHARE_LOCATION_ERROR",
                    "Error processing share location request",
                    "/private/map/location/share", authenticatedUser);
        }
    }

    public void processShareRoute(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();
        log.info("Starting to process share route request for user: {}", authenticatedUser.getId());

        try {
            String qrCodeUrl = request.get("qrCodeUrl");

            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());
            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SHARE_ROUTE_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' shared a route via QR code. QR code URL: %s",
                            authenticatedUser.getId(), qrCodeUrl),
                    HttpMethod.POST,
                    "/private/map/route/share",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Route successfully shared via QR code for user: {}", authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing share route request: {}", e.getMessage());
            logError(e, kafkaMessage, "SHARE_ROUTE_ERROR",
                    "Error processing share route request",
                    "/private/map/route/share", authenticatedUser);
        }
    }

    private void logError(Exception e, KafkaMessage kafkaMessage, String errorCode,
                          String message, String endpoint, User user) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        logUtils.buildAndSaveLog(
                LogLevel.ERROR,
                errorCode,
                kafkaMessage.getIpAddress(),
                message + ": " + e.getMessage(),
                HttpMethod.POST,
                endpoint,
                "map-service",
                stackTrace,
                user != null ? user.getId() : null
        );
        throw new RuntimeException(message + ": " + e.getMessage(), e);
    }
}