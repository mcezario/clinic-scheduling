package com.janeapp.clinicscheduling.entity;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public enum AppointmentType {

    INITIAL(90, ChronoUnit.MINUTES),
    STANDARD(60, ChronoUnit.MINUTES),
    CHECK_IN(30, ChronoUnit.MINUTES);

    private final int amount;
    private final TemporalUnit unit;

    AppointmentType(int amount, TemporalUnit unit) {
        this.amount = amount;
        this.unit = unit;
    }

    public int amount() {
        return amount;
    }

    public TemporalUnit unit() {
        return unit;
    }

    public static AppointmentType fromString(String value) {
        return AppointmentType.valueOf(value.toUpperCase());
    }
}
