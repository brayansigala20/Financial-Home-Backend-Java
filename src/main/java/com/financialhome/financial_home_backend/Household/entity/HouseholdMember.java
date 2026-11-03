package com.financialhome.financial_home_backend.Household.entity;

import com.financialhome.financial_home_backend.People.entity.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "household_members", uniqueConstraints = @UniqueConstraint(
        name = "household_members_uk", columnNames = {"household_id", "person_id"}))
@Getter
public class HouseholdMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "family_role", length = 30)
    private String familyRole;

    @Column(name = "access_role", nullable = false, length = 20)
    private String accessRole;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected HouseholdMember() {
        // Required by JPA.
    }

    public HouseholdMember(Household household, Person person, String familyRole, String accessRole) {
        this.household = Objects.requireNonNull(household, "Household is required");
        this.person = Objects.requireNonNull(person, "Person is required");
        this.familyRole = familyRole;
        this.accessRole = accessRole == null ? "NO_ACCESS" : accessRole;
        this.joinedAt = Instant.now();
    }
}
