package com.elpandor.hlh.modules.parametrage.menus.validator;

import com.elpandor.hlh.modules.parametrage.menus.model.dto.SubMenuDto;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SousMenuDtoValidator {
    public static List<String> validate(SubMenuDto dto) {

        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Veuillez renseigner le libéllé du sous-menu");
            errors.add("Veuillez renseigner la route du sous-menu");

            return errors;
        }

        if (!StringUtils.hasLength(dto.getLabel())) {
            errors.add("Veuillez renseigner le libéllé");
        }
        if (!StringUtils.hasLength(dto.getLink())) {
            errors.add("Veuillez renseigner la route");
        }

        return errors;
    }
}
