package com.elpandor.hlh.modules.parametrage.roleutilisateur.validator;


import com.elpandor.hlh.modules.parametrage.roleutilisateur.model.dto.RoleUtilisateurDto;

import java.util.ArrayList;
import java.util.List;

public class RoleUtilisateurDtoValidator {
    public static List<String> validate(RoleUtilisateurDto dto) {

        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Veuillez renseigner la date");
            errors.add("Veuillez renseigner la mission");
            errors.add("Veuillez renseigner la direction");

            return errors;
        }

        if (dto.getDate() == null) {
            errors.add("Veuillez renseigner la date");
        }
        if (dto.getRole() == null) {
            errors.add("Veuillez renseigner le rôle");
        } else if (dto.getRole().getId() == null || dto.getRole().getId() == 0) {
            errors.add("Veuillez renseigner le rôle");
        }

        if (dto.getCompteUtilisateur() == null) {
            errors.add("Veuillez renseigner l'utilisateur");
        } else if (dto.getCompteUtilisateur().getId() == null || dto.getCompteUtilisateur().getId() == 0) {
            errors.add("Veuillez renseigner l'utilisateur");
        }

        return errors;
    }
}
