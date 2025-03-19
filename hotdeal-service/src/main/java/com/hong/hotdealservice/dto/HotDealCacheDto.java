package com.hong.hotdealservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.hotdealservice.domain.HotDeal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HotDealCacheDto {

    private Long hotDealId;
    private Long adminId;
    private String title;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime endTime;
    private String status;
    private Boolean deleted;
    private List<HotDealProductResponseDto> hotDealProducts;


    public HotDealCacheDto(HotDeal hotDeal){
        this.hotDealId = hotDeal.getId();
        this.adminId = hotDeal.getUserId();
        this.title = hotDeal.getTitle();
        this.description = hotDeal.getDescription();
        this.startTime = hotDeal.getStartTime();
        this.endTime = hotDeal.getEndTime();
        this.status = hotDeal.getStatus().name();
        this.deleted = hotDeal.getDeleted();
    }

    public HotDealCacheDto(HotDeal hotDeal, List<HotDealProductResponseDto> hotDealProducts){
        this.hotDealId = hotDeal.getId();
        this.adminId = hotDeal.getUserId();
        this.title = hotDeal.getTitle();
        this.description = hotDeal.getDescription();
        this.startTime = hotDeal.getStartTime();
        this.endTime = hotDeal.getEndTime();
        this.status = hotDeal.getStatus().name();
        this.deleted = hotDeal.getDeleted();
        this.hotDealProducts = hotDealProducts;
    }

}
