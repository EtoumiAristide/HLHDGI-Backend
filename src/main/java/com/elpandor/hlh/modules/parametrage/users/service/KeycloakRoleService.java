package com.elpandor.hlh.modules.parametrage.users.service;

import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import com.elpandor.hlh.modules.parametrage.users.dto.CreateUserResponse;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;
import java.util.Optional;

public interface KeycloakRoleService {

    RoleRepresentation findByName(String roleName);
    List<RoleRepresentation> getAll();
    Optional<CreateUserResponse> create(RoleDto role);
    Optional<CreateUserResponse> delete(RoleDto role);
}
