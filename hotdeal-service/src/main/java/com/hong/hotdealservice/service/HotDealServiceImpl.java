package com.hong.hotdealservice.service;

import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.dto.ProductStockUpdateResponseDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealException;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.client.Resilience4JProductServiceClient;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.dto.HotDealPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealProductResponseDto;
import com.hong.hotdealservice.dto.HotDealResponseDto;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.web.dto.HotDealProductRequestDto;
import com.hong.hotdealservice.web.dto.HotDealProductUpdateRequestDto;
import com.hong.hotdealservice.web.dto.HotDealRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealServiceImpl]")
@Transactional(readOnly = true)
public class HotDealServiceImpl implements HotDealService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HotDealRepository hotDealRepository;
    private final Resilience4JProductServiceClient resilience4JProductServiceClient;

    // HotDeal 생성
    @Override
    @Transactional
    @CacheEvict(cacheNames = "getHotDeals", allEntries = true)
    public HotDealResponseDto createHotDeal(Long adminId, HotDealRequestDto requestDto) {
        // 같은 title 로 HotDeal 이 존재 하는지 검증
        existsByTitleAndValidate(requestDto);

        // HotDeal 시작 시간, 종료 시간 검증
        validateHotDealDate(requestDto.getStartTime(), requestDto.getEndTime());

        // requestDto 에서 핫딜 상품 등록하고자 하는 정보 Dto 변환
        List<ProductStockUpdateRequestDto> productStockUpdateRequestDtos = convertToProductUpdateStockDtos(requestDto.getProductInfos());

        // feignClient 로 Product 재고 감소 feignClient 호출, 검증
        List<ProductStockUpdateResponseDto> productStockUpdateResponseDtos = decreaseOriginalProductStockAndValidate(productStockUpdateRequestDtos);

        // HotDeal 생성, 저장
        HotDeal savedHotDeal = createHotDealAndSave(adminId, productStockUpdateResponseDtos, requestDto);

        // 응답 dto 변환
        return convertHotDealResponseDtoWithHotDealProducts(savedHotDeal);
    }

    // HotDeal 페이징 조회 (hotDealProduct 미포함)
    @Override
    @Cacheable(cacheNames = "getHotDeals"
            , key = "'hot_deals:cursor:' + #cursor + ':size:' + #size + ':search:' + (#search != null ? #search : '')"
            , cacheManager = "HotDealCacheManager")
    public HotDealPagingResponseDto getHotDeals(String search, Long cursor, int size) {

        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if (cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        List<HotDeal> page = hotDealRepository.findByCursorAndSearchAndSizeHotDeals(cursor, search, pageRequest);

        // Dto 변환
        List<HotDealResponseDto> hotDealResponseDtos = page.stream().map(this::convertHotDealResponseDto)
                .collect(Collectors.toList());

        // nextCursor 지정
        Long nextCursor = hotDealResponseDtos.isEmpty() ? 0 : hotDealResponseDtos.get(hotDealResponseDtos.size() - 1).getHotDealId();

        return new HotDealPagingResponseDto(nextCursor, hotDealResponseDtos);
    }

    // HotDeal 단건 조회
    @Override
    @Cacheable(cacheNames = "getHotDeal", key = "'hot_deals:' + #hotDealId", cacheManager = "HotDealCacheManager")
    public HotDealResponseDto getHotDeal(Long hotDealId) {
        // hotDealProducts Fetch Join 조회, 검증
        HotDeal hotDeal = fetchHotDealAndValidate(hotDealId);

        // 응답 Dto 변환
        return convertHotDealResponseDto(hotDeal);
    }

    // HotDeal 수정
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "getHotDeals", allEntries = true),
            @CacheEvict(cacheNames = "getHotDeal", key = "'hot_deals:' + #hotDealId"),
            @CacheEvict(cacheNames = "getHotDealProducts", allEntries = true)
    })
    public HotDealResponseDto updateHotDeal(Long hotDealId, HotDealUpdateRequestDto requestDto) {
        // hotDeal 조회 및 기본 검증
        HotDeal hotDeal = fetchHotDealByIdWithHotDealProductsAndValidate(hotDealId);
        // title 검증
        validateNewTitle(requestDto, hotDeal);
        // 날짜 검증
        validateHotDealDate(requestDto.getStartTime(), requestDto.getEndTime());

        // HotDeal 필드 업데이트
        hotDeal.updateFields(
                requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getStartTime(),
                requestDto.getEndTime(),
                requestDto.getStatus());

        // 원본 상품에 대한 재고 감소, 증가 위한 리스트
        List<ProductStockUpdateRequestDto> increaseProductStockRequestDtos = new ArrayList<>();
        List<ProductStockUpdateRequestDto> decreaseProductStockRequestDtos = new ArrayList<>();
        // 삭제, 수정 된 HotDealProductId 에 대한 캐시 부분 무효화를 위한 리스트
        List<Long> hotDealProductIdsToEvict = new ArrayList<>();

        // 삭제 대상 hotDealProduct 삭제 처리
        deleteHotDealProductsFromUpdateRequest(requestDto, hotDeal, increaseProductStockRequestDtos, hotDealProductIdsToEvict);

        // 수정 대상 stock increase, decrease, discountRate Update 처리
        updateHotDealProducts(requestDto, hotDeal, increaseProductStockRequestDtos, decreaseProductStockRequestDtos, hotDealProductIdsToEvict);

        // 새로 생성 요청 받은 hotDealProduct 생성, product-service 재고 감소 호출
        createNewHotDealProductsAndDecreaseOriginalProductStock(requestDto, hotDeal, decreaseProductStockRequestDtos);

        // product-service 재고 증가 호출
        // decreaseStock 은 성공 했지만, increaseStock 이 실패 하면 decreaseStock 처리 된 데이터 increaseStock 처리
        try{
            increaseOriginalProductStockAndValidate(increaseProductStockRequestDtos);
        }catch (HotDealProductException e){
            increaseOriginalProductStockAndValidate(decreaseProductStockRequestDtos);
            throw e;
        }
        // 삭제, 수정 된 HotDealProductId 에 대한 캐시 부분 무효화
        List<String> keys = hotDealProductIdsToEvict.stream()
                .map(id -> "getHotDealProduct::hotdeal_products:" + id)
                .collect(Collectors.toList());
        redisTemplate.delete(keys);

        // 응답 Dto 변환
        return convertHotDealResponseDtoWithHotDealProducts(hotDeal);
    }

    // hotDeal 삭제
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "getHotDeals", allEntries = true),
            @CacheEvict(cacheNames = "getHotDeal", key = "'hot_deals:' + #hotDealId")
    })
    public HotDealResponseDto deleteHotDeal(Long hotDealId) {
        // hotDealProducts Fetch Join 조회, 검증
        HotDeal hotDeal = fetchHotDealByIdWithHotDealProductsAndValidate(hotDealId);

        // HotDeal 삭제 (softDelete)
        // 1. 핫딜 상품 남은 재고 원본 상품에 반영
        // hotDealProducts 재고 감소 요청 Dto 변환
        List<ProductStockUpdateRequestDto> increaseProductStockRequestDtos = hotDeal.getHotDealProducts().stream()
                .map(hp -> new ProductStockUpdateRequestDto(hp.getProductId(), hp.getStock()))
                .collect(Collectors.toList());
        // 2. 핫딜 상품 남은 재고 원본 상품에 재고 감소 요청
        increaseOriginalProductStockAndValidate(increaseProductStockRequestDtos);

        hotDeal.softDelete();

        // 응답 Dto 변환
        return convertHotDealResponseDtoWithHotDealProducts(hotDeal);
    }

    // 같은 title 로 HotDeal 이 존재 하는지 검증
    private void existsByTitleAndValidate(HotDealRequestDto requestDto) {
        if (hotDealRepository.existsByTitle(requestDto.getTitle())) {
            log.debug("이미 존재하는 핫딜 Title 입니다. hotDealTitle = {}", requestDto.getTitle());
            throw new HotDealException(ErrorCode.HOTDEAL_TITLE_ALREADY_EXISTS, requestDto.getTitle());
        }
    }

    // HotDeal 시작 시간, 종료 시간 검증
    private void validateHotDealDate(LocalDateTime startTime, LocalDateTime endTime) {
        // 시작 시간이 종료 시간보다 늦으면
        if (startTime.isAfter(endTime)) {
            log.debug("시작 시간이 종료 시간보다 이후일 수 없습니다. startTime = {}, endTime = {}", startTime, endTime);
            throw new HotDealException(ErrorCode.HOTDEAL_INVALID_TIME, startTime, endTime);
        }
    }

    // requestDto 에서 핫딜 상품 등록하고자 하는 정보 Dto 변환
    private List<ProductStockUpdateRequestDto> convertToProductUpdateStockDtos(List<HotDealProductRequestDto> productInfos) {
        return productInfos.stream()
                .map(request -> new ProductStockUpdateRequestDto(
                        request.getProductId(),
                        request.getQuantity()))
                .collect(Collectors.toList());
    }

    // HotDeal 생성, 저장
    private HotDeal createHotDealAndSave(Long adminId,
                                         List<ProductStockUpdateResponseDto> productStockUpdateResponseDtos,
                                         HotDealRequestDto requestDto) {

        // productStockUpdateResponseDtos Map 변환
        Map<Long, ProductStockUpdateResponseDto> productMap = productStockUpdateResponseDtos.stream()
                .collect(Collectors.toMap(ProductStockUpdateResponseDto::getProductId, product -> product));

        // 핫딜 상품을 저장할 List
        List<HotDealProduct> hotDealProducts = new ArrayList<>();

        // HotDealProduct 생성
        for (HotDealProductRequestDto productInfo : requestDto.getProductInfos()) {
            Long productId = productInfo.getProductId();
            ProductStockUpdateResponseDto responseDto = productMap.get(productId);

            // HotDealProduct 생성, hotDealProducts 에 Add
            hotDealProducts.add(HotDealProduct.create(
                    responseDto.getProductId(),
                    responseDto.getTitle(),
                    responseDto.getPrice(),
                    productInfo.getDiscountRate(),
                    productInfo.getQuantity()
            ));
        }

        // HotDeal 생성
        HotDeal hotDeal = HotDeal.create(
                adminId,
                requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getStartTime(),
                requestDto.getEndTime(),
                hotDealProducts);

        // startTime 이 시작 시간 지나고, endTime 이전 이면 생성 시점 활성화
        LocalDateTime now = LocalDateTime.now();
        if (requestDto.getStartTime().isBefore(now) && requestDto.getEndTime().isAfter(now)) {
            hotDeal.updateStatus(HotDealStatus.ACTIVE);
        }

        // HotDeal 저장
        // cascade 로 인해 HotDealProduct 또한 저장 처리
        return hotDealRepository.save(hotDeal);
    }

    // hotDealProducts Fetch Join 조회, 검증
    private HotDeal fetchHotDealByIdWithHotDealProductsAndValidate(Long hotDealId) {
        return hotDealRepository.findByIdWithHotDealProducts(hotDealId).orElseThrow(() -> {
            log.debug("요청된 핫딜이 존재하지 않습니다. hotDealId = {}", hotDealId);
            return new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, hotDealId);
        });
    }

    // hotDeal 조회, 검증
    private HotDeal fetchHotDealAndValidate(Long hotDealId){
        return hotDealRepository.findById(hotDealId).orElseThrow(() -> {
            log.debug("요청된 핫딜이 존재하지 않습니다. hotDealId = {}", hotDealId);
            return new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, hotDealId);
        });
    }

    private String validateNewTitle(HotDealUpdateRequestDto requestDto, HotDeal hotDeal) {
        String title = requestDto.getTitle();
        if (hotDealRepository.existsByTitle(title) && !title.equals(hotDeal.getTitle())) {
            log.debug("이미 존재하는 핫딜 title입니다. hotDealTitle = {}", title);
            throw new HotDealException(ErrorCode.HOTDEAL_TITLE_ALREADY_EXISTS, title);
        }
        return title;
    }
    // 새로 생성 요청 받은 hotDealProduct 생성, product-service 재고 감소 호출
    private void createNewHotDealProductsAndDecreaseOriginalProductStock(HotDealUpdateRequestDto requestDto,
                                                                         HotDeal hotDeal,
                                                                         List<ProductStockUpdateRequestDto> decreaseProductStockRequestDtos) {
        // 생성 대상 hotDealProducts 추출
        List<@Valid HotDealProductUpdateRequestDto> productsToCreate = requestDto.getProductInfos()
                .stream()
                .filter(rp -> rp.getHotDealProductId() == null)
                .toList();

        // 원본 상품에 대한 재고 감소 요청
        decreaseProductStockRequestDtos.addAll(productsToCreate.stream()
                .map(rp -> new ProductStockUpdateRequestDto(rp.getProductId(), rp.getQuantity()))
                .toList());

        List<ProductStockUpdateResponseDto> productStockUpdateResponseDtos = decreaseOriginalProductStockAndValidate(decreaseProductStockRequestDtos);
        List<HotDealProduct> hotDealProductsToCreate = new ArrayList<>();
        productsToCreate
                .forEach(rp -> productStockUpdateResponseDtos.stream()
                        .filter(product -> product.getProductId().equals(rp.getProductId()))
                        .findFirst()
                        .ifPresent(product -> {
                            hotDealProductsToCreate.add(
                                    HotDealProduct.create(
                                            product.getProductId(),
                                            product.getTitle(),
                                            product.getPrice(),
                                            rp.getDiscountRate(),
                                            rp.getQuantity()
                                    ));
                        }));

        hotDeal.addHotDealProducts(hotDealProductsToCreate);
    }

    // 수정 대상 stock increase, decrease, discountRate Update 처리
    private void updateHotDealProducts(HotDealUpdateRequestDto requestDto,
                                       HotDeal hotDeal,
                                       List<ProductStockUpdateRequestDto> increaseStocks,
                                       List<ProductStockUpdateRequestDto> decreaseStocks,
                                       List<Long> hotDealProductIdsToEvict) {

        // 업데이트 대상 hotDealProducts 추출
        List<HotDealProduct> productsToUpdate = hotDeal.getHotDealProducts().stream()
                .filter(hp -> requestDto.getProductInfos().stream()
                        .filter(rp -> rp.getHotDealProductId() != null)
                        .anyMatch(rp -> rp.getHotDealProductId().equals(hp.getId())))
                .toList();


        // 업데이트 대상 hotDealProductIds 캐싱 부분 무효화 대상에 add
        hotDealProductIdsToEvict.addAll(
                productsToUpdate.stream()
                        .map(HotDealProduct::getId)
                        .toList());

        productsToUpdate
                .forEach(hp -> requestDto.getProductInfos().stream()
                        .filter(rp -> rp.getHotDealProductId() != null && rp.getHotDealProductId().equals(hp.getId()))
                        .findFirst()
                        .ifPresent(rp -> {
                            Integer originalStock = hp.getStock();
                            Integer requestedQuantity = rp.getQuantity();
                            int diffAmount = originalStock - requestedQuantity;

                            // 재고 증가
                            if (diffAmount > 0) {
                                increaseStocks.add(new ProductStockUpdateRequestDto(hp.getProductId(), diffAmount));
                            }
                            // 재고 감소
                            else if (diffAmount < 0) {
                                decreaseStocks.add(new ProductStockUpdateRequestDto(hp.getProductId(), Math.abs(diffAmount)));
                            }
                            // hotDealProduct 에 update 처리
                            hotDeal.updateHotDealProducts(hp.getProductId(), requestedQuantity, rp.getDiscountRate());
                        }));
    }

    // 삭제 대상 hotDealProduct 삭제 처리
    private void deleteHotDealProductsFromUpdateRequest(HotDealUpdateRequestDto requestDto,
                                                        HotDeal hotDeal,
                                                        List<ProductStockUpdateRequestDto> increaseStocks,
                                                        List<Long> hotDealProductIdsToEvict) {
        // 삭제 대상 hotDealProducts 추출
        List<HotDealProduct> productsToDelete = hotDeal.getHotDealProducts().stream()
                .filter(hp -> requestDto.getProductInfos().stream()
                        .map(HotDealProductUpdateRequestDto::getHotDealProductId)
                        .filter(Objects::nonNull)
                        .noneMatch(id -> id.equals(hp.getId())))
                .toList();


//        List<HotDealProduct> productsToDelete = hotDeal.getHotDealProducts().stream()
//                .filter(hp -> requestDto.getProductInfos().stream()
//                        .map(HotDealProductUpdateRequestDto::getHotDealProductId)
//                        .filter(Objects::nonNull)
//                        .noneMatch(id -> id.equals(hp.getId())))
//                .toList();

        // 삭제 대상 hotDealProductIds 캐싱 부분 무효화 대상에 add
        hotDealProductIdsToEvict.addAll(
                productsToDelete.stream()
                .map(HotDealProduct::getId)
                .toList());

        // hotDeal 에서 삭제
        hotDeal.removeHotDealProducts(productsToDelete);

        // increaseStocks 에 add (원본 product 의 stock 증가)
        increaseStocks.addAll(productsToDelete.stream()
                .map(hp -> new ProductStockUpdateRequestDto(hp.getProductId(), hp.getStock()))
                .toList());
    }

    // Product 재고 감소 feignClient 호출, 검증
    private List<ProductStockUpdateResponseDto> decreaseOriginalProductStockAndValidate(List<ProductStockUpdateRequestDto> productStockUpdateRequestDtos) {
        // Product 재고 감소 feignClient 호출
        List<ProductStockUpdateResponseDto> responseDtos = resilience4JProductServiceClient.decreaseStock(productStockUpdateRequestDtos);

        // circuitBreaker OPEN 
        if (responseDtos.isEmpty()) {
            log.debug("원본 상품 재고 감소 호출을 실패했습니다. request = {}", productStockUpdateRequestDtos);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_ORIGINAL_STOCK_DECREASE_FAILED, productStockUpdateRequestDtos);
        }
        return responseDtos;
    }

    // Product 재고 증가 feignClient 호출, 검증
    private List<ProductStockUpdateResponseDto> increaseOriginalProductStockAndValidate(List<ProductStockUpdateRequestDto> productStockUpdateRequestDtos) {

        // product 없으면 empty List 반환 => productService 로 요청 보낼 필요 없음
        if (productStockUpdateRequestDtos.isEmpty()) return new ArrayList<>();

        // Product 재고 증가 feignClient 호출
        List<ProductStockUpdateResponseDto> responseDtos = resilience4JProductServiceClient.increaseStock(productStockUpdateRequestDtos);

        // circuitBreaker OPEN
        if (responseDtos.isEmpty()) {
            log.debug("원본 상품 재고 증가 호출을 실패했습니다. request = {}", productStockUpdateRequestDtos);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_ORIGINAL_STOCK_INCREASE_FAILED, productStockUpdateRequestDtos);
        }
        return responseDtos;
    }


    // 응답 Dto 변환
    private HotDealResponseDto convertHotDealResponseDtoWithHotDealProducts(HotDeal hotDeal) {
        List<HotDealProductResponseDto> hotDealProductResponseDtos = convertHotDealProductDto(hotDeal);
        return new HotDealResponseDto(
                hotDeal.getId(),
                hotDeal.getUserId(),
                hotDeal.getTitle(),
                hotDeal.getDescription(),
                hotDeal.getStartTime(),
                hotDeal.getEndTime(),
                hotDeal.getStatus().name(),
                hotDeal.getDeleted(),
                hotDealProductResponseDtos
        );
    }

    // 응답 Dto 변환
    private List<HotDealProductResponseDto> convertHotDealProductDto(HotDeal hotDeal) {
        return hotDeal.getHotDealProducts().stream()
                .map(hp -> new HotDealProductResponseDto(
                        hp.getId(),
                        hp.getProductTitle(),
                        hp.getOriginalPrice(),
                        hp.getHotDealPrice(),
                        hp.getDiscountRate(),
                        hp.getStock()))
                .collect(Collectors.toList());
    }

    // 응답 Dto 변환
    private HotDealResponseDto convertHotDealResponseDto(HotDeal hotDeal) {
        return new HotDealResponseDto(
                hotDeal.getId(),
                hotDeal.getUserId(),
                hotDeal.getTitle(),
                hotDeal.getDescription(),
                hotDeal.getStartTime(),
                hotDeal.getEndTime(),
                hotDeal.getStatus().name(),
                hotDeal.getDeleted());
    }
}
