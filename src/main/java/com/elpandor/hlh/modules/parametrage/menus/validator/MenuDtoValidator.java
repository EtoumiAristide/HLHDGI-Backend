package com.elpandor.hlh.modules.parametrage.menus.validator;

import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuDto;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MenuDtoValidator {
    public static List<String> validate(MenuDto dto) {

        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Veuillez renseigner le libéllé du menu");
            errors.add("Veuillez renseigner la route/page du menu");
            errors.add("Veuillez renseigner l'icone du menu");

            return errors;
        }

        if (!StringUtils.hasLength(dto.getLabel())) {
            errors.add("Veuillez renseigner le libéllé du menu");
        }
        /*if (!StringUtils.hasLength(dto.getRoute()) && !StringUtils.hasLength(dto.getPage())) {
            errors.add("Veuillez renseigner la route/page du menu");
        }*/
        if (!StringUtils.hasLength(dto.getIcon())) {
            errors.add("Veuillez renseigner l'icône du menu");
        }

        return errors;
    }
}
