package com.keycloakk.example.keycloak_exemple.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "store")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private User merchant;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    // Helper methods
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setStore(this);
    }

    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setStore(null);
    }

    // Factory method to create a new store
    public static Store createStore(String name, String description, String address, User merchant) {
        Store store = new Store();
        store.setName(name);
        store.setDescription(description);
        store.setAddress(address);
        store.setMerchant(merchant);
        return store;
    }
}