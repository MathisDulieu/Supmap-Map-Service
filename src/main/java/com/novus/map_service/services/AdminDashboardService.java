package com.novus.map_service.services;

import com.novus.map_service.dao.AdminDashboardDaoUtils;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final LogUtils logUtils;
    private final AdminDashboardDaoUtils adminDashboardDaoUtils;
    private final UserDaoUtils userDaoUtils;

    public void processGetMapAdminDashboardData(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();

        try {
            log.info("Retrieving map admin dashboard data for user: {}", authenticatedUser.getId());

            authenticatedUser.setLastActivityDate(new Date());
            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "GET_MAP_ADMIN_DASHBOARD_DATA_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' retrieved admin dashboard data", authenticatedUser.getId()),
                    HttpMethod.GET,
                    "/map/admin-dashboard",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
        } catch (Exception e) {
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
                    "/map/admin-dashboard",
                    "map-service",
                    stackTrace,
                    authenticatedUser.getId()
            );
            throw new RuntimeException("Failed to process get map admin dashboard data request: " + e.getMessage(), e);
        }
    }
}