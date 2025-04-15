package com.keycloakk.example.keycloak_exemple.services;

import com.keycloakk.example.keycloak_exemple.dtos.TransactionCreationDTO;
import com.keycloakk.example.keycloak_exemple.dtos.TransactionDTO;
import com.keycloakk.example.keycloak_exemple.exception.ResourceNotFoundException;
import com.keycloakk.example.keycloak_exemple.exception.UnauthorizedOperationException;
import com.keycloakk.example.keycloak_exemple.model.*;
import com.keycloakk.example.keycloak_exemple.repositories.StoreRepository;
import com.keycloakk.example.keycloak_exemple.repositories.TransactionRepository;
import com.keycloakk.example.keycloak_exemple.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

@Transactional
public TransactionDTO createTransaction(TransactionCreationDTO transactionCreationDTO, String customerId) {
    // Use findByKeycloakId to fetch the user
    User customer = userRepository.findByKeycloakId(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with keycloakId: " + customerId));

    if (customer.getRole() != UserRole.CUSTOMER) {
        throw new UnauthorizedOperationException("Only customers can create transactions");
    }

    Store store = storeRepository.findById(transactionCreationDTO.getStoreId())
            .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + transactionCreationDTO.getStoreId()));

    Transaction transaction = Transaction.createTransaction(
            transactionCreationDTO.getAmount(),
            transactionCreationDTO.getDescription(),
            customer,
            store
    );

    Transaction savedTransaction = transactionRepository.save(transaction);
    return mapToDTO(savedTransaction);
}

    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(String id, String userId, UserRole role) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        // Verify that the user has access to this transaction
        if (role == UserRole.CUSTOMER && !transaction.getCustomer().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only view your own transactions");
        } else if (role == UserRole.MERCHANT && !transaction.getStore().getMerchant().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only view transactions from your stores");
        }

        return mapToDTO(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByCustomer(String customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        return transactionRepository.findByCustomer(customer).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByStore(String storeId, String merchantId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));

        // Verify that the merchant owns the store
        if (!store.getMerchant().getId().equals(merchantId)) {
            throw new UnauthorizedOperationException("You can only view transactions from your own stores");
        }

        return transactionRepository.findByStore(store).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end, String storeId, String merchantId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));

        // Verify that the merchant owns the store
        if (!store.getMerchant().getId().equals(merchantId)) {
            throw new UnauthorizedOperationException("You can only view transactions from your own stores");
        }

        return transactionRepository.findByStoreAndTransactionDateBetween(store, start, end).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionDTO updateTransactionStatus(String id, TransactionStatus status, String merchantId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        // Verify that the merchant owns the store associated with the transaction
        if (!transaction.getStore().getMerchant().getId().equals(merchantId)) {
            throw new UnauthorizedOperationException("You can only update transactions from your own stores");
        }

        transaction.setStatus(status);
        Transaction updatedTransaction = transactionRepository.save(transaction);
        return mapToDTO(updatedTransaction);
    }

    private TransactionDTO mapToDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(), // No conversion needed
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                transaction.getStatus(),
                String.valueOf(transaction.getCustomer().getId()), // Convert Long to String
                transaction.getStore().getId() // No conversion needed
        );
    }
}

