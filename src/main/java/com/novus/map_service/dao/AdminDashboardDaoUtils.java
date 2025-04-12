package com.novus.map_service.dao;

import com.novus.database_utils.AdminDashboard.AdminDashboardDao;
import com.novus.shared_models.common.AdminDashboard.AdminDashboard;
import com.novus.shared_models.response.Map.HourlyRouteRecalculationResponse;
import com.novus.shared_models.response.User.MonthlyUserStatsResponse;
import com.novus.shared_models.response.User.UserActivityMetricsResponse;
import com.novus.shared_models.response.User.UserContributionResponse;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AdminDashboardDaoUtils {

    private final AdminDashboardDao<AdminDashboard> adminDashboardDao;

    public AdminDashboardDaoUtils(MongoTemplate mongoTemplate) {
        this.adminDashboardDao = new AdminDashboardDao<>(mongoTemplate);
    }

    public Optional<AdminDashboard> find() {
        return adminDashboardDao.findMe(AdminDashboard.class);
    }

    public void save(String adminDashboardId, Map<Integer, Double> appRatingByNumberOfRate,
                     List<UserContributionResponse> topContributors, List<MonthlyUserStatsResponse> userGrowthStats,
                     UserActivityMetricsResponse userActivityMetrics, List<HourlyRouteRecalculationResponse> routeRecalculations,
                     Double incidentConfirmationRate, Map<String, Integer> incidentsByType, int totalRoutesProposed
    ) {
        adminDashboardDao.upsert(
                adminDashboardId,
                appRatingByNumberOfRate,
                topContributors,
                userGrowthStats,
                userActivityMetrics,
                routeRecalculations,
                incidentConfirmationRate,
                incidentsByType,
                totalRoutesProposed
        );
    }

}