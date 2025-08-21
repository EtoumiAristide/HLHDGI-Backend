package com.elpandor.hlh.modules.parametrage.menus.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuSectionDto {
    private Integer id;
    private String label;
    private boolean isTitle;

    private Integer position;

    private List<MenuDto> menu;

}
