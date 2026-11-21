package com.financialhome.financial_home_backend;

import com.financialhome.financial_home_backend.Household.controller.HouseholdController;
import com.financialhome.financial_home_backend.Household.entity.HouseholdMember;
import com.financialhome.financial_home_backend.Household.repository.HouseholdMemberRepository;
import com.financialhome.financial_home_backend.Household.repository.HouseholdRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("db-test")
class HouseholdCreationTests {
    @Autowired
    HouseholdController controller;

    @Autowired
    HouseholdRepository households;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    HouseholdMemberRepository members;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createsActiveHouseholdAndAdminMembership() throws Exception {
        UUID userId = createUserWithPerson();
        String name = UUID.randomUUID().toString();

        mvc.perform(post("/api/households").principal(userId::toString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertEquals("ADMIN", jdbc.queryForObject("""
                SELECT m.access_role FROM household_members m
                JOIN households h ON h.id = m.household_id
                JOIN people p ON p.id = m.person_id
                WHERE h.name = ? AND p.user_id = ? AND m.left_at IS NULL
                """, String.class, name, userId));
    }

    @Test
    void rejectsRequestsWithoutAuthenticatedUser() throws Exception {
        mvc.perform(post("/api/households").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Home\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUsersWithoutPerson() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email) VALUES (?, ?)", userId, userId + "@example.test");

        mvc.perform(post("/api/households").principal(userId::toString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Home\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsBlankNames() throws Exception {
        UUID userId = createUserWithPerson();
        mvc.perform(post("/api/households").principal(userId::toString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rollsBackHouseholdWhenMembershipSaveFails() {
        UUID userId = createUserWithPerson();
        String name = UUID.randomUUID().toString();
        var failure = new IllegalStateException("Simulated membership save failure");
        doAnswer(invocation -> {
            households.flush();
            assertEquals(1, countHouseholds(name));
            throw failure;
        }).when(members).save(any(HouseholdMember.class));

        var exception = assertThrows(jakarta.servlet.ServletException.class,
                () -> mvc.perform(post("/api/households").principal(userId::toString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")));
        assertEquals(failure, exception.getCause());
        assertEquals(0, countHouseholds(name));
    }

    private UUID createUserWithPerson() {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email) VALUES (?, ?)", userId, userId + "@example.test");
        jdbc.update("INSERT INTO people (id, user_id, name) VALUES (?, ?, ?)",
                UUID.randomUUID(), userId, "Test creator");
        return userId;
    }

    private int countHouseholds(String name) {
        return jdbc.queryForObject("SELECT count(*) FROM households WHERE name = ?", Integer.class, name);
    }
}
