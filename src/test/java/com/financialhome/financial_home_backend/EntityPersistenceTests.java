package com.financialhome.financial_home_backend;

import com.financialhome.financial_home_backend.Accounts.entity.FinancialAccount;
import com.financialhome.financial_home_backend.Accounts.entity.HouseholdAccount;
import com.financialhome.financial_home_backend.Cards.entity.Card;
import com.financialhome.financial_home_backend.Household.entity.Household;
import com.financialhome.financial_home_backend.Household.entity.HouseholdInvitation;
import com.financialhome.financial_home_backend.Household.entity.HouseholdMember;
import com.financialhome.financial_home_backend.People.entity.Person;
import com.financialhome.financial_home_backend.Transactions.entity.FinancialTransaction;
import com.financialhome.financial_home_backend.Transactions.entity.TransactionEntry;
import com.financialhome.financial_home_backend.Users.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("db-test")
@Transactional
class EntityPersistenceTests {
    @Autowired
    EntityManager entityManager;

    @Test
    void persistsAndReloadsAllTenEntitiesAndTheirRelationships() {
        var model = createModel();
        var member = persist(new HouseholdMember(model.household(), model.person(), "FATHER", null));
        var invitation = persist(new HouseholdInvitation(model.household(), model.invitedUser(),
                model.user(), "MEMBER", Instant.now().plus(1, ChronoUnit.DAYS)));
        var link = persist(new HouseholdAccount(model.account(), model.household()));
        var entry = persist(new TransactionEntry(model.transaction(), model.account(), "DEBIT_CARD",
                model.card(), new BigDecimal("-100.50")));
        var reversal = persist(new FinancialTransaction(model.household(), "REVERSAL", "Correction",
                model.person(), model.person(), model.transaction(), Instant.now()));

        entityManager.flush();
        entityManager.clear();

        var user = entityManager.find(User.class, model.user().getId());
        assertEquals("owner@example.test", user.getEmail());
        assertNull(user.getPasswordHash());
        assertEquals("ACTIVE", user.getStatus());
        assertNotNull(user.getCreatedAt());

        var person = entityManager.find(Person.class, model.person().getId());
        assertEquals(user.getId(), person.getUser().getId());
        assertEquals("Alex", person.getName());
        assertEquals("Smith", person.getLastName());
        assertEquals("UNSPECIFIED", person.getSex());

        var household = entityManager.find(Household.class, model.household().getId());
        assertEquals("Family home", household.getName());
        assertEquals("ACTIVE", household.getStatus());

        var storedMember = entityManager.find(HouseholdMember.class, member.getId());
        assertEquals(household.getId(), storedMember.getHousehold().getId());
        assertEquals(person.getId(), storedMember.getPerson().getId());
        assertEquals("FATHER", storedMember.getFamilyRole());
        assertEquals("NO_ACCESS", storedMember.getAccessRole());
        assertNotNull(storedMember.getJoinedAt());
        assertNull(storedMember.getLeftAt());

        var storedInvitation = entityManager.find(HouseholdInvitation.class, invitation.getId());
        assertEquals(household.getId(), storedInvitation.getHousehold().getId());
        assertEquals(model.invitedUser().getId(), storedInvitation.getInvitedUser().getId());
        assertEquals(user.getId(), storedInvitation.getInvitedByUser().getId());
        assertEquals("MEMBER", storedInvitation.getProposedRole());
        assertEquals("PENDING", storedInvitation.getStatus());
        assertTrue(storedInvitation.getExpiresAt().isAfter(storedInvitation.getCreatedAt()));
        assertNull(storedInvitation.getRespondedAt());

        var account = entityManager.find(FinancialAccount.class, model.account().getId());
        assertEquals("Shared account", account.getName());
        assertEquals("BANK", account.getType());
        assertEquals("HOUSEHOLD", account.getOwnerType());
        assertEquals(household.getId(), account.getOwnerHousehold().getId());
        assertNull(account.getOwnerPerson());
        assertEquals("MXN", account.getCurrency());
        assertNotNull(account.getCreatedAt());

        var storedLink = entityManager.find(HouseholdAccount.class, link.getId());
        assertEquals(account.getId(), storedLink.getAccount().getId());
        assertEquals(household.getId(), storedLink.getHousehold().getId());
        assertNotNull(storedLink.getLinkedAt());

        var card = entityManager.find(Card.class, model.card().getId());
        assertEquals(account.getId(), card.getAccount().getId());
        assertEquals("Daily card", card.getName());
        assertEquals("PHYSICAL", card.getType());
        assertEquals("1234", card.getLastFourDigits());
        assertEquals("ACTIVE", card.getStatus());
        assertNotNull(card.getCreatedAt());

        var transaction = entityManager.find(FinancialTransaction.class, model.transaction().getId());
        assertEquals(household.getId(), transaction.getHousehold().getId());
        assertEquals("EXPENSE", transaction.getType());
        assertEquals("DRAFT", transaction.getStatus());
        assertEquals("Groceries", transaction.getDescription());
        assertEquals(person.getId(), transaction.getRecordedByPerson().getId());
        assertEquals(person.getId(), transaction.getPerformedByPerson().getId());
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), transaction.getTransactionAt());
        assertNotNull(transaction.getRecordedAt());
        assertNull(transaction.getConfirmedAt());
        assertNull(transaction.getReversedTransaction());

        var storedReversal = entityManager.find(FinancialTransaction.class, reversal.getId());
        assertEquals("REVERSAL", storedReversal.getType());
        assertEquals(transaction.getId(), storedReversal.getReversedTransaction().getId());

        var storedEntry = entityManager.find(TransactionEntry.class, entry.getId());
        assertEquals(transaction.getId(), storedEntry.getTransaction().getId());
        assertEquals(account.getId(), storedEntry.getAccount().getId());
        assertEquals(card.getId(), storedEntry.getCard().getId());
        assertEquals("DEBIT_CARD", storedEntry.getPaymentMethod());
        assertEquals(new BigDecimal("-100.50"), storedEntry.getAmount());
    }

    @Test
    void persistsOptionalRelationshipsAndPersonalAccounts() {
        var model = createModel();
        var person = persist(new Person(null, "Guest", null, null));
        var personalAccount = persist(new FinancialAccount("Savings", "SAVINGS", person, null, "USD"));
        var link = persist(new HouseholdAccount(personalAccount, model.household()));
        var entry = persist(new TransactionEntry(model.transaction(), personalAccount, null, null,
                new BigDecimal("25.00")));
        var card = persist(new Card(personalAccount, "Virtual card", "DIGITAL", null));

        entityManager.flush();
        entityManager.clear();

        var storedPerson = entityManager.find(Person.class, person.getId());
        assertNull(storedPerson.getUser());
        assertNull(storedPerson.getLastName());
        assertNull(storedPerson.getSex());
        var account = entityManager.find(FinancialAccount.class, personalAccount.getId());
        assertEquals("PERSON", account.getOwnerType());
        assertEquals(person.getId(), account.getOwnerPerson().getId());
        assertNull(account.getOwnerHousehold());
        assertEquals("USD", account.getCurrency());
        assertEquals(account.getId(), entityManager.find(HouseholdAccount.class, link.getId())
                .getAccount().getId());
        var storedEntry = entityManager.find(TransactionEntry.class, entry.getId());
        assertNull(storedEntry.getCard());
        assertNull(storedEntry.getPaymentMethod());
        assertNull(entityManager.find(Card.class, card.getId()).getLastFourDigits());
    }

    @Test
    void readsLifecycleFieldsFromExistingRows() {
        var model = createModel();
        var member = persist(new HouseholdMember(model.household(), model.person(), null, "MEMBER"));
        var invitation = persist(new HouseholdInvitation(model.household(), model.invitedUser(),
                model.user(), "MEMBER", Instant.now().plus(1, ChronoUnit.DAYS)));
        entityManager.flush();

        // Exercise nullable lifecycle columns without implementing business transitions.
        entityManager.createNativeQuery("""
                UPDATE household_members SET left_at = joined_at + interval '1 day' WHERE id = :id
                """).setParameter("id", member.getId()).executeUpdate();
        entityManager.createNativeQuery("""
                UPDATE household_invitations SET status = 'ACCEPTED', responded_at = created_at
                WHERE id = :id
                """).setParameter("id", invitation.getId()).executeUpdate();
        entityManager.createNativeQuery("""
                UPDATE financial_transactions SET status = 'CONFIRMED', confirmed_at = recorded_at
                WHERE id = :id
                """).setParameter("id", model.transaction().getId()).executeUpdate();
        entityManager.clear();

        var storedMember = entityManager.find(HouseholdMember.class, member.getId());
        assertNull(storedMember.getFamilyRole());
        assertTrue(storedMember.getLeftAt().isAfter(storedMember.getJoinedAt()));
        var storedInvitation = entityManager.find(HouseholdInvitation.class, invitation.getId());
        assertEquals("ACCEPTED", storedInvitation.getStatus());
        assertNotNull(storedInvitation.getRespondedAt());
        var transaction = entityManager.find(FinancialTransaction.class, model.transaction().getId());
        assertEquals("CONFIRMED", transaction.getStatus());
        assertNotNull(transaction.getConfirmedAt());
    }

    @Test
    void rejectsAccountsWithoutExactlyOneOwner() {
        var household = new Household("Family home");
        var person = new Person(null, "Alex", null, null);

        assertThrows(IllegalArgumentException.class,
                () -> new FinancialAccount("Invalid", "BANK", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new FinancialAccount("Invalid", "BANK", person, household, null));
    }

    @Test
    void rejectsCardFromAnotherAccountThroughCompositeForeignKey() {
        var model = createModel();
        var anotherAccount = persist(new FinancialAccount("Other account", "BANK", null,
                model.household(), null));
        entityManager.flush();

        persist(new TransactionEntry(model.transaction(), anotherAccount, "DEBIT_CARD",
                model.card(), new BigDecimal("-10.00")));

        rejectsFlushWithSqlState("23503");
    }

    @Test
    void rejectsDuplicateHouseholdMembership() {
        var model = createModel();
        persist(new HouseholdMember(model.household(), model.person(), null, "ADMIN"));
        entityManager.flush();

        persist(new HouseholdMember(model.household(), model.person(), null, "MEMBER"));

        rejectsFlushWithSqlState("23505");
    }

    @Test
    void rejectsDuplicatePendingInvitationsThroughPartialIndex() {
        var model = createModel();
        var expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);
        persist(new HouseholdInvitation(model.household(), model.invitedUser(), model.user(),
                "MEMBER", expiresAt));
        entityManager.flush();

        persist(new HouseholdInvitation(model.household(), model.invitedUser(), model.user(),
                "MEMBER", expiresAt));

        rejectsFlushWithSqlState("23505");
    }

    @Test
    void rejectsSecondPersonForTheSameUser() {
        var model = createModel();
        entityManager.flush();

        persist(new Person(model.user(), "Duplicate", null, null));

        rejectsFlushWithSqlState("23505");
    }

    @Test
    void rejectsCardWithoutDebitCardPaymentMethod() {
        var model = createModel();
        entityManager.flush();

        persist(new TransactionEntry(model.transaction(), model.account(), "CASH", model.card(),
                new BigDecimal("-10.00")));

        rejectsFlushWithSqlState("23514");
    }

    @Test
    void rejectsZeroAmount() {
        var model = createModel();
        entityManager.flush();

        persist(new TransactionEntry(model.transaction(), model.account(), null, null, BigDecimal.ZERO));

        rejectsFlushWithSqlState("23514");
    }

    private Model createModel() {
        var user = persist(new User("owner@example.test", null));
        var invitedUser = persist(new User("invitee@example.test", null));
        var person = persist(new Person(user, "Alex", "Smith", "UNSPECIFIED"));
        var household = persist(new Household("Family home"));
        var account = persist(new FinancialAccount("Shared account", "BANK", null, household, null));
        var card = persist(new Card(account, "Daily card", "PHYSICAL", "1234"));
        var transaction = persist(new FinancialTransaction(household, "EXPENSE", "Groceries",
                person, person, null, Instant.parse("2026-01-01T12:00:00Z")));
        return new Model(user, invitedUser, person, household, account, card, transaction);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private void rejectsFlushWithSqlState(String expectedState) {
        var error = assertThrows(PersistenceException.class, entityManager::flush);
        Throwable cause = error;
        while (cause != null && !(cause instanceof SQLException)) {
            cause = cause.getCause();
        }
        var sqlError = assertInstanceOf(SQLException.class, cause);
        assertEquals(expectedState, sqlError.getSQLState());
    }

    private record Model(User user, User invitedUser, Person person, Household household,
            FinancialAccount account, Card card, FinancialTransaction transaction) {
    }
}
