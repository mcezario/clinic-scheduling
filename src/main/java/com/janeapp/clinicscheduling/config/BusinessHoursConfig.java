package com.janeapp.clinicscheduling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( prefix = "app.business-hours" )
public record BusinessHoursConfig(String timezone, int ampmStart, int ampmEnd, int hhEnd, boolean weekends, int appointmentNoticeHours) {
}
