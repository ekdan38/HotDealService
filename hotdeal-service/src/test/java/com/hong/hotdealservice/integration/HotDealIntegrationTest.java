package com.hong.hotdealservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.common.exception.ErrorCode;
import com.hong.hotdealservice.HotDealTestUtil;
import com.hong.hotdealservice.client.Resilience4JProductServiceClient;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.repository.HotDealProductRedisRepository;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.web.dto.HotDealProductRequestDto;
import com.hong.hotdealservice.web.dto.HotDealProductUpdateRequestDto;
import com.hong.hotdealservice.web.dto.HotDealRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HotDealIntegrationTest {

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

    private List<HotDeal> createTestHotDealsForPaging(){
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        ArrayList<HotDeal> hotDeals = new ArrayList<>();
        for(long i = 1; i <= 10; i++){
            HotDealProduct hotDealProduct = HotDealProduct.create(i, "product" + i, 10000, 0.1, 100);
            if(i % 2 == 0) hotDeals.add(HotDeal.create(adminId, "evenHotDeal" + i, "description", startTime, endTime, List.of(hotDealProduct)));
            else hotDeals.add(HotDeal.create(adminId, "oddHotDeal" + i, "description", startTime, endTime, List.of(hotDealProduct)));
        }
        hotDealRepository.saveAll(hotDeals);
        em.flush();
        em.clear();
        return hotDeals;
    }

    @AfterEach
    void clearRedis(){
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 생성_성공")
    public void createHotDeal_success() throws Exception {
        //given

        // create 요청 hotDealProduct
        ArrayList<HotDealProductRequestDto> hotDealProductRequestDtos = new ArrayList<>();
        HotDealProductRequestDto hpRequestDto1 = new HotDealProductRequestDto(1L, 100, 0.1);
        HotDealProductRequestDto hpRequestDto2 = new HotDealProductRequestDto(2L, 200, 0.2);
        hotDealProductRequestDtos.add(hpRequestDto1);
        hotDealProductRequestDtos.add(hpRequestDto2);

        // request => create 요청 hotDeal, hotDealProduct
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDealRequestDto request = new HotDealRequestDto("hotDeal", "description", startTime, endTime, hotDealProductRequestDtos);

        // 원본 상품 재고 감소 feignClient 호출 mock 처리
        int originalStock = 1000;
        ArrayList<ProductStockUpdateResponseDto> productStockUpdateResponseDtos = new ArrayList<>();
        productStockUpdateResponseDtos.add(new ProductStockUpdateResponseDto(
                hpRequestDto1.getProductId(),
                "product" + hpRequestDto1.getProductId(),
                10000,
                hpRequestDto1.getQuantity()));

        productStockUpdateResponseDtos.add(new ProductStockUpdateResponseDto(
                hpRequestDto2.getProductId(),
                "product" + hpRequestDto2.getProductId(),
                10000,
                hpRequestDto2.getQuantity()));

        when(resilience4JProductServiceClient.decreaseStock(anyList())).thenReturn(productStockUpdateResponseDtos);

        //when && then
        ProductStockUpdateResponseDto hpRequest1 = productStockUpdateResponseDtos.get(0);
        ProductStockUpdateResponseDto hpRequest2 = productStockUpdateResponseDtos.get(1);
        mockMvc.perform(post("/hotdeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", adminId)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("핫딜 생성 성공"))
                // hotDeal
                .andExpect(jsonPath("$.data.hotDealId").exists())
                .andExpect(jsonPath("$.data.adminId").value(adminId))
                .andExpect(jsonPath("$.data.title").value(request.getTitle()))
                .andExpect(jsonPath("$.data.description").value(request.getDescription()))
                .andExpect(jsonPath("$.data.startTime").value(request.getStartTime().toString()))
                .andExpect(jsonPath("$.data.endTime").value(request.getEndTime().toString()))
                .andExpect(jsonPath("$.data.status").value(HotDealStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.data.deleted").value(false))
                // hotDealProduct
                .andExpect(jsonPath("$.data.hotDealProducts[0].hotDealProductId").exists())
                .andExpect(jsonPath("$.data.hotDealProducts[0].originalProductId").value(hpRequest1.getProductId()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].productTitle").value(hpRequest1.getTitle()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].originalPrice").value(10000))
                .andExpect(jsonPath("$.data.hotDealProducts[0].hotDealPrice").value(9000))
                .andExpect(jsonPath("$.data.hotDealProducts[0].discountRate").value(hpRequestDto1.getDiscountRate()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].stock").value(hpRequestDto1.getQuantity()))

                .andExpect(jsonPath("$.data.hotDealProducts[1].hotDealProductId").exists())
                .andExpect(jsonPath("$.data.hotDealProducts[1].originalProductId").value(hpRequest2.getProductId()))
                .andExpect(jsonPath("$.data.hotDealProducts[1].productTitle").value(hpRequest2.getTitle()))
                .andExpect(jsonPath("$.data.hotDealProducts[1].originalPrice").value(10000))
                .andExpect(jsonPath("$.data.hotDealProducts[1].hotDealPrice").value(8000))
                .andExpect(jsonPath("$.data.hotDealProducts[1].discountRate").value(hpRequestDto2.getDiscountRate()))
                .andExpect(jsonPath("$.data.hotDealProducts[1].stock").value(hpRequestDto2.getQuantity()));
    }

    @ParameterizedTest
    @CsvSource({
            // 1. title 빈 문자열
            "'', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '1|100|0.1', 'NotBlank', 'title 은 필수입니다.'",
            "' ', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '1|100|0.1', 'NotBlank', 'title 은 필수입니다.'",
            // 2. description 빈 문자열
            "'title', '', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '1|100|0.1', 'NotBlank', 'description 은 필수입니다.'",
            "'title', ' ', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '1|100|0.1', 'NotBlank', 'description 은 필수입니다.'",
            // 3. startTime null
            "'title', 'description', null, 2025-01-01T11:00:00, '1|100|0.1', 'NotNull', 'startTime 은 필수입니다.'",
            // 4. endTime null
            "'title', 'description', 2025-01-01T10:00:00, null, '1|100|0.1', 'NotNull', 'endTime 은 필수입니다.'",
            // 5. productInfos empty
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '', 'NotEmpty', 'productInfos 는 최소 1개 이상이어야 합니다.'",
            // 6. productInfos productId null
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'null|100|0.1', 'NotNull', 'productId 는 필수입니다.'",
            // 7. productInfos productId 음수
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '-1|100|0.1', 'Positive', 'productId 는 양수여야 합니다.'",
            // 8. productInfos quantity null
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '1|null|0.1', 'NotNull', 'quantity 는 필수입니다.'",
            // 9. productInfos quantity 음수
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '1|-1|0.1', 'Positive', 'quantity 는 양수여야 합니다.'",
            // 10. productInfos discountRate null
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '1|100|null', 'NotNull', 'discountRate 는 필수입니다.'",
            // 11. productInfos discountRate 범위
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '1|100|-0.1', 'DecimalMin', 'discountRate 는 0.0 이상이어야 합니다.'",
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, '1|100|1.2', 'DecimalMax', 'discountRate 는 1.0 이하이어야 합니다.'",
    })
    @DisplayName("hotDeal 생성_실패_입력 값 오류")
    public void createHotDeal_failure_invalidInput(String title,
                                                   String description,
                                                   String startTimeStr,
                                                   String endTimeStr,
                                                   String productInfosStr,
                                                   String expectedError,
                                                   String expectedValue) throws Exception {
        //given
        Long adminId = 1L;
        LocalDateTime startTime = (startTimeStr.equals("null")) ? null : LocalDateTime.parse(startTimeStr);
        LocalDateTime endTime = (endTimeStr.equals("null")) ? null : LocalDateTime.parse(endTimeStr);
        List<HotDealProductRequestDto> productInfos = HotDealTestUtil.createHotDealParseProductInfos(productInfosStr);

        HotDealRequestDto request = new HotDealRequestDto(title, description, startTime, endTime, productInfos);

        //when && then
        mockMvc.perform(post("/hotdeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", adminId)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("입력 값에 대한 검증을 실패했습니다."))
                .andExpect(jsonPath("$.errors[?(@.code == '" + expectedError + "')].defaultMessage").value(expectedValue));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 생성_실패_이미 존재 하는 hotDeal Title")
    public void createHotDeal_failure_existsTitle() throws Exception {
        //given
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        String duplicatedTitle = "hotDeal";
        createTestHotDeal(duplicatedTitle, startTime, endTime, List.of());

        HotDealRequestDto request =
                new HotDealRequestDto(duplicatedTitle, "description", startTime, endTime,
                        List.of(new HotDealProductRequestDto(1L, 100, 0.2)));

        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_TITLE_ALREADY_EXISTS.getErrorMessage(), request.getTitle());

        //when && then
        mockMvc.perform(post("/hotdeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", adminId)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_TITLE_ALREADY_EXISTS.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @DisplayName("hotDeal 생성_실패_starTime 이 endTime 이후(유효 하지 않는 이벤트 시간)")
    public void createHotDeal_failure_invalidTime() throws Exception {
        //given
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.minusHours(1);

        HotDealRequestDto request =
                new HotDealRequestDto("hotDeal", "description", startTime, endTime,
                        List.of(new HotDealProductRequestDto(1L, 100, 0.2)));

        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_INVALID_TIME.getErrorMessage(), startTime, endTime);

        //when && then
        mockMvc.perform(post("/hotdeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", adminId)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_INVALID_TIME.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 생성_실패_원본 상품 재고 감소 호출 실패")
    public void createHotDeal_failure_decreaseStock() throws Exception {
        //given
        // create 요청 hotDealProduct
        ArrayList<HotDealProductRequestDto> hotDealProductRequestDtos = new ArrayList<>();
        HotDealProductRequestDto hpRequestDto1 = new HotDealProductRequestDto(1L, 100, 0.1);
        HotDealProductRequestDto hpRequestDto2 = new HotDealProductRequestDto(2L, 200, 0.2);
        hotDealProductRequestDtos.add(hpRequestDto1);
        hotDealProductRequestDtos.add(hpRequestDto2);

        // request => create 요청 hotDeal, hotDealProduct
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDealRequestDto request = new HotDealRequestDto("hotDeal", "description", startTime, endTime, hotDealProductRequestDtos);

        // 원본 상품 재고 감소 feignClient 호출 mock 처리
        List<ProductStockUpdateRequestDto> requestDecrease = request.getProductInfos()
                .stream()
                .map(p -> new ProductStockUpdateRequestDto(p.getProductId(), p.getQuantity()))
                .toList();
        when(resilience4JProductServiceClient.decreaseStock(requestDecrease)).thenReturn(List.of());

        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_PRODUCT_ORIGINAL_STOCK_DECREASE_FAILED.getErrorMessage(),requestDecrease);

        //when && then
        mockMvc.perform(post("/hotdeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", adminId)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is(503))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_ORIGINAL_STOCK_DECREASE_FAILED.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 페이징 조회_search 미 포함")
    public void getHotDeals_withoutSearch() throws Exception {
        //given
        List<HotDeal> hotDeals = createTestHotDealsForPaging();
        Long cursor = 100L;
        int size = 3;

        Long expectedCursor = hotDeals.get(7).getId();

        //when && then
        ResultActions resultActions = mockMvc.perform(get("/hotdeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", adminId)
                        .param("cursor", Long.toString(cursor))
                        .param("size", Integer.toString(size)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 페이징 조회 성공"))
                .andExpect(jsonPath("$.data.cursor").value(expectedCursor));
        // hotDeals
        for(int i = 0; i < 3; i++){
            int idx = 9 - i;
            resultActions
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].hotDealId").value(hotDeals.get(idx).getId()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].adminId").value(hotDeals.get(idx).getUserId()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].title").value(hotDeals.get(idx).getTitle()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].description").value(hotDeals.get(idx).getDescription()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].startTime").value(hotDeals.get(idx).getStartTime().toString()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].endTime").value(hotDeals.get(idx).getEndTime().toString()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].status").value(hotDeals.get(idx).getStatus().name()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].deleted").value(hotDeals.get(idx).getDeleted()));
        }
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 페이징 조회_search 포함")
    public void getHotDeals_withSearch() throws Exception {
        //given
        List<HotDeal> hotDeals = createTestHotDealsForPaging();
        int size = 3;
        String search = "even";
        Long expectedCursor = hotDeals.get(5).getId();

        //when && then
        ResultActions resultActions = mockMvc.perform(get("/hotdeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", adminId)
                        .param("size", Integer.toString(size))
                        .param("search", search))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 페이징 조회 성공"))
                .andExpect(jsonPath("$.data.cursor").value(expectedCursor));
        // hotDeals
        for(int i = 0; i < 3; i++){
            int idx = 9 - 2 * i;
            resultActions
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].hotDealId").value(hotDeals.get(idx).getId()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].adminId").value(hotDeals.get(idx).getUserId()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].title").value(hotDeals.get(idx).getTitle()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].description").value(hotDeals.get(idx).getDescription()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].startTime").value(hotDeals.get(idx).getStartTime().toString()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].endTime").value(hotDeals.get(idx).getEndTime().toString()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].status").value(hotDeals.get(idx).getStatus().name()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].deleted").value(hotDeals.get(idx).getDeleted()));
        }
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 단건 조회_성공")
    public void getHotDeal_success() throws Exception {
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product1");
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, List.of(hotDealProduct));

        //when && then
        mockMvc.perform(get("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 단건 조회 성공"))
                .andExpect(jsonPath("$.data.hotDealId").value(hotDeal.getId()))
                .andExpect(jsonPath("$.data.adminId").value(hotDeal.getUserId()))
                .andExpect(jsonPath("$.data.title").value(hotDeal.getTitle()))
                .andExpect(jsonPath("$.data.description").value(hotDeal.getDescription()))
                .andExpect(jsonPath("$.data.startTime").value(hotDeal.getStartTime().toString()))
                .andExpect(jsonPath("$.data.endTime").value(hotDeal.getEndTime().toString()))
                .andExpect(jsonPath("$.data.status").value(hotDeal.getStatus().name()))
                .andExpect(jsonPath("$.data.deleted").value(hotDeal.getDeleted()));
    }

    @Test
    @DisplayName("hotDeal 단건 조회_실패_존재 하지 않는 hotDeal")
    public void getHotDeal_failure_notFoundHotDeal() throws Exception {
        //given
        Long hotDealProductId = 100L;
        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_NOT_FOUND.getErrorMessage(),hotDealProductId);

        //when && then
        mockMvc.perform(get("/hotdeals/" + hotDealProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 수정_성공_필드 업데이트, 상품 수정(재고 증가 감소), 신규 상품 추가, 기존 상품 삭제")
    public void updateHotDeal_success() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        HotDealProduct hotDealProduct3 = createTestHotDealProduct(3L, "product3");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);
        hotDealProducts.add(hotDealProduct3);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // hotDeal field 값 update
        String newTitle = "Updated HotDeal";
        String newDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(3);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus newStatus = HotDealStatus.ACTIVE;

        // 1번 상품 수정, 2번 상품 그대로, 3번 상품 삭제, 상품 추가
        // update 상품
        // 1번 상품 update
        HotDealProductUpdateRequestDto updateProduct =
                new HotDealProductUpdateRequestDto(hotDealProduct1.getId(), hotDealProduct1.getProductId(), 80, 0.3);

        // 2번 기존 상품
        HotDealProductUpdateRequestDto originalProduct2 = new HotDealProductUpdateRequestDto(
                hotDealProduct2.getId(), hotDealProduct2.getProductId(), hotDealProduct2.getStock(), hotDealProduct2.getDiscountRate());

        // 상품 추가 , 3번 상품 삭제됨
        HotDealProductUpdateRequestDto newProduct = new HotDealProductUpdateRequestDto(null, 4L, 50, 0.2);

        // request
        List<HotDealProductUpdateRequestDto> updateHotDealProducts = List.of(updateProduct, originalProduct2, newProduct);
        HotDealUpdateRequestDto request =
                new HotDealUpdateRequestDto(newTitle, newDescription,
                        newStartTime, newEndTime, newStatus.name(), updateHotDealProducts);

        // 원본 상품 재고 감소
        // 상품 추가 => 원본 상폼 재고 감소
        ProductStockUpdateRequestDto decreaseRequest = new ProductStockUpdateRequestDto(newProduct.getProductId(), newProduct.getQuantity());
        ProductStockUpdateResponseDto decreaseResponse =
                new ProductStockUpdateResponseDto(newProduct.getProductId(), "product4", 100, newProduct.getQuantity());
        when(resilience4JProductServiceClient.decreaseStock(List.of(decreaseRequest))).thenReturn(List.of(decreaseResponse));

        // 원본 상품 재고 증가
        // 1번, 3번 상품 원본 상품 재고 증가
        ProductStockUpdateRequestDto increaseRequest1 =
                new ProductStockUpdateRequestDto(updateProduct.getProductId(), hotDealProduct1.getStock() - updateProduct.getQuantity());
        ProductStockUpdateRequestDto increaseRequest2 =
                new ProductStockUpdateRequestDto(hotDealProduct3.getProductId(), hotDealProduct3.getStock());

        ProductStockUpdateResponseDto increaseResponse1 =
                new ProductStockUpdateResponseDto(updateProduct.getProductId(), "product4", 1000,
                        updateProduct.getQuantity());
        ProductStockUpdateResponseDto increaseResponse2 =
                new ProductStockUpdateResponseDto(hotDealProduct3.getProductId(), "product3", 1000,
                        hotDealProduct3.getStock());

        when(resilience4JProductServiceClient.increaseStock(List.of(increaseRequest2, increaseRequest1)))
                .thenReturn(List.of(increaseResponse1, increaseResponse2));

        //when && then
        mockMvc.perform(put("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 수정 성공"))
                .andExpect(jsonPath("$.data.hotDealId").value(hotDeal.getId()))
                .andExpect(jsonPath("$.data.adminId").value(hotDeal.getUserId()))
                .andExpect(jsonPath("$.data.title").value(newTitle))
                .andExpect(jsonPath("$.data.description").value(newDescription))
                .andExpect(jsonPath("$.data.startTime").value(newStartTime.toString()))
                .andExpect(jsonPath("$.data.endTime").value(newEndTime.toString()))
                .andExpect(jsonPath("$.data.status").value(newStatus.toString()))
                .andExpect(jsonPath("$.data.deleted").value(hotDeal.getDeleted()))
                .andExpect(jsonPath("$.data.hotDealProducts").isArray())

                // 업데이트 상품
                .andExpect(jsonPath("$.data.hotDealProducts[0].hotDealProductId").value(updateProduct.getHotDealProductId()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].originalProductId").value(updateProduct.getProductId()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].productTitle").value("product1"))
                .andExpect(jsonPath("$.data.hotDealProducts[0].originalPrice").value(1000))
                .andExpect(jsonPath("$.data.hotDealProducts[0].hotDealPrice").value(700))
                .andExpect(jsonPath("$.data.hotDealProducts[0].discountRate").value(0.3))
                .andExpect(jsonPath("$.data.hotDealProducts[0].stock").value(updateProduct.getQuantity()))
                // 기존 상품
                .andExpect(jsonPath("$.data.hotDealProducts[1].hotDealProductId").value(hotDealProduct2.getId()))
                .andExpect(jsonPath("$.data.hotDealProducts[1].originalProductId").value(hotDealProduct2.getProductId()))
                .andExpect(jsonPath("$.data.hotDealProducts[1].productTitle").value("product2"))
                .andExpect(jsonPath("$.data.hotDealProducts[1].originalPrice").value(1000))
                .andExpect(jsonPath("$.data.hotDealProducts[1].hotDealPrice").value(900))
                .andExpect(jsonPath("$.data.hotDealProducts[1].discountRate").value(0.1))
                .andExpect(jsonPath("$.data.hotDealProducts[1].stock").value(hotDealProduct2.getStock()))
                // 추가된 상품
                .andExpect(jsonPath("$.data.hotDealProducts[2].originalProductId").value(newProduct.getProductId()))
                .andExpect(jsonPath("$.data.hotDealProducts[2].productTitle").value("product4"))
                .andExpect(jsonPath("$.data.hotDealProducts[2].originalPrice").value(100))
                .andExpect(jsonPath("$.data.hotDealProducts[2].hotDealPrice").value(80))
                .andExpect(jsonPath("$.data.hotDealProducts[2].discountRate").value(0.2))
                .andExpect(jsonPath("$.data.hotDealProducts[2].stock").value(newProduct.getQuantity()));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 수정_실패_존재 하지 않는 hotDeal")
    public void updateHotDeal_failure_notFoundHotDeal() throws Exception {
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product1");

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, List.of(hotDealProduct));

        // hotDeal field 값 update
        String newTitle = "Updated HotDeal";
        String newDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(3);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus newStatus = HotDealStatus.ACTIVE;

        // 상품 update
        HotDealProductUpdateRequestDto updateProduct =
                new HotDealProductUpdateRequestDto(hotDealProduct.getId(), hotDealProduct.getProductId(), 80, 0.3);

        // request
        List<HotDealProductUpdateRequestDto> updateHotDealProducts = List.of(updateProduct);
        HotDealUpdateRequestDto request =
                new HotDealUpdateRequestDto(newTitle, newDescription,
                        newStartTime, newEndTime, newStatus.name(), updateHotDealProducts);
        Long hotDealId = 100L;

        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_NOT_FOUND.getErrorMessage(),hotDealId);

        //when && then
        mockMvc.perform(put("/hotdeals/" + hotDealId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 수정_실패_이미 존재 하는 hotDeal Title")
    public void updateHotDeal_failure_existsTitle() throws Exception {
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product1");

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, List.of(hotDealProduct));

        // hotDeal field 값 update
        String newTitle = "Updated HotDeal";
        String newDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(3);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus newStatus = HotDealStatus.ACTIVE;

        HotDeal existsHotDeal = createTestHotDeal(newTitle, startTime, endTime, List.of(
                createTestHotDealProduct(10L, "product100")));

        // update 상품
        HotDealProductUpdateRequestDto updateProduct =
                new HotDealProductUpdateRequestDto(hotDealProduct.getId(), hotDealProduct.getProductId(), 80, 0.3);

        // request
        List<HotDealProductUpdateRequestDto> updateHotDealProducts = List.of(updateProduct);
        HotDealUpdateRequestDto request =
                new HotDealUpdateRequestDto(newTitle, newDescription,
                        newStartTime, newEndTime, newStatus.name(), updateHotDealProducts);

        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_TITLE_ALREADY_EXISTS.getErrorMessage(),newTitle);

        //when && then
        mockMvc.perform(put("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_TITLE_ALREADY_EXISTS.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 수정_실패_startTime 이 endTime 보다 늦음")
    public void updateHotDeal_failure_invalidDate() throws Exception {
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product1");

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, List.of(hotDealProduct));

        // hotDeal field 값 update
        String newTitle = "Updated HotDeal";
        String newDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newEndTime = newNow.minusHours(3);
        LocalDateTime newStartTime = newNow.plusHours(3);
        HotDealStatus newStatus = HotDealStatus.ACTIVE;

        // update 상품
        HotDealProductUpdateRequestDto updateProduct =
                new HotDealProductUpdateRequestDto(hotDealProduct.getId(), hotDealProduct.getProductId(), 80, 0.3);

        // request
        List<HotDealProductUpdateRequestDto> updateHotDealProducts = List.of(updateProduct);
        HotDealUpdateRequestDto request =
                new HotDealUpdateRequestDto(newTitle, newDescription,
                        newStartTime, newEndTime, newStatus.name(), updateHotDealProducts);

        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_INVALID_TIME.getErrorMessage(), newStartTime, newEndTime);

        //when && then
        mockMvc.perform(put("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_INVALID_TIME.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 수정_실패_원본 상품 재고 감소 실패")
    public void updateHotDeal_failure_failDecreaseStock() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        HotDealProduct hotDealProduct3 = createTestHotDealProduct(3L, "product3");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);
        hotDealProducts.add(hotDealProduct3);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // hotDeal field 값 update
        String newTitle = "Updated HotDeal";
        String newDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(3);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus newStatus = HotDealStatus.ACTIVE;

        // 1번 상품 수정, 2번 상품 그대로, 3번 상품 삭제, 상품 추가
        // update 상품
        // 1번 상품 update
        HotDealProductUpdateRequestDto updateProduct =
                new HotDealProductUpdateRequestDto(hotDealProduct1.getId(), hotDealProduct1.getProductId(), 80, 0.3);

        // 2번 기존 상품
        HotDealProductUpdateRequestDto originalProduct2 = new HotDealProductUpdateRequestDto(
                hotDealProduct2.getId(), hotDealProduct2.getProductId(), hotDealProduct2.getStock(), hotDealProduct2.getDiscountRate());

        // 상품 추가 , 3번 상품 삭제됨
        HotDealProductUpdateRequestDto newProduct = new HotDealProductUpdateRequestDto(null, 4L, 50, 0.2);

        // request
        List<HotDealProductUpdateRequestDto> updateHotDealProducts = List.of(updateProduct, originalProduct2, newProduct);
        HotDealUpdateRequestDto request =
                new HotDealUpdateRequestDto(newTitle, newDescription,
                        newStartTime, newEndTime, newStatus.name(), updateHotDealProducts);

        // 원본 상품 재고 감소
        // 상품 추가 => 원본 상폼 재고 감소
        List<ProductStockUpdateRequestDto> decreaseRequest = List.of(
                new ProductStockUpdateRequestDto(newProduct.getProductId(), newProduct.getQuantity()));
        when(resilience4JProductServiceClient.decreaseStock(decreaseRequest)).thenReturn(List.of());

        //when && then
        mockMvc.perform(put("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is(503))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_ORIGINAL_STOCK_DECREASE_FAILED.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(containsString("원본 상품 재고 감소 호출을 실패했습니다.")));

    }

    @Test
    @Transactional
    @DisplayName("hotDeal 수정_실패_원본 상품 재고 증가 실패")
    public void updateHotDeal_failure_failIncreaseStock() throws Exception {
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        HotDealProduct hotDealProduct3 = createTestHotDealProduct(3L, "product3");
        hotDealProducts.add(hotDealProduct1);
        hotDealProducts.add(hotDealProduct2);
        hotDealProducts.add(hotDealProduct3);

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, hotDealProducts);

        // hotDeal field 값 update
        String newTitle = "Updated HotDeal";
        String newDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(3);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus newStatus = HotDealStatus.ACTIVE;

        // 1번 상품 수정, 2번 상품 그대로, 3번 상품 삭제, 상품 추가
        // update 상품
        // 1번 상품 update
        HotDealProductUpdateRequestDto updateProduct =
                new HotDealProductUpdateRequestDto(hotDealProduct1.getId(), hotDealProduct1.getProductId(), 80, 0.3);

        // 2번 기존 상품
        HotDealProductUpdateRequestDto originalProduct2 = new HotDealProductUpdateRequestDto(
                hotDealProduct2.getId(), hotDealProduct2.getProductId(), hotDealProduct2.getStock(), hotDealProduct2.getDiscountRate());

        // 상품 추가 , 3번 상품 삭제됨
        HotDealProductUpdateRequestDto newProduct = new HotDealProductUpdateRequestDto(null, 4L, 50, 0.2);

        // request
        List<HotDealProductUpdateRequestDto> updateHotDealProducts = List.of(updateProduct, originalProduct2, newProduct);
        HotDealUpdateRequestDto request =
                new HotDealUpdateRequestDto(newTitle, newDescription,
                        newStartTime, newEndTime, newStatus.name(), updateHotDealProducts);

        // 원본 상품 재고 감소
        // 상품 추가 => 원본 상폼 재고 감소
        ProductStockUpdateRequestDto decreaseRequest = new ProductStockUpdateRequestDto(newProduct.getProductId(), newProduct.getQuantity());
        ProductStockUpdateResponseDto decreaseResponse =
                new ProductStockUpdateResponseDto(newProduct.getProductId(), "product4", 100, newProduct.getQuantity());
        when(resilience4JProductServiceClient.decreaseStock(List.of(decreaseRequest))).thenReturn(List.of(decreaseResponse));

        // 원본 상품 재고 증가
        // 1번, 3번 상품 원본 상품 재고 증가
        ArrayList<ProductStockUpdateRequestDto> increaseRequest = new ArrayList<>();
        ProductStockUpdateRequestDto increaseRequest1 =
                new ProductStockUpdateRequestDto(updateProduct.getProductId(), hotDealProduct1.getStock() - updateProduct.getQuantity());
        ProductStockUpdateRequestDto increaseRequest2 =
                new ProductStockUpdateRequestDto(hotDealProduct3.getProductId(), hotDealProduct3.getStock());
        increaseRequest.add(increaseRequest1);
        increaseRequest.add(increaseRequest2);

        when(resilience4JProductServiceClient.increaseStock(increaseRequest)).thenReturn(List.of());

        //when && then
        mockMvc.perform(put("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is(503))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_ORIGINAL_STOCK_INCREASE_FAILED.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(containsString("원본 상품 재고 증가 호출을 실패했습니다")));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 삭제_성공")
    public void deleteHotDeal_success() throws Exception {
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product1");
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, List.of(hotDealProduct));

        // hotDealProduct 재고 원본 상품에 반영
        List<ProductStockUpdateRequestDto> increaseRequest = hotDeal.getHotDealProducts()
                .stream()
                .map(hp -> new ProductStockUpdateRequestDto(hp.getProductId(), hp.getStock()))
                .toList();
        List<ProductStockUpdateResponseDto> increaseResponse = hotDeal.getHotDealProducts()
                .stream()
                .map(hp -> new ProductStockUpdateResponseDto(
                        hp.getProductId(), hp.getProductTitle(), 1000, hp.getStock()))
                .toList();
        when(resilience4JProductServiceClient.increaseStock(increaseRequest)).thenReturn(increaseResponse);

        //when && then
        mockMvc.perform(delete("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 삭제 성공"))
                .andExpect(jsonPath("$.data.hotDealId").value(hotDeal.getId()))
                .andExpect(jsonPath("$.data.adminId").value(hotDeal.getUserId()))
                .andExpect(jsonPath("$.data.title").value(hotDeal.getTitle()))
                .andExpect(jsonPath("$.data.description").value(hotDeal.getDescription()))
                .andExpect(jsonPath("$.data.startTime").value(hotDeal.getStartTime().toString()))
                .andExpect(jsonPath("$.data.endTime").value(hotDeal.getEndTime().toString()))
                .andExpect(jsonPath("$.data.status").value(HotDealStatus.EXPIRED.name()))
                .andExpect(jsonPath("$.data.deleted").value(true))
                .andExpect(jsonPath("$.data.hotDealProducts").isArray());
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 삭제_실패_존재 하지 않는 hotDeal")
    public void deleteHotDeal_failure_notFoundHotDeal() throws Exception {
        //given
        Long hotDealId = 1L;

        String expectedErrorMessage = String.format(ErrorCode.HOTDEAL_NOT_FOUND.getErrorMessage(),hotDealId);

        //when && then
        mockMvc.perform(delete("/hotdeals/" + hotDealId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("hotDeal 삭제_실패_hotDealProduct 남은 재고 원본 상품에 반영 실패")
    public void deleteHotDeal_failure_failIncreaseStock() throws Exception {
        //given
        HotDealProduct hotDealProduct = createTestHotDealProduct(1L, "product1");
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal("hotDeal", startTime, endTime, List.of(hotDealProduct));

        // hotDealProduct 재고 원본 상품에 반영
        List<ProductStockUpdateRequestDto> increaseRequest = hotDeal.getHotDealProducts()
                .stream()
                .map(hp -> new ProductStockUpdateRequestDto(hp.getProductId(), hp.getStock()))
                .toList();

        when(resilience4JProductServiceClient.increaseStock(increaseRequest)).thenReturn(List.of());

        //when && then
        mockMvc.perform(delete("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(503))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.HOTDEAL_PRODUCT_ORIGINAL_STOCK_INCREASE_FAILED.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(containsString("원본 상품 재고 증가 호출을 실패했습니다.")));
    }

}
