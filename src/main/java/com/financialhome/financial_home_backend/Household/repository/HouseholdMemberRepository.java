package com.financialhome.financial_home_backend.Household.repository;

import com.financialhome.financial_home_backend.Household.entity.HouseholdMember;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {
}
