package com.elpandor.hlh.modules.parametrage.users.service;

import com.elpandor.hlh.modules.parametrage.users.dto.CreateUserResponse;
import com.elpandor.hlh.modules.parametrage.users.dto.UserDto;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

public interface KeycloakUserService {

    Optional<CreateUserResponse> create(UserDto userDto) throws ParseException;
    Optional<CreateUserResponse> update(String userId, UserDto userDto);
    Optional<CreateUserResponse> reinitPassword(String userId, String password);
    Optional<String> getUserId();
    void assignRole(String userId, RoleRepresentation roleRepresentation);
    List<UserRepresentation> getByUsername(String userName);
    UserDto getById(String userId);
    List<UserDto> getAll();
}
