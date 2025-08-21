package com.elpandor.hlh.modules.parametrage.compteutilisateur.validator;

import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CompteUtilisateurDtoValidator {
    public static List<String> validate(CompteUtilisateurDto dto, boolean isCreate) {

        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Veuillez renseigner le login");
            errors.add("Veuillez renseigner le mot de passe");
            errors.add("Aucun rôle lié au compte");
            return errors;
        }

        if (!StringUtils.hasLength(dto.getLogin())) {
            errors.add("Veuillez renseigner le login");
        }
        if (isCreate && !StringUtils.hasLength(dto.getPassword())) {
            errors.add("Veuillez renseigner le mot de passe");
        }

        if (dto.getRoles() == null) {
            errors.add("Aucun rôle lié au compte");
        } else if (dto.getRoles().isEmpty()) {
            errors.add("Aucun rôle lié au compte");
        }

        return errors;
    }
}
