package com.elpandor.hlh.modules.parametrage.organisations.dto;

import com.elpandor.hlh.modules.parametrage.organisations.model.Organisation;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PointVenteDto {
    private Integer id;

    @Size(max = 150)
    private String nom;

    private OrganisationDto organisation;

}
