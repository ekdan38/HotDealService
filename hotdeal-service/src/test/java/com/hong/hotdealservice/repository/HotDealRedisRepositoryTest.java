package com.hong.hotdealservice.repository;

import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.dto.HotDealCacheDto;
import com.hong.hotdealservice.dto.HotDealPagingCacheDto;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class HotDealRedisRepositoryTest {

    @Autowired
    HotDealRedisRepository hotDealRedisRepository;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private HotDeal createTestHotDeal(Long hotDealId){
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime startTime = now.minusDays(1);
        LocalDateTime endTime = now.plusDays(1);
        HotDeal hotDeal = HotDeal.create(1L, "hotDeal", "description", startTime, endTime, List.of());
        ReflectionTestUtils.setField(hotDeal, "id", hotDealId);
        return hotDeal;
    }

    @AfterEach
    void clearRedis(){
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("hotDeal 저장 및 조회")
    public void saveAndFindHotDealCacheDto(){
        //given
        ArrayList<HotDeal> hotDeals = new ArrayList<>();
        HotDeal hotDeal1 = createTestHotDeal(1L);
        hotDeals.add(hotDeal1);
        HotDeal hotDeal2 = createTestHotDeal(2L);
        hotDeals.add(hotDeal2);

        HotDealCacheDto hotDealCacheDto1 = new HotDealCacheDto(hotDeal1);
        HotDealCacheDto hotDealCacheDto2 = new HotDealCacheDto(hotDeal2);

        //when
        hotDealRedisRepository.saveAllHotDealWithTTL(List.of(hotDealCacheDto1, hotDealCacheDto2));
        List<HotDealCacheDto> result = hotDealRedisRepository.findAllHotDealByIds(List.of(hotDeal1.getId(), hotDeal2.getId()));

        //then
        assertThat(result).hasSize(2);
        for(int i = 0; i < 2; i++){
            HotDealCacheDto dto = result.get(i);
            HotDeal hotDeal = hotDeals.get(i);
            assertThat(dto.getHotDealId()).isEqualTo(hotDeal.getId());
            assertThat(dto.getAdminId()).isEqualTo(hotDeal.getUserId());
            assertThat(dto.getTitle()).isEqualTo(hotDeal.getTitle());
            assertThat(dto.getDescription()).isEqualTo(hotDeal.getDescription());
            assertThat(dto.getStartTime()).isEqualTo(hotDeal.getStartTime());
            assertThat(dto.getEndTime()).isEqualTo(hotDeal.getEndTime());
            assertThat(dto.getStatus()).isEqualTo(hotDeal.getStatus().name());
            assertThat(dto.getDeleted()).isEqualTo(hotDeal.getDeleted());
        }
    }

    @Test
    @DisplayName("hotDealPaging 조회")
    public void findPagingByCursorAndSizeAndSearch(){
        //given
        ArrayList<HotDeal> hotDeals = new ArrayList<>();
        HotDeal hotDeal1 = createTestHotDeal(1L);
        hotDeals.add(hotDeal1);
        HotDeal hotDeal2 = createTestHotDeal(2L);
        hotDeals.add(hotDeal2);

        Long cursor = null;
        int size = 5;
        String search = "search";

        HotDealPagingCacheDto hotDealPagingCacheDto = new HotDealPagingCacheDto(5L, List.of(
                new HotDealCacheDto(hotDeal1),
                new HotDealCacheDto(hotDeal2)
        ));
        String key = "getHotDeals::hot_deals:cursor:" + (cursor == null ? "" : cursor)
                + ":size:" + size + ":search:" + (search == null ? "" : search);
        redisTemplate.opsForValue().set(key, hotDealPagingCacheDto);

        //when
        HotDealPagingCacheDto result = hotDealRedisRepository.findPagingByCursorAndSizeAndSearch(cursor, size, search);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getCursor()).isEqualTo(5);
        assertThat(result.getHotDeals()).hasSize(2);
        for(int i = 0; i < 2; i++){
            HotDealCacheDto dto = result.getHotDeals().get(i);
            HotDeal hotDeal = hotDeals.get(i);
            assertThat(dto.getHotDealId()).isEqualTo(hotDeal.getId());
            assertThat(dto.getAdminId()).isEqualTo(hotDeal.getUserId());
            assertThat(dto.getTitle()).isEqualTo(hotDeal.getTitle());
            assertThat(dto.getDescription()).isEqualTo(hotDeal.getDescription());
            assertThat(dto.getStartTime()).isEqualTo(hotDeal.getStartTime());
            assertThat(dto.getEndTime()).isEqualTo(hotDeal.getEndTime());
            assertThat(dto.getStatus()).isEqualTo(hotDeal.getStatus().name());
            assertThat(dto.getDeleted()).isEqualTo(hotDeal.getDeleted());
        }
    }


}