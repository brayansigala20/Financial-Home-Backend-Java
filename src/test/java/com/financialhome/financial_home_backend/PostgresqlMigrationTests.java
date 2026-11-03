package com.financialhome.financial_home_backend;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import com.financialhome.financial_home_backend.Household.entity.Household;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("db-test")
class PostgresqlMigrationTests {

    @Autowired
    DataSource dataSource;

    @Autowired
    Flyway flyway;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndReloadsHouseholdWithInitialValues() {
        var household = new Household("Family home");

        entityManager.persist(household);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(household.getId());
        var stored = entityManager.find(Household.class, household.getId());
        assertNotNull(stored);
        assertEquals("Family home", stored.getName());
        assertEquals("ACTIVE", stored.getStatus());
        assertNotNull(stored.getCreatedAt());
        // PostgreSQL stores microsecond precision; Instant supports nanoseconds.
        assertTrue(Duration.between(household.getCreatedAt(), stored.getCreatedAt())
                .abs().compareTo(Duration.ofMillis(1)) < 0);
    }

    @Test
    void rejectsInvalidHouseholdNames() {
        assertThrows(IllegalArgumentException.class, () -> new Household(null));
        assertThrows(IllegalArgumentException.class, () -> new Household(" "));
        assertThrows(IllegalArgumentException.class, () -> new Household("a".repeat(151)));
        assertDoesNotThrow(() -> new Household("a".repeat(150)));
    }

    @Test
    void createsExactlyTenModelTablesAndAllForeignKeys() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            var tables = new HashSet<String>();
            try (var result = statement.executeQuery("""
                    SELECT tablename FROM pg_tables WHERE schemaname = 'public'
                    """)) {
                while (result.next()) {
                    tables.add(result.getString(1));
                }
            }
            assertEquals(Set.of("users", "people", "households", "household_members",
                    "household_invitations", "financial_accounts", "household_accounts", "cards",
                    "financial_transactions", "transaction_entries", "flyway_schema_history"), tables);

            var relations = new HashSet<String>();
            try (var result = statement.executeQuery("""
                    SELECT conrelid::regclass::text || ':' || pg_get_constraintdef(oid)
                    FROM pg_constraint WHERE contype = 'f' AND connamespace = 'public'::regnamespace
                    """)) {
                while (result.next()) {
                    relations.add(result.getString(1));
                }
            }
            assertEquals(Set.of(
                    "people:FOREIGN KEY (user_id) REFERENCES users(id)",
                    "household_members:FOREIGN KEY (household_id) REFERENCES households(id)",
                    "household_members:FOREIGN KEY (person_id) REFERENCES people(id)",
                    "household_invitations:FOREIGN KEY (household_id) REFERENCES households(id)",
                    "household_invitations:FOREIGN KEY (invited_user_id) REFERENCES users(id)",
                    "household_invitations:FOREIGN KEY (invited_by_user_id) REFERENCES users(id)",
                    "financial_accounts:FOREIGN KEY (owner_person_id) REFERENCES people(id)",
                    "financial_accounts:FOREIGN KEY (owner_household_id) REFERENCES households(id)",
                    "household_accounts:FOREIGN KEY (account_id) REFERENCES financial_accounts(id)",
                    "household_accounts:FOREIGN KEY (household_id) REFERENCES households(id)",
                    "cards:FOREIGN KEY (account_id) REFERENCES financial_accounts(id)",
                    "financial_transactions:FOREIGN KEY (household_id) REFERENCES households(id)",
                    "financial_transactions:FOREIGN KEY (recorded_by_person_id) REFERENCES people(id)",
                    "financial_transactions:FOREIGN KEY (performed_by_person_id) REFERENCES people(id)",
                    "financial_transactions:FOREIGN KEY (reversed_transaction_id) REFERENCES financial_transactions(id)",
                    "transaction_entries:FOREIGN KEY (transaction_id) REFERENCES financial_transactions(id)",
                    "transaction_entries:FOREIGN KEY (account_id) REFERENCES financial_accounts(id)",
                    "transaction_entries:FOREIGN KEY (card_id, account_id) REFERENCES cards(id, account_id)"
            ), relations);
        }
    }

    @Test
    void validatesMigrationAndDoesNotApplyItTwice() {
        flyway.validate();
        assertEquals("1", flyway.info().current().getVersion().toString());
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    @Test
    void acceptsValidDataAndRejectsInvalidRelationshipsAndChecks() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                execute(connection, """
                        INSERT INTO users (id, email) VALUES
                          ('00000000-0000-0000-0000-000000000001', 'migration@example.test');
                        INSERT INTO people (id, user_id, name) VALUES
                          ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Test');
                        INSERT INTO households (id, name) VALUES
                          ('00000000-0000-0000-0000-000000000003', 'Test');
                        INSERT INTO household_members (id, household_id, person_id) VALUES
                          ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002');
                        INSERT INTO financial_accounts (id, name, type, owner_type, owner_household_id) VALUES
                          ('00000000-0000-0000-0000-000000000005', 'One', 'BANK', 'HOUSEHOLD', '00000000-0000-0000-0000-000000000003'),
                          ('00000000-0000-0000-0000-000000000006', 'Two', 'BANK', 'HOUSEHOLD', '00000000-0000-0000-0000-000000000003');
                        INSERT INTO cards (id, account_id, name, type) VALUES
                          ('00000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000005', 'Test', 'PHYSICAL');
                        INSERT INTO financial_transactions
                          (id, household_id, type, description, recorded_by_person_id, performed_by_person_id, transaction_at) VALUES
                          ('00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000003', 'EXPENSE', 'Test',
                           '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', now());
                        INSERT INTO transaction_entries (id, transaction_id, account_id, payment_method, card_id, amount) VALUES
                          ('00000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000008',
                           '00000000-0000-0000-0000-000000000005', 'DEBIT_CARD', '00000000-0000-0000-0000-000000000007', -100);
                        """);
                rejects(connection, "23514", "UPDATE transaction_entries SET payment_method = NULL");
                rejects(connection, "23514", "UPDATE transaction_entries SET payment_method = 'CASH'");
                rejects(connection, "23503", "UPDATE transaction_entries SET account_id = '00000000-0000-0000-0000-000000000006'");
                rejects(connection, "23514", "UPDATE transaction_entries SET amount = 0");
                rejects(connection, "23514", "UPDATE financial_accounts SET owner_household_id = NULL");
                rejects(connection, "23514", "UPDATE users SET status = 'UNKNOWN'");
                rejects(connection, "23514", "UPDATE financial_transactions SET status = 'CONFIRMED'");
                rejects(connection, "23505", """
                        INSERT INTO household_members (id, household_id, person_id) VALUES
                        ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002')
                        """);
            } finally {
                connection.rollback();
            }
        }
    }

    private void rejects(Connection connection, String sqlState, String sql) throws SQLException {
        var savepoint = connection.setSavepoint();
        try {
            var exception = assertThrows(SQLException.class, () -> execute(connection, sql));
            assertEquals(sqlState, exception.getSQLState(), sql);
        } finally {
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
