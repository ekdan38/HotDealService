package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.HotDealProductCacheDto;
import com.hong.hotdealservice.dto.HotDealProductPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealProductResponseDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class HotDealProductRedisRepositoryTest {

    @Autowired
    HotDealProductRedisRepository hotDealProductRedisRepository;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private HotDeal createTestHotDeal(Long hotDealId, List<HotDealProduct> hotDealProducts){
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime startTime = now.minusDays(1);
        LocalDateTime endTime = now.plusDays(1);
        HotDeal hotDeal = HotDeal.create(1L, "hotDeal", "description", startTime, endTime, hotDealProducts);
        ReflectionTestUtils.setField(hotDeal, "id", hotDealId);
        return hotDeal;
    }
    private HotDealProduct createTestHotDealProduct(Long hotDealProductId, Long productId, String productTitle, int stock){
        HotDealProduct hotDealProduct = HotDealProduct.create(productId, productTitle, 1000, 0.1, stock);
        ReflectionTestUtils.setField(hotDealProduct, "id", hotDealProductId);
        return hotDealProduct;
    }
    private String gethotDealProductPagingKey(Long hotDealId, Long cursor, int size, String search){
        return "getHotDealProducts::hotdeal:" + hotDealId +
                "hotdeal_products:cursor:" + (cursor == null ? "" : cursor)
                + ":size:" + size + ":search:" + (search == null ? "" : search);
    }

    @AfterEach
    void clearRedis(){
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("hotDealProduct 저장 및 조회")
    public void saveAndFindHotDealProductCacheDto(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, 1L, "product1", 100);
        hotDealProducts.add(hotDealProduct1);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, 2L, "product1", 100);
        hotDealProducts.add(hotDealProduct2);

        createTestHotDeal(1L, hotDealProducts);

        List<HotDealProductCacheDto> hotDealProductCacheDtos = List.of(
                new HotDealProductCacheDto(hotDealProduct1),
                new HotDealProductCacheDto(hotDealProduct2));

        //when
        hotDealProductRedisRepository.saveAllHotDealProductWithTTL(hotDealProductCacheDtos);
        List<HotDealProductCacheDto> result = hotDealProductRedisRepository
                .findAllHotDealProductByIds(List.of(hotDealProduct1.getId(), hotDealProduct2.getId()));

        //then
        assertThat(result).hasSize(2);
        for(int i = 0; i < 2; i++){
            HotDealProductCacheDto dto = result.get(i);
            HotDealProduct hp = hotDealProducts.get(i);
            assertThat(dto.getHotDealId()).isEqualTo(hp.getHotDeal().getId());
            assertThat(dto.getHotDealProductId()).isEqualTo(hp.getId());
            assertThat(dto.getOriginalProductId()).isEqualTo(hp.getProductId());
            assertThat(dto.getProductTitle()).isEqualTo(hp.getProductTitle());
            assertThat(dto.getOriginalPrice()).isEqualTo(hp.getOriginalPrice());
            assertThat(dto.getHotDealPrice()).isEqualTo(hp.getHotDealPrice());
            assertThat(dto.getDiscountRate()).isEqualTo(hp.getDiscountRate());
        }
    }

    @Test
    @DisplayName("hotDealProduct 삭제(부분 무효화)")
    public void deleteAllHotDealProductByIds(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, 1L, "product1", 100);
        hotDealProducts.add(hotDealProduct1);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, 2L, "product1", 100);
        hotDealProducts.add(hotDealProduct2);

        createTestHotDeal(1L, hotDealProducts);

        List<HotDealProductCacheDto> hotDealProductCacheDtos = List.of(
                new HotDealProductCacheDto(hotDealProduct1),
                new HotDealProductCacheDto(hotDealProduct2));

        hotDealProductRedisRepository.saveAllHotDealProductWithTTL(hotDealProductCacheDtos);

        //when
        hotDealProductRedisRepository.deleteAllHotDealProductByIds(List.of(hotDealProduct1.getId()));

        //then
        List<HotDealProductCacheDto> result = hotDealProductRedisRepository
                .findAllHotDealProductByIds(List.of(hotDealProduct1.getId(), hotDealProduct2.getId()));
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isNull();
        HotDealProductCacheDto dto = result.get(1);
        HotDealProduct hp = hotDealProducts.get(1);
        assertThat(dto.getHotDealId()).isEqualTo(hp.getHotDeal().getId());
        assertThat(dto.getHotDealProductId()).isEqualTo(hp.getId());
        assertThat(dto.getOriginalProductId()).isEqualTo(hp.getProductId());
        assertThat(dto.getProductTitle()).isEqualTo(hp.getProductTitle());
        assertThat(dto.getOriginalPrice()).isEqualTo(hp.getOriginalPrice());
        assertThat(dto.getHotDealPrice()).isEqualTo(hp.getHotDealPrice());
        assertThat(dto.getDiscountRate()).isEqualTo(hp.getDiscountRate());
    }

    @Test
    @DisplayName("hotDealProductPaging 삭제(전면 무효화)")
    public void deleteAllGetHotDealProductsKeys(){
        //given
        ArrayList<HotDealProduct> hotDealProducts = new ArrayList<>();
        HotDealProduct hotDealProduct1 = createTestHotDealProduct(1L, 1L, "product1", 100);
        hotDealProducts.add(hotDealProduct1);
        HotDealProduct hotDealProduct2 = createTestHotDealProduct(2L, 2L, "product1", 100);
        hotDealProducts.add(hotDealProduct2);

        HotDeal hotDeal = createTestHotDeal(1L, hotDealProducts);
        Long cursor = 10L;
        int size = 3;
        String search = "search";

        List<HotDealProductResponseDto> hotDealProductResponseDtos = List.of(
                new HotDealProductResponseDto(hotDealProduct1),
                new HotDealProductResponseDto(hotDealProduct2));
        HotDealProductPagingResponseDto cache = new HotDealProductPagingResponseDto(8L, hotDeal.getId(), hotDealProductResponseDtos);

        String key = gethotDealProductPagingKey(hotDeal.getId(), cursor, size, search);
        redisTemplate.opsForValue().set(key, cache);

        //when
        hotDealProductRedisRepository.deleteAllGetHotDealProductsKeys();

        //then
        Set<String> keys = redisTemplate.keys(key);
        assertThat(keys).isEmpty();
    }
}
