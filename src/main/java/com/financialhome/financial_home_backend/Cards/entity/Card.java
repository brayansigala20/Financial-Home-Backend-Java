package com.financialhome.financial_home_backend.Cards.entity;

import com.financialhome.financial_home_backend.Accounts.entity.FinancialAccount;
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
@Table(name = "cards", uniqueConstraints = @UniqueConstraint(
        name = "cards_id_account_uk", columnNames = {"id", "account_id"}))
@Getter
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private FinancialAccount account;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "last_four_digits", length = 4)
    private String lastFourDigits;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Card() {
        // Required by JPA.
    }

    public Card(FinancialAccount account, String name, String type, String lastFourDigits) {
        this.account = Objects.requireNonNull(account, "Account is required");
        this.name = Objects.requireNonNull(name, "Card name is required");
        this.type = Objects.requireNonNull(type, "Card type is required");
        this.lastFourDigits = lastFourDigits;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }
}
