package com.hong.hotdealservice.dto.projection;

import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.dto.HotDealCacheDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotDealSimpleDto {
    private Long id;
    private String title;
    private String description;
    private HotDealStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public HotDealSimpleDto(HotDealCacheDto cacheDto) {
        this.id = cacheDto.getId();
        this.title = cacheDto.getTitle();
        this.description = cacheDto.getDescription();
        this.status = cacheDto.getStatus();
        this.startTime = cacheDto.getStartTime();
        this.endTime = cacheDto.getEndTime();
    }
}
