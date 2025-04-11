package com.keycloakk.example.keycloak_exemple.Controllers;

import com.keycloakk.example.keycloak_exemple.dtos.TransactionCreationDTO;
import com.keycloakk.example.keycloak_exemple.dtos.TransactionDTO;
import com.keycloakk.example.keycloak_exemple.model.TransactionStatus;
import com.keycloakk.example.keycloak_exemple.model.UserRole;
import com.keycloakk.example.keycloak_exemple.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionDTO> createTransaction(
            @Valid @RequestBody TransactionCreationDTO transactionCreationDTO,
            @AuthenticationPrincipal Jwt jwt) {
        String customerId = jwt.getSubject();
        TransactionDTO createdTransaction = transactionService.createTransaction(transactionCreationDTO, customerId);
        return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<TransactionDTO> getTransactionById(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        // Adapter cette ligne pour extraire le rôle du JWT selon votre configuration
        UserRole role = extractRoleFromJwt(jwt);
        return ResponseEntity.ok(transactionService.getTransactionById(id, userId, role));
    }

    private UserRole extractRoleFromJwt(Jwt jwt) {
        // Adapter cette méthode selon la structure de votre JWT
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles.contains("ADMIN")) return UserRole.ADMIN;
        if (roles.contains("MERCHANT")) return UserRole.MERCHANT;
        if (roles.contains("CUSTOMER")) return UserRole.CUSTOMER;
        return null;
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByCustomer(@AuthenticationPrincipal Jwt jwt) {
        String customerId = jwt.getSubject();
        return ResponseEntity.ok(transactionService.getTransactionsByCustomer(customerId));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByStore(
            @PathVariable String storeId,
            @AuthenticationPrincipal Jwt jwt) {
        String merchantId = jwt.getSubject();
        return ResponseEntity.ok(transactionService.getTransactionsByStore(storeId, merchantId));
    }

    @GetMapping("/store/{storeId}/date-range")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateRange(
            @PathVariable String storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @AuthenticationPrincipal Jwt jwt) {
        String merchantId = jwt.getSubject();
        return ResponseEntity.ok(transactionService.getTransactionsByDateRange(start, end, storeId, merchantId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<TransactionDTO> updateTransactionStatus(
            @PathVariable String id,
            @RequestParam TransactionStatus status,
            @AuthenticationPrincipal Jwt jwt) {
        String merchantId = jwt.getSubject();
        return ResponseEntity.ok(transactionService.updateTransactionStatus(id, status, merchantId));
    }
}

