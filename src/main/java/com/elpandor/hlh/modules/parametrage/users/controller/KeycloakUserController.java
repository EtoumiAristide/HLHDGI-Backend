package com.elpandor.hlh.modules.parametrage.users.controller;

import com.elpandor.hlh.modules.parametrage.users.dto.CreateUserResponse;
import com.elpandor.hlh.modules.parametrage.users.dto.UserDto;
import com.elpandor.hlh.modules.parametrage.users.service.KeycloakRoleService;
import com.elpandor.hlh.modules.parametrage.users.service.KeycloakUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/keyclaok")
@Tag(name = "Users", description = "Ensemble des APIs de manipulation des utilisateurs de keyclaok")
@CrossOrigin
public class KeycloakUserController {

    private final KeycloakUserService userService;
    private final KeycloakRoleService roleService;

//    private final OrganisationUtilisateurRepository organisationUtilisateurRepository;


    public KeycloakUserController(KeycloakUserService userService, KeycloakRoleService roleService /*,OrganisationUtilisateurRepository organisationUtilisateurRepository*/) {
        this.userService = userService;
        this.roleService = roleService;
//        this.organisationUtilisateurRepository = organisationUtilisateurRepository;
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> create(@RequestBody UserDto userDto) {
        CreateUserResponse response = new CreateUserResponse();
        try {
            response = userService.create(userDto)
                    .orElseThrow(() -> new RuntimeException("Impossible de creer l'utilisateur"));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<CreateUserResponse> update(@RequestBody UserDto userDto, @PathVariable("userId") String userId) {
        CreateUserResponse response = new CreateUserResponse();
        response = userService.update(userId, userDto)
                .orElseThrow(() -> new RuntimeException("Impossible de modifier l'utilisateur"));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{userId}/{password}")
    public ResponseEntity<CreateUserResponse> reinitPassword(@PathVariable("password") String password, @PathVariable("userId") String userId) {
        CreateUserResponse response = new CreateUserResponse();
        response = userService.reinitPassword(userId, password)
                .orElseThrow(() -> new RuntimeException("Impossible de réinitialiser le mot de passe de l'utilisateur"));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/getByUsername/{username}")
    public ResponseEntity<List<UserRepresentation>> getByUsername(@PathVariable("username") String username) {
        return new ResponseEntity<>(userService.getByUsername(username), HttpStatus.OK);
    }

    @GetMapping("/getById/{userId}")
    public ResponseEntity<UserDto> getById(@PathVariable("userId") String userId) {
        return new ResponseEntity<>(userService.getById(userId), HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<List<UserDto>> getAll() {
        return new ResponseEntity<>(userService.getAll(), HttpStatus.OK);
    }

    @GetMapping("/roles-frontend-client")
    public ResponseEntity<List<RoleRepresentation>> getRole() {
        return new ResponseEntity<>(roleService.getAll(), HttpStatus.OK);
    }

    @GetMapping("/roles-frontend-client/{rolename}")
    public ResponseEntity<RoleRepresentation> getRole(@PathVariable("rolename") String rolename) {
        return new ResponseEntity<>(roleService.findByName(rolename), HttpStatus.OK);
    }

    /* @PostMapping("/demo")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody UserDto userDto) {
        Map<String, Object> response = new HashMap<>();
        Optional<String> userId = userService.create(userDto);
        if(userId.isPresent()) {
            response.put("message", "L'utilisateur " + userId + " a été créé avec succès" );
            response.put("status", true);
            return new ResponseEntity<>()
        }
        return new ResponseEntity<>(, HttpStatus.CREATED);
    } */


}
