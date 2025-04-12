package com.novus.map_service.services;

import com.novus.map_service.configuration.EnvConfiguration;
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
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharingService {

    private final LogUtils logUtils;
    private final EnvConfiguration envConfiguration;
    private final UserDaoUtils userDaoUtils;

    public void processShareLocation(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();

        try {
            String qrCodeUrl = request.get("qrCodeUrl");

            log.info("Generating QR code for location sharing for user {}, QR code URL: {}",
                    authenticatedUser.getId(), qrCodeUrl);

            authenticatedUser.setLastActivityDate(new Date());
            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SHARE_LOCATION_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' shared their location via QR code. QR code URL: %s",
                            authenticatedUser.getId(), qrCodeUrl),
                    HttpMethod.POST,
                    "/map/share/location",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "SHARE_LOCATION_ERROR",
                    "Error processing share location request",
                    HttpMethod.POST, "/map/share/location", authenticatedUser);
        }
    }

    public void processShareRoute(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();

        try {
            String qrCodeUrl = request.get("qrCodeUrl");

            log.info("Generating QR code for location sharing for user {}, QR code URL: {}",
                    authenticatedUser.getId(), qrCodeUrl);

            authenticatedUser.setLastActivityDate(new Date());
            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SHARE_ROUTE_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' shared a route via QR code. QR code URL: %s",
                            authenticatedUser.getId(), qrCodeUrl),
                    HttpMethod.POST,
                    "/map/share/route",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "SHARE_ROUTE_ERROR",
                    "Error processing share route request",
                    HttpMethod.POST, "/map/share/route", authenticatedUser);
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