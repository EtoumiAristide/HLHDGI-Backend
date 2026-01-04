package com.elpandor.hlh.modules.parametrage.organisations.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OrganisationDto {
    private Integer id;

    @Size(max = 50)
    private String numcc;

    @Size(max = 200)
    private String raisonSocial;

    @Size(max = 200)
    private String logo;

    private String sigle;

    private Integer indexLectureFichier;
    private Boolean isOrderedByPaiementMethod;
    private Boolean isPrixUnitaireDefined;
    private Boolean isFactureInitiale;
    private Boolean isTDTBaseTVA;
    private Boolean isFacturationMultiple;

//    private MultipartFile image;
}
