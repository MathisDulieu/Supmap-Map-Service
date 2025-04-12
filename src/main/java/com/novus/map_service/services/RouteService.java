package com.novus.map_service.services;

import com.novus.map_service.UuidProvider;
import com.novus.map_service.dao.RouteDaoUtils;
import com.novus.map_service.dao.UserDaoUtils;
import com.novus.map_service.utils.LogUtils;
import com.novus.shared_models.GeoPoint;
import com.novus.shared_models.common.Kafka.KafkaMessage;
import com.novus.shared_models.common.Log.HttpMethod;
import com.novus.shared_models.common.Log.LogLevel;
import com.novus.shared_models.common.Route.Route;
import com.novus.shared_models.common.User.User;
import com.novus.shared_models.common.User.UserStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final LogUtils logUtils;
    private final RouteDaoUtils routeDaoUtils;
    private final UserDaoUtils userDaoUtils;
    private final UuidProvider uuidProvider;

    public void processSaveUserRoute(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();

        try {
            String startAddress = request.get("startAddress");
            String endAddress = request.get("endAddress");
            double kilometersDistance = Double.parseDouble(request.get("kilometersDistance"));
            double endPointLatitude = Double.parseDouble(request.get("endPointLatitude"));
            double endPointLongitude = Double.parseDouble(request.get("endPointLongitude"));
            double startPointLatitude = Double.parseDouble(request.get("startPointLatitude"));
            double startPointLongitude = Double.parseDouble(request.get("startPointLongitude"));
            int estimatedDurationInSeconds = Integer.parseInt(request.get("estimatedDurationInSeconds"));

            log.info("Saving route from '{}' to '{}' ({} km) for user: {}",
                    startAddress, endAddress, kilometersDistance, authenticatedUser.getId());

            GeoPoint startPoint = GeoPoint.builder()
                    .latitude(startPointLatitude)
                    .longitude(startPointLongitude)
                    .build();

            GeoPoint endPoint = GeoPoint.builder()
                    .latitude(endPointLatitude)
                    .longitude(endPointLongitude)
                    .build();

            Route route = Route.builder()
                    .id(uuidProvider.generateUuid())
                    .endAddress(endAddress)
                    .startAddress(startAddress)
                    .endPoint(endPoint)
                    .startPoint(startPoint)
                    .estimatedDurationInSeconds(estimatedDurationInSeconds)
                    .kilometersDistance(kilometersDistance)
                    .userId(authenticatedUser.getId())
                    .build();

            authenticatedUser.setLastActivityDate(new Date());

            List<String> recentRouteIds = authenticatedUser.getRecentRouteIds();
            if (recentRouteIds.size() >= 5) {
                recentRouteIds.remove(0);
            }
            recentRouteIds.add(route.getId());

            UserStats stats = authenticatedUser.getStats();
            stats.setTotalDistanceTraveled(stats.getTotalDistanceTraveled() + (int)Math.round(kilometersDistance));
            stats.setTotalRoutesCompleted(stats.getTotalRoutesCompleted() + 1);

            userDaoUtils.save(authenticatedUser);
            routeDaoUtils.save(route);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SAVE_USER_ROUTE_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' saved a route from '%s' to '%s'",
                            authenticatedUser.getId(), startAddress, endAddress),
                    HttpMethod.POST,
                    "/map/routes",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "SAVE_USER_ROUTE_ERROR",
                    "Error processing save user route request",
                    HttpMethod.POST, "/map/routes", authenticatedUser);
        }
    }

    public void processGetUserRouteHistory(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();

        try {
            log.info("Retrieving route history for user: {}", authenticatedUser.getId());

            authenticatedUser.setLastActivityDate(new Date());

            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "GET_USER_ROUTE_HISTORY_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' retrieved their route history", authenticatedUser.getId()),
                    HttpMethod.GET,
                    "/map/routes/history",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "GET_USER_ROUTE_HISTORY_ERROR",
                    "Error processing get user route history request",
                    HttpMethod.GET, "/map/routes/history", authenticatedUser);
        }
    }

    public void processSaveNewRouteRecalculation(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();

        try {
            log.info("Logging route recalculation for user: {}", authenticatedUser.getId());

            authenticatedUser.setLastActivityDate(new Date());

            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SAVE_NEW_ROUTE_RECALCULATION_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' triggered a route recalculation", authenticatedUser.getId()),
                    HttpMethod.POST,
                    "/map/routes/recalculation",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (Exception e) {
            logError(e, kafkaMessage, "SAVE_NEW_ROUTE_RECALCULATION_ERROR",
                    "Error processing save new route recalculation request",
                    HttpMethod.POST, "/map/routes/recalculation", authenticatedUser);
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