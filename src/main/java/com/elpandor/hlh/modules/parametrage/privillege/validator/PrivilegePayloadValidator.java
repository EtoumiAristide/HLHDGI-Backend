package com.elpandor.hlh.modules.parametrage.privillege.validator;


import com.elpandor.hlh.modules.parametrage.privillege.model.dto.PrivilegePayload;

import java.util.ArrayList;
import java.util.List;

public class PrivilegePayloadValidator {
    public static List<String> validate(PrivilegePayload dto) {

        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Veuillez renseigner le role");
            errors.add("Veuillez renseigner les menus");

            return errors;
        }

        if (dto.getRole() == null) {
            errors.add("Veuillez renseigner le role");
        } else if (dto.getRole().getId() == null || dto.getRole().getId() == 0) {
            errors.add("Veuillez renseigner le role");
        }

        if (dto.getSubMenus() == null) {
            errors.add("Veuillez renseigner les menus");
        }

        return errors;
    }
}
