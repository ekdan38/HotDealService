package com.hong.hotdealservice.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.HotDealProductStockCheckRequestDto;
import com.hong.common.dto.HotDealProductStockCheckResponseDto;
import com.hong.common.dto.HotDealProductStockUpdateRequestDto;
import com.hong.common.dto.HotDealProductStockUpdateResponseDto;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.service.HotDealApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HotDealApiController.class)
class HotDealApiControllerUnitTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    HotDealApiService hotDealApiService;

    private HotDealProduct createTestHotDealProduct(Long hotDealProductId, Long productId, String productTitle, int stock){
        HotDealProduct hotDealProduct = HotDealProduct.create(productId, productTitle, 1000, 0.1, stock);
        ReflectionTestUtils.setField(hotDealProduct, "id", hotDealProductId);
        return hotDealProduct;
    }

    private HotDeal createTestHotDeal(List<HotDealProduct> hp){
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = HotDeal.create(1L, "title", "description", startTime, endTime, hp);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);
        return hotDeal;
    }

    @Test
    @DisplayName("hotDealProduct 조회 및 재고 검증")
    public void fetchProductsAndValidateStock() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, 1L, "product1", 100);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, 2L, "product2", 200);
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        createTestHotDeal(hotDealProducts);

        // request
        List<HotDealProductStockCheckRequestDto> request = hotDealProducts.stream().map(hp -> new HotDealProductStockCheckRequestDto(hp.getId(), 5)).toList();

        List<HotDealProductStockCheckResponseDto> expectedResult = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockCheckResponseDto(
                        hp.getId(),
                        hp.getProductTitle(),
                        5,
                        hp.getHotDealPrice()))
                .toList();
        when(hotDealApiService.fetchHotDealProductsStockAndValidateStock(request)).thenReturn(expectedResult);

        //when && then
        ResultActions resultActions = mockMvc.perform(post("/hotdeal-service/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
        // hotDeals
        for(int i = 0; i < 2; i++){
            resultActions
                    .andExpect(jsonPath("$[" + i + "].hotDealProductId").value(expectedResult.get(i).getHotDealProductId()))
                    .andExpect(jsonPath("$[" + i + "].productTitle").value(expectedResult.get(i).getProductTitle()))
                    .andExpect(jsonPath("$[" + i + "].requestedQuantity").value(expectedResult.get(i).getRequestedQuantity()))
                    .andExpect(jsonPath("$[" + i + "].hotDealPrice").value(expectedResult.get(i).getHotDealPrice()));
        }
    }

    @Test
    @DisplayName("hotDealProduct 재고 감소")
    public void decreaseStock() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, 1L, "product1", 100);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, 2L, "product2", 200);
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        createTestHotDeal(hotDealProducts);

        // request
        List<HotDealProductStockUpdateRequestDto> request = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateRequestDto(hp.getId(), 50))
                .toList();

        List<HotDealProductStockUpdateResponseDto> expectedResult = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateResponseDto(
                        hp.getId(),
                        hp.getProductTitle(),
                        50))
                .toList();

        when(hotDealApiService.decreaseStock(request)).thenReturn(expectedResult);

        //when && then
        ResultActions resultActions = mockMvc.perform(post("/hotdeal-service/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
        for(int i = 0; i < 2; i++){
            resultActions
                    .andExpect(jsonPath("$[" + i + "].hotDealProductId").value(expectedResult.get(i).getHotDealProductId()))
                    .andExpect(jsonPath("$[" + i + "].title").value(expectedResult.get(i).getTitle()))
                    .andExpect(jsonPath("$[" + i + "].requestedQuantity").value(expectedResult.get(i).getRequestedQuantity()));
        }
    }

    @Test
    @DisplayName("hotDealProduct 재고 증가")
    public void increaseStock() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, 1L, "product1", 100);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, 2L, "product2", 200);
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        createTestHotDeal(hotDealProducts);

        // request
        List<HotDealProductStockUpdateRequestDto> request = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateRequestDto(hp.getId(), 50))
                .toList();

        List<HotDealProductStockUpdateResponseDto> expectedResult = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateResponseDto(
                        hp.getId(),
                        hp.getProductTitle(),
                        50))
                .toList();

        when(hotDealApiService.increaseStock(request)).thenReturn(expectedResult);

        //when && then
        ResultActions resultActions = mockMvc.perform(post("/hotdeal-service/increase-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
        for(int i = 0; i < 2 ; i++){
            resultActions
                    .andExpect(jsonPath("$[" + i + "].hotDealProductId").value(expectedResult.get(i).getHotDealProductId()))
                    .andExpect(jsonPath("$[" + i + "].title").value(expectedResult.get(i).getTitle()))
                    .andExpect(jsonPath("$[" + i + "].requestedQuantity").value(expectedResult.get(i).getRequestedQuantity()));
        }
    }

}