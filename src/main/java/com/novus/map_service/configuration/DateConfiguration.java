package com.novus.map_service.configuration;

import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DateConfiguration {

    public Date newDate() {
        return new Date(System.currentTimeMillis() + 7200000);
    }

}