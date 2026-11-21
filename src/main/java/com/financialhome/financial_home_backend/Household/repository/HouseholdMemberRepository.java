package com.financialhome.financial_home_backend.Household.repository;

import com.financialhome.financial_home_backend.Household.entity.Household;
import com.financialhome.financial_home_backend.Household.entity.HouseholdMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {
    String VISIBLE_HOUSEHOLDS = """
            select household
            from HouseholdMember member
            join member.household household
            where member.person.id = :personId
              and member.leftAt is null
              and member.accessRole in ('ADMIN', 'MEMBER')
              and household.status = 'ACTIVE'
            """;

    @Query(VISIBLE_HOUSEHOLDS + "order by household.name, household.id")
    List<Household> findVisibleHouseholds(@Param("personId") UUID personId);

    @Query(VISIBLE_HOUSEHOLDS + "and household.id = :householdId")
    Optional<Household> findVisibleHousehold(@Param("personId") UUID personId,
            @Param("householdId") UUID householdId);
}
