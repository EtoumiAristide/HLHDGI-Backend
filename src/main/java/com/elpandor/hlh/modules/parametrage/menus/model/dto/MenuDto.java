package com.elpandor.hlh.modules.parametrage.menus.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuDto {
    private Integer id;
    private String label;
    private String icon;
    private String link;
    private boolean isLayout;
    private boolean isTitle;

    private Integer position;

    private List<SubMenuDto> subMenus;

    private MenuSectionDto menuSection;

}
