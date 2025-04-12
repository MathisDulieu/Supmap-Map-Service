package com.novus.map_service.services;

import com.novus.map_service.UuidProvider;
import com.novus.map_service.dao.AlertDaoUtils;
import com.novus.map_service.dao.UserDaoUtils;
import com.novus.map_service.utils.LogUtils;
import com.novus.shared_models.GeoPoint;
import com.novus.shared_models.common.Alert.Alert;
import com.novus.shared_models.common.Alert.AlertType;
import com.novus.shared_models.common.Kafka.KafkaMessage;
import com.novus.shared_models.common.Log.HttpMethod;
import com.novus.shared_models.common.Log.LogLevel;
import com.novus.shared_models.common.User.User;
import com.novus.shared_models.common.User.UserRank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final LogUtils logUtils;
    private final AlertDaoUtils alertDaoUtils;
    private final UuidProvider uuidProvider;
    private final UserDaoUtils userDaoUtils;

    public void processSaveNewAlert(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();

        try {
            String alertType = request.get("alertType");
            double latitude = Double.parseDouble(request.get("latitude"));
            double longitude = Double.parseDouble(request.get("longitude"));

            log.info("Saving new alert of type {} at coordinates [{}, {}] for user {}",
                    alertType, latitude, longitude, authenticatedUser.getId());

            GeoPoint location = GeoPoint.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();

            Date expiresAt = new Date(System.currentTimeMillis() + (30 * 60 * 1000));

            Alert alert = Alert.builder()
                    .id(uuidProvider.generateUuid())
                    .expiresAt(expiresAt)
                    .type(AlertType.valueOf(alertType))
                    .description(generateAlertDescription(alertType))
                    .location(location)
                    .reportedByUserId(authenticatedUser.getId())
                    .build();

            alertDaoUtils.save(alert);

            authenticatedUser.setLastActivityDate(new Date());
            authenticatedUser.getStats().setTotalReportsSubmitted(authenticatedUser.getStats().getTotalReportsSubmitted() + 1);
            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SAVE_NEW_ALERT_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' created a new alert of type '%s'",
                            authenticatedUser.getId(), alertType),
                    HttpMethod.POST,
                    "/map/alerts",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "SAVE_NEW_ALERT_ERROR",
                    "Error processing save new alert request",
                    HttpMethod.POST, "/map/alerts", authenticatedUser);
        }
    }

    public void processGetAllAlertsByPosition(KafkaMessage kafkaMessage) {
        try {
            log.info("Processing get all alerts by position request");

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "GET_ALL_ALERTS_BY_POSITION_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    "Successfully retrieved alerts by position",
                    HttpMethod.GET,
                    "/map/alerts/by-position",
                    "map-service",
                    null,
                    null
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "GET_ALL_ALERTS_BY_POSITION_ERROR",
                    "Error processing get alerts by position request",
                    HttpMethod.GET, "/map/alerts/by-position", null);
        }
    }

    public void processGetAllAlertsByRoute(KafkaMessage kafkaMessage) {
        try {
            log.info("Processing get all alerts by route request");

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "GET_ALL_ALERTS_BY_ROUTE_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    "Successfully retrieved alerts by route",
                    HttpMethod.GET,
                    "/map/alerts/by-route",
                    "map-service",
                    null,
                    null
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "GET_ALL_ALERTS_BY_ROUTE_ERROR",
                    "Error processing get alerts by route request",
                    HttpMethod.GET, "/map/alerts/by-route", null);
        }
    }

    public void processValidateUserAlert(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();

        try {
            String alertId = request.get("alertId");

            log.info("Validating alert with ID {} by user {}", alertId, authenticatedUser.getId());

            Optional<Alert> optionalAlert = alertDaoUtils.findById(alertId);
            if (optionalAlert.isEmpty()) {
                String errorMessage = String.format("Alert with ID '%s' not found", alertId);
                throw new ResourceNotFoundException(errorMessage);
            }

            String reportedByUserId = optionalAlert.get().getReportedByUserId();
            Optional<User> optionalUser = userDaoUtils.findById(reportedByUserId);
            if (optionalUser.isEmpty()) {
                String errorMessage = String.format("User with ID '%s' not found", reportedByUserId);
                throw new ResourceNotFoundException(errorMessage);
            }

            User alertOwner = optionalUser.get();
            Alert alert = optionalAlert.get();

            alertOwner.getStats().setReportsValidatedByOthers(
                    alertOwner.getStats().getReportsValidatedByOthers() + 1
            );

            int alertOwnerTrustScore = alertOwner.getStats().getTrustScore();
            alertOwner.getStats().setTrustScore(alertOwnerTrustScore +1);

            updateUserRank(alertOwner);

            Date currentExpirationDate = alert.getExpiresAt();
            Date newExpirationDate = new Date(currentExpirationDate.getTime() + (15 * 60 * 1000));
            alert.setExpiresAt(newExpirationDate);

            alert.setUpdatedAt(new Date());

            authenticatedUser.getStats().setValidatedReports(
                    authenticatedUser.getStats().getValidatedReports() + 1
            );

            authenticatedUser.setLastActivityDate(new Date());
            alertOwner.setUpdatedAt(new Date());
            authenticatedUser.setUpdatedAt(new Date());

            userDaoUtils.save(authenticatedUser);
            userDaoUtils.save(alertOwner);
            alertDaoUtils.save(alert);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "VALIDATE_USER_ALERT_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' validated alert with ID '%s'",
                            authenticatedUser.getId(), alertId),
                    HttpMethod.POST,
                    "/map/alerts/validate",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (ResourceNotFoundException e) {
            logError(e, kafkaMessage, "VALIDATE_USER_ALERT_ERROR",
                    e.getMessage(),
                    HttpMethod.POST, "/map/alerts/validate", authenticatedUser);
        } catch (Exception e) {
            logError(e, kafkaMessage, "VALIDATE_USER_ALERT_ERROR",
                    "Error processing validate user alert request",
                    HttpMethod.POST, "/map/alerts/validate", authenticatedUser);
        }
    }

    public void processInvalidateUserAlert(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();

        try {
            String alertId = request.get("alertId");

            log.info("Invalidating alert with ID {} by user {}", alertId, authenticatedUser.getId());

            Optional<Alert> optionalAlert = alertDaoUtils.findById(alertId);
            if (optionalAlert.isEmpty()) {
                String errorMessage = String.format("Alert with ID '%s' not found", alertId);
                throw new ResourceNotFoundException(errorMessage);
            }

            String reportedByUserId = optionalAlert.get().getReportedByUserId();
            Optional<User> optionalUser = userDaoUtils.findById(reportedByUserId);
            if (optionalUser.isEmpty()) {
                String errorMessage = String.format("User with ID '%s' not found", reportedByUserId);
                throw new ResourceNotFoundException(errorMessage);
            }

            User alertOwner = optionalUser.get();
            Alert alert = optionalAlert.get();

            int alertOwnerTrustScore = alertOwner.getStats().getTrustScore();
            alertOwner.getStats().setTrustScore(alertOwnerTrustScore -1);

            updateUserRank(alertOwner);

            Date currentExpirationDate = alert.getExpiresAt();
            Date newExpirationDate = new Date(currentExpirationDate.getTime() - (5 * 60 * 1000));
            alert.setExpiresAt(newExpirationDate);

            alert.setUpdatedAt(new Date());

            authenticatedUser.getStats().setValidatedReports(
                    authenticatedUser.getStats().getValidatedReports() + 1
            );

            authenticatedUser.setLastActivityDate(new Date());
            alertOwner.setUpdatedAt(new Date());
            authenticatedUser.setUpdatedAt(new Date());

            userDaoUtils.save(authenticatedUser);
            userDaoUtils.save(alertOwner);
            alertDaoUtils.save(alert);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "INVALIDATE_USER_ALERT_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' invalidated alert with ID '%s'",
                            authenticatedUser.getId(), alertId),
                    HttpMethod.POST,
                    "/map/alerts/invalidate",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (ResourceNotFoundException e) {
            logError(e, kafkaMessage, "INVALIDATE_USER_ALERT_ERROR",
                    e.getMessage(),
                    HttpMethod.POST, "/map/alerts/invalidate", authenticatedUser);
        } catch (Exception e) {
            logError(e, kafkaMessage, "INVALIDATE_USER_ALERT_ERROR",
                    "Error processing invalidate user alert request",
                    HttpMethod.POST, "/map/alerts/invalidate", authenticatedUser);
        }
    }

    private void updateUserRank(User user) {
        if (user.getStats().getTrustScore() <= 50) {
            user.getStats().setRank(UserRank.NAVIGATOR_NOVICE);
            user.getStats().setRankImage("https://i.ibb.co/dw3g2VJg/Season-2023-Emerald.webp");
        }

        if (user.getStats().getTrustScore() > 50 && user.getStats().getTrustScore() <= 60) {
            user.getStats().setRank(UserRank.ROAD_EXPLORER);
            user.getStats().setRankImage("https://i.ibb.co/46pfzD8/Diamond-aca4ca7.png");
        }

        if (user.getStats().getTrustScore() > 60 && user.getStats().getTrustScore() <= 70) {
            user.getStats().setRank(UserRank.TRAFFIC_SCOUT);
            user.getStats().setRankImage("https://i.ibb.co/gMDTPvTF/Season-2022-Master.webp");
        }

        if (user.getStats().getTrustScore() > 70 && user.getStats().getTrustScore() <= 80) {
            user.getStats().setRank(UserRank.ROUTE_MASTER);
            user.getStats().setRankImage("https://i.ibb.co/cSTHVtcv/Season-2023-Grandmaster.webp");
        }

        if (user.getStats().getTrustScore() > 80) {
            user.getStats().setRank(UserRank.NAVIGATION_LEGEND);
            user.getStats().setRankImage("https://i.ibb.co/jZqTHfcr/Season-2022-Challenger.webp");
        }
    }

    private String generateAlertDescription(String alertType) {
        return switch (alertType) {
            case "ACCIDENT" -> "Traffic accident reported in this area. Use caution and consider alternative routes.";
            case "ROAD_CLOSURE" -> "Road closed at this location. Please use an alternative route.";
            case "TRAFFIC_JAM" -> "Heavy traffic reported in this area. Expect delays.";
            case "POLICE_CONTROL" -> "Police control reported in this area. Reduce speed and be prepared to stop.";
            case "OBSTACLE" -> "Obstacle reported on the road. Drive with caution.";
            case "CONSTRUCTION" -> "Road construction at this location. Be prepared for reduced speeds and possible delays.";
            case "HAZARD" -> "Road hazard reported. Proceed with caution.";
            case "WEATHER" -> "Adverse weather conditions reported. Drive with caution.";
            default -> "Alert reported in this area. Use caution.";
        };
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