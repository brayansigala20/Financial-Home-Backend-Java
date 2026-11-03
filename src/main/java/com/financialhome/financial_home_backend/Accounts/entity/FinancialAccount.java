package com.financialhome.financial_home_backend.Accounts.entity;

import com.financialhome.financial_home_backend.Household.entity.Household;
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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "financial_accounts")
@Getter
public class FinancialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "owner_type", nullable = false, length = 20)
    private String ownerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_person_id")
    private Person ownerPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_household_id")
    private Household ownerHousehold;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FinancialAccount() {
        // Required by JPA.
    }

    public FinancialAccount(String name, String type, Person ownerPerson,
            Household ownerHousehold, String currency) {
        if ((ownerPerson == null) == (ownerHousehold == null)) {
            throw new IllegalArgumentException("An account must have exactly one owner: person or household");
        }

        this.name = Objects.requireNonNull(name, "Account name is required");
        this.type = Objects.requireNonNull(type, "Account type is required");
        this.ownerPerson = ownerPerson;
        this.ownerHousehold = ownerHousehold;
        this.ownerType = ownerPerson == null ? "HOUSEHOLD" : "PERSON";
        this.currency = currency == null ? "MXN" : currency;
        this.createdAt = Instant.now();
    }
}
