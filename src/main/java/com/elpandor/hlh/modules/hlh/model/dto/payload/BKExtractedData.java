package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Data;

import java.util.List;

@Data
public class BKExtractedData {
    List<Payment> payments;
    List<Comp> comps;
    List<Promo> promos;
}
