package com.hong.hotdealservice.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.HotDealProductStockCheckRequestDto;
import com.hong.common.dto.HotDealProductStockUpdateRequestDto;
import com.hong.common.exception.ErrorCode;
import com.hong.hotdealservice.client.Resilience4JProductServiceClient;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.repository.HotDealProductRedisRepository;
import com.hong.hotdealservice.repository.HotDealRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HotDealProductIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    HotDealRepository hotDealRepository;
    @Autowired
    HotDealProductRedisRepository hotDealProductRedisRepository;
    @MockitoBean
    Resilience4JProductServiceClient resilience4JProductServiceClient;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;
    @Autowired
    EntityManager em;

    private Long adminId = 1L;

    private HotDealProduct createTestHotDealProduct(Long productId, String productTitle){
        return HotDealProduct.create(productId, productTitle,1000,0.1,  100);
    }

    private HotDeal createTestHotDeal(String title, LocalDateTime startTime, LocalDateTime endTime, List<HotDealProduct> hp){
        HotDeal hotDeal = HotDeal.create(adminId, title, "description", startTime, endTime, hp);
        hotDealRepository.save(hotDeal);
        em.flush();
        em.clear();
        return hotDeal;
    }

    private List<HotDealProduct> createTestHotDealProductsForPaging(){
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        for(long i = 1; i <= 10; i++){
            HotDealProduct hotDealProduct;
            if(i % 2 == 0) hotDealProduct = HotDealProduct.create(i, "evenProduct" + i, 10000, 0.1, 100);
            else hotDealProduct = HotDealProduct.create(i, "oddProduct" + i, 10000, 0.1, 100);
            hotDealProducts.add(hotDealProduct);
        }
        return hotDealProducts;
    }

    @AfterEach
    void clearRedis(){
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 조회 및 재고 검증_성공")
    public void fetchProductsAndValidateStock_success() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // request
        int requestQuantity = 50;
        List<HotDealProductStockCheckRequestDto> request = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getId(), requestQuantity))
                .toList();

        //when && then
        HotDealProduct hotDealProduct = null;
        ResultActions resultActions = mockMvc.perform(post("/hotdeal-service/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
        for(int i = 0; i < 2; i++){
            if(i == 0) hotDealProduct = hotDealProduct1;
            else hotDealProduct = hotDealProduct2;
            resultActions
                    .andExpect(jsonPath("$.[" + i + "].hotDealProductId").value(hotDealProduct.getId()))
                    .andExpect(jsonPath("$.[" + i + "].productTitle").value(hotDealProduct.getProductTitle()))
                    .andExpect(jsonPath("$.[" + i + "].requestedQuantity").value(requestQuantity));
        }
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 조회 및 재고 검증_실패_존재 하지 않는 hotDealProduct")
    public void fetchProductsAndValidateStock_failure_notFoundHotDealProduct() throws Exception {
        //given
        // request
        ArrayList<HotDealProductStockCheckRequestDto> request = new ArrayList<>();
        HotDealProductStockCheckRequestDto stockCheckRequest1 = new HotDealProductStockCheckRequestDto(100L, 1);
        HotDealProductStockCheckRequestDto stockCheckRequest2 = new HotDealProductStockCheckRequestDto(200L, 2);
        request.add(stockCheckRequest1);
        request.add(stockCheckRequest2);

        List<Long> expectErrorMessageParameter = request.stream().map(r -> r.getHotDealProductId()).toList();
        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorMessage(), expectErrorMessageParameter);

        //when && then
        mockMvc.perform(post("/hotdeal-service/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 조회 및 재고 검증_실패_주문 가능 상태가 아닌 hotDeal")
    public void fetchProductsAndValidateStock_failure_nonActive() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // request
        int requestQuantity = 50;
        List<HotDealProductStockCheckRequestDto> request = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getId(), requestQuantity))
                .toList();

        List<Long> expectErrorMessageParameter = List.of(hotDeal.getId());
        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_NON_ACTIVE.getErrorMessage(),expectErrorMessageParameter);

        //when && then
        mockMvc.perform(post("/hotdeal-service/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_NON_ACTIVE.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));;
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 조회 및 재고 검증_실패_요청 수량 보다 재고 부족")
    public void fetchProductsAndValidateStock_failure_notEnoughStock() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // request
        int requestQuantity = 120;
        List<HotDealProductStockCheckRequestDto> request = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockCheckRequestDto(hp.getId(), requestQuantity))
                .toList();

        List<Long> expectErrorMessageParameter = request.stream().map(r -> r.getHotDealProductId()).toList();
        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_PRODUCT_STOCK_NOT_ENOUGH.getErrorMessage(),expectErrorMessageParameter);

        //when && then
        mockMvc.perform(post("/hotdeal-service/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_STOCK_NOT_ENOUGH.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 재고 감소_성공")
    public void decreaseStock_success() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // request
        int requestQuantity = 10;
        List<HotDealProductStockUpdateRequestDto> request = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateRequestDto(hp.getId(), requestQuantity))
                .toList();

        //when && then
        HotDealProduct hotDealProduct = null;
        ResultActions resultActions = mockMvc.perform(post("/hotdeal-service/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
        for(int i = 0; i < 2; i++){
            if(i == 0) hotDealProduct = hotDealProduct1;
            else hotDealProduct = hotDealProduct2;
            resultActions
                    .andExpect(jsonPath("$.[" + i + "].hotDealProductId").value(hotDealProduct.getId()))
                    .andExpect(jsonPath("$.[" + i + "].title").value(hotDealProduct.getProductTitle()))
                    .andExpect(jsonPath("$.[" + i + "].requestedQuantity").value(requestQuantity));
        }
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 재고 감소_실패_존재 하지 않는 hotDealProduct")
    public void decreaseStock_failure_notFoundHotDealProduct() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // request
        int requestQuantity = 10;
        ArrayList<HotDealProductStockUpdateRequestDto> request = new ArrayList<>();
        HotDealProductStockUpdateRequestDto requestDto1 = new HotDealProductStockUpdateRequestDto(hotDealProduct1.getId(), requestQuantity);
        HotDealProductStockUpdateRequestDto requestDto2 = new HotDealProductStockUpdateRequestDto(100L, requestQuantity);
        request.add(requestDto1);
        request.add(requestDto2);

        Long expectedErrorParameter = requestDto2.getHotDealProductId();
        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorMessage(), List.of(expectedErrorParameter));

        //when && then
        mockMvc.perform(post("/hotdeal-service/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 재고 감소_실패_재고 부족")
    public void decreaseStock_failure_notEnoughStock() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // request
        ArrayList<HotDealProductStockUpdateRequestDto> request = new ArrayList<>();
        HotDealProductStockUpdateRequestDto requestDto1 = new HotDealProductStockUpdateRequestDto(hotDealProduct1.getId(), 50);
        HotDealProductStockUpdateRequestDto requestDto2 = new HotDealProductStockUpdateRequestDto(hotDealProduct2.getId(), 120);
        request.add(requestDto1);
        request.add(requestDto2);

        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_PRODUCT_INSUFFICIENT_STOCK.getErrorMessage(),
                requestDto2.getHotDealProductId(), requestDto2.getQuantity(), hotDealProduct2.getStock());

        //when && then
        mockMvc.perform(post("/hotdeal-service/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_INSUFFICIENT_STOCK.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 재고 증가_성공")
    public void increaseStock_success() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // request
        int requestQuantity = 10;
        List<HotDealProductStockUpdateRequestDto> request = hotDealProducts
                .stream()
                .map(hp -> new HotDealProductStockUpdateRequestDto(hp.getId(), requestQuantity))
                .toList();

        //when && then
        HotDealProduct hotDealProduct = null;
        ResultActions resultActions = mockMvc.perform(post("/hotdeal-service/increase-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
        for(int i = 0; i < 2; i++){
            if(i == 0) hotDealProduct = hotDealProduct1;
            else hotDealProduct = hotDealProduct2;
            resultActions
                    .andExpect(jsonPath("$.[" + i + "].hotDealProductId").value(hotDealProduct.getId()))
                    .andExpect(jsonPath("$.[" + i + "].title").value(hotDealProduct.getProductTitle()))
                    .andExpect(jsonPath("$.[" + i + "].requestedQuantity").value(requestQuantity));
        }
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 재고 증가_실패_존재 하지 않는 hotDealProduct")
    public void increase_failure_notFoundHotDealProduct() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // request
        int requestQuantity = 10;
        ArrayList<HotDealProductStockUpdateRequestDto> request = new ArrayList<>();
        HotDealProductStockUpdateRequestDto requestDto1 = new HotDealProductStockUpdateRequestDto(hotDealProduct1.getId(), requestQuantity);
        HotDealProductStockUpdateRequestDto requestDto2 = new HotDealProductStockUpdateRequestDto(100L, requestQuantity);
        request.add(requestDto1);
        request.add(requestDto2);

        Long expectedErrorParameter = requestDto2.getHotDealProductId();
        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorMessage(), List.of(expectedErrorParameter));

        //when && then
        mockMvc.perform(post("/hotdeal-service/increase-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 페이징 조회_search 포함")
    public void getHotDealProducts_withSearch() throws Exception {
        //given
        List<HotDealProduct> hotDealProducts = createTestHotDealProductsForPaging();
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        int size = 3;
        String search = "even";
        Long expectCursor = hotDealProducts.get(5).getId();

        //when && then
        ResultActions resultActions = mockMvc.perform(get("/hotdeals/" + hotDeal.getId() + "/hotDealProducts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("size", Integer.toString(size))
                        .param("search", search))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 상품 페이징 조회 성공"))
                .andExpect(jsonPath("$.data.cursor").value(expectCursor))
                .andExpect(jsonPath("$.data.hotDealId").value(hotDeal.getId()));
        // hotDealProducts
        for(int i = 0; i < 3; i++){
            int idx = 9 - 2 * i;
            resultActions
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].hotDealProductId").value(hotDealProducts.get(idx).getId()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].originalProductId").value(hotDealProducts.get(idx).getProductId()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].productTitle").value(hotDealProducts.get(idx).getProductTitle()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].originalPrice").value(hotDealProducts.get(idx).getOriginalPrice()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].hotDealPrice").value(hotDealProducts.get(idx).getHotDealPrice()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].discountRate").value(hotDealProducts.get(idx).getDiscountRate()));
        }
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 페이징 조회_search 미 포함")
    public void getHotDealProducts_withoutSearch() throws Exception {
        //given
        List<HotDealProduct> hotDealProducts = createTestHotDealProductsForPaging();
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        int size = 3;
        Long expectCursor = hotDealProducts.get(7).getId();

        //when && then
        ResultActions resultActions = mockMvc.perform(get("/hotdeals/" + hotDeal.getId() + "/hotDealProducts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("size", Integer.toString(size)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 상품 페이징 조회 성공"))
                .andExpect(jsonPath("$.data.cursor").value(expectCursor))
                .andExpect(jsonPath("$.data.hotDealId").value(hotDeal.getId()));
        // hotDealProducts
        for(int i = 0; i < 3; i++){
            int idx = 9 - i;
            resultActions
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].hotDealProductId").value(hotDealProducts.get(idx).getId()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].originalProductId").value(hotDealProducts.get(idx).getProductId()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].productTitle").value(hotDealProducts.get(idx).getProductTitle()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].originalPrice").value(hotDealProducts.get(idx).getOriginalPrice()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].hotDealPrice").value(hotDealProducts.get(idx).getHotDealPrice()))
                    .andExpect(jsonPath("$.data.hotDealProducts[" + i + "].discountRate").value(hotDealProducts.get(idx).getDiscountRate()));
        }
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 단건 조회_성공")
    public void getHotDealProduct_success() throws Exception {
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product");
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
       createTestHotDeal("hotDeal", startTime, endTime, List.of(hotDealProduct));

        //when && then
        mockMvc.perform(get("/hotdeals/hotDealProducts/" + hotDealProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 상품 단건 조회 성공"))
                .andExpect(jsonPath("$.data.hotDealId").value(hotDealProduct.getHotDeal().getId()))
                .andExpect(jsonPath("$.data.hotDealProductId").value(hotDealProduct.getId()))
                .andExpect(jsonPath("$.data.originalProductId").value(hotDealProduct.getProductId()))
                .andExpect(jsonPath("$.data.productTitle").value(hotDealProduct.getProductTitle()))
                .andExpect(jsonPath("$.data.originalPrice").value(hotDealProduct.getOriginalPrice()))
                .andExpect(jsonPath("$.data.hotDealPrice").value(hotDealProduct.getHotDealPrice()))
                .andExpect(jsonPath("$.data.discountRate").value(hotDealProduct.getDiscountRate()));
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 단건 조회_실패_존재 하지 않는 hotDealProduct")
    public void getHotDealProduct_failure_notFoundHotDealProduct() throws Exception {
        //given
        Long hotDealProductId = 100L;
        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorMessage(), 100L);

        //when && then
        mockMvc.perform(get("/hotdeals/hotDealProducts/" + hotDealProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 재고 조회_성공")
    public void getHotDealProductStock_success() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        List<Long> hotDealProductIds = hotDealProducts.stream().map(hp -> hp.getId()).toList();

        //when && then
        mockMvc.perform(get("/hotdeals/hotDealProducts/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("hotDealProductId", hotDealProductIds.stream()
                                .map(String::valueOf).toArray(String[]::new)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 상품 재고 조회 성공"))
                .andExpect(jsonPath("$.data[0].id").value(hotDealProduct1.getId()))
                .andExpect(jsonPath("$.data[0].stock").value(hotDealProduct1.getStock()))
                .andExpect(jsonPath("$.data[1].id").value(hotDealProduct2.getId()))
                .andExpect(jsonPath("$.data[1].stock").value(hotDealProduct2.getStock()));
    }

    @Test
    @Transactional
    @DisplayName("hotDealProduct 재고 조회_실패_존재 하지 않는 hotDealProduct")
    public void getHotDealProductStock_failure_notFoundHotDealProduct() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        List<Long> hotDealProductIds = new ArrayList<>();
        hotDealProductIds.add(hotDealProduct1.getId());
        hotDealProductIds.add(100L);

        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorMessage(), List.of(100L));

        //when && then
        mockMvc.perform(get("/hotdeals/hotDealProducts/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("hotDealProductId", hotDealProductIds.stream()
                                .map(String::valueOf).toArray(String[]::new)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }
}
