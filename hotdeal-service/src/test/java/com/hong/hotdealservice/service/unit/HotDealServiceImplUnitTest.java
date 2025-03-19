package com.hong.hotdealservice.service.unit;

import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.common.exception.custom.HotDealException;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.client.Resilience4JProductServiceClient;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.dto.HotDealPagingCacheDto;
import com.hong.hotdealservice.dto.HotDealProductResponseDto;
import com.hong.hotdealservice.dto.HotDealCacheDto;
import com.hong.hotdealservice.repository.HotDealProductRedisRepository;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.service.HotDealServiceImpl;
import com.hong.hotdealservice.web.dto.HotDealProductRequestDto;
import com.hong.hotdealservice.web.dto.HotDealProductUpdateRequestDto;
import com.hong.hotdealservice.web.dto.HotDealRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotDealServiceImplUnitTest {

    @InjectMocks
    HotDealServiceImpl hotDealService;
    @Mock
    HotDealRepository hotDealRepository;
    @Mock
    Resilience4JProductServiceClient resilience4JProductServiceClient;
    @Mock
    HotDealProductRedisRepository hotDealProductRedisRepository;

    private Long adminId = 1L;
    private int originalPrice = 10000;
    private double discountRate = 0.1;
    private int hotDealPrice = (int) Math.floor(originalPrice * (1 - discountRate));
    private int stock = 100;

    private HotDealProduct createTestHotDealProduct(Long productId, String productTitle){
        return HotDealProduct.create(productId, productTitle, originalPrice, discountRate, 100);
    }

    private HotDeal createTestHotDeal(Long adminId, String hotDealTitle, LocalDateTime startTime, LocalDateTime endTime, List<HotDealProduct> hp){
        return HotDeal.create(adminId, hotDealTitle, "description", startTime, endTime, hp);
    }

    @Test
    @DisplayName("hotDeal 생성_성공")
    public void createHotDeal_success(){
        //given
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        List<HotDealProduct> hotDealProducts = List.of(hotDealProduct1, hotDealProduct2);
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal",startTime, endTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        List<HotDealProductRequestDto> productInfos = List.of(
                new HotDealProductRequestDto(1L, 100, 0.1),
                new HotDealProductRequestDto(2L, 200, 0.2));

        HotDealRequestDto requestDto = new HotDealRequestDto(hotDeal.getTitle(), hotDeal.getDescription(), hotDeal.getStartTime(),
                hotDeal.getEndTime(), productInfos);
        when(hotDealRepository.existsByTitle(requestDto.getTitle())).thenReturn(false);


        List<ProductStockUpdateRequestDto> decreaseReqeust = requestDto.getProductInfos().stream()
                .map(request -> new ProductStockUpdateRequestDto(
                        request.getProductId(),
                        request.getQuantity()))
                .toList();
        List<ProductStockUpdateResponseDto> resilienceResult = List.of(
                new ProductStockUpdateResponseDto(1L, "product1", 1000, 100),
                new ProductStockUpdateResponseDto(2L, "product2", 2000, 100));
        when(resilience4JProductServiceClient.decreaseStock(decreaseReqeust)).thenReturn(resilienceResult);

        when(hotDealRepository.save(any(HotDeal.class))).thenReturn(hotDeal);

        //when
        HotDealCacheDto result = hotDealService.createHotDeal(adminId, requestDto);

        //then
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(result.getAdminId()).isEqualTo(hotDeal.getUserId());
        assertThat(result.getTitle()).isEqualTo(hotDeal.getTitle());
        assertThat(result.getDescription()).isEqualTo(hotDeal.getDescription());
        assertThat(result.getStartTime()).isEqualTo(hotDeal.getStartTime());
        assertThat(result.getEndTime()).isEqualTo(hotDeal.getEndTime());
        assertThat(result.getDeleted()).isEqualTo(hotDeal.getDeleted());
        assertThat(result.getHotDealProducts()).hasSize(2);
        for(int i = 1; i <= 2; i++){
            HotDealProductResponseDto hp = result.getHotDealProducts().get(i - 1);
            if(i == 1)assertThat(hp.getProductTitle()).isEqualTo(hotDealProduct1.getProductTitle());
            else assertThat(hp.getProductTitle()).isEqualTo(hotDealProduct2.getProductTitle());
            assertThat(hp.getOriginalPrice()).isEqualTo(originalPrice);
            assertThat(hp.getHotDealPrice()).isEqualTo(hotDealPrice);
            assertThat(hp.getDiscountRate()).isEqualTo(discountRate);
            assertThat(hp.getStock()).isEqualTo(stock);
        }
    }

    @Test
    @DisplayName("hotDeal 생성_실패_이미 존재 하는 title")
    public void createHotDeal_failure_existsTitle(){
        //given
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDealRequestDto requestDto = setUpForCreateHotDealTest(adminId, startTime, endTime);

        when(hotDealRepository.existsByTitle(requestDto.getTitle())).thenReturn(true);

        //when && then
        assertThatThrownBy(() -> hotDealService.createHotDeal(adminId, requestDto)).isInstanceOf(HotDealException.class);
    }

    @Test
    @DisplayName("hotDeal 생성_실패_starTime 이 endTime 이후(유효 하지 않는 이벤트 시간)")
    public void createHotDeal_failure_invalidTime(){
        //given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime startTime = now.plusHours(1);
        LocalDateTime endTime = now.plusMinutes(1);
        HotDealRequestDto requestDto = setUpForCreateHotDealTest(adminId, startTime, endTime);

        when(hotDealRepository.existsByTitle(requestDto.getTitle())).thenReturn(false);

        //when && then
        assertThatThrownBy(() -> hotDealService.createHotDeal(adminId, requestDto)).isInstanceOf(HotDealException.class);
    }

    @Test
    @DisplayName("hotDeal 생성_실패_원본 상품 재고 감소 호출 실패")
    public void createHotDeal_failure_failToDecreaseStock(){
        //given
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusHours(1);
        HotDealRequestDto requestDto = setUpForCreateHotDealTest(adminId, startTime, endTime);

        when(hotDealRepository.existsByTitle(requestDto.getTitle())).thenReturn(false);
        when(resilience4JProductServiceClient.decreaseStock(anyList())).thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> hotDealService.createHotDeal(adminId, requestDto)).isInstanceOf(HotDealProductException.class);
    }

    @Test
    @DisplayName("hotDeals 페이징 조회_search 미 포함")
    public void getHotDeals_withoutSearch(){
        //given
        Long cursor = 50L;
        int size = 3;
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusMinutes(10);

        List<HotDeal> hotDeals = new ArrayList<>();
        for(long i = 1; i <= 3; i++){
            HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal" + i, startTime, endTime, List.of());
            ReflectionTestUtils.setField(hotDeal, "id", i);
            hotDeals.add(hotDeal);
        }
        List<HotDeal> reversedHotDeals = hotDeals.reversed();
        Long expectedCursor = 1L;

        when(hotDealRepository.findByCursorAndSearchAndSizeHotDeals(eq(cursor), eq(null), any(PageRequest.class))).thenReturn(reversedHotDeals);

        //when
        HotDealPagingCacheDto result = hotDealService.getHotDeals(null, cursor, size);

        //then
        assertThat(result.getCursor()).isEqualTo(expectedCursor);
        assertThat(result.getHotDeals()).hasSize(size);
        List<HotDealCacheDto> resultHotDeals = result.getHotDeals();
        resultHotDeals.forEach(h -> {
            assertThat(h.getAdminId()).isEqualTo(adminId);
            assertThat(h.getStartTime()).isEqualTo(startTime);
            assertThat(h.getEndTime()).isEqualTo(endTime);
        });
    }

    @Test
    @DisplayName("hotDeals 페이징 조회_search 포함")
    public void getHotDeals_withSearch(){
        //given
        String search = "even";
        Long cursor = 50L;
        int size = 3;
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusMinutes(10);

        List<HotDeal> hotDeals = new ArrayList<>();
        for(long i = 1; i <= 10; i++){
            HotDeal hotDeal;
            if(i % 2 == 0) hotDeal = createTestHotDeal(adminId, "evenHotDeal" + i, startTime, endTime, List.of());
            else hotDeal = createTestHotDeal(adminId, "oddHotDeal" + i, startTime, endTime, List.of());
            ReflectionTestUtils.setField(hotDeal, "id", i);
            if(i % 2 == 0 && i > 5) hotDeals.add(hotDeal);
        }
        List<HotDeal> reversedHotDeals = hotDeals.reversed();
        Long expectedCursor = 6L;

        when(hotDealRepository.findByCursorAndSearchAndSizeHotDeals(eq(cursor), eq(search), any(PageRequest.class))).thenReturn(reversedHotDeals);

        //when
        HotDealPagingCacheDto result = hotDealService.getHotDeals(search, cursor, size);

        //then
        assertThat(result.getCursor()).isEqualTo(expectedCursor);
        assertThat(result.getHotDeals()).hasSize(size);
        List<HotDealCacheDto> resultHotDeals = result.getHotDeals();
        resultHotDeals.forEach(h -> {
            assertThat(h.getAdminId()).isEqualTo(adminId);
            assertThat(h.getTitle()).startsWith(search);
            assertThat(h.getStartTime()).isEqualTo(startTime);
            assertThat(h.getEndTime()).isEqualTo(endTime);
        });
    }

    @Test
    @DisplayName("hotDeal 단건 조회_success")
    public void getHotDeal_success(){
        //given
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = startTime.plusMinutes(10);
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", startTime, endTime, List.of());
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        when(hotDealRepository.findById(hotDeal.getId())).thenReturn(Optional.of(hotDeal));

        //when
        HotDealCacheDto result = hotDealService.getHotDeal(hotDeal.getId());

        //then
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(result.getAdminId()).isEqualTo(hotDeal.getUserId());
        assertThat(result.getTitle()).isEqualTo(hotDeal.getTitle());
        assertThat(result.getDescription()).isEqualTo(hotDeal.getDescription());
        assertThat(result.getStartTime()).isEqualTo(hotDeal.getStartTime());
        assertThat(result.getEndTime()).isEqualTo(hotDeal.getEndTime());
        assertThat(result.getDeleted()).isEqualTo(hotDeal.getDeleted());
    }

    @Test
    @DisplayName("hotDeal 단건 조회_실패_존재 하지 않는 hotDeal")
    public void getHotDeal_failure_notFoundProduct(){
        //given
        Long productId = 1L;
        when(hotDealRepository.findById(productId)).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> hotDealService.getHotDeal(productId)).isInstanceOf(HotDealException.class);
    }

    @Test
    @DisplayName("updateHotDeal_성공_필드 업데이트, 상품 수정(재고 증가, 감소), 신규 상품 추가, 기존 상품 삭제")
    public void updateHotDeal_success() {
        // given
        Long hotDealId = 1L;
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime originalStart = now.minusHours(2);
        LocalDateTime originalEnd = now.plusHours(2);

        // 기존 상품
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L,  "Product1");
        ReflectionTestUtils.setField(hotDealProduct1, "id", 1L);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L,  "Product2");
        ReflectionTestUtils.setField(hotDealProduct2, "id", 2L);
        HotDealProduct hotDealProduct3 = createTestHotDealProduct(3L,  "Product3");
        ReflectionTestUtils.setField(hotDealProduct2, "id", 3L);
        List<HotDealProduct> originalProducts = List.of(hotDealProduct1, hotDealProduct2, hotDealProduct3);
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", originalStart, originalEnd, originalProducts);

        when(hotDealRepository.findByIdWithHotDealProducts(hotDealId)).thenReturn(java.util.Optional.of(hotDeal));

        // hotDeal field 값 update
        String updatedTitle = "Updated HotDeal";
        String updatedDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(3);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus updatedStatus = HotDealStatus.ACTIVE;

        // 1번 상품 수정, 2번 상품 그대로, 3번 상품 삭제, 4번 상품 추가
        // update 상품
        HotDealProductUpdateRequestDto updateProduct = new HotDealProductUpdateRequestDto(1L, 100L, 80, 0.3);
        // new 상품
        HotDealProductUpdateRequestDto newProduct = new HotDealProductUpdateRequestDto(null, 3L, 120, 0.2);
        // 기존 상품
        HotDealProductUpdateRequestDto originalProduct2 = new HotDealProductUpdateRequestDto(
                hotDealProduct2.getId(), hotDealProduct2.getProductId(), hotDealProduct2.getStock(), hotDealProduct2.getDiscountRate());

        // requestDto
        List<HotDealProductUpdateRequestDto> hotDealProducts = List.of(originalProduct2, updateProduct, newProduct);
        HotDealUpdateRequestDto requestDto = new HotDealUpdateRequestDto(
                updatedTitle, updatedDescription, newStartTime, newEndTime, updatedStatus.name(), hotDealProducts);

        List<ProductStockUpdateResponseDto> decreaseResponse = List.of(
                new ProductStockUpdateResponseDto(3L, "newProduct", 30000, 100));
        when(resilience4JProductServiceClient.decreaseStock(anyList())).thenReturn(decreaseResponse);

        List<ProductStockUpdateResponseDto> increaseResponse = List.of(
                new ProductStockUpdateResponseDto(1L, "Product1", 10000, 100));
        when(resilience4JProductServiceClient.increaseStock(anyList())).thenReturn(increaseResponse);

        // when
        HotDealCacheDto result = hotDealService.updateHotDeal(hotDealId, requestDto);

        // then
        assertThat(result.getTitle()).isEqualTo(updatedTitle);
        assertThat(result.getDescription()).isEqualTo(updatedDescription);
        assertThat(result.getStartTime()).isEqualTo(newStartTime);
        assertThat(result.getEndTime()).isEqualTo(newEndTime);

        List<HotDealProductResponseDto> updatedProducts = result.getHotDealProducts();
        assertThat(updatedProducts).hasSize(3);

        HotDealProductResponseDto originalProduct = updatedProducts.stream()
                .filter(p -> p.getProductTitle().equals("Product2"))
                .findFirst().orElse(null);
        assertThat(originalProduct).isNotNull();

        HotDealProductResponseDto updatedProduct = updatedProducts.stream()
                .filter(p -> p.getProductTitle().equals("Product1"))
                .findFirst().orElse(null);
        assertThat(updatedProduct).isNotNull();

        HotDealProductResponseDto newProductResponse = updatedProducts.stream()
                .filter(p -> p.getProductTitle().equals("newProduct"))
                .findFirst().orElse(null);
        assertThat(newProductResponse).isNotNull();
    }

    @Test
    @DisplayName("updateHotDeal_실패_존재 하지 않는 hotDeal")
    public void updateHotDeal_failure_notFoundHotDeal() {
        // given
        Long hotDealId = 1L;
        String updatedTitle = "Updated HotDeal";
        String updatedDescription = "Updated Description";
        LocalDateTime newStart = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newEnd = newStart.plusHours(3);
        HotDealStatus updatedStatus = HotDealStatus.ACTIVE;
        HotDealUpdateRequestDto updateRequest = new HotDealUpdateRequestDto(updatedTitle, updatedDescription, newStart, newEnd, updatedStatus.name(), List.of());

        when(hotDealRepository.findByIdWithHotDealProducts(hotDealId)).thenReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> hotDealService.updateHotDeal(hotDealId, updateRequest)).isInstanceOf(HotDealException.class);
    }

    @Test
    @DisplayName("updateHotDeal_실패_이미 존재 하는 hotDeal Title")
    public void updateHotDeal_failure_already_exists_title() {
        // given
        Long hotDealId = 1L;
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime originalStart = now.minusHours(2);
        LocalDateTime originalEnd = now.plusHours(2);

        // 기존 상품
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", originalStart, originalEnd, List.of());

        when(hotDealRepository.findByIdWithHotDealProducts(hotDealId)).thenReturn(java.util.Optional.of(hotDeal));

        // hotDeal field 업데이트
        String updatedTitle = "Updated HotDeal";
        String updatedDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(3);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus updatedStatus = HotDealStatus.ACTIVE;

        // requestDto
        HotDealUpdateRequestDto requestDto = new HotDealUpdateRequestDto(
                updatedTitle, updatedDescription, newStartTime, newEndTime, updatedStatus.name(), List.of());

        when(hotDealRepository.existsByTitle(updatedTitle)).thenReturn(true);

        // when && then
        assertThatThrownBy(() -> hotDealService.updateHotDeal(hotDealId, requestDto)).isInstanceOf(HotDealException.class);
    }

    @Test
    @DisplayName("updateHotDeal_실패_starTime 이 endTime 이후(유효 하지 않는 이벤트 시간)")
    public void updateHotDeal_failure_invalidTime() {
        // given
        Long hotDealId = 1L;
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime originalStart = now.minusHours(2);
        LocalDateTime originalEnd = now.plusHours(2);

        // 기존 상품
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", originalStart, originalEnd, List.of());

        when(hotDealRepository.findByIdWithHotDealProducts(hotDealId)).thenReturn(java.util.Optional.of(hotDeal));

        // hotDeal field 업데이트
        String updatedTitle = "Updated HotDeal";
        String updatedDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.plusHours(5);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus updatedStatus = HotDealStatus.ACTIVE;

        // requestDto
        HotDealUpdateRequestDto requestDto = new HotDealUpdateRequestDto(
                updatedTitle, updatedDescription, newStartTime, newEndTime, updatedStatus.name(), List.of());

        when(hotDealRepository.existsByTitle(updatedTitle)).thenReturn(false);

        // when && then
        assertThatThrownBy(() -> hotDealService.updateHotDeal(hotDealId, requestDto)).isInstanceOf(HotDealException.class);
    }

    @Test
    @DisplayName("updateHotDeal_실패_원본 상품 재고 감소 호출 실패")
    public void updateHotDeal_failure_failToDecreaseStock() {
        // given
        Long hotDealId = 1L;
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime originalStart = now.minusHours(2);
        LocalDateTime originalEnd = now.plusHours(2);

        // 기존 상품
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L,  "Product1");
        ReflectionTestUtils.setField(hotDealProduct1, "id", 1L);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L,  "Product2");
        ReflectionTestUtils.setField(hotDealProduct2, "id", 2L);
        HotDealProduct hotDealProduct3 = createTestHotDealProduct(3L,  "Product3");
        ReflectionTestUtils.setField(hotDealProduct2, "id", 3L);
        List<HotDealProduct> originalProducts = List.of(hotDealProduct1, hotDealProduct2, hotDealProduct3);
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", originalStart, originalEnd, originalProducts);

        when(hotDealRepository.findByIdWithHotDealProducts(hotDealId)).thenReturn(java.util.Optional.of(hotDeal));

        // hotDeal field 값 update
        String updatedTitle = "Updated HotDeal";
        String updatedDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(3);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus updatedStatus = HotDealStatus.ACTIVE;

        // update 상품
        HotDealProductUpdateRequestDto updateProduct = new HotDealProductUpdateRequestDto(1L, 100L, 80, 0.3);
        // new 상품
        HotDealProductUpdateRequestDto newProduct = new HotDealProductUpdateRequestDto(null, 3L, 120, 0.2);
        // 기존 상품
        HotDealProductUpdateRequestDto originalProduct2 = new HotDealProductUpdateRequestDto(
                hotDealProduct2.getId(), hotDealProduct2.getProductId(), hotDealProduct2.getStock(), hotDealProduct2.getDiscountRate());

        // requestDto
        List<HotDealProductUpdateRequestDto> hotDealProducts = List.of(originalProduct2, updateProduct, newProduct);
        HotDealUpdateRequestDto requestDto = new HotDealUpdateRequestDto(
                updatedTitle, updatedDescription, newStartTime, newEndTime, updatedStatus.name(), hotDealProducts);

        when(resilience4JProductServiceClient.decreaseStock(anyList())).thenReturn(List.of());

        // when && then
        assertThatThrownBy(() -> hotDealService.updateHotDeal(hotDealId, requestDto)).isInstanceOf(HotDealProductException.class);
    }

    @Test
    @DisplayName("updateHotDeal_실패_원본 상품 재고 증가 호출 실패")
    public void updateHotDeal_failure_failToIncreaseStock() {
        // given
        Long hotDealId = 1L;
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime originalStart = now.minusHours(2);
        LocalDateTime originalEnd = now.plusHours(2);

        // 기존 상품
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L,  "Product1");
        ReflectionTestUtils.setField(hotDealProduct1, "id", 1L);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L,  "Product2");
        ReflectionTestUtils.setField(hotDealProduct2, "id", 2L);
        HotDealProduct hotDealProduct3 = createTestHotDealProduct(3L,  "Product3");
        ReflectionTestUtils.setField(hotDealProduct2, "id", 3L);
        List<HotDealProduct> originalProducts = List.of(hotDealProduct1, hotDealProduct2, hotDealProduct3);
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", originalStart, originalEnd, originalProducts);

        when(hotDealRepository.findByIdWithHotDealProducts(hotDealId)).thenReturn(java.util.Optional.of(hotDeal));

        // hotDeal field 값 update
        String updatedTitle = "Updated HotDeal";
        String updatedDescription = "Updated Description";
        LocalDateTime newNow = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newStartTime = newNow.minusHours(3);
        LocalDateTime newEndTime = newNow.plusHours(3);
        HotDealStatus updatedStatus = HotDealStatus.ACTIVE;

        // update 상품
        HotDealProductUpdateRequestDto updateProduct = new HotDealProductUpdateRequestDto(1L, 100L, 80, 0.3);
        // new 상품
        HotDealProductUpdateRequestDto newProduct = new HotDealProductUpdateRequestDto(null, 3L, 120, 0.2);
        // 기존 상품
        HotDealProductUpdateRequestDto originalProduct2 = new HotDealProductUpdateRequestDto(
                hotDealProduct2.getId(), hotDealProduct2.getProductId(), hotDealProduct2.getStock(), hotDealProduct2.getDiscountRate());

        // requestDto
        List<HotDealProductUpdateRequestDto> hotDealProducts = List.of(originalProduct2, updateProduct, newProduct);
        HotDealUpdateRequestDto requestDto = new HotDealUpdateRequestDto(
                updatedTitle, updatedDescription, newStartTime, newEndTime, updatedStatus.name(), hotDealProducts);

        List<ProductStockUpdateResponseDto> decreaseResponse = List.of(
                new ProductStockUpdateResponseDto(3L, "newProduct", 30000, 100));
        when(resilience4JProductServiceClient.decreaseStock(anyList())).thenReturn(decreaseResponse);
        when(resilience4JProductServiceClient.increaseStock(anyList())).thenReturn(List.of());

        // when && then
        assertThatThrownBy(() -> hotDealService.updateHotDeal(hotDealId, requestDto)).isInstanceOf(HotDealProductException.class);
    }

    @Test
    @DisplayName("hotDeal 삭제_성공")
    public void deleteHotDeal_success(){
        //given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime startTime = now.minusHours(2);
        LocalDateTime endTime = now.plusHours(2);
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        List<HotDealProduct> hotDealProducts = List.of(hotDealProduct1, hotDealProduct2);
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", startTime, endTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        when(hotDealRepository.findByIdWithHotDealProducts(hotDeal.getId())).thenReturn(Optional.of(hotDeal));

        List<ProductStockUpdateResponseDto> increaseResponse = List.of(
                new ProductStockUpdateResponseDto(1L, "Product1", 10000, 100),
                new ProductStockUpdateResponseDto(2L, "Product2", 10000, 100));
        when(resilience4JProductServiceClient.increaseStock(anyList())).thenReturn(increaseResponse);

        //when
        HotDealCacheDto result = hotDealService.deleteHotDeal(hotDeal.getId());

        //then
        assertThat(result.getHotDealId()).isEqualTo(hotDeal.getId());
        assertThat(result.getDeleted()).isTrue();
        assertThat(result.getHotDealProducts()).hasSize(2);
        result.getHotDealProducts().forEach(hp -> assertThat(hp.getStock()).isEqualTo(0));
    }

    @Test
    @DisplayName("hotDeal 삭제_실패_존재 하지 않는 hotDeal")
    public void deleteHotDeal_failure_notFoundHotDeal(){
        //given
        Long hotDealId = 1L;

        when(hotDealRepository.findByIdWithHotDealProducts(hotDealId)).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> hotDealService.deleteHotDeal(hotDealId)).isInstanceOf(HotDealException.class);
    }

    @Test
    @DisplayName("hotDeal 삭제_실패_원본 상품 재고 증가 요청 실패")
    public void deleteHotDeal_failure_failToIncreaseStock(){
        //given
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime startTime = now.minusHours(2);
        LocalDateTime endTime = now.plusHours(2);
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        List<HotDealProduct> hotDealProducts = List.of(hotDealProduct1, hotDealProduct2);
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal", startTime, endTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);

        when(hotDealRepository.findByIdWithHotDealProducts(hotDeal.getId())).thenReturn(Optional.of(hotDeal));
        when(resilience4JProductServiceClient.increaseStock(anyList())).thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> hotDealService.deleteHotDeal(hotDeal.getId())).isInstanceOf(HotDealProductException.class);
    }

    private HotDealRequestDto setUpForCreateHotDealTest(Long adminId, LocalDateTime startTime, LocalDateTime endTime){
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, "product1");
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, "product2");
        List<HotDealProduct> hotDealProducts = List.of(hotDealProduct1, hotDealProduct2);
        HotDeal hotDeal = createTestHotDeal(adminId, "hotDeal",startTime, endTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", 1L);
        List<HotDealProductRequestDto> productInfos = List.of(
                new HotDealProductRequestDto(1L, 100, 0.1),
                new HotDealProductRequestDto(2L, 200, 0.2));

        return new HotDealRequestDto(hotDeal.getTitle(), hotDeal.getDescription(), hotDeal.getStartTime(), hotDeal.getEndTime(), productInfos);
    }

}