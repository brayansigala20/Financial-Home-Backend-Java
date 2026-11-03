package com.financialhome.financial_home_backend.People.entity;

import com.financialhome.financial_home_backend.Users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "people")
@Getter
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "sex", length = 20)
    private String sex;

    protected Person() {
        // Required by JPA.
    }

    public Person(User user, String name, String lastName, String sex) {
        this.user = user;
        this.name = Objects.requireNonNull(name, "Name is required");
        this.lastName = lastName;
        this.sex = sex;
    }
}
