package com.novus.map_service.services;

import com.novus.map_service.UuidProvider;
import com.novus.map_service.configuration.DateConfiguration;
import com.novus.map_service.dao.AdminDashboardDaoUtils;
import com.novus.map_service.dao.RouteDaoUtils;
import com.novus.map_service.dao.UserDaoUtils;
import com.novus.map_service.utils.LogUtils;
import com.novus.shared_models.GeoPoint;
import com.novus.shared_models.common.AdminDashboard.AdminDashboard;
import com.novus.shared_models.common.Kafka.KafkaMessage;
import com.novus.shared_models.common.Log.HttpMethod;
import com.novus.shared_models.common.Log.LogLevel;
import com.novus.shared_models.common.Route.Route;
import com.novus.shared_models.common.User.User;
import com.novus.shared_models.common.User.UserStats;
import com.novus.shared_models.response.Map.HourlyRouteRecalculationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final LogUtils logUtils;
    private final RouteDaoUtils routeDaoUtils;
    private final UserDaoUtils userDaoUtils;
    private final UuidProvider uuidProvider;
    private final DateConfiguration dateConfiguration;
    private final AdminDashboardDaoUtils adminDashboardDaoUtils;

    public void processSaveUserRoute(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();
        log.info("Starting to process save user route request for user: {}", authenticatedUser.getId());

        try {
            String startAddress = request.get("startAddress");
            String endAddress = request.get("endAddress");
            double kilometersDistance = Double.parseDouble(request.get("kilometersDistance"));
            double endPointLatitude = Double.parseDouble(request.get("endPointLatitude"));
            double endPointLongitude = Double.parseDouble(request.get("endPointLongitude"));
            double startPointLatitude = Double.parseDouble(request.get("startPointLatitude"));
            double startPointLongitude = Double.parseDouble(request.get("startPointLongitude"));
            int estimatedDurationInSeconds = Integer.parseInt(request.get("estimatedDurationInSeconds"));

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

            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());

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

            Optional<AdminDashboard> optionalAdminDashboard = adminDashboardDaoUtils.find();
            if (optionalAdminDashboard.isEmpty()) {
                throw new RuntimeException("Admin dashboard not found");
            }

            AdminDashboard adminDashboard = optionalAdminDashboard.get();

            int totalRoutesProposed = adminDashboard.getTotalRoutesProposed() + 1;

            adminDashboardDaoUtils.save(
                    adminDashboard.getId(),
                    adminDashboard.getAppRatingByNumberOfRate(),
                    adminDashboard.getTopContributors(),
                    adminDashboard.getUserGrowthStats(),
                    adminDashboard.getUserActivityMetrics(),
                    adminDashboard.getRouteRecalculations(),
                    adminDashboard.getIncidentConfirmationRate(),
                    adminDashboard.getIncidentsByType(),
                    totalRoutesProposed
            );

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SAVE_USER_ROUTE_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' saved a route from '%s' to '%s'",
                            authenticatedUser.getId(), startAddress, endAddress),
                    HttpMethod.POST,
                    "/private/map/save-route",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Route from '{}' to '{}' successfully saved for user: {}", startAddress, endAddress, authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing save user route request: {}", e.getMessage());
            logError(e, kafkaMessage, "SAVE_USER_ROUTE_ERROR",
                    "Error processing save user route request",
                    HttpMethod.POST, "/private/map/save-route", authenticatedUser);
        }
    }

    public void processGetUserRouteHistory(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        log.info("Starting to process get user route history request for user: {}", authenticatedUser.getId());

        try {
            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());

            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "GET_USER_ROUTE_HISTORY_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' retrieved their route history", authenticatedUser.getId()),
                    HttpMethod.GET,
                    "/private/map/history/routes",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Route history successfully retrieved for user: {}", authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing get user route history request: {}", e.getMessage());
            logError(e, kafkaMessage, "GET_USER_ROUTE_HISTORY_ERROR",
                    "Error processing get user route history request",
                    HttpMethod.GET, "/private/map/history/routes", authenticatedUser);
        }
    }

    public void processSaveNewRouteRecalculation(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        log.info("Starting to process save new route recalculation request for user: {}", authenticatedUser.getId());

        try {
            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());
            userDaoUtils.save(authenticatedUser);

            Optional<AdminDashboard> optionalAdminDashboard = adminDashboardDaoUtils.find();
            if (optionalAdminDashboard.isEmpty()) {
                throw new RuntimeException("Admin dashboard not found");
            }

            AdminDashboard adminDashboard = optionalAdminDashboard.get();
            List<HourlyRouteRecalculationResponse> routeRecalculations = adminDashboard.getRouteRecalculations();

            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            boolean hourFound = false;
            for (HourlyRouteRecalculationResponse recalculation : routeRecalculations) {
                if (recalculation.getHour() == currentHour) {
                    recalculation.setRecalculationCount(recalculation.getRecalculationCount() + 1);
                    hourFound = true;
                    break;
                }
            }

            if (!hourFound) {
                HourlyRouteRecalculationResponse newHourRecalculation = HourlyRouteRecalculationResponse.builder()
                        .hour(currentHour)
                        .recalculationCount(1)
                        .build();

                routeRecalculations.add(newHourRecalculation);
            }

            adminDashboardDaoUtils.save(
                    adminDashboard.getId(),
                    adminDashboard.getAppRatingByNumberOfRate(),
                    adminDashboard.getTopContributors(),
                    adminDashboard.getUserGrowthStats(),
                    adminDashboard.getUserActivityMetrics(),
                    routeRecalculations,
                    adminDashboard.getIncidentConfirmationRate(),
                    adminDashboard.getIncidentsByType(),
                    adminDashboard.getTotalRoutesProposed()
            );

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SAVE_NEW_ROUTE_RECALCULATION_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' triggered a route recalculation", authenticatedUser.getId()),
                    HttpMethod.POST,
                    "/private/map/route-recalculation",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Route recalculation successfully logged for user: {}", authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing save new route recalculation request: {}", e.getMessage());
            logError(e, kafkaMessage, "SAVE_NEW_ROUTE_RECALCULATION_ERROR",
                    "Error processing save new route recalculation request",
                    HttpMethod.POST, "/private/map/route-recalculation", authenticatedUser);
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