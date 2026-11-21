package com.financialhome.financial_home_backend.Accounts.repository;

import com.financialhome.financial_home_backend.Accounts.entity.FinancialAccount;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<FinancialAccount, UUID> {
    List<FinancialAccount> findAllByOwnerHousehold_Id(UUID householdId);
}
