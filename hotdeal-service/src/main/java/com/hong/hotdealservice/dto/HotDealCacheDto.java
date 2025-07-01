package com.hong.hotdealservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.dto.projection.HotDealSimpleDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HotDealCacheDto {

    private Long id;
    private String title;
    private String description;
    private HotDealStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;


    public HotDealCacheDto(HotDeal hotDeal){
        this.id = hotDeal.getId();
        this.title = hotDeal.getTitle();
        this.description = hotDeal.getDescription();
        this.status = hotDeal.getStatus();
        this.startTime = hotDeal.getStartTime();
        this.endTime = hotDeal.getEndTime();
    }

    public HotDealCacheDto(HotDealSimpleDto hotDeal){
        this.id = hotDeal.getId();
        this.title = hotDeal.getTitle();
        this.description = hotDeal.getDescription();
        this.status = hotDeal.getStatus();
        this.startTime = hotDeal.getStartTime();
        this.endTime = hotDeal.getEndTime();
    }
}
