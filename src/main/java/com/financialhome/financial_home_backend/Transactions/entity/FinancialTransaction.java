package com.financialhome.financial_home_backend.Transactions.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "financial_transactions")
@Getter
public class FinancialTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_person_id", nullable = false)
    private Person recordedByPerson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by_person_id", nullable = false)
    private Person performedByPerson;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_transaction_id", unique = true)
    private FinancialTransaction reversedTransaction;

    @Column(name = "transaction_at", nullable = false)
    private Instant transactionAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected FinancialTransaction() {
        // Required by JPA.
    }

    public FinancialTransaction(Household household, String type, String description,
            Person recordedByPerson, Person performedByPerson,
            FinancialTransaction reversedTransaction, Instant transactionAt) {
        this.household = Objects.requireNonNull(household, "Household is required");
        this.type = Objects.requireNonNull(type, "Transaction type is required");
        this.description = Objects.requireNonNull(description, "Description is required");
        this.recordedByPerson = Objects.requireNonNull(recordedByPerson, "Recording person is required");
        this.performedByPerson = Objects.requireNonNull(performedByPerson, "Performing person is required");
        this.reversedTransaction = reversedTransaction;
        this.transactionAt = Objects.requireNonNull(transactionAt, "Transaction time is required");
        this.status = "DRAFT";
        this.recordedAt = Instant.now();
    }
}
