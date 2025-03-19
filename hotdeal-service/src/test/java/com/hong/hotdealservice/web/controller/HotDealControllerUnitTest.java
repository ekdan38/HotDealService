package com.hong.hotdealservice.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.hotdealservice.HotDealTestUtil;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.dto.HotDealCacheDto;
import com.hong.hotdealservice.dto.HotDealPagingCacheDto;
import com.hong.hotdealservice.dto.HotDealProductResponseDto;
import com.hong.hotdealservice.service.HotDealService;
import com.hong.hotdealservice.web.dto.HotDealProductRequestDto;
import com.hong.hotdealservice.web.dto.HotDealProductUpdateRequestDto;
import com.hong.hotdealservice.web.dto.HotDealRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HotDealController.class)
class HotDealControllerUnitTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    HotDealService hotDealService;

    @Test
    @DisplayName("hotDeal 생성_성공")
    public void createHotDeal_success() throws Exception {
        //given
        Long adminId = 1L;

        List<HotDealProductRequestDto> hotDealProductRequestDtos = List.of(
                new HotDealProductRequestDto(1L, 100, 0.2),
                new HotDealProductRequestDto(2L, 100, 0.2)
        );

        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        // request
        HotDealRequestDto request = new HotDealRequestDto("hotDeal", "description", startTime, endTime, hotDealProductRequestDtos);

        List<HotDealProductResponseDto> hotDealProductResponseDtos = List.of(
                new HotDealProductResponseDto(1L, 1L, 1L, "product1", 1000, 800, 0.2, 100),
                new HotDealProductResponseDto(1L, 2L, 2L, "product2", 1000, 800, 0.2, 100)
        );

        HotDealCacheDto expectedDto = new HotDealCacheDto(1L, adminId, "hotDeal", "description",
                startTime, endTime, HotDealStatus.ACTIVE.name(), false, hotDealProductResponseDtos);
        when(hotDealService.createHotDeal(adminId, request)).thenReturn(expectedDto);

        //when && then
        mockMvc.perform(post("/hotdeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", adminId)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                // hotDeal
                .andExpect(jsonPath("$.message").value("핫딜 생성 성공"))
                .andExpect(jsonPath("$.data.hotDealId").value(expectedDto.getHotDealId()))
                .andExpect(jsonPath("$.data.adminId").value(expectedDto.getAdminId()))
                .andExpect(jsonPath("$.data.title").value(expectedDto.getTitle()))
                .andExpect(jsonPath("$.data.description").value(expectedDto.getDescription()))
                .andExpect(jsonPath("$.data.startTime").value(expectedDto.getStartTime().toString()))
                .andExpect(jsonPath("$.data.endTime").value(expectedDto.getEndTime().toString()))
                .andExpect(jsonPath("$.data.status").value(expectedDto.getStatus()))
                .andExpect(jsonPath("$.data.deleted").value(expectedDto.getDeleted()))
                // hotDealProducts
                .andExpect(jsonPath("$.data.hotDealProducts[0].hotDealId").value(1))
                .andExpect(jsonPath("$.data.hotDealProducts[0].hotDealProductId").value(1))
                .andExpect(jsonPath("$.data.hotDealProducts[0].originalProductId").value(1))
                .andExpect(jsonPath("$.data.hotDealProducts[0].productTitle").value("product1"))
                .andExpect(jsonPath("$.data.hotDealProducts[0].originalPrice").value(1000))
                .andExpect(jsonPath("$.data.hotDealProducts[0].hotDealPrice").value(800))
                .andExpect(jsonPath("$.data.hotDealProducts[0].discountRate").value(0.2))
                .andExpect(jsonPath("$.data.hotDealProducts[0].stock").value(100))
                .andExpect(jsonPath("$.data.hotDealProducts[1].hotDealId").value(1))
                .andExpect(jsonPath("$.data.hotDealProducts[1].hotDealProductId").value(2))
                .andExpect(jsonPath("$.data.hotDealProducts[1].originalProductId").value(2))
                .andExpect(jsonPath("$.data.hotDealProducts[1].productTitle").value("product2"))
                .andExpect(jsonPath("$.data.hotDealProducts[1].originalPrice").value(1000))
                .andExpect(jsonPath("$.data.hotDealProducts[1].hotDealPrice").value(800))
                .andExpect(jsonPath("$.data.hotDealProducts[1].discountRate").value(0.2))
                .andExpect(jsonPath("$.data.hotDealProducts[1].stock").value(100));
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
        LocalDateTime startTime = (startTimeStr.equals("null")) ? null : LocalDateTime.parse(startTimeStr);
        LocalDateTime endTime = (endTimeStr.equals("null")) ? null : LocalDateTime.parse(endTimeStr);
        List<HotDealProductRequestDto> productInfos = HotDealTestUtil.createHotDealParseProductInfos(productInfosStr);
        Long adminId = 1L;

        // request
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
    @DisplayName("hotDeal 페이징 조회_성공")
    public void getHotDeals_success() throws Exception {
        //given
        Long cursor = 10L;
        int size = 3;
        String search = "product";
        Long expectedCursor = 8L;
        ArrayList<HotDealCacheDto> hotDeals = new ArrayList<>();
        HotDeal hotDeal1 = createTestHotDeal("hotDeal1", 1L, List.of());
        HotDealCacheDto hotDealCacheDto1 = new HotDealCacheDto(hotDeal1);
        hotDeals.add(hotDealCacheDto1);
        HotDeal hotDeal2 = createTestHotDeal("hotDeal2", 2L, List.of());
        HotDealCacheDto hotDealCacheDto2 = new HotDealCacheDto(hotDeal2);
        hotDeals.add(hotDealCacheDto2);

        HotDealPagingCacheDto expectedResponse = new HotDealPagingCacheDto(expectedCursor, hotDeals);
        when(hotDealService.getHotDeals(search, cursor, size)).thenReturn(expectedResponse);

        //when && then
        ResultActions resultActions = mockMvc.perform(get("/hotdeals")
                        .param("cursor", Long.toString(cursor))
                        .param("size", Integer.toString(size))
                        .param("search", search)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 페이징 조회 성공"))
                .andExpect(jsonPath("$.data.cursor").value(expectedCursor))
                .andExpect(jsonPath("$.data.hotDeals").isArray());
        // hotDeal
        for(int i = 0; i < 2; i++){
            HotDeal hotDeal = null;
            if(i == 0) hotDeal = hotDeal1;
            else hotDeal = hotDeal2;
            resultActions
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].hotDealId").value(hotDeal.getId()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].adminId").value(hotDeal.getUserId()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].title").value(hotDeal.getTitle()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].description").value(hotDeal.getDescription()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].startTime").value(hotDeal.getStartTime().toString()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].endTime").value(hotDeal.getEndTime().toString()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].status").value(hotDeal.getStatus().name()))
                    .andExpect(jsonPath("$.data.hotDeals[" + i + "].deleted").value(hotDeal.getDeleted())) ;
        }
    }

    @Test
    @DisplayName("hotDeal 단건 조회_성공")
    public void getHotDeal_success() throws Exception {
        //given
        HotDeal hotDeal = createTestHotDeal("hotDeal", 1L, List.of());
        HotDealCacheDto hotDealCacheDto = new HotDealCacheDto(hotDeal);

        when(hotDealService.getHotDeal(hotDeal.getId())).thenReturn(hotDealCacheDto);

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
    @DisplayName("hotDeal 수정_성공")
    public void updateHotDeal_success() throws Exception {
        //given
        HotDealProduct hotDealProduct = HotDealProduct.create(1L, "hotDealProduct", 1000, 0.1, 100);
        ReflectionTestUtils.setField(hotDealProduct, "id", 1L);
        HotDeal hotDeal = createTestHotDeal("hotDeal", 1L, List.of(hotDealProduct));

        HotDealProductUpdateRequestDto hotDealProductUpdateRequestDto =
                new HotDealProductUpdateRequestDto(hotDealProduct.getId(), 2L, 1000, 0.2);

        HotDealUpdateRequestDto request = new HotDealUpdateRequestDto("newHotDeal", "newDescription",
                hotDeal.getStartTime(), hotDeal.getEndTime(), hotDeal.getStatus().name(), List.of(hotDealProductUpdateRequestDto));

        HotDealProductResponseDto hotDealProductResponseDto = new HotDealProductResponseDto(
                hotDealProduct.getHotDeal().getId(),
                hotDealProduct.getId(),
                hotDealProductUpdateRequestDto.getProductId(),
                hotDealProduct.getProductTitle(),
                hotDealProduct.getOriginalPrice(),
                800,
                hotDealProductUpdateRequestDto.getDiscountRate(),
                hotDealProductUpdateRequestDto.getQuantity());

        HotDealCacheDto hotDealCacheDto =
                new HotDealCacheDto(
                        hotDeal.getId(),
                        hotDeal.getUserId(),
                        request.getTitle(),
                        request.getDescription(),
                        request.getStartTime(),
                        request.getEndTime(),
                        request.getStatus(),
                        hotDeal.getDeleted(),
                        List.of(hotDealProductResponseDto));
        when(hotDealService.updateHotDeal(hotDeal.getId(), request)).thenReturn(hotDealCacheDto);

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
                .andExpect(jsonPath("$.data.title").value(request.getTitle()))
                .andExpect(jsonPath("$.data.description").value(request.getDescription()))
                .andExpect(jsonPath("$.data.startTime").value(request.getStartTime().toString()))//
                .andExpect(jsonPath("$.data.endTime").value(request.getEndTime().toString()))
                .andExpect(jsonPath("$.data.status").value(request.getStatus()))
                .andExpect(jsonPath("$.data.deleted").value(hotDeal.getDeleted()))
                .andExpect(jsonPath("$.data.hotDealProducts").isArray())
                .andExpect(jsonPath("$.data.hotDealProducts[0].hotDealProductId").value(hotDealProduct.getId()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].originalProductId").value(hotDealProductUpdateRequestDto.getProductId()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].productTitle").value(hotDealProduct.getProductTitle()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].originalPrice").value(hotDealProduct.getOriginalPrice()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].hotDealPrice").value(800))
                .andExpect(jsonPath("$.data.hotDealProducts[0].discountRate").value(hotDealProductUpdateRequestDto.getDiscountRate()))
                .andExpect(jsonPath("$.data.hotDealProducts[0].stock").value(hotDealProductUpdateRequestDto.getQuantity()));
    }

    @ParameterizedTest
    @CsvSource({
            // 1. title 빈 문자열
            "'', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '1|1|100|0.1', 'NotBlank', 'title 은 필수입니다.'",
            // 2. description 빈 문자열
            "' ', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '1|1|100|0.1', 'NotBlank', 'title 은 필수입니다.'",
            // 3. startTime null
            "'title', 'description', null, 2025-01-01T11:00:00, 'ACTIVE', '1|1|100|0.1', 'NotNull', 'startTime 은 필수입니다.'",
            // 4. endTime null
            "'title', 'description', 2025-01-01T10:00:00, null, 'ACTIVE', '1|1|100|0.1', 'NotNull', 'endTime 은 필수입니다.'",
            // 5. status null
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, ' ', '1|1|100|0.1', 'NotBlank', 'status 는 필수입니다.'",
            // 6. status HotDealStatus 값 아님
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'status', '1|1|100|0.1', 'Enum', 'ACTIVE, EXPIRED, SCHEDULED 만 허용합니다.'",
            // 7. productInfos empty
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '', 'NotEmpty', 'productInfos 는 최소 1개 이상이어야 합니다.'",
            // 8. productInfos hotDealProductId 음수
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '-1|1|100|0.1', 'Positive', 'hotDealProductId 는 양수여야 합니다.'",
            // 9. productInfos productId null
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '1|null|100|0.1', 'NotNull', 'productId 는 필수입니다.'",
            // 10. productInfos productId 음수
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '1|-1|100|0.1', 'Positive', 'productId 는 양수여야 합니다.'",
            // 11. productInfos quantity null
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '1|1|null|0.1', 'NotNull', 'quantity 는 필수입니다.'",
            // 12. productInfos quantity 음수
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '1|1|-1|0.1', 'Positive', 'quantity 는 양수여야 합니다.'",
            // 13. productInfos discountRate null
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '1|1|1|null', 'NotNull', 'discountRate 는 필수입니다.'",
            // 14. productInfos discountRate 범위
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '1|1|1|-0.1', 'DecimalMin', 'discountRate 는 0.0 이상이어야 합니다.'",
            "'title', 'description', 2025-01-01T10:00:00, 2025-01-01T11:00:00, 'ACTIVE', '1|1|1|1.1', 'DecimalMax', 'discountRate 는 1.0 이하이어야 합니다.'"
    })
    @DisplayName("hotDeal 수정_실패_입력 값 오류")
    public void updateHotDeal_failure_invalidInput(String title,
                                                   String description,
                                                   String startTimeStr,
                                                   String endTimeStr,
                                                   String statusStr,
                                                   String productInfosStr,
                                                   String expectedError,
                                                   String expectedValue) throws Exception {
        //given
        LocalDateTime startTime = (startTimeStr.equals("null")) ? null : LocalDateTime.parse(startTimeStr);
        LocalDateTime endTime = (endTimeStr.equals("null")) ? null : LocalDateTime.parse(endTimeStr);
        List<HotDealProductUpdateRequestDto> productInfos = HotDealTestUtil.updateHotDealParseProductInfos(productInfosStr);

        HotDealProduct hotDealProduct = HotDealProduct.create(1L, "hotDealProduct", 1000, 0.1, 100);
        ReflectionTestUtils.setField(hotDealProduct, "id", 1L);
        HotDeal hotDeal = createTestHotDeal("hotDeal", 1L, List.of(hotDealProduct));

        HotDealUpdateRequestDto request = new HotDealUpdateRequestDto(title, description,
                startTime, endTime, statusStr, productInfos);

        //when && then
        mockMvc.perform(put("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("입력 값에 대한 검증을 실패했습니다."))
                .andExpect(jsonPath("$.errors[?(@.code == '" + expectedError + "')].defaultMessage").value(expectedValue));
    }

    @Test
    @DisplayName("hotDeal 삭제_성공")
    public void deleteHotDeal_success() throws Exception {
        //given
        HotDealProduct hotDealProduct = HotDealProduct.create(1L, "hotDealProduct", 1000, 0.1, 100);
        ReflectionTestUtils.setField(hotDealProduct, "id", 1L);
        HotDeal hotDeal = createTestHotDeal("hotDeal", 1L, List.of(hotDealProduct));

        HotDealProductResponseDto hotDealProductResponseDto = new HotDealProductResponseDto(hotDealProduct, 0);
        HotDealCacheDto hotDealCacheDto = new HotDealCacheDto(hotDeal, List.of(hotDealProductResponseDto));
        when(hotDealService.deleteHotDeal(hotDeal.getId())).thenReturn(hotDealCacheDto);

        //when && then
        mockMvc.perform(delete("/hotdeals/" + hotDeal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("핫딜 삭제 성공"))
                .andExpect(jsonPath("$.data.hotDealId").value(hotDeal.getId()));
    }

    private HotDeal createTestHotDeal(String hotDealTitle, Long hotDealId, List<HotDealProduct> hp){
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = HotDeal.create(1L, hotDealTitle, "description", startTime, endTime, hp);
        ReflectionTestUtils.setField(hotDeal, "id", hotDealId);
        return hotDeal;
    }

}