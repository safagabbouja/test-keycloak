package com.keycloakk.example.keycloak_exemple.services;

import com.keycloakk.example.keycloak_exemple.model.User;
import com.keycloakk.example.keycloak_exemple.model.UserRole;
import com.keycloakk.example.keycloak_exemple.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloak;
    private final UserRepository userRepository;

    @Value("${keycloak.realm}")
    private String realm;

    @PostConstruct
    public void synchronizeUsersOnStartup() {
        synchronizeKeycloakUsers();
    }

    public String createKeycloakUser(User user, String password) {
        if (user.getUsername() == null || user.getFirstName() == null ||
            user.getLastName() == null || user.getEmail() == null) {
            throw new IllegalArgumentException("User properties cannot be null");
        }

        System.out.println("Creating Keycloak user: " + user);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(user.getUsername());
        userRepresentation.setFirstName(user.getFirstName());
        userRepresentation.setLastName(user.getLastName());
        userRepresentation.setEmail(user.getEmail());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);

        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        Response response = usersResource.create(userRepresentation);

        System.out.println("Keycloak create user response status: " + response.getStatus());
        if (response.getStatus() != 201) {
            System.out.println("Keycloak create user error: " + response.readEntity(String.class));
            throw new RuntimeException("Failed to create user in Keycloak");
        }

        String userId = extractCreatedId(response);

        if (userId != null) {
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setValue(password);
            credentialRepresentation.setTemporary(false);

            UserResource userResource = usersResource.get(userId);
            userResource.resetPassword(credentialRepresentation);

            String roleName = mapRoleToKeycloak(user.getRole());
            RoleRepresentation roleRepresentation = realmResource.roles().get(roleName).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));
        }

        return userId;
    }


    public void updateKeycloakUser(User user) {
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        List<UserRepresentation> userRepresentations = usersResource.search(user.getUsername());
        if (!userRepresentations.isEmpty()) {
            UserRepresentation userRepresentation = userRepresentations.get(0);
            userRepresentation.setFirstName(user.getFirstName());
            userRepresentation.setLastName(user.getLastName());
            userRepresentation.setEmail(user.getEmail());

            UserResource userResource = usersResource.get(userRepresentation.getId());
            userResource.update(userRepresentation);

            String roleName = mapRoleToKeycloak(user.getRole());
            List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();

            userResource.roles().realmLevel().remove(currentRoles);

            RoleRepresentation roleRepresentation = realmResource.roles().get(roleName).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));
        }
    }

    public void deleteKeycloakUser(String username) {
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        List<UserRepresentation> userRepresentations = usersResource.search(username);
        if (!userRepresentations.isEmpty()) {
            UserRepresentation userRepresentation = userRepresentations.get(0);
            usersResource.delete(userRepresentation.getId());
        }
    }

    private String extractCreatedId(Response response) {
        if (response.getStatus() == 201) {
            String location = response.getHeaderString("Location");
            if (location != null) {
                return location.substring(location.lastIndexOf("/") + 1);
            }
        }
        return null;
    }

    private String mapRoleToKeycloak(UserRole role) {
        return switch (role) {
            case ADMIN -> "ADMIN";
            case CUSTOMER -> "CUSTOMER";
            case MERCHANT -> "MERCHANT";
        };
    }

//    public void synchronizeKeycloakUsers() {
//        RealmResource realmResource = keycloak.realm(realm);
//        UsersResource usersResource = realmResource.users();
//
//        List<UserRepresentation> keycloakUsers = usersResource.list();
//        System.out.println("Synchronizing users from Keycloak...");
//        System.out.println("Fetched users: " + keycloakUsers.size());
//
//        for (UserRepresentation keycloakUser : keycloakUsers) {
//            String keycloakId = keycloakUser.getId();
//
//            if (!userRepository.findByKeycloakId(keycloakId).isPresent()) {
//                User user = new User();
//                user.setKeycloakId(keycloakId);
//                user.setUsername(keycloakUser.getUsername());
//                user.setEmail(keycloakUser.getEmail());
//                user.setFirstName(keycloakUser.getFirstName());
//                user.setLastName(keycloakUser.getLastName());
//                user.setRole(UserRole.CUSTOMER);
//
//                userRepository.save(user);
//                System.out.println("Saved user: " + user.getUsername());
//            } else {
//                System.out.println("User already exists: " + keycloakUser.getUsername());
//            }
//        }
//    }
    // Ajoutez cette méthode
public void synchronizeKeycloakUsers() {
    RealmResource realmResource = keycloak.realm(realm);
    UsersResource usersResource = realmResource.users();

    // Récupérer tous les utilisateurs Keycloak
    List<UserRepresentation> keycloakUsers = usersResource.list();

    // Récupérer tous les IDs Keycloak de la base de données
    List<String> existingIds = userRepository.findAll().stream()
            .map(User::getKeycloakId)
            .collect(Collectors.toList());

    // Synchronisation
    for (UserRepresentation kcUser : keycloakUsers) {
        processKeycloakUser(kcUser);
    }

    // Supprimer les utilisateurs supprimés dans Keycloak
    List<String> keycloakIds = keycloakUsers.stream()
            .map(UserRepresentation::getId)
            .collect(Collectors.toList());

    existingIds.removeAll(keycloakIds);
    existingIds.forEach(this::deleteByKeycloakId);
}

    private void processKeycloakUser(UserRepresentation kcUser) {
        userRepository.findByKeycloakId(kcUser.getId()).ifPresentOrElse(
                existingUser -> updateExistingUser(existingUser, kcUser),
                () -> createNewUser(kcUser)
        );
    }

    private void createNewUser(UserRepresentation kcUser) {
        User user = new User();
        user.setKeycloakId(kcUser.getId());
        user.setUsername(kcUser.getUsername());
        user.setEmail(kcUser.getEmail());
        user.setFirstName(kcUser.getFirstName());
        user.setLastName(kcUser.getLastName());
        user.setRole(determineRole(kcUser)); // Implémentez cette méthode
        userRepository.save(user);
    }

    private void updateExistingUser(User existingUser, UserRepresentation kcUser) {
        if (!existingUser.getEmail().equals(kcUser.getEmail()) ||
                !existingUser.getUsername().equals(kcUser.getUsername())) {

            existingUser.setEmail(kcUser.getEmail());
            existingUser.setUsername(kcUser.getUsername());
            existingUser.setFirstName(kcUser.getFirstName());
            existingUser.setLastName(kcUser.getLastName());
            userRepository.save(existingUser);
        }
    }

    private void deleteByKeycloakId(String keycloakId) {
        userRepository.findByKeycloakId(keycloakId).ifPresent(user -> {
            userRepository.delete(user);
            System.out.println("Deleted user: " + user.getUsername());
        });
    }



    @Scheduled(fixedRate = 30000) // Synchronise toutes les 30 secondes
    public void scheduledSynchronization() {
        System.out.println("Running scheduled user synchronization...");
        synchronizeKeycloakUsers();
    }
    private UserRole determineRole(UserRepresentation kcUser) {
        // Récupérer les rôles de l'utilisateur
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();
        UserResource userResource = usersResource.get(kcUser.getId());

        List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();

        // Mapper le premier rôle valide trouvé
        return roles.stream()
                .map(RoleRepresentation::getName)
                .map(this::mapKeycloakRoleToUserRole)
                .filter(role -> role != UserRole.CUSTOMER) // Prioriser les rôles non par défaut
                .findFirst()
                .orElse(UserRole.CUSTOMER); // Valeur par défaut
    }

    private UserRole mapKeycloakRoleToUserRole(String keycloakRole) {
        return switch (keycloakRole) {
            case "ADMIN" -> UserRole.ADMIN;
            case "MERCHANT" -> UserRole.MERCHANT;
            default -> UserRole.CUSTOMER;
        };
    }
public UserRole determineRoleFromKeycloak(String keycloakId) {
    RealmResource realmResource = keycloak.realm(realm);
    UserResource userResource = realmResource.users().get(keycloakId);

    List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();

    // Map the first valid role found
    return roles.stream()
            .map(RoleRepresentation::getName)
            .map(this::mapKeycloakRoleToUserRole)
            .filter(role -> role != UserRole.CUSTOMER) // Prioritize non-default roles
            .findFirst()
            .orElse(UserRole.CUSTOMER); // Default value
}
}