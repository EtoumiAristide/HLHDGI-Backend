package com.elpandor.hlh.modules.parametrage.organisations.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EtablissementDto {
    private Integer id;

    @Size(max = 150)
    private String nom;

    private OrganisationDto organisation;

}
