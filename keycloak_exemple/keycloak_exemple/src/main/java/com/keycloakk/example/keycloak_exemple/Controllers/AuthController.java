package com.keycloakk.example.keycloak_exemple.Controllers;

import com.keycloakk.example.keycloak_exemple.dtos.UserCreationDTO;
import com.keycloakk.example.keycloak_exemple.dtos.UserDTO;
import com.keycloakk.example.keycloak_exemple.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody UserCreationDTO userCreationDTO) {
        UserDTO createdUser = userService.createUser(userCreationDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }
}