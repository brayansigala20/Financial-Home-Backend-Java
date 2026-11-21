package com.financialhome.financial_home_backend.Household.controller;

import com.financialhome.financial_home_backend.Household.entity.Household;
import com.financialhome.financial_home_backend.Household.service.HouseholdService;
import com.financialhome.financial_home_backend.People.entity.Person;
import com.financialhome.financial_home_backend.People.repository.PersonRepository;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/households")
public class HouseholdController {
    private final HouseholdService householdService;
    private final PersonRepository personRepository;

    public HouseholdController(HouseholdService householdService, PersonRepository personRepository) {
        this.householdService = householdService;
        this.personRepository = personRepository;
    }

    @GetMapping
    public List<HouseholdResponse> list(Principal principal) {
        return householdService.listVisibleHouseholds(activePerson(principal).getId()).stream()
                .map(HouseholdController::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public HouseholdResponse get(@PathVariable("id") UUID id, Principal principal) {
        return householdService.findVisibleHousehold(activePerson(principal).getId(), id)
                .map(HouseholdController::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdResponse create(@RequestBody CreateHouseholdRequest request, Principal principal) {
        Person creator = activePerson(principal);

        Household household;
        try {
            household = householdService.createHousehold(request.name(), creator);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        return toResponse(household);
    }

    private static HouseholdResponse toResponse(Household household) {
        return new HouseholdResponse(household.getId(), household.getName(), household.getStatus());
    }

    private Person activePerson(Principal principal) {
        return personRepository.findByUserIdAndUserStatus(authenticatedUserId(principal), "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "An active user account linked to a person is required"));
    }

    private UUID authenticatedUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    public record CreateHouseholdRequest(String name) {
    }

    public record HouseholdResponse(UUID id, String name, String status) {
    }
}
