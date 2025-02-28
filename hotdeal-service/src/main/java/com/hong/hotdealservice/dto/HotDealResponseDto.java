package com.hong.hotdealservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HotDealResponseDto {

    private Long hotDealId;

    private Long adminId;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private Boolean deleted;

    private List<HotDealProductResponseDto> hotDealProducts;


    public HotDealResponseDto(Long hotDealId, Long adminId, String title, String description, LocalDateTime startTime, LocalDateTime endTime, String status, Boolean deleted) {
        this.hotDealId = hotDealId;
        this.adminId = adminId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.deleted = deleted;
    }

}
