package com.elpandor.hlh.modules.parametrage.users.service.impl;


import com.elpandor.hlh.common.service.UtiliityService;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.service.CompteUtilisateurService;
import com.elpandor.hlh.modules.parametrage.users.dto.CreateUserResponse;
import com.elpandor.hlh.modules.parametrage.users.dto.UserDto;
import com.elpandor.hlh.modules.parametrage.users.service.KeycloakRoleService;
import com.elpandor.hlh.modules.parametrage.users.service.KeycloakUserService;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;

@Service
@Slf4j
public class KeycloakUserServiceImpl implements KeycloakUserService {


    private final Keycloak keycloak;
    @Value("${keycloak.admin.realm}")
    private String KEYCLOAK_REALM;
    @Value("${keycloak.admin.clientId}")
    private String KEYCLOAK_CLIENT_ID;
    @Value("${keycloak.admin.clientSecret}")
    private String KEYCLOAK_CLIENT_SECRET;
    @Value("${keycloak.admin.clientUUID}")
    private String KEYCLOAK_CLIENT_UUID;

    private final KeycloakRoleService roleService;

    private final CompteUtilisateurService compteUtilisateurService;

    private final UtiliityService utiliityService;

    public KeycloakUserServiceImpl(Keycloak keycloak, KeycloakRoleService roleService, CompteUtilisateurService compteUtilisateurService, UtiliityService utiliityService) {
        this.keycloak = keycloak;
        this.roleService = roleService;
        this.compteUtilisateurService = compteUtilisateurService;
        this.utiliityService = utiliityService;
    }

    @Override
    public Optional<CreateUserResponse> create(UserDto userDto) throws ParseException {
        CreateUserResponse response = new CreateUserResponse();
        //try {

        //Vérification de l'existence du mail
        List<UserRepresentation> users = keycloak.realm(KEYCLOAK_REALM).users().searchByEmail(userDto.getEmail().trim(), true);
        if (users != null && !users.isEmpty()) {
            response.setStatus(false);
            response.setMessage("Impossible de créer l'utilisateur");
            response.setData("L'email existe déjà");

            return Optional.of(response);
        }

        //Verification de l'existence du login
        users = keycloak.realm(KEYCLOAK_REALM).users().searchByUsername(userDto.getUsername().trim(), true);
        if (users != null && !users.isEmpty()) {
            response.setStatus(false);
            response.setMessage("Impossible de créer l'utilisateur");
            response.setData("Le nom d'utilisateur existe déjà");

            return Optional.of(response);
        }

        List<CredentialRepresentation> cR = new ArrayList<>();
        cR.add(preparePasswordRepresentation(userDto.getPassword()));
        UserRepresentation uR = prepareUserRepresentation(userDto, cR, true);
        MultivaluedMap<String, Object> resp = keycloak.realm(KEYCLOAK_REALM).users().create(uR).getHeaders();
//            System.out.println("resp " + resp);
        //Récupération de l'identifiant de l'utilisateur
        List<Object> result = resp.get("Location");
        String url = result.get(0).toString();
        String[] urls = url.split("/");
        String userId = urls[urls.length - 1];

        //Assignation du role to user

        userDto.getRoles().forEach(role -> {
            RoleRepresentation roleRepresentation = roleService.findByName(role);
            assignRole(userId, roleRepresentation);
        });
        response.setStatus(true);
        response.setMessage("Utilisateur crée avec succès");
        response.setData(userId);

        return Optional.of(response);
        /*} catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            response.setStatus(false);
            response.setMessage(ex.getMessage());
            return Optional.empty();
        }*/
    }

    @Override
    public Optional<CreateUserResponse> update(String userId, UserDto userDto) {
        CreateUserResponse response = new CreateUserResponse();
        UserRepresentation userGet = keycloak.realm(KEYCLOAK_REALM).users().get(userId).toRepresentation();
        //CompteUtilisateurDto compteUtilisateurDto = compteUtilisateurService.findByUtilisateurId(userId);

        if (userGet == null) {
            response.setStatus(false);
            response.setMessage("Utilisateur introuvable");
            response.setData(userId);

            return Optional.of(response);
        }

        //try {
        List<CredentialRepresentation> cR = new ArrayList<>();
        cR.add(preparePasswordRepresentation(userDto.getPassword()));
        UserRepresentation uR = prepareUserRepresentation(userDto, cR, false);
        keycloak.realm(KEYCLOAK_REALM).users().get(userId).update(uR);

        //Suppression de.s ancien.s role du user
        List<RoleRepresentation> roleRepresentations = new ArrayList<>();
        userDto.getOldRoles().forEach(role -> {
            roleRepresentations.add(roleService.findByName(role));
        });
        keycloak.realm(KEYCLOAK_REALM).users().get(userId).roles().clientLevel(KEYCLOAK_CLIENT_UUID).remove(roleRepresentations);
        //Assignation du role to user
        userDto.getRoles().forEach(role -> {
            RoleRepresentation roleRepresentation = roleService.findByName(role);
            assignRole(userId, roleRepresentation);
        });

        //Mise à jour de l'user dans la BD
        //compteUtilisateurDto.setPersonne(userDto.getPersonne());
        //compteUtilisateurDto.setEmail(userDto.getEmail());
        //compteUtilisateurDto.setKeycloakUserId(UUID.fromString(userId));
        //compteUtilisateurDto.setLogin(userDto.getUsername());
//            compteUtilisateurDto.setKeycloakRole(userDto.getRole());
        //compteUtilisateurService.saveOrUpdate(compteUtilisateurDto);

        response.setStatus(true);
        response.setMessage("Utilisateur modifié avec succès");
        response.setData(userId);
        return Optional.of(response);
        /*} catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            response.setStatus(false);
            response.setMessage(ex.getMessage());
            return Optional.empty();
        }*/
    }

    @Override
    public Optional<CreateUserResponse> reinitPassword(String userId, String password) {
        CreateUserResponse response = new CreateUserResponse();
        UserRepresentation userGet = keycloak.realm(KEYCLOAK_REALM).users().get(userId).toRepresentation();

        if (userGet == null) {
            response.setStatus(false);
            response.setMessage("Utilisateur introuvable");
            response.setData(userId);

            return Optional.of(response);
        }

        try {
            List<CredentialRepresentation> cR = new ArrayList<>();
            cR.add(preparePasswordRepresentation(password));
            UserRepresentation uR = prepareUserRepresentation(cR);
            keycloak.realm(KEYCLOAK_REALM).users().get(userId).update(uR);

            response.setStatus(true);
            response.setMessage("Mot de passe réinitialisé avec succès");
            response.setData(userId);
            return Optional.of(response);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            response.setStatus(false);
            response.setMessage(ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUserName = authentication.getName();
            return Optional.of(currentUserName);
        } else {
            return Optional.empty();
        }
        //return Optional.empty();
    }

    @Override
    public void assignRole(String userId, RoleRepresentation roleRepresentation) {
        List<RoleRepresentation> roleRepresentations = new ArrayList<>();
        roleRepresentations.add(roleRepresentation);
        keycloak
                .realm(KEYCLOAK_REALM)
                .users()
                .get(userId)
                .roles()
                .clientLevel(KEYCLOAK_CLIENT_UUID)//.realmLevel()
                .add(roleRepresentations);
    }

    private CredentialRepresentation preparePasswordRepresentation(String password) {
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setTemporary(true);
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(password);
        return credentialRepresentation;
    }

    @Override
    public List<UserRepresentation> getByUsername(String userName) {
        return keycloak.realm(KEYCLOAK_REALM).users().searchByUsername(userName, true);
    }

    @Override
    public UserDto getById(String userId) {
        UserRepresentation userRepresentation = keycloak.realm(KEYCLOAK_REALM).users().get(userId).toRepresentation();
        if (userRepresentation != null) {
            //OrganisationUtilisateurDto organisationUtilisateurDto = organisationUtilisateurService.findByUtilisateurId(userRepresentation.getId());
            return UserDto.builder()
                    .id(userRepresentation.getId())
                    .firstName(userRepresentation.getFirstName())
                    .lastName(userRepresentation.getLastName())
                    .email(userRepresentation.getEmail())
                    .enable(userRepresentation.isEnabled())
                    //.organisation(organisationUtilisateurDto.getOrganisation())
                    .username(userRepresentation.getUsername())
//                    .role(organisationUtilisateurDto.getRole())
                    .roles(List.of())
                    .phone("")
                    .password("test")
                    .build();
        }
        return null;
    }

    @Override
    public List<UserDto> getAll() {
        List<UserRepresentation> userRepresentations = keycloak.realm(KEYCLOAK_REALM).users().list();
        List<UserDto> userDtos = new ArrayList<>();
        if (userRepresentations != null) {

            //On tri la liste par date de creation du plus récent au plus ancien
            userRepresentations.sort(Comparator.comparing(UserRepresentation::getCreatedTimestamp, Comparator.reverseOrder()));

            userRepresentations.forEach(userRepresentation -> {
                /*OrganisationUtilisateurDto organisationUtilisateurDto = organisationUtilisateurService.findByUtilisateurId(userRepresentation.getId());
                if (organisationUtilisateurDto != null) {
                    userDtos.add(UserDto.builder()
                            .id(userRepresentation.getId())
                            .firstName(userRepresentation.getFirstName())
                            .lastName(userRepresentation.getLastName())
                            .email(userRepresentation.getEmail())
                            .enable(userRepresentation.isEnabled())
                            .organisation(organisationUtilisateurDto.getOrganisation())
                            .username(userRepresentation.getUsername())
                            .role(organisationUtilisateurDto.getRole())
                            .phone(organisationUtilisateurDto.getPhone())
                            .build()
                    );
                }*/
            });
        }
        return userDtos;
    }

    private UserRepresentation prepareUserRepresentation(UserDto userRequest,
                                                         List<CredentialRepresentation> credentialRepresentation, boolean isCreate) {

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userRequest.getUsername());
        userRepresentation.setFirstName(userRequest.getFirstName());
        userRepresentation.setLastName(userRequest.getLastName());
        userRepresentation.setEmail(userRequest.getEmail());
        userRepresentation.setEnabled(userRequest.isEnable());

        /* ## Ajout des attributs */
        Map<String, List<String>> attributes = new HashMap<>();
        //Ajout du matricule
        List<String> phoneNumbers = new ArrayList<>();
        phoneNumbers.add(userRequest.getPhone());
        attributes.put("phone_number", phoneNumbers);

        userRepresentation.setAttributes(attributes);
        if (isCreate) {
            userRepresentation.setCredentials(credentialRepresentation);
        }
        //userRepresentation.setEnabled(true);

        return userRepresentation;
    }

    private UserRepresentation prepareUserRepresentation(List<CredentialRepresentation> credentialRepresentation) {

        UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setCredentials(credentialRepresentation);

        return userRepresentation;
    }
}
