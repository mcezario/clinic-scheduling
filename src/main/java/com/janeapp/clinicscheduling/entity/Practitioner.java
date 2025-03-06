package com.janeapp.clinicscheduling.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.Set;

@Entity
public class Practitioner extends BaseEntity {

    @Column( nullable = false )
    private String firstName;

    @Column( nullable = false )
    private String lastName;

    @Column( nullable = false, unique = true )
    private String email;

    @Column( nullable = false )
    private String phone;

    @OneToMany( mappedBy = "practitioner", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
    private Set<PractitionerUnavailability> unavailability;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Set<PractitionerUnavailability> getUnavailability() {
        return unavailability;
    }

    public void setUnavailability(Set<PractitionerUnavailability> unavailability) {
        this.unavailability = unavailability;
    }

    public String fullName() {
        return String.format("%s %s", firstName, lastName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof Practitioner)) return false;
        Practitioner that = (Practitioner) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
