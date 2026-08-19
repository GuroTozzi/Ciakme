package com.ciakme.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
* Stai dicendo a Spring due cose:
* Questo repository gestisce l'entità User
* La chiave primaria di User è di tipo UUID
*/

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // SELECT COUNT(*) FROM users WHERE email = ?
    boolean existsByEmail(String email);

    // SELECT * FROM users WHERE role = ?
    List<User> findByRole(UserRole role);
}
