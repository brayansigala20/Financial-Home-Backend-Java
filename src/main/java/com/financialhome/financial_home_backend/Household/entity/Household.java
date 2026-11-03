package com.financialhome.financial_home_backend.Household.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "households")
@Getter
public class Household {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Household() {
        // Required by JPA.
    }

    public Household(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Household name is required");
        }
        if (name.codePointCount(0, name.length()) > 150) {
            throw new IllegalArgumentException("Household name must not exceed 150 characters");
        }

        this.name = name;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }
}
