package com.elpandor.hlh.modules.parametrage.menus.model.dto;

import com.elpandor.hlh.modules.parametrage.menus.model.Menu;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubMenuDto {
    private Integer id;
    private String label;
    private String link;

    private MenuDto menu;

    private Integer position;
}
