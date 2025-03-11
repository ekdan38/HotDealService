package com.hong.productservice.web.controller.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.productservice.dto.wishlist.WishlistPagingResponseDto;
import com.hong.productservice.dto.wishlist.WishlistProductDto;
import com.hong.productservice.dto.wishlist.WishlistResponseDto;
import com.hong.productservice.service.wishlist.WishlistService;
import com.hong.productservice.web.controller.WishlistController;
import com.hong.productservice.web.dto.wishlist.WishlistRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class)
class WishlistControllerUnitTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    WishlistService wishlistService;

    @Test
    @DisplayName("wishlist 생성_성공")
    public void createWishlist_success() throws Exception {
        //given
        Long userId = 1L;
        Long productId = 1L;
        Long wishlistId = 1L;
        int quantity = 5;
        WishlistRequestDto requestDto = new WishlistRequestDto(productId, quantity);
        WishlistResponseDto responseDto = new WishlistResponseDto(wishlistId, userId, productId, quantity);
        when(wishlistService.createWishlist(userId, requestDto)).thenReturn(responseDto);

        //when && then
        mockMvc.perform(post("/wishlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("위시리스트 등록 완료"))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.quantity").value(quantity));
    }


    @ParameterizedTest
    @CsvSource({
            "null, '5', 'NotNull', 'productId 는 필수입니다.'",
            "1, null , 'NotNull', 'quantity 는 필수입니다.'"
    })
    @DisplayName("wishlist 생성_실패_입력 값 오류")
    public void createWishlist_failure_invalidInput(String productIdStr,
                                                    String quantityStr,
                                                    String expectedError,
                                                    String expectedValue ) throws Exception {
        //given
        Long productId = "null".equals(productIdStr) ? null : Long.valueOf(productIdStr);
        Integer quantity = "null".equals(quantityStr) ? null : Integer.valueOf(quantityStr);

        Long userId = 1L;
        WishlistRequestDto requestDto = new WishlistRequestDto(productId, quantity);

        //when && then
        mockMvc.perform(post("/wishlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.code == '" + expectedError + "')].defaultMessage").value(expectedValue));
    }

    @Test
    @DisplayName("wishlist 전체 조회_성공")
    public void getWishlists_success() throws Exception {
        //given
        Long userId = 1L;
        Long wishlistId = 1L;
        List<WishlistProductDto> wishlistProductDtos = List.of(
                new WishlistProductDto(3L, "product3", 3000, 3),
                new WishlistProductDto(2L, "product2", 2000, 2),
                new WishlistProductDto(1L, "product1", 1000, 1));
        WishlistPagingResponseDto responseDto = new WishlistPagingResponseDto(wishlistId, 1L, wishlistProductDtos);

        when(wishlistService.getWishlists(any(Long.class), any(Long.class), any(Integer.class))).thenReturn(responseDto);

        //when && then
        mockMvc.perform(get("/wishlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId)
                        .param("cursor", "3")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("위시리스트 조회 완료"))
                .andExpect(jsonPath("$.data.wishlistId").value(wishlistId))
                .andExpect(jsonPath("$.data.nextCursor").value(1L))
                .andExpect(jsonPath("$.data.products[0].id").value(3L))
                .andExpect(jsonPath("$.data.products[0].title").value("product3"))
                .andExpect(jsonPath("$.data.products[0].quantity").value(3))
                .andExpect(jsonPath("$.data.products[1].id").value(2L))
                .andExpect(jsonPath("$.data.products[1].title").value("product2"))
                .andExpect(jsonPath("$.data.products[1].quantity").value(2))
                .andExpect(jsonPath("$.data.products[2].id").value(1L))
                .andExpect(jsonPath("$.data.products[2].title").value("product1"))
                .andExpect(jsonPath("$.data.products[2].quantity").value(1));
    }

    @Test
    @DisplayName("wishlist 삭제_성공")
    public void deleteWishlist_success() throws Exception {
        //given
        Long userId = 1L;
        Long wishlistId = 1L;

        when(wishlistService.deleteWishlist(userId)).thenReturn(wishlistId);

        //when && then
        mockMvc.perform(delete("/wishlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("wishlist 삭제 성공"))
                .andExpect(jsonPath("$.data").value(wishlistId));
    }
}