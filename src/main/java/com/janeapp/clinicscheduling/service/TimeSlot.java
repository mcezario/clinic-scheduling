package com.janeapp.clinicscheduling.service;

import java.time.Instant;

public record TimeSlot(Instant start, Instant end) {

    boolean overlapsWith(TimeSlot other) {
        return (other.start.isBefore(this.end) && other.end.isAfter(this.start));
    }
}
