package com.hong.hotdealservice.web.dto;

import com.hong.hotdealservice.domain.status.HotDealStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealUpdateRequestDto {
    @NotBlank(message = "title 은 필수입니다.")
    private String title;

    @NotBlank(message = "description 은 필수입니다.")
    private String description;

    @NotNull(message = "startTime 은 필수입니다.")
    private LocalDateTime startTime;

    @NotNull(message = "endTime 은 필수입니다.")
    private LocalDateTime endTime;

    @NotNull(message = "status 는 필수입니다.")
    // ACTIVE, EXPIRED, SCHEDULED
    private HotDealStatus status;

    @NotEmpty(message = "productInfos 는 최소 1개 이상이어야 합니다.")
    private List<@Valid HotDealProductRequestDto> productInfos;
}
