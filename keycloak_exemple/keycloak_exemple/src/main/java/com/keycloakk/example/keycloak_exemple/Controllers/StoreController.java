package com.keycloakk.example.keycloak_exemple.Controllers;

import com.keycloakk.example.keycloak_exemple.dtos.StoreCreationDTO;
import com.keycloakk.example.keycloak_exemple.dtos.StoreDTO;
import com.keycloakk.example.keycloak_exemple.services.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<StoreDTO> createStore(
            @Valid @RequestBody StoreCreationDTO storeCreationDTO,
            @AuthenticationPrincipal Jwt jwt) {
        String merchantId = jwt.getSubject();
        StoreDTO createdStore = storeService.createStore(storeCreationDTO, merchantId);
        return new ResponseEntity<>(createdStore, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDTO> getStoreById(@PathVariable String id) {
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @GetMapping
    public ResponseEntity<List<StoreDTO>> getAllStores() {
        return ResponseEntity.ok(storeService.getAllStores());
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<List<StoreDTO>> getStoresByMerchant(@AuthenticationPrincipal Jwt jwt) {
        String merchantId = jwt.getSubject();
        return ResponseEntity.ok(storeService.getStoresByMerchant(merchantId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<StoreDTO>> searchStoresByName(@RequestParam String name) {
        return ResponseEntity.ok(storeService.searchStoresByName(name));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<StoreDTO> updateStore(
            @PathVariable String id,
            @RequestBody StoreDTO storeDTO,
            @AuthenticationPrincipal Jwt jwt) {
        String merchantId = jwt.getSubject();
        return ResponseEntity.ok(storeService.updateStore(id, storeDTO, merchantId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<Void> deleteStore(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String merchantId = jwt.getSubject();
        storeService.deleteStore(id, merchantId);
        return ResponseEntity.noContent().build();
    }
}

