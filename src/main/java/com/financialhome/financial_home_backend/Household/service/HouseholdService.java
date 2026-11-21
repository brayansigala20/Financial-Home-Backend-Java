package com.financialhome.financial_home_backend.Household.service;

import com.financialhome.financial_home_backend.Household.entity.Household;
import com.financialhome.financial_home_backend.Household.entity.HouseholdMember;
import com.financialhome.financial_home_backend.Household.repository.HouseholdMemberRepository;
import com.financialhome.financial_home_backend.Household.repository.HouseholdRepository;
import com.financialhome.financial_home_backend.People.entity.Person;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;

    public HouseholdService(HouseholdRepository householdRepository,
            HouseholdMemberRepository householdMemberRepository) {
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<Household> listVisibleHouseholds(UUID personId) {
        return householdMemberRepository.findVisibleHouseholds(personId);
    }

    @Transactional(readOnly = true)
    public Optional<Household> findVisibleHousehold(UUID personId, UUID householdId) {
        return householdMemberRepository.findVisibleHousehold(personId, householdId);
    }

    @Transactional
    public Household createHousehold(String name, Person creator) {
        if (creator == null || creator.getId() == null || creator.getUser() == null) {
            throw new IllegalArgumentException("Creator must be an existing person with a user account");
        }

        Household household = householdRepository.save(new Household(name));
        householdMemberRepository.save(new HouseholdMember(household, creator, null, "ADMIN"));
        return household;
    }
}
