package com.novus.map_service.dao;

import com.novus.database_utils.Alert.AlertDao;
import com.novus.shared_models.common.Alert.Alert;
import com.novus.shared_models.common.User.User;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AlertDaoUtils {

    private final AlertDao<Alert> alertDao;

    public AlertDaoUtils(MongoTemplate mongoTemplate) {
        this.alertDao = new AlertDao<>(mongoTemplate);
    }

    public void save(Alert alert) {
        alertDao.save(alert);
    }

    public Optional<Alert> findById(String id) {
        return alertDao.findById(id, Alert.class);
    }

}
