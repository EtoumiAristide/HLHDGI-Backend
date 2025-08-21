package com.elpandor.hlh.modules.parametrage.role.validator;

import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RoleDtoValidator {
    public static List<String> validate(RoleDto dto) {

        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Veuillez renseigner le libellé");

            return errors;
        }

        if (!StringUtils.hasLength(dto.getLibelle())) {
            errors.add("Veuillez renseigner le libellé");
        }

        return errors;
    }
}
