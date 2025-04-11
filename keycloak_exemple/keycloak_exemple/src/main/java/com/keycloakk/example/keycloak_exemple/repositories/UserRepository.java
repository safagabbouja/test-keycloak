package com.keycloakk.example.keycloak_exemple.repositories;



import com.keycloakk.example.keycloak_exemple.model.User;
import com.keycloakk.example.keycloak_exemple.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);
    List<User> findByRole(UserRole role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}


