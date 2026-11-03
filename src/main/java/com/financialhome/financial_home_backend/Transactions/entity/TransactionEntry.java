package com.financialhome.financial_home_backend.Transactions.entity;

import com.financialhome.financial_home_backend.Accounts.entity.FinancialAccount;
import com.financialhome.financial_home_backend.Cards.entity.Card;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "transaction_entries")
@Getter
public class TransactionEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private FinancialTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private FinancialAccount account;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    // The SQL composite foreign key also requires the card to belong to account_id.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    protected TransactionEntry() {
        // Required by JPA.
    }

    public TransactionEntry(FinancialTransaction transaction, FinancialAccount account,
            String paymentMethod, Card card, BigDecimal amount) {
        this.transaction = Objects.requireNonNull(transaction, "Transaction is required");
        this.account = Objects.requireNonNull(account, "Account is required");
        this.paymentMethod = paymentMethod;
        this.card = card;
        this.amount = Objects.requireNonNull(amount, "Amount is required");
    }
}
