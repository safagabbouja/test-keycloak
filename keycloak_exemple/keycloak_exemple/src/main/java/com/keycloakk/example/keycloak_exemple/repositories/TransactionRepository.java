package com.keycloakk.example.keycloak_exemple.repositories;


import com.keycloakk.example.keycloak_exemple.model.Store;
import com.keycloakk.example.keycloak_exemple.model.Transaction;
import com.keycloakk.example.keycloak_exemple.model.TransactionStatus;
import com.keycloakk.example.keycloak_exemple.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByCustomer(User customer);

    List<Transaction> findByStore(Store store);

    List<Transaction> findByStoreAndTransactionDateBetween(Store store, LocalDateTime start, LocalDateTime end);

    List<Transaction> findByCustomerAndTransactionDateBetween(User customer, LocalDateTime start, LocalDateTime end);

    List<Transaction> findByStatus(TransactionStatus status);
}

