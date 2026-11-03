package com.financialhome.financial_home_backend.Household.entity;

import com.financialhome.financial_home_backend.Users.entity.User;
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
@Table(name = "household_invitations")
@Getter
public class HouseholdInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private User invitedUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedByUser;

    @Column(name = "proposed_role", nullable = false, length = 20)
    private String proposedRole;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected HouseholdInvitation() {
        // Required by JPA.
    }

    public HouseholdInvitation(Household household, User invitedUser, User invitedByUser,
            String proposedRole, Instant expiresAt) {
        this.household = Objects.requireNonNull(household, "Household is required");
        this.invitedUser = Objects.requireNonNull(invitedUser, "Invited user is required");
        this.invitedByUser = Objects.requireNonNull(invitedByUser, "Inviting user is required");
        this.proposedRole = Objects.requireNonNull(proposedRole, "Proposed role is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration time is required");
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }
}
