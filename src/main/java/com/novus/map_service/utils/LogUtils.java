package com.novus.map_service.utils;

import com.novus.map_service.UuidProvider;
import com.novus.map_service.dao.LogDaoUtils;
import com.novus.shared_models.common.Log.HttpMethod;
import com.novus.shared_models.common.Log.Log;
import com.novus.shared_models.common.Log.LogLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class LogUtils {

    private final UuidProvider uuidProvider;
    private final LogDaoUtils logDaoUtils;

    public void buildAndSaveLog(LogLevel logLevel, String action, String ipAddress, String message, HttpMethod httpMethod,
                                String requestPath, String service, String stackTrace, String userId) {

        Log log = Log.builder()
                .id(uuidProvider.generateUuid())
                .level(logLevel)
                .action(action)
                .ipAddress(ipAddress)
                .message(message)
                .requestMethod(httpMethod)
                .requestPath(requestPath)
                .service(service)
                .stackTrace(stackTrace)
                .timestamp(new Date())
                .userId(userId)
                .build();

        logDaoUtils.save(log);
    }

}
