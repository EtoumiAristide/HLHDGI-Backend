package com.elpandor.hlh.modules.bk.mapper;

import com.elpandor.hlh.modules.bk.model.BkTimbreDetail;
import com.elpandor.hlh.modules.bk.model.BkTimbreMonthlyReport;
import com.elpandor.hlh.modules.bk.model.BkTimbreRequest;
import com.elpandor.hlh.modules.bk.model.dto.BkPaymentDto;
import com.elpandor.hlh.modules.bk.model.dto.BkTimbreDetailDto;
import com.elpandor.hlh.modules.bk.model.dto.BkTimbreMonthlyReportDto;
import com.elpandor.hlh.modules.bk.model.dto.BkTimbreRequestDto;
import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface BkTimbreMapper {
    BkTimbreRequest toEntity(BkTimbreRequestDto requestDto);

    BkTimbreRequestDto toDto(BkTimbreRequest request);

    BkPaymentDto toDto(Payment payment);

    Payment toEntity(BkPaymentDto paymentDto);

    BkTimbreDetailDto toDto(BkTimbreDetail detail);

    List<BkTimbreDetailDto> toDto(List<BkTimbreDetail> details);

    BkTimbreMonthlyReportDto toDto(BkTimbreMonthlyReport report);
}
