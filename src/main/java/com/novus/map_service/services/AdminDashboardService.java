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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final LogUtils logUtils;
    private final UserDaoUtils userDaoUtils;
    private final DateConfiguration dateConfiguration;

    public void processGetMapAdminDashboardData(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        log.info("Starting to process map admin dashboard data request for user: {}", authenticatedUser.getId());

        try {
            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());
            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "GET_MAP_ADMIN_DASHBOARD_DATA_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' retrieved admin dashboard data", authenticatedUser.getId()),
                    HttpMethod.GET,
                    "/private/admin/map/dashboard-data",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Map admin dashboard data successfully retrieved for user: {}", authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing map admin dashboard data request: {}", e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();

            logUtils.buildAndSaveLog(
                    LogLevel.ERROR,
                    "GET_MAP_ADMIN_DASHBOARD_DATA_ERROR",
                    kafkaMessage.getIpAddress(),
                    "Error processing get map admin dashboard data request: " + e.getMessage(),
                    HttpMethod.GET,
                    "/private/admin/map/dashboard-data",
                    "map-service",
                    stackTrace,
                    authenticatedUser.getId()
            );
            throw new RuntimeException("Failed to process get map admin dashboard data request: " + e.getMessage(), e);
        }
    }
}