package com.elpandor.hlh.modules.parametrage.privillege.validator;


import com.elpandor.hlh.modules.parametrage.privillege.model.dto.PrivilegeDto;

import java.util.ArrayList;
import java.util.List;

public class PrivilegeDtoValidator {
    public static List<String> validate(PrivilegeDto dto) {

        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Veuillez renseigner le libellé");
            errors.add("Veuillez renseigner le role");

            return errors;
        }

//        if (!StringUtils.hasLength(dto.getLibelle())) {
//            errors.add("Veuillez renseigner le libellé");
//        }
        if (dto.getRole() == null) {
            errors.add("Veuillez renseigner le role");
        } else if (dto.getRole().getId() == null || dto.getRole().getId() == 0) {
            errors.add("Veuillez renseigner le role");
        }

        return errors;
    }
}
