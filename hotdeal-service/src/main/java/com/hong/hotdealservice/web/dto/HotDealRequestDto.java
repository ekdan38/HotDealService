package com.hong.hotdealservice.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealRequestDto {

    @NotBlank(message = "title 은 필수입니다.")
    private String title;

    @NotBlank(message = "description 은 필수입니다.")
    private String description;

    @NotNull(message = "startTime 은 필수입니다.")
    private LocalDateTime startTime;

    @NotNull(message = "endTime 은 필수입니다.")
    private LocalDateTime endTime;

    @NotEmpty(message = "productInfos 는 최소 1개 이상이어야 합니다.")
    private List<@Valid HotDealProductRequestDto> productInfos;


}
