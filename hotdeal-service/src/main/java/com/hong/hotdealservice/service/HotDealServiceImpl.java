package com.hong.hotdealservice.service;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealException;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.domain.status.HotDealStatus;
import com.hong.hotdealservice.dto.*;
import com.hong.hotdealservice.dto.projection.HotDealSimpleDto;
import com.hong.hotdealservice.event.HotDealStockEvent;
import com.hong.hotdealservice.repository.HotDealRedisRepository;
import com.hong.hotdealservice.repository.ProductRedisRepository;
import com.hong.hotdealservice.repository.HotDealProductRepository;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.web.dto.HotDealProductRequestDto;
import com.hong.hotdealservice.web.dto.HotDealCreateRequestDto;
import com.hong.hotdealservice.web.dto.HotDealProductUpdateRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealServiceImpl]")
@Transactional(readOnly = true)
public class HotDealServiceImpl implements HotDealService {

    private final HotDealRepository hotDealRepository;
    private final HotDealProductRepository hotDealProductRepository;
    private final HotDealRedisRepository hotDealRedisRepository;
    private final ProductRedisRepository productRedisRepository;
    private final ApplicationEventPublisher eventPublisher;

    // HotDeal 생성
    @Override
    @Transactional
    @CacheEvict(cacheNames = "getHotDeals", allEntries = true)
    public HotDealResponseDto createHotDeal(Long adminId, HotDealCreateRequestDto requestDto) {
        // 1. 같은 title 로 HotDeal 이 존재 하는지 검증
        validateDuplicateHotDealTitle(requestDto);

        // 2. hotDeal 시작 시간, 종료 시간 검증
        validateHotDealTime(requestDto.getStartTime(), requestDto.getEndTime());

        // 3. 같은 title 로 product 가 존재 하는지 검증
        validateDuplicateProductTitle(requestDto.getProducts());

        // 4. hotDeal 생성, 저장
        HotDeal savedHotDeal = createHotDealAndSave(adminId, requestDto);

        // 5. 트랜잭션 commit 이후 비동기로 hotDealProduct stock Redis 에 저장
        eventPublisher.publishEvent(new HotDealStockEvent(savedHotDeal));

        // 6. 응답 dto 변환
        return convertHotDealResponseDtoWithProducts(savedHotDeal);
    }

    // HotDeal 페이징 조회 (hotDealProduct 미포함)
    @Override
    @Cacheable(cacheNames = "getHotDeals"
            , key = "'hotdeals:cursor:' + (#cursor == null ? '' : #cursor) + ':size:' + #size + ':search:' + (#search == null ? '' : #search)"
            , cacheManager = "HotDealCacheManager")
    public HotDealPagingCacheDto getHotDeals(String search, Long cursor, int size) {

        // 1. hotDeal 페이징 조회(cursor 기반)
        List<HotDealSimpleDto> page = fetchHotDealsByCursor(search, cursor, size);

        // 2. cursor 지정 및 응답 Dto 변환
        return convertToHotDealPagingResponse(page);
    }

    // HotDeal 단건 조회
    @Override
    public HotDealCacheDto getHotDeal(Long hotDealId) {
        // 1. hotDeal 조회 및 검증
        HotDealSimpleDto hotDeal = getHotDealAndValidate(hotDealId);

        // 2. 응답 Dto 변환
        return convertHotDealResponseDto(hotDeal);
    }

    // HotDeal 수정
    @Transactional
    @Override
    @CacheEvict(cacheNames = "getHotDeals", allEntries = true)
    public HotDealResponseDto updateHotDeal(Long hotDealId, HotDealUpdateRequestDto requestDto) {
        // 1. hotDeal 조회 및 검증
        HotDeal hotDeal = getHotDealWithProductsAndValidate(hotDealId);

        // 2. title 검증
        validateNewTitle(requestDto, hotDeal);

        // 3. 날짜 검증
        validateHotDealTime(requestDto.getStartTime(), requestDto.getEndTime());

        // 4. HotDeal 필드 업데이트
        updateHotDealFields(requestDto, hotDeal);

        AtomicBoolean isStockUpdated = new AtomicBoolean(false);
        // hotDealProducts 에 대한 캐시 무효화 위한 list
        List<Long> productIdsToEvict = new ArrayList<>();

        // 5. 삭제 대상 상품 제거 (삭제)
        deleteProductsFromUpdateRequest(productIdsToEvict, requestDto, hotDeal, isStockUpdated);

        // 6. 기존 상품 필드 업데이트 (수정)
        updateProductsFormUpdateRequest(productIdsToEvict, requestDto, hotDeal, isStockUpdated);

        // 6. 신규 상품 추가 (추가)
        createNewProductsFromUpdateRequest(requestDto, hotDeal, isStockUpdated);

        // 7. 삭제, 수정 된 product 에 대한 캐시 부분(단건 조회), 전면(페이징 조회) 무효화
        hotDealRedisRepository.deleteById(hotDealId);
        productRedisRepository.deleteAllProductByIds(productIdsToEvict);
        productRedisRepository.deleteAllGetProductsKeys();

        // 5. 트랜잭션 commit 이후 비동기로 product 변경된 stock Redis 에 저장
        // stock 변경이 있었을 경우만 작동
        if(isStockUpdated.get()) eventPublisher.publishEvent(new HotDealStockEvent(hotDeal));

        // 8. 응답 Dto 변환
        return convertHotDealResponseDtoWithProducts(hotDeal);
    }

    // hotDeal 삭제
    @Transactional
    @Override
    @CacheEvict(cacheNames = "getHotDeals", allEntries = true)
    public HotDealResponseDto deleteHotDeal(Long hotDealId) {
        // 1. hotDeal 조회 (hotDealProducts fetch join) 및 검증
        HotDeal hotDeal = getHotDealWithProductsAndValidate(hotDealId);

        // 2. 남은 stock 0개로 처리
        hotDeal.getHotDealProducts().forEach(product -> product.decreaseStock(product.getStock()));

        // 3. softDelete 처리
        hotDeal.softDelete();

        // 4. 삭제, 수정 된 hotDeal, hotDealProductId 에 대한 캐시 부분, 전면 무효화
        // hotDealProducts 캐시 무효화 처리
        hotDealRedisRepository.deleteById(hotDealId);
        cacheEvictToProducts(hotDeal);

        // 5. 트랜잭션 commit 이후 비동기로 hotDealProduct 변경된 stock Redis 에 저장
        eventPublisher.publishEvent(new HotDealStockEvent(hotDeal));

        // 6. 응답 Dto 변환
        return convertHotDealResponseDtoWithProducts(hotDeal);
    }

    private void cacheEvictToProducts(HotDeal hotDeal) {
        List<Long> productIdsToEvict = hotDeal.getHotDealProducts()
                .stream()
                .map(HotDealProduct::getId)
                .toList();
        productRedisRepository.deleteAllProductByIds(productIdsToEvict);
        productRedisRepository.deleteAllGetProductsKeys();
    }


    // 같은 title 로 HotDeal 이 존재 하는지 검증
    private void validateDuplicateHotDealTitle(HotDealCreateRequestDto requestDto) {
        if (hotDealRepository.existsByTitle(requestDto.getTitle())) {
            log.debug("이미 존재하는 핫딜 Title 입니다. hotDealTitle = {}", requestDto.getTitle());
            throw new HotDealException(ErrorCode.HOTDEAL_TITLE_ALREADY_EXISTS, requestDto.getTitle());
        }
    }

    // HotDeal 시작 시간, 종료 시간 검증
    private void validateHotDealTime(LocalDateTime startTime, LocalDateTime endTime) {
        // 시작 시간이 종료 시간보다 늦으면
        if (startTime.isAfter(endTime)) {
            log.debug("시작 시간이 종료 시간보다 이후일 수 없습니다. startTime = {}, endTime = {}", startTime, endTime);
            throw new HotDealException(ErrorCode.HOTDEAL_INVALID_TIME, startTime, endTime);
        }
    }

    // 같은 title 로 hotDealProduct 가 존재 하는지 검증
    private void validateDuplicateProductTitle(List<HotDealProductRequestDto> requestProducts) {
        // 1. 같은 title 로 hotDealProduct 가 존재 하는지 검증
        ArrayList<String> existsTitles = new ArrayList<>();
        for (HotDealProductRequestDto product : requestProducts) {
            boolean exists = hotDealProductRepository.existsByTitle(product.getTitle());
            if(exists) existsTitles.add(product.getTitle());
        }
        // 2. 존재 한다면 모아서 예외 및 로그 처리
        if(!existsTitles.isEmpty()){
            log.info("이미 존재 하는 핫딜 상품 title 입니다. requestedTitle ={}", existsTitles);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_TITLE_ALREADY_EXISTS, existsTitles);
        }
    }

    // HotDeal 생성, 저장
    private HotDeal createHotDealAndSave(Long adminId,
                                         HotDealCreateRequestDto requestDto) {
        // 1. hotDealProduct 생성
        List<HotDealProduct> hotDealProducts = requestDto.getProducts()
                .stream()
                .map(dto -> HotDealProduct.create(
                        dto.getTitle(),
                        dto.getPrice(),
                        dto.getStock()))
                .collect(Collectors.toList());

        // 2. hotDeal 생성
        HotDeal hotDeal = HotDeal.create(
                adminId,
                requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getStartTime(),
                requestDto.getEndTime(),
                hotDealProducts);

        // 3. startTime 이 시작 시간 지나고, endTime 이전 이면 생성 시점 활성화
        if (hotDeal.orderAble()) hotDeal.updateStatus(HotDealStatus.ACTIVE);

        // 5. HotDeal 저장
        // cascade 로 인해 HotDealProduct 또한 저장 처리
        return hotDealRepository.save(hotDeal);
    }

    private HotDealPagingCacheDto convertToHotDealPagingResponse(List<HotDealSimpleDto> page) {
        // Dto 변환
        List<HotDealCacheDto> HotDealCacheDtos = page.stream().map(this::convertHotDealResponseDto)
                .collect(Collectors.toList());

        // nextCursor 지정
        Long nextCursor = HotDealCacheDtos.isEmpty() ? 0 : HotDealCacheDtos.get(HotDealCacheDtos.size() - 1).getId();

        return new HotDealPagingCacheDto(nextCursor, HotDealCacheDtos);
    }

    private List<HotDealSimpleDto> fetchHotDealsByCursor(String search, Long cursor, int size) {
        if (cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        return hotDealRepository.findByCursorAndSearchAndSizeHotDeals(cursor, search, pageRequest);
    }

    // hotDealProducts Fetch Join 조회, 검증
    private HotDeal getHotDealWithProductsAndValidate(Long hotDealId) {
        return hotDealRepository.findByIdWithHotDealProducts(hotDealId).orElseThrow(() -> {
            log.debug("요청된 핫딜이 존재하지 않습니다. hotDealId = {}", hotDealId);
            return new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, hotDealId);
        });
    }

    // hotDeal 조회, 검증
    private HotDealSimpleDto getHotDealAndValidate(Long hotDealId){
        // 1. Redis 조회
        HotDealCacheDto cachedData = hotDealRedisRepository.findById(hotDealId);
        if(cachedData != null){
            return new HotDealSimpleDto(cachedData);
        }

        // 2. cacheMiss -> DB 조회
        HotDealSimpleDto hotDeal = hotDealRepository.findHotDealById(hotDealId).orElseThrow(() -> {
            log.debug("요청된 핫딜이 존재하지 않습니다. hotDealId = {}", hotDealId);
            return new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, hotDealId);
        });

        // 3. Redis save
        HotDealCacheDto cacheDto = new HotDealCacheDto(hotDeal);
        hotDealRedisRepository.saveWithTTL(cacheDto);

        return hotDeal;
    }

    private String validateNewTitle(HotDealUpdateRequestDto requestDto, HotDeal hotDeal) {
        String title = requestDto.getTitle();
        if (hotDealRepository.existsByTitle(title) && !title.equals(hotDeal.getTitle())) {
            log.debug("이미 존재하는 핫딜 title입니다. hotDealTitle = {}", title);
            throw new HotDealException(ErrorCode.HOTDEAL_TITLE_ALREADY_EXISTS, title);
        }
        return title;
    }

    // update 요청 에서 hotDalProduct 생성 대상 생성
    private void createNewProductsFromUpdateRequest(HotDealUpdateRequestDto requestDto,
                                                    HotDeal hotDeal,
                                                    AtomicBoolean isStockUpdated) {
        List<HotDealProduct> newProducts = requestDto.getProducts().stream()
                .filter(r -> r.getProductId() == null)
                .map(r -> HotDealProduct.create(r.getTitle(), r.getPrice(), r.getStock()))
                .toList();
        if(!newProducts.isEmpty()) isStockUpdated.set(true);
        // 신규 상품 생성
        hotDeal.addHotDealProducts(newProducts);
    }

    // update 요청 에서 hotDalProduct 수정 대상 수정
    private void updateProductsFormUpdateRequest(List<Long> hotDealProductIdsToEvict,
                                                 HotDealUpdateRequestDto requestDto,
                                                 HotDeal hotDeal,
                                                 AtomicBoolean isStockUpdated) {
        // 요청에서 기존 상품 수정
        hotDeal.getHotDealProducts().forEach(exists ->
                requestDto.getProducts().stream()
                        .filter(r -> r.getProductId() != null && r.getProductId().equals(exists.getId()))
                        .findFirst()
                        .ifPresent(r -> exists.updateFields(r.getTitle(), r.getPrice(), r.getStock()))
        );


        List<Long> updatedIds = hotDeal.getHotDealProducts().stream()
                .map(HotDealProduct::getId)
                .filter(id -> requestDto.getProducts().stream()
                        .anyMatch(r -> r.getProductId() != null && r.getProductId().equals(id)))
                .toList();

        if(!updatedIds.isEmpty()) isStockUpdated.set(true);
        // hotDealProduct 캐싱 무효화 대상 처리
        hotDealProductIdsToEvict.addAll(updatedIds);
    }

    // update 요청 에서 hotDealProduct 삭제 대상 삭제
    private void deleteProductsFromUpdateRequest(List<Long> hotDealProductIdsToEvict,
                                                 HotDealUpdateRequestDto requestDto,
                                                 HotDeal hotDeal,
                                                 AtomicBoolean isStockUpdated) {
        // 요청에서 기존 상품 추출 (hotDealProductId 존재)
        List<Long> incomingIds = requestDto.getProducts().stream()
                .map(HotDealProductUpdateRequestDto::getProductId)
                .filter(Objects::nonNull)
                .toList();
        // DB 값과 비교하여 삭제 대상 추출
        List<HotDealProduct> productsToDelete = hotDeal.getHotDealProducts().stream()
                .filter(exists -> !incomingIds.contains(exists.getId()))
                .toList();

        if(!productsToDelete.isEmpty()) isStockUpdated.set(true);

        // 삭제 처리
        hotDeal.removeHotDealProducts(productsToDelete);

        // hotDealProduct 캐싱 무효화 대상 처리
        hotDealProductIdsToEvict.addAll(
                productsToDelete.stream()
                        .map(hp -> hp.getId())
                        .toList());
    }

    private void updateHotDealFields(HotDealUpdateRequestDto requestDto, HotDeal hotDeal) {
        hotDeal.updateFields(
                requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getStartTime(),
                requestDto.getEndTime(),
                HotDealStatus.valueOf(requestDto.getStatus()));
    }

    // 응답 Dto 변환
    private HotDealResponseDto convertHotDealResponseDtoWithProducts(HotDeal hotDeal) {
        List<ProductResponseDto> productResponseDtos = convertHotDealProductDto(hotDeal);
        return new HotDealResponseDto(hotDeal, productResponseDtos);
    }

    // 응답 Dto 변환
    private List<ProductResponseDto> convertHotDealProductDto(HotDeal hotDeal) {
        return hotDeal.getHotDealProducts().stream()
                .map(hp -> new ProductResponseDto(hp, hp.getStock()))
                .collect(Collectors.toList());
    }

    // 단건 조회 응답 Dto 변환
    private HotDealCacheDto convertHotDealResponseDto(HotDealSimpleDto hotDeal) {
        return new HotDealCacheDto(hotDeal);
    }
}
