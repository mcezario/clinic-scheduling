package com.janeapp.clinicscheduling.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint( name = "unique_practitioner_unavailability", columnNames = {"practitioner_id", "start_at", "end_at"} )
)
public class PractitionerUnavailability extends BaseEntity {

    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "practitioner_id", unique = true )
    private Practitioner practitioner;

    @Column( nullable = false, unique = true )
    private Instant startAt;

    @Column( nullable = false )
    private Instant endAt;

    private String reason;

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(Practitioner practitioner) {
        this.practitioner = practitioner;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
