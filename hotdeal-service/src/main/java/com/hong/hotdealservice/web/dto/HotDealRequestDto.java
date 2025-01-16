package com.hong.hotdealservice.web.dto;

import jakarta.persistence.Column;
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
    private List<@Valid HotDealProductRequest> productInfos;

    @Data
    @AllArgsConstructor
    public static class HotDealProductRequest{
        @NotNull(message = "productId 는 필수입니다.")
        @Positive(message = "productId 는 양수여야 합니다.")
        private Long productId;

        @NotNull(message = "quantity 는 필수입니다.")
        @Positive(message = "quantity 는 양수여야 합니다.")
        private Integer quantity;

        @NotNull(message = "discountRate 는 필수입니다.")
        @DecimalMin(value = "0.0",  message = "discountRate 는 0.0 이상이어야 합니다.")
        @DecimalMax(value = "1.0",  message = "discountRate 는 1.0 이하이어야 합니다.")
        private Double discountRate;
    }
}
