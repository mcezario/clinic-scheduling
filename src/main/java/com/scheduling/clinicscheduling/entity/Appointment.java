package com.janeapp.clinicscheduling.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint( name = "unique_practitioner_schedule", columnNames = {"practitioner_id", "start_at", "end_at"} )
)
public class Appointment extends BaseEntity {

    @Enumerated( EnumType.STRING )
    @Column( name = "appointment_type", nullable = false, columnDefinition = "appointment_type_enum" )
    @ColumnTransformer( write = "?::appointment_type_enum" )
    private AppointmentType type;

    @Column( nullable = false, unique = true )
    private Instant startAt;

    @Column( nullable = false, unique = true )
    private Instant endAt;

    private String comment;

    @OneToOne
    @JoinColumn( name = "patient_id", referencedColumnName = "id", nullable = false )
    private Patient patient;

    @OneToOne
    @JoinColumn( name = "practitioner_id", nullable = false, unique = true )
    private Practitioner practitioner;

    @Version
    private Long version;

    public AppointmentType getType() {
        return type;
    }

    public void setType(AppointmentType type) {
        this.type = type;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(Practitioner practitioner) {
        this.practitioner = practitioner;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        removeSecondsFromScheduleTimes();
    }

    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        removeSecondsFromScheduleTimes();
    }

    private void removeSecondsFromScheduleTimes() {
        startAt = startAt.truncatedTo(ChronoUnit.MINUTES);
        endAt = endAt.truncatedTo(ChronoUnit.MINUTES);
    }
}
