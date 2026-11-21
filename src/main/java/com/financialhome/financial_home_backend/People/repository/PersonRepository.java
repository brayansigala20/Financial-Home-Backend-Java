package com.financialhome.financial_home_backend.People.repository;

import com.financialhome.financial_home_backend.People.entity.Person;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    Optional<Person> findByUserIdAndUserStatus(UUID userId, String status);
}
