package com.financialhome.financial_home_backend.Accounts.entity;

import com.financialhome.financial_home_backend.Household.entity.Household;
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
@Table(name = "household_accounts", uniqueConstraints = @UniqueConstraint(
        name = "household_accounts_uk", columnNames = {"account_id", "household_id"}))
@Getter
public class HouseholdAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private FinancialAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    protected HouseholdAccount() {
        // Required by JPA.
    }

    public HouseholdAccount(FinancialAccount account, Household household) {
        this.account = Objects.requireNonNull(account, "Account is required");
        this.household = Objects.requireNonNull(household, "Household is required");
        this.linkedAt = Instant.now();
    }
}
