package com.keycloakk.example.keycloak_exemple.dtos;



import com.keycloakk.example.keycloak_exemple.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
}

