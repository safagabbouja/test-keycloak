package com.keycloakk.example.keycloak_exemple.repositories;


import com.keycloakk.example.keycloak_exemple.model.Store;
import com.keycloakk.example.keycloak_exemple.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, String> {

    List<Store> findByMerchant(User merchant);

    List<Store> findByNameContainingIgnoreCase(String name);
}

