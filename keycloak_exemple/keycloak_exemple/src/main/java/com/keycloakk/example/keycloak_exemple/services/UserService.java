package com.keycloakk.example.keycloak_exemple.services;


import com.keycloakk.example.keycloak_exemple.model.UserRole;

import com.keycloakk.example.keycloak_exemple.dtos.UserCreationDTO;
import com.keycloakk.example.keycloak_exemple.dtos.UserDTO;
import com.keycloakk.example.keycloak_exemple.exception.ResourceAlreadyExistsException;
import com.keycloakk.example.keycloak_exemple.exception.ResourceNotFoundException;
import com.keycloakk.example.keycloak_exemple.model.User;
import com.keycloakk.example.keycloak_exemple.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;

//create user in keycloak and database
   @Transactional
   public UserDTO createUser(UserCreationDTO userCreationDTO) {
      // Check if username or email already exists
      if (userRepository.existsByUsername(userCreationDTO.getUsername())) {
          throw new ResourceAlreadyExistsException("Username already exists: " + userCreationDTO.getUsername());
      }
      if (userRepository.existsByEmail(userCreationDTO.getEmail())) {
          throw new ResourceAlreadyExistsException("Email already exists: " + userCreationDTO.getEmail());
      }

//       Convertit le DTO en entité User utilisable par l’application.
      User user = User.createUser(
              userCreationDTO.getUsername(),
              userCreationDTO.getFirstName(),
              userCreationDTO.getLastName(),
              userCreationDTO.getEmail(),
              userCreationDTO.getRole()
      );
//create user in keycloak
      String keycloakId = keycloakService.createKeycloakUser(user, userCreationDTO.getPassword());
      user.setKeycloakId(keycloakId); // Set Keycloak ID
      // Save user in database
      User savedUser = userRepository.save(user);
      return mapToDTO(savedUser);
  }
    @Transactional(readOnly = true)
    public UserDTO getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return mapToDTO(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(String id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());

        // Update user in Keycloak
        keycloakService.updateKeycloakUser(user);

        // Save updated user in database
        User updatedUser = userRepository.save(user);
        return mapToDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Delete user from Keycloak
        keycloakService.deleteKeycloakUser(user.getUsername());

        // Delete user from database
        userRepository.delete(user);
    }

    private UserDTO mapToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }
}


