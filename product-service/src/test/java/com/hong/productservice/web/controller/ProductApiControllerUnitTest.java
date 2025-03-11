package com.hong.productservice.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.ProductStockCheckRequestDto;
import com.hong.common.dto.ProductStockCheckResponseDto;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.productservice.dto.product.ProductStockDto;
import com.hong.productservice.service.product.ProductApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductApiController.class)
class ProductApiControllerUnitTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    ProductApiService productApiService;

    @Test
    @DisplayName("product 조회(재고 포함)_성공")
    public void fetchProducts_success() throws Exception {
        //given
        List<ProductStockCheckRequestDto> requestDtos = List.of(
                new ProductStockCheckRequestDto(1L, 100),
                new ProductStockCheckRequestDto(1L, 100));
        List<ProductStockCheckResponseDto> responseDtos = List.of(
                new ProductStockCheckResponseDto(1L, "product1", 100, 1000),
                new ProductStockCheckResponseDto(2L, "product2", 100, 2000));
        when(productApiService.fetchProductAndValidateStock(anyList())).thenReturn(responseDtos);

        //when && then
        mockMvc.perform(post("/product-service/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].title").value("product1"))
                .andExpect(jsonPath("$[0].quantity").value(100))
                .andExpect(jsonPath("$[0].price").value(1000))
                .andExpect(jsonPath("$[1].productId").value(2))
                .andExpect(jsonPath("$[1].title").value("product2"))
                .andExpect(jsonPath("$[1].quantity").value(100))
                .andExpect(jsonPath("$[1].price").value(2000));
    }

    @Test
    @DisplayName("product 재고 감소_성공")
    public void decreaseStock_success() throws Exception {
        //given
        List<ProductStockUpdateRequestDto> requestDtos = List.of(
                new ProductStockUpdateRequestDto(1L, 100),
                new ProductStockUpdateRequestDto(1L, 100));
        List<ProductStockUpdateResponseDto> responseDtos = List.of(
                new ProductStockUpdateResponseDto(1L, "product1", 1000, 100, 1000, 900),
                new ProductStockUpdateResponseDto(2L, "product2", 2000, 100, 2000, 1900));
        when(productApiService.decreaseStock(anyList())).thenReturn(responseDtos);

        //when && then
        mockMvc.perform(post("/product-service/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].title").value("product1"))
                .andExpect(jsonPath("$[0].requestedQuantity").value(100))
                .andExpect(jsonPath("$[0].originalStock").value(1000))
                .andExpect(jsonPath("$[0].remainingStock").value(900))
                .andExpect(jsonPath("$[1].productId").value(2))
                .andExpect(jsonPath("$[1].title").value("product2"))
                .andExpect(jsonPath("$[1].requestedQuantity").value(100))
                .andExpect(jsonPath("$[1].originalStock").value(2000))
                .andExpect(jsonPath("$[1].remainingStock").value(1900));
    }

    @Test
    @DisplayName("product 재고 증가_성공")
    public void increaseStock_success() throws Exception {
        //given
        List<ProductStockUpdateRequestDto> requestDtos = List.of(
                new ProductStockUpdateRequestDto(1L, 100),
                new ProductStockUpdateRequestDto(1L, 100));
        List<ProductStockUpdateResponseDto> responseDtos = List.of(
                new ProductStockUpdateResponseDto(1L, "product1", 1000, 100, 1000, 1100),
                new ProductStockUpdateResponseDto(2L, "product2", 2000, 100, 2000, 2100));
        when(productApiService.increaseStock(anyList())).thenReturn(responseDtos);

        //when && then
        mockMvc.perform(post("/product-service/increase-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].title").value("product1"))
                .andExpect(jsonPath("$[0].requestedQuantity").value(100))
                .andExpect(jsonPath("$[0].originalStock").value(1000))
                .andExpect(jsonPath("$[0].remainingStock").value(1100))
                .andExpect(jsonPath("$[1].productId").value(2))
                .andExpect(jsonPath("$[1].title").value("product2"))
                .andExpect(jsonPath("$[1].requestedQuantity").value(100))
                .andExpect(jsonPath("$[1].originalStock").value(2000))
                .andExpect(jsonPath("$[1].remainingStock").value(2100));
    }

}