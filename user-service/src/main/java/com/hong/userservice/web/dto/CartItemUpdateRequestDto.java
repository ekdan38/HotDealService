package com.hong.userservice.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CartItemUpdateRequestDto {

    @NotNull(message = "requestItems 는 null 일 수 없습니다.")
    @Size(min = 1, max = 10, message = "requestItems 는 최소 1개, 최대 10개의 상품을 담을 수 있습니다.")
    private List<@Valid CartItemRequestDto> requestItems;
}
