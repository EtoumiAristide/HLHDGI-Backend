package com.elpandor.hlh.modules.parametrage.menus.validator;


import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuSectionDto;

import java.util.ArrayList;
import java.util.List;

public class MenuSectionDtoValidator {
    public static List<String> validate(MenuSectionDto dto) {

        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Veuillez renseigner le titre de la section");

            return errors;
        }

        /*if (!StringUtils.hasLength(dto.getTittle())) {
            errors.add("Veuillez renseigner le titre de la section");
        }*/

        return errors;
    }
}
