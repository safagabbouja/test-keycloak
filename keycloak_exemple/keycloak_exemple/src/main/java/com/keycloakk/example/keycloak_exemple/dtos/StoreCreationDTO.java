package com.keycloakk.example.keycloak_exemple.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreCreationDTO {

    @NotBlank(message = "Store name is required")
    private String name;

    private String description;

    private String address;
}

