package com.keycloakk.example.keycloak_exemple.services;


import com.keycloakk.example.keycloak_exemple.dtos.StoreCreationDTO;
import com.keycloakk.example.keycloak_exemple.dtos.StoreDTO;
import com.keycloakk.example.keycloak_exemple.exception.ResourceNotFoundException;
import com.keycloakk.example.keycloak_exemple.exception.UnauthorizedOperationException;
import com.keycloakk.example.keycloak_exemple.model.Store;
import com.keycloakk.example.keycloak_exemple.model.User;
import com.keycloakk.example.keycloak_exemple.model.UserRole;
import com.keycloakk.example.keycloak_exemple.repositories.StoreRepository;
import com.keycloakk.example.keycloak_exemple.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;

@Transactional
public StoreDTO createStore(StoreCreationDTO storeCreationDTO, String merchantId) {
    // Find the merchant by keycloakId
    User merchant = userRepository.findByKeycloakId(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with Keycloak ID: " + merchantId));

    // Fetch the user's roles from Keycloak
    UserRole keycloakRole = keycloakService.determineRoleFromKeycloak(merchantId);

    // Update the user's role in the database if it differs from Keycloak
    if (merchant.getRole() != keycloakRole) {
        merchant.setRole(keycloakRole);
        userRepository.save(merchant);
    }

    // Verify that the user is a merchant
    if (keycloakRole != UserRole.MERCHANT) {
        throw new UnauthorizedOperationException("Only merchants can create stores");
    }

    Store store = Store.createStore(
            storeCreationDTO.getName(),
            storeCreationDTO.getDescription(),
            storeCreationDTO.getAddress(),
            merchant
    );

    Store savedStore = storeRepository.save(store);
    return mapToDTO(savedStore);
}
    @Transactional(readOnly = true)
    public StoreDTO getStoreById(String id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + id));
        return mapToDTO(store);
    }

    @Transactional(readOnly = true)
    public List<StoreDTO> getAllStores() {
        return storeRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreDTO> getStoresByMerchant(String merchantId) {
        // Find the merchant by keycloakId instead of id
        User merchant = userRepository.findByKeycloakId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with Keycloak ID: " + merchantId));

        return storeRepository.findByMerchant(merchant).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreDTO> searchStoresByName(String name) {
        return storeRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public StoreDTO updateStore(String id, StoreDTO storeDTO, String merchantId) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + id));

        // Verify that the user is the owner of the store
        if (!store.getMerchant().getKeycloakId().equals(merchantId)) {
            throw new UnauthorizedOperationException("You can only update your own stores");
        }

        store.setName(storeDTO.getName());
        store.setDescription(storeDTO.getDescription());
        store.setAddress(storeDTO.getAddress());

        Store updatedStore = storeRepository.save(store);
        return mapToDTO(updatedStore);
    }

    @Transactional
    public void deleteStore(String id, String merchantId) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + id));

        // Verify that the user is the owner of the store
        if (!store.getMerchant().getKeycloakId().equals(merchantId)) {
            throw new UnauthorizedOperationException("You can only delete your own stores");
        }

        storeRepository.delete(store);
    }

    private StoreDTO mapToDTO(Store store) {
        return new StoreDTO(
                store.getId(),
                store.getName(),
                store.getDescription(),
                store.getAddress(),
                store.getMerchant().getId()
        );
    }
}

