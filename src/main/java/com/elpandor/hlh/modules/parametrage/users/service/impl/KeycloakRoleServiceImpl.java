package com.elpandor.hlh.modules.parametrage.users.service.impl;

import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import com.elpandor.hlh.modules.parametrage.users.dto.CreateUserResponse;
import com.elpandor.hlh.modules.parametrage.users.service.KeycloakRoleService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class KeycloakRoleServiceImpl implements KeycloakRoleService {

    private final Keycloak keycloak;

    @Value("${keycloak.admin.realm}")
    private String KEYCLOAK_REALM;
    @Value("${keycloak.admin.clientUUID}")
    private String KEYCLOAK_CLIENT_UUID;

    public KeycloakRoleServiceImpl(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    @Override
    public RoleRepresentation findByName(String roleName) {
        List<RoleRepresentation> roleRepresentationList = keycloak
                .realm(KEYCLOAK_REALM)
                .clients()
                .get(KEYCLOAK_CLIENT_UUID)
                .roles()
                .list(roleName, true);
        if (roleRepresentationList == null || roleRepresentationList.isEmpty()) return null;

        return roleRepresentationList.get(0);
    }

    @Override
    public List<RoleRepresentation> getAll() {
        return keycloak
                .realm(KEYCLOAK_REALM)
                .clients()
                .get(KEYCLOAK_CLIENT_UUID)
                .roles()
                .list();
    }

    @Override
    public Optional<CreateUserResponse> create(RoleDto role) {
        CreateUserResponse response = new CreateUserResponse();
        //try {
        //Verification de l'existence du role
        RoleRepresentation roleExist = findByName(role.getLibelle());
        if (roleExist != null) {
            response.setStatus(false);
            response.setMessage("Impossible de créer le rôle");
            response.setData("Le rôle existe déjà");

            return Optional.of(response);
        }

        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName(role.getLibelle());
        roleRepresentation.setDescription(role.getDescription());
        keycloak.realm(KEYCLOAK_REALM).clients().get(KEYCLOAK_CLIENT_UUID).roles().create(roleRepresentation);

        response.setStatus(true);
        response.setMessage("Rôle crée avec succès");
        response.setData(roleRepresentation);

        return Optional.of(response);
        /*} catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            response.setStatus(false);
            response.setMessage(ex.getMessage());
            return Optional.of(response);
        }*/
    }

    @Override
    public Optional<CreateUserResponse> delete(RoleDto role) {
        CreateUserResponse response = new CreateUserResponse();
        //try {
        //Verification de l'existence du role
        RoleRepresentation roleExist = findByName(role.getLibelle());
        if (roleExist == null) {
            response.setStatus(false);
            response.setMessage("Impossible de supprimer le rôle");
            response.setData("Le rôle n'existe pas");

            return Optional.of(response);
        }

        keycloak.realm(KEYCLOAK_REALM).clients().get(KEYCLOAK_CLIENT_UUID).roles().deleteRole(role.getLibelle());

        response.setStatus(true);
        response.setMessage("Rôle supprimé avec succès");
        response.setData(role);

        return Optional.of(response);
        /*} catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            response.setStatus(false);
            response.setMessage(ex.getMessage());
            return Optional.of(response);
        }*/
    }

}
