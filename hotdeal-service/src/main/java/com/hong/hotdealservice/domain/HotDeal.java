package com.hong.hotdealservice.domain;

import com.hong.hotdealservice.domain.base.TimeEntity;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class HotDeal extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hotdeal_id")
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HotDealStatus status;

    @Column(nullable = false)
    private Boolean deleted;

    @Column(nullable = true)
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Boolean isStockSynced = false;

    @Column(nullable = true)
    private LocalDateTime stockSyncedAt;

    @OneToMany(mappedBy = "hotDeal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HotDealProduct> hotDealProducts = new ArrayList<>();

    private HotDeal(Long adminId, String title, String description, LocalDateTime startTime, LocalDateTime endTime) {
        this.userId = adminId;
        this.deleted = false;
        this.title = title;
        this.description = description;
        this.status = HotDealStatus.SCHEDULED;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // == 생성 메서드 ==
    public static HotDeal create(Long adminId, String title, String description,
                                 LocalDateTime startTime, LocalDateTime endTime, List<HotDealProduct> hotDealProducts){
        HotDeal hotDeal = new HotDeal(adminId, title, description, startTime.truncatedTo(ChronoUnit.MILLIS), endTime.truncatedTo(ChronoUnit.MILLIS));
        hotDeal.addHotDealProducts(hotDealProducts);
        return hotDeal;
    }

    // == HotDealProduct 와 연관 관계 메서드 ==
    public void addHotDealProducts(List<HotDealProduct> hotDealProducts){
        hotDealProducts.forEach(this::addHotDealProduct);
    }
    private void addHotDealProduct(HotDealProduct hotDealProduct){
        this.hotDealProducts.add(hotDealProduct);
        hotDealProduct.setHotDeal(this);
    }

    // == HotDeal softDelete 처리
    public void softDelete(){
        this.deleted = true;
        this.status = HotDealStatus.DELETED;
    }

    //== HotDeal 이 현재 시각 기준으로 주문 처리가 가능한지 판단 ==
    public boolean orderAble(){
        if(this.deleted) return false;
        if(this.status == HotDealStatus.EXPIRED) return false;
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    // == HotDealProducts remove 메서드 ==
    public void removeHotDealProducts(List<HotDealProduct> hotDealProducts) {
        hotDealProducts.forEach(hp -> hp.setHotDeal(null));
        this.hotDealProducts.removeAll(hotDealProducts);
    }

    // == Status 변경 메서드 ==
    public void updateStatus(HotDealStatus status){
        this.status = status;
    }

    // == HotDeal Field update 메서드 ==
    public void updateFields(String title, String description, LocalDateTime startTime, LocalDateTime endTime, HotDealStatus status){
        this.title = title;
        this.description = description;
        this.startTime = startTime.truncatedTo(ChronoUnit.MILLIS);
        this.endTime = endTime.truncatedTo(ChronoUnit.MILLIS);
        updateStatus(status);
    }

    // == 핫딜 종료 후 Redis와 재고 동기화 처리 ==
    public void syncRedisStock(LocalDateTime syncedAt){
        this.isStockSynced = true;
        this.stockSyncedAt = syncedAt;
    }
}
