package com.financialhome.financial_home_backend.Household.repository;

import com.financialhome.financial_home_backend.Household.entity.Household;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
}
