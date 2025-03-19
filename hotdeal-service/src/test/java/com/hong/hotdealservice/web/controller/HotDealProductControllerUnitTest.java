package com.hong.hotdealservice.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.HotDealProductCacheDto;
import com.hong.hotdealservice.dto.HotDealProductPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealProductResponseDto;
import com.hong.hotdealservice.dto.HotDealProductStockProjection;
import com.hong.hotdealservice.service.HotDealApiService;
import com.hong.hotdealservice.service.HotDealProductServiceImpl;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HotDealProductController.class)
public class HotDealProductControllerUnitTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    HotDealApiService hotDealApiService;
    @MockitoBean
    HotDealProductServiceImpl hotDealProductService;

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

    private List<HotDealProduct> createTestHotDealProducts(){
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        for(long i = 1; i <= 5; i++){
            HotDealProduct hotDealProduct = createTestHotDealProduct(i, i, "product" + i, 100);
            hotDealProducts.add(hotDealProduct);
        }
        return hotDealProducts;
    }

    @Test
    @DisplayName("hotDealProduct 페이징 조회_성공")
    public void getHotDealProducts_success() throws Exception {
        //given
        List<HotDealProduct> hotDealProducts = createTestHotDealProducts();
        HotDeal hotDeal = createTestHotDeal(hotDealProducts);

        Long cursor = 5L;
        int size = 3;
        String search = null;

        List<HotDealProductResponseDto> hotDealProductResponseDtos = hotDealProducts.stream()
                .filter(hp -> hp.getId() >= 3)
                .map(HotDealProductResponseDto::new)
                .toList()
                .reversed();
        HotDealProductPagingResponseDto expectedResult = new HotDealProductPagingResponseDto(3L, hotDeal.getId(), hotDealProductResponseDtos);
        when(hotDealProductService.getHotDealProducts(hotDeal.getId(), search, cursor, size)).thenReturn(expectedResult);

        //when && then
        List<HotDealProductResponseDto> expectedHotDealProducts = expectedResult.getHotDealProducts();
        ResultActions resultActions = mockMvc.perform(get("/hotdeals/" + hotDeal.getId() + "/hotDealProducts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("cursor", Long.toString(cursor))
                        .param("size", Integer.toString(size))
                        .param("search", search))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 상품 페이징 조회 성공"))
                .andExpect(jsonPath("$.data.cursor").value(3L))
                .andExpect(jsonPath("$.data.hotDealId").value(hotDeal.getId()));
        // hotDelProducts
        for(int i = 0; i <= 2; i++){
            resultActions
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].hotDealProductId").value(expectedHotDealProducts.get(i).getHotDealProductId()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].originalProductId").value(expectedHotDealProducts.get(i).getOriginalProductId()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].productTitle").value(expectedHotDealProducts.get(i).getProductTitle()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].originalPrice").value(expectedHotDealProducts.get(i).getOriginalPrice()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].hotDealPrice").value(expectedHotDealProducts.get(i).getHotDealPrice()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].discountRate").value(expectedHotDealProducts.get(i).getDiscountRate()));
        }
    }

    @Test
    @DisplayName("hotDealProduct 단건 조회_성공")
    public void getHotDealProduct_success() throws Exception {
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, 1L, "product1", 100);
        HotDeal hotDeal = createTestHotDeal(List.of(hotDealProduct));

        HotDealProductCacheDto expectedResult = new HotDealProductCacheDto(hotDealProduct);
        when(hotDealProductService.getHotDealProduct(hotDealProduct.getId())).thenReturn(expectedResult);

        //when && then
        mockMvc.perform(get("/hotdeals/hotDealProducts/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 상품 단건 조회 성공"))
                .andExpect(jsonPath("$.data.hotDealId").value(expectedResult.getHotDealId()))
                .andExpect(jsonPath("$.data.hotDealProductId").value(expectedResult.getHotDealProductId()))
                .andExpect(jsonPath("$.data.originalProductId").value(expectedResult.getOriginalProductId()))
                .andExpect(jsonPath("$.data.productTitle").value(expectedResult.getProductTitle()))
                .andExpect(jsonPath("$.data.originalPrice").value(expectedResult.getOriginalPrice()))
                .andExpect(jsonPath("$.data.hotDealPrice").value(expectedResult.getHotDealPrice()))
                .andExpect(jsonPath("$.data.discountRate").value(expectedResult.getDiscountRate()));
    }

    @Test
    @DisplayName("hotDealProduct 재고 조회")
    public void getHotDealProductStock() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, 1L, "product1", 100);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, 2L, "product2", 200);
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        createTestHotDeal(hotDealProducts);

        List<Long> hotDealProductIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();

        List<HotDealProductStockProjection> expectedResult = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockProjection(hp.getId(), hp.getStock()))
                .toList();
        when(hotDealApiService.fetchStock(hotDealProductIds)).thenReturn(expectedResult);

        //when && then
        mockMvc.perform(get("/hotdeals/hotDealProducts/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("hotDealProductId", hotDealProductIds.stream()
                                .map(String::valueOf).toArray(String[]::new)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 상품 재고 조회 성공"))
                .andExpect(jsonPath("$.data[0].id").value(expectedResult.get(0).getId()))
                .andExpect(jsonPath("$.data[0].stock").value(expectedResult.get(0).getStock()))
                .andExpect(jsonPath("$.data[1].id").value(expectedResult.get(1).getId()))
                .andExpect(jsonPath("$.data[1].stock").value(expectedResult.get(1).getStock()));
    }
}
