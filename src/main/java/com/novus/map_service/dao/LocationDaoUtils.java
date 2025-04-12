package com.novus.map_service.dao;

import com.novus.database_utils.Location.LocationDao;
import com.novus.shared_models.common.Location.Location;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LocationDaoUtils {

    private final LocationDao<Location> locationDao;

    public LocationDaoUtils(MongoTemplate mongoTemplate) {
        this.locationDao = new LocationDao<>(mongoTemplate);
    }

    public void save(Location location) {
        locationDao.save(location);
    }

    public void delete(Location location) {
        locationDao.delete(location);
    }

    public Optional<Location> findById(String locationId) {
        return locationDao.findById(locationId, Location.class);
    }
}
