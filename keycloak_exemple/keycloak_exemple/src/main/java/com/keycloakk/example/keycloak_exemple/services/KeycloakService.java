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
        System.out.println("Synchronisation des utilisateurs au démarrage...");
        synchronizeKeycloakUsers();
    }

    public String createKeycloakUser(User user, String password) {
        if (user.getUsername() == null || user.getFirstName() == null ||
                user.getLastName() == null || user.getEmail() == null) {
            throw new IllegalArgumentException("Les propriétés de l'utilisateur ne peuvent pas être nulles");
        }

        System.out.println("Création d'un utilisateur Keycloak: " + user.getUsername());

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

        System.out.println("Réponse de création Keycloak: " + response.getStatus());
        if (response.getStatus() != 201) {
            System.out.println("Erreur de création Keycloak: " + response.readEntity(String.class));
            throw new RuntimeException("Échec de la création de l'utilisateur dans Keycloak");
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
            System.out.println("Attribution du rôle " + roleName + " à l'utilisateur " + user.getUsername());

            try {
                RoleRepresentation roleRepresentation = realmResource.roles().get(roleName).toRepresentation();
                userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));

                // Vérification que le rôle a bien été attribué
                List<RoleRepresentation> assignedRoles = userResource.roles().realmLevel().listEffective();
                boolean roleFound = assignedRoles.stream()
                        .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));

                if (!roleFound) {
                    System.out.println("AVERTISSEMENT: Le rôle " + roleName + " n'a pas été correctement attribué à " + user.getUsername());
                } else {
                    System.out.println("Rôle " + roleName + " correctement attribué à " + user.getUsername());
                }
            } catch (Exception e) {
                System.out.println("Erreur lors de l'attribution du rôle: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return userId;
    }

    public void updateKeycloakUser(User user) {
        System.out.println("Mise à jour de l'utilisateur Keycloak: " + user.getUsername());
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
            System.out.println("Mise à jour du rôle de " + user.getUsername() + " vers " + roleName);

            try {
                // Supprimer tous les rôles actuels
                List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
                if (!currentRoles.isEmpty()) {
                    userResource.roles().realmLevel().remove(currentRoles);
                }

                // Ajouter le nouveau rôle
                RoleRepresentation roleRepresentation = realmResource.roles().get(roleName).toRepresentation();
                userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));

                // Vérification
                List<RoleRepresentation> assignedRoles = userResource.roles().realmLevel().listEffective();
                boolean roleFound = assignedRoles.stream()
                        .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));

                if (!roleFound) {
                    System.out.println("AVERTISSEMENT: Le rôle " + roleName + " n'a pas été correctement mis à jour pour " + user.getUsername());
                } else {
                    System.out.println("Rôle " + roleName + " correctement mis à jour pour " + user.getUsername());
                }
            } catch (Exception e) {
                System.out.println("Erreur lors de la mise à jour du rôle: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Utilisateur " + user.getUsername() + " non trouvé dans Keycloak");
        }
    }

    public void deleteKeycloakUser(String username) {
        System.out.println("Suppression de l'utilisateur Keycloak: " + username);
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        List<UserRepresentation> userRepresentations = usersResource.search(username);
        if (!userRepresentations.isEmpty()) {
            UserRepresentation userRepresentation = userRepresentations.get(0);
            usersResource.delete(userRepresentation.getId());
            System.out.println("Utilisateur " + username + " supprimé de Keycloak");
        } else {
            System.out.println("Utilisateur " + username + " non trouvé dans Keycloak pour suppression");
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

    @Scheduled(fixedRate = 30000) // Synchronise toutes les 30 secondes
    public void scheduledSynchronization() {
        System.out.println("Exécution de la synchronisation planifiée des utilisateurs...");
        synchronizeKeycloakUsers();
    }

    public void synchronizeKeycloakUsers() {
        System.out.println("=== DÉBUT DE SYNCHRONISATION DES UTILISATEURS ===");
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        // Récupérer tous les utilisateurs Keycloak
        List<UserRepresentation> keycloakUsers = usersResource.list();
        System.out.println("Nombre d'utilisateurs Keycloak trouvés: " + keycloakUsers.size());

        // Récupérer tous les IDs Keycloak de la base de données
        List<String> existingIds = userRepository.findAll().stream()
                .map(User::getKeycloakId)
                .collect(Collectors.toList());

        // Synchronisation
        for (UserRepresentation kcUser : keycloakUsers) {
            System.out.println("\nTraitement de l'utilisateur: " + kcUser.getUsername() + " (ID: " + kcUser.getId() + ")");
            processKeycloakUser(kcUser);
        }

        // Supprimer les utilisateurs supprimés dans Keycloak
        List<String> keycloakIds = keycloakUsers.stream()
                .map(UserRepresentation::getId)
                .collect(Collectors.toList());

        existingIds.removeAll(keycloakIds);
        if (!existingIds.isEmpty()) {
            System.out.println("Suppression des utilisateurs qui n'existent plus dans Keycloak: " + existingIds.size());
            existingIds.forEach(this::deleteByKeycloakId);
        }

        System.out.println("=== FIN DE SYNCHRONISATION DES UTILISATEURS ===");
    }

    private void processKeycloakUser(UserRepresentation kcUser) {
        userRepository.findByKeycloakId(kcUser.getId()).ifPresentOrElse(
                existingUser -> {
                    System.out.println("Utilisateur existant trouvé: " + existingUser.getUsername() + " avec rôle: " + existingUser.getRole());
                    updateExistingUser(existingUser, kcUser);
                },
                () -> {
                    System.out.println("Création d'un nouvel utilisateur: " + kcUser.getUsername());
                    createNewUser(kcUser);
                }
        );
    }

    private void createNewUser(UserRepresentation kcUser) {
        // Attendre un peu pour s'assurer que les rôles sont complètement assignés
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        UserRole role = determineRoleWithRetry(kcUser.getId());
        System.out.println("Rôle déterminé pour " + kcUser.getUsername() + ": " + role);

        User user = new User();
        user.setKeycloakId(kcUser.getId());
        user.setUsername(kcUser.getUsername());
        user.setEmail(kcUser.getEmail());
        user.setFirstName(kcUser.getFirstName());
        user.setLastName(kcUser.getLastName());
        user.setRole(role);

        User savedUser = userRepository.save(user);
        System.out.println("Utilisateur sauvegardé dans la base de données: " + savedUser.getUsername() + " avec rôle: " + savedUser.getRole());
    }

    private void updateExistingUser(User existingUser, UserRepresentation kcUser) {
        boolean needsUpdate = false;

        if (!existingUser.getEmail().equals(kcUser.getEmail())) {
            existingUser.setEmail(kcUser.getEmail());
            needsUpdate = true;
        }

        if (!existingUser.getUsername().equals(kcUser.getUsername())) {
            existingUser.setUsername(kcUser.getUsername());
            needsUpdate = true;
        }

        if (!existingUser.getFirstName().equals(kcUser.getFirstName())) {
            existingUser.setFirstName(kcUser.getFirstName());
            needsUpdate = true;
        }

        if (!existingUser.getLastName().equals(kcUser.getLastName())) {
            existingUser.setLastName(kcUser.getLastName());
            needsUpdate = true;
        }

        // Vérifier si le rôle a changé
        UserRole newRole = determineRoleWithRetry(kcUser.getId());
        if (existingUser.getRole() != newRole) {
            System.out.println("Mise à jour du rôle pour " + existingUser.getUsername() + ": " + existingUser.getRole() + " -> " + newRole);
            existingUser.setRole(newRole);
            needsUpdate = true;
        }

        if (needsUpdate) {
            userRepository.save(existingUser);
            System.out.println("Utilisateur mis à jour dans la base de données: " + existingUser.getUsername());
        } else {
            System.out.println("Aucune mise à jour nécessaire pour: " + existingUser.getUsername());
        }
    }

    private void deleteByKeycloakId(String keycloakId) {
        userRepository.findByKeycloakId(keycloakId).ifPresent(user -> {
            userRepository.delete(user);
            System.out.println("Utilisateur supprimé de la base de données: " + user.getUsername());
        });
    }

    // Méthode améliorée pour déterminer le rôle avec plusieurs tentatives
    private UserRole determineRoleWithRetry(String keycloakId) {
        // Première tentative
        UserRole role = attemptRoleDetermination(keycloakId);

        // Si le rôle est CUSTOMER, faire une seconde tentative après un délai
        if (role == UserRole.CUSTOMER) {
            System.out.println("Premier essai a retourné CUSTOMER, nouvelle tentative après délai...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Deuxième tentative
            role = attemptRoleDetermination(keycloakId);

            // Si toujours CUSTOMER, faire une dernière tentative avec une approche différente
            if (role == UserRole.CUSTOMER) {
                System.out.println("Deuxième essai a retourné CUSTOMER, dernière tentative avec approche alternative...");
                role = attemptAlternativeRoleDetermination(keycloakId);
            }
        }

        return role;
    }

    private UserRole attemptRoleDetermination(String keycloakId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(keycloakId);

            // Récupérer les rôles effectifs
            List<RoleRepresentation> effectiveRoles = userResource.roles().realmLevel().listEffective();
            System.out.println("Rôles effectifs trouvés: " + effectiveRoles.size());

            for (RoleRepresentation role : effectiveRoles) {
                System.out.println("- Rôle effectif: " + role.getName());
                String roleName = role.getName().toUpperCase();

                if (roleName.equals("ADMIN") || roleName.equals("REALM-ADMIN")) {
                    return UserRole.ADMIN;
                } else if (roleName.equals("MERCHANT")) {
                    return UserRole.MERCHANT;
                }
            }

            // Si aucun rôle spécifique n'est trouvé, vérifier tous les rôles
            List<RoleRepresentation> allRoles = userResource.roles().realmLevel().listAll();
            System.out.println("Tous les rôles disponibles: " + allRoles.size());

            for (RoleRepresentation role : allRoles) {
                System.out.println("- Rôle disponible: " + role.getName());
                String roleName = role.getName().toUpperCase();

                if (roleName.equals("ADMIN") || roleName.equals("REALM-ADMIN")) {
                    return UserRole.ADMIN;
                } else if (roleName.equals("MERCHANT")) {
                    return UserRole.MERCHANT;
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la détermination du rôle: " + e.getMessage());
            e.printStackTrace();
        }

        return UserRole.CUSTOMER;
    }

    private UserRole attemptAlternativeRoleDetermination(String keycloakId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(keycloakId);

            // Récupérer la représentation complète de l'utilisateur
            UserRepresentation userRep = userResource.toRepresentation();

            // Vérifier les attributs de l'utilisateur
            if (userRep.getAttributes() != null) {
                System.out.println("Vérification des attributs utilisateur...");
                userRep.getAttributes().forEach((key, values) -> {
                    System.out.println("Attribut: " + key + " = " + String.join(", ", values));
                });
            }

            // Vérifier les rôles directement
            List<RoleRepresentation> allRoles = userResource.roles().realmLevel().listAll();
            System.out.println("Tous les rôles disponibles: " + allRoles.size());

            for (RoleRepresentation role : allRoles) {
                System.out.println("- Rôle disponible: " + role.getName());
                String roleName = role.getName().toUpperCase();

                if (roleName.equals("ADMIN") || roleName.equals("REALM-ADMIN")) {
                    return UserRole.ADMIN;
                } else if (roleName.equals("MERCHANT")) {
                    return UserRole.MERCHANT;
                }
            }

            // Dernière tentative: vérifier directement les groupes
            if (userRep.getGroups() != null && !userRep.getGroups().isEmpty()) {
                System.out.println("Groupes trouvés: " + String.join(", ", userRep.getGroups()));

                for (String group : userRep.getGroups()) {
                    String groupUpper = group.toUpperCase();
                    if (groupUpper.contains("ADMIN")) {
                        return UserRole.ADMIN;
                    } else if (groupUpper.contains("MERCHANT")) {
                        return UserRole.MERCHANT;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la détermination alternative du rôle: " + e.getMessage());
            e.printStackTrace();
        }

        return UserRole.CUSTOMER;
    }

    // Méthode publique pour vérifier le rôle d'un utilisateur Keycloak
    public UserRole determineRoleFromKeycloak(String keycloakId) {
        return determineRoleWithRetry(keycloakId);
    }
}