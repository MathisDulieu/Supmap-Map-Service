package com.novus.map_service.dao;

import com.novus.database_utils.Log.LogDao;
import com.novus.shared_models.common.Log.Log;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class LogDaoUtils {

    private final LogDao<Log> logDao;

    public LogDaoUtils(MongoTemplate mongoTemplate) {
        this.logDao = new LogDao<>(mongoTemplate);
    }

    public void save(Log log) {
        logDao.save(log);
    }

}

