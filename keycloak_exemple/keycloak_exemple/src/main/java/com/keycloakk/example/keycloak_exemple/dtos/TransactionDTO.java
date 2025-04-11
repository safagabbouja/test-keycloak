package com.keycloakk.example.keycloak_exemple.dtos;


import com.keycloakk.example.keycloak_exemple.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {

    private long id;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private TransactionStatus status;
    private String customerId;
    private long storeId;
}

