package com.keycloakk.example.keycloak_exemple.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreDTO {

    private Long id;
    private String name;
    private String description;
    private String address;
    private long merchantId;
}

