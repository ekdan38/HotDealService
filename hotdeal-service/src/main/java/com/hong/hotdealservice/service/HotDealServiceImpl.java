package com.hong.hotdealservice.service;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.HotDealException;
import com.hong.common.exception.custom.HotDealProductException;
import com.hong.hotdealservice.client.Resilience4JProductServiceClient;
import com.hong.hotdealservice.domain.HotDeal;
import com.hong.hotdealservice.domain.HotDealProduct;
import com.hong.hotdealservice.dto.HotDealPagingResponseDto;
import com.hong.hotdealservice.dto.HotDealResponseDto;
import com.hong.hotdealservice.repository.HotDealRepository;
import com.hong.hotdealservice.web.dto.HotDealProductRequestDto;
import com.hong.hotdealservice.web.dto.HotDealRequestDto;
import com.hong.hotdealservice.web.dto.HotDealUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[HotDealServiceImpl]")
@Transactional(readOnly = true)
public class HotDealServiceImpl implements HotDealService {

    private final HotDealRepository hotDealRepository;
    private final Resilience4JProductServiceClient resilience4JProductServiceClient;


    // HotDeal 생성
    // Admin
    @Override
    @Transactional
    public HotDealResponseDto createHotDeal(Long adminId, HotDealRequestDto requestDto) {
        // 같은 title 로 HotDeal 이 존재 하는지 검증
        existsByTitleAndValidate(requestDto);

        // HotDeal 시작 시간, 종료 시간 검증
        validateHotDealDate(requestDto.getStartTime(), requestDto.getEndTime());

        // product 의 id만 추출
        List<Long> requestedProductIds = extractProductIdsFromRequestDto(requestDto.getProductInfos());

        // product 별 quantity Map 변환
        Map<Long, Integer> quantityMap = buildIdQuantityMap(requestDto.getProductInfos());

        // products 조회, 검증
        Map<Long, ProductCommonDto> productMap = fetchAndValidate(requestedProductIds, quantityMap);

        // HotDeal 생성, 저장
        HotDeal savedHotDeal = createHotDealAndSave(adminId, requestDto, productMap);

        // 응답 dto 변환
        return convertHotDealResponseDto(savedHotDeal);
    }


    // HotDeal 페이징 조회
    // 간단하게 HotDeal 내역 조회
    @Override
    public HotDealPagingResponseDto getHotDeals(String search, Long cursor, int size) {
        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if (cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        List<HotDeal> page = hotDealRepository.findByCursorAndSearchAndSize(cursor, search, pageRequest);

        // Dto 변환
        List<HotDealResponseDto> hotDealResponseDtos = page.stream().map(this::convertHotDealResponseDtoWithoutProducts)
                .collect(Collectors.toList());

        // nextCursor 지정
        Long nextCursor = hotDealResponseDtos.isEmpty() ? 0 : hotDealResponseDtos.get(hotDealResponseDtos.size() - 1).getHotDealId();

        return new HotDealPagingResponseDto(nextCursor, hotDealResponseDtos);
    }

    // HotDeal 단건 조회
    @Override
    public HotDealResponseDto getHotDeal(Long hotDealId) {
        // hotDealProducts Fetch Join 조회, 검증
        HotDeal hotDeal = findByIdWithHotDealProductsAndValidate(hotDealId);

        // 응답 Dto 변환
        return convertHotDealResponseDto(hotDeal);
    }

    // HotDeal 수정
    // Admin
    // HotDeal 삭제
    @Transactional
    @Override
    public HotDealResponseDto updateHotDeal(Long hotDealId, HotDealUpdateRequestDto updateRequestDto) {

        HotDeal hotDeal = findByIdWithHotDealProductsAndValidate(hotDealId);

        // title 검증
        validateNewTitle(updateRequestDto, hotDeal);

        // 날짜 검증
        validateHotDealDate(updateRequestDto.getStartTime(), updateRequestDto.getEndTime());

        // HotDeal 필드 업데이트
        hotDeal.updateFields(updateRequestDto.getTitle(), updateRequestDto.getDescription(), updateRequestDto.getStartTime(), updateRequestDto.getEndTime(), updateRequestDto.getStatus());

        // 요청된 Product 정보와 기존 HotDealProducts 추출
        // 요청된 ProductId 추출
        List<Long> requestedProductIds = extractProductIdsFromRequestDto(updateRequestDto.getProductInfos());

        // 기존 ProductId 추출
        List<Long> existingProductIds = extractExistsProductIdsFromHotDeal(hotDeal);

        // 삭제할 HotDealProducts 삭제
        deleteHotDealProducts(hotDeal, requestedProductIds);

        // product 별 quantity Map 변환
        Map<Long, Integer> quantityMap = buildIdQuantityMap(updateRequestDto.getProductInfos());

        // feignClient 상품 조회, map 변환
        Map<Long, ProductCommonDto> productMap = fetchAndValidate(requestedProductIds, quantityMap);

        // HotDealProduct 수정, HotDeal 저장
        HotDeal savedHotDeal = updateHotDealProductsAndSaveHotDeal(updateRequestDto, hotDeal, existingProductIds, productMap);

        // Dto 변환
        return convertHotDealResponseDto(savedHotDeal);
    }



    // Admin
    @Transactional
    @Override
    public HotDealResponseDto deleteHotDeal(Long hotDealId) {

        // hotDealProducts Fetch Join 조회, 검증
        HotDeal hotDeal = findByIdWithHotDealProductsAndValidate(hotDealId);

        // HotDeal 삭제
        hotDealRepository.delete(hotDeal);

        // 응답 Dto 변환
        return convertHotDealResponseDto(hotDeal);
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

    // RequestDto 에서 상품의 id만 추출
    private List<Long> extractProductIdsFromRequestDto(List<@Valid HotDealProductRequestDto> productRequestDtos) {
        return productRequestDtos.stream()
                .map(HotDealProductRequestDto::getProductId)
                .collect(Collectors.toList());
    }

    // HotDealProductRequestDto <ProductId, Quantity> 형식 Map으로 변환
    private Map<Long, Integer> buildIdQuantityMap(List<HotDealProductRequestDto> productRequestDtos){
        return productRequestDtos
                .stream()
                .collect(Collectors.toMap(HotDealProductRequestDto::getProductId, HotDealProductRequestDto::getQuantity));
    }

    // HotDeal 에서 기존 상품 id 추출
    private List<Long> extractExistsProductIdsFromHotDeal(HotDeal hotDeal) {
        List<HotDealProduct> hotDealProducts = hotDeal.getHotDealProducts();
        return hotDealProducts.stream()
                .map(HotDealProduct::getProductId)
                .collect(Collectors.toList());
    }

    // 삭제 해야 할 HotDealProduct 삭제
    private void deleteHotDealProducts(HotDeal hotDeal, List<Long> requestedProductIds) {
        List<HotDealProduct> productsToDelete = hotDeal.getHotDealProducts().stream()
                .filter(product -> !requestedProductIds.contains(product.getProductId()))
                .collect(Collectors.toList());
        hotDeal.removeHotDealProducts(productsToDelete);
    }

    // HotDeal 생성, 저장
    private HotDeal createHotDealAndSave(Long adminId, HotDealRequestDto requestDto, Map<Long, ProductCommonDto> productMap) {
        // 핫딜 상품을 저장할 List
        List<HotDealProduct> hotDealProducts = new ArrayList<>();

        // HotDealProduct 생성
        for (HotDealProductRequestDto requestProduct : requestDto.getProductInfos()) {
            Long productId = requestProduct.getProductId();
            ProductCommonDto productCommonDto = productMap.get(productId);

            // HotDealProduct 생성, hotDealProducts 에 Add
            HotDealProduct hotDealProduct = HotDealProduct.create(productId, productCommonDto.getTitle(), productCommonDto.getPrice(),
                    requestProduct.getDiscountRate(), requestProduct.getQuantity());

            hotDealProducts.add(hotDealProduct);
        }
        // HotDeal 생성
        HotDeal hotDeal = HotDeal.create(adminId, requestDto.getTitle(), requestDto.getDescription(),
                requestDto.getStartTime(), requestDto.getEndTime(), hotDealProducts);

        // HotDeal 저장
        // cascade 로 인해 HotDealProduct 또한 저장 처리
        return hotDealRepository.save(hotDeal);
    }

    // hotDealProducts Fetch Join 조회, 검증
    private HotDeal findByIdWithHotDealProductsAndValidate(Long hotDealId) {
        HotDeal hotDeal = hotDealRepository.findByIdWithHotDealProducts(hotDealId);
        if (hotDeal == null) {
            log.debug("요청된 핫딜이 존재하지 않습니다. hotDealId = {}", hotDealId);
            throw new HotDealException(ErrorCode.HOTDEAL_NOT_FOUND, hotDealId);
        }
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

    // feignClient 상품 조회, map 변환
    private Map<Long, ProductCommonDto> fetchAndValidate(List<Long> requestedProductIds, Map<Long, Integer> requestedQuantities) {

        // feignClient 로 Product 조회
        List<ProductCommonDto> productCommonDtos = resilience4JProductServiceClient.getProductsByIds(requestedProductIds);

        // 상품 정보가 없다면
        if(productCommonDtos.isEmpty()){
            log.debug("요청된 핫딜 상품이 존재하지 않습니다. hotDealProductId = {}", requestedProductIds);
            throw new HotDealProductException(ErrorCode.HOTDEAL_PRODUCT_NOT_FOUND, requestedProductIds);
        }

        // productId 기준 Map 변환
        Map<Long, ProductCommonDto> productMap = productCommonDtos.stream()
                .collect(Collectors.toMap(ProductCommonDto::getId, product -> product));

        // 상품 재고, 핫딜 요청 재고 비교 검증
        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            Integer productStock = productMap.get(productId).getStock();
            if(productStock < quantity){
                log.debug("요청 수량보다 재고가 부족합니다. hotDealProductId = {}, 요청 수량 = {}, 재고 수량 = {}",
                        productId, productStock, quantity);
                throw new HotDealException(ErrorCode.HOTDEAL_PRODUCT_INSUFFICIENT_STOCK, productId, productStock, quantity);
            }
        }
        // ProductId를 기준으로 Map 변환
        return productMap;
    }
    
    // HotDealProduct 수정, HotDeal 저장
    private HotDeal updateHotDealProductsAndSaveHotDeal(HotDealUpdateRequestDto updateRequestDto, HotDeal hotDeal, List<Long> existingProductIds, Map<Long, ProductCommonDto> validProductInfoMap) {
        // HotDealProducts 수정 처리
        hotDeal.getHotDealProducts().forEach(hotDealProduct -> {
            HotDealProductRequestDto updatedInfo = updateRequestDto.getProductInfos().stream()
                    .filter(info -> info.getProductId().equals(hotDealProduct.getProductId()))
                    .findFirst()
                    .orElse(null);
            if (updatedInfo != null) hotDealProduct.updateQuantityAndDiscountRate(updatedInfo.getQuantity(), updatedInfo.getDiscountRate());
        });

        // HotDealProducts 등록 처리
        List<HotDealProductRequestDto> productsToAdd = updateRequestDto.getProductInfos().stream()
                .filter(info -> !existingProductIds.contains(info.getProductId()))
                .collect(Collectors.toList());

        List<HotDealProduct> newProducts = productsToAdd.stream()
                .map(info -> {
                    ProductCommonDto productInfo = validProductInfoMap.get(info.getProductId());
                    return HotDealProduct.create(
                            info.getProductId(),
                            productInfo.getTitle(),
                            productInfo.getPrice(),
                            info.getDiscountRate(),
                            info.getQuantity()
                    );
                })
                .collect(Collectors.toList());
        hotDeal.addHotDealProducts(newProducts);

        // HotDeal save (명시적으로 처리)
        return hotDealRepository.save(hotDeal);
    }

    // 응답 Dto 변환
    private HotDealResponseDto convertHotDealResponseDto(HotDeal hotDeal) {
        List<HotDealResponseDto.HotDealProductDto> hotDealProductDtos = convertHotDealProductDto(hotDeal);
        return new HotDealResponseDto(
                hotDeal.getId(),
                hotDeal.getUserId(),
                hotDeal.getTitle(),
                hotDeal.getDescription(),
                hotDeal.getStartTime(),
                hotDeal.getEndTime(),
                hotDeal.getStatus().name(),
                hotDealProductDtos
        );
    }

    // 응답 Dto 변환
    private List<HotDealResponseDto.HotDealProductDto> convertHotDealProductDto(HotDeal hotDeal) {
        return hotDeal.getHotDealProducts().stream()
                .map(hp -> new HotDealResponseDto.HotDealProductDto(
                        hp.getProductId(),
                        hp.getProductTitle(),
                        hp.getOriginalPrice(),
                        hp.getHotDealPrice(),
                        hp.getDiscountRate(),
                        hp.getStock()))
                .collect(Collectors.toList());
    }

    // 응답 Dto 변환
    private HotDealResponseDto convertHotDealResponseDtoWithoutProducts(HotDeal hotDeal) {
        return new HotDealResponseDto(
                hotDeal.getId(),
                hotDeal.getUserId(),
                hotDeal.getTitle(),
                hotDeal.getDescription(),
                hotDeal.getStartTime(),
                hotDeal.getEndTime(),
                hotDeal.getStatus().name());
    }
}
