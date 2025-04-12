package com.novus.map_service.dao;

import com.novus.database_utils.Route.RouteDao;
import com.novus.shared_models.common.Route.Route;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class RouteDaoUtils {

    private final RouteDao<Route> routeDao;

    public RouteDaoUtils(MongoTemplate mongoTemplate) {
        this.routeDao = new RouteDao<>(mongoTemplate);
    }

    public void save(Route route) {
        routeDao.save(route);
    }

}
