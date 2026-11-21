package com.financialhome.financial_home_backend;

import com.financialhome.financial_home_backend.Accounts.entity.FinancialAccount;
import com.financialhome.financial_home_backend.Accounts.entity.HouseholdAccount;
import com.financialhome.financial_home_backend.Accounts.repository.AccountRepository;
import com.financialhome.financial_home_backend.Household.entity.Household;
import com.financialhome.financial_home_backend.People.entity.Person;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("db-test")
@Transactional
class AccountRepositoryTests {
    @Autowired
    AccountRepository accounts;

    @Autowired
    EntityManager entityManager;

    @Test
    void returnsOnlyAccountsOwnedByTheRequestedHousehold() {
        Household household = new Household("Requested home");
        Household otherHousehold = new Household("Other home");
        Person person = new Person(null, "Account owner", null, null);
        entityManager.persist(household);
        entityManager.persist(otherHousehold);
        entityManager.persist(person);

        FinancialAccount first = accounts.save(new FinancialAccount("Cash", "CASH", null, household, "MXN"));
        FinancialAccount second = accounts.save(new FinancialAccount("Savings", "SAVINGS", null, household, "MXN"));
        accounts.save(new FinancialAccount("Other bank", "BANK", null, otherHousehold, "MXN"));
        FinancialAccount personal = accounts.save(new FinancialAccount("Personal bank", "BANK", person, null, "MXN"));
        entityManager.persist(new HouseholdAccount(personal, household));
        entityManager.flush();
        entityManager.clear();

        assertThat(accounts.findAllByOwnerHousehold_Id(household.getId()))
                .extracting(FinancialAccount::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }
}
