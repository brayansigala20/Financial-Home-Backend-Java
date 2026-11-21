package com.financialhome.financial_home_backend;

import com.financialhome.financial_home_backend.Household.controller.HouseholdController;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("db-test")
@Transactional
class HouseholdReadTests {
    @Autowired
    HouseholdController controller;

    @Autowired
    JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listsAndReadsActiveHouseholdsForAdminsAndMembers() throws Exception {
        UUID personId = createPersonWithActiveAccount();
        UUID adminHousehold = createHousehold("Zeta", "ACTIVE");
        UUID memberHousehold = createHousehold("Alfa", "ACTIVE");
        addMembership(adminHousehold, personId, "ADMIN", false);
        addMembership(memberHousehold, personId, "MEMBER", false);

        mvc.perform(get("/api/households").principal(personId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(memberHousehold.toString()))
                .andExpect(jsonPath("$[1].id").value(adminHousehold.toString()));

        mvc.perform(get("/api/households/{id}", memberHousehold).principal(personId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberHousehold.toString()))
                .andExpect(jsonPath("$.name").value("Alfa"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(get("/api/households/{id}", adminHousehold).principal(personId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminHousehold.toString()));
    }

    @Test
    void excludesInaccessibleHouseholdsFromBothEndpoints() throws Exception {
        UUID personId = createPersonWithActiveAccount();
        UUID archived = createHousehold("Archived", "ARCHIVED");
        UUID former = createHousehold("Former", "ACTIVE");
        UUID noAccess = createHousehold("No access", "ACTIVE");
        UUID unrelated = createHousehold("Unrelated", "ACTIVE");
        addMembership(archived, personId, "ADMIN", false);
        addMembership(former, personId, "MEMBER", true);
        addMembership(noAccess, personId, "NO_ACCESS", false);
        addMembership(unrelated, createPersonWithActiveAccount(), "ADMIN", false);

        mvc.perform(get("/api/households").principal(personId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        for (UUID id : new UUID[] {archived, former, noAccess, unrelated, UUID.randomUUID()}) {
            mvc.perform(get("/api/households/{id}", id).principal(personId::toString))
                    .andExpect(status().isNotFound());
        }
    }

    private UUID createPersonWithActiveAccount() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email) VALUES (?, ?)", id, id + "@example.test");
        jdbc.update("INSERT INTO people (id, user_id, name) VALUES (?, ?, ?)", id, id, "Test person");
        return id;
    }

    private UUID createHousehold(String name, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO households (id, name, status) VALUES (?, ?, ?)", id, name, status);
        return id;
    }

    private void addMembership(UUID householdId, UUID personId, String role, boolean former) {
        jdbc.update("""
                INSERT INTO household_members (id, household_id, person_id, access_role, left_at)
                VALUES (?, ?, ?, ?, CASE WHEN ? THEN now() END)
                """, UUID.randomUUID(), householdId, personId, role, former);
    }
}
