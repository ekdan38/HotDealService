package com.hong.productservice.service.wishlist;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.ProductException;
import com.hong.common.exception.custom.WishlistException;
import com.hong.productservice.domain.Product;
import com.hong.productservice.domain.Wishlist;
import com.hong.productservice.domain.WishlistProduct;
import com.hong.productservice.dto.wishlist.WishlistPagingResponseDto;
import com.hong.productservice.dto.wishlist.WishlistProductDto;
import com.hong.productservice.dto.wishlist.WishlistResponseDto;
import com.hong.productservice.repository.WishlistRepository;
import com.hong.productservice.repository.WishlistProductRepository;
import com.hong.productservice.service.product.ProductApiService;
import com.hong.productservice.web.dto.wishlist.WishlistRequestDto;
import com.hong.productservice.web.dto.wishlist.WishlistUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[WishlistServiceImpl]")
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private final WishlistProductRepository wishlistProductRepository;
    private final WishlistRepository wishlistRepository;
    private final ProductApiService productApiService;

    // wishlist 에 product 등록
    @Transactional
    @Override
    public WishlistResponseDto createWishlist(Long userId, WishlistRequestDto requestDto) {
        // 1. wishlist 에 등록 시도 하는 product 가 존재 하는지 조회 및 검증
        Product product = productApiService.getProduct(requestDto.getProductId());

        // 2. user 의 wishlist 조회 (없으면 생성)
        Wishlist wishlist = wishlistRepository.findWithProductsByUserId(userId)
                .orElseGet(() -> wishlistRepository.save(Wishlist.create(userId)));

        // 3. wishlistProduct 조회 또는 생성/수량 업데이트
        WishlistProduct wishlistProduct = findOrCreateWishlistProduct(requestDto, wishlist, requestDto.getProductId(), product);

        // 4. 변경 사항 명시적 저장
        Wishlist savedWishlist = wishlistRepository.save(wishlist);

        // 5. 응답 Dto 변환
        return convertToCreateWishlistResponse(savedWishlist, wishlistProduct);
    }

    // wishlist Products cursor 기반 페이징 조회
    @Override
    public WishlistPagingResponseDto getWishlists(Long userId, Long cursor, int size) {
        // 1. wishlist 조회 wishlist 없으면 return 처리
        Wishlist wishlist;
        Optional<Wishlist> optionalWishlist = wishlistRepository.findByUserId(userId);
        // wishlist 없으면 return 처리
        if(optionalWishlist.isEmpty()) return new WishlistPagingResponseDto(null, 0L);
        else wishlist = optionalWishlist.get();

        // 2. wishlistProducts 커서 기반 페이징 조회
        List<WishlistProduct> wishlistProducts = fetchWishllistProductsByCursor(cursor, size, wishlist);

        // 3. cursor 지정 및 응답 Dto 변환
        return convertToWishlistPagingResponse(wishlistProducts, wishlist);
    }

    // wishlist 수정 (상품 수량 변경 포함)
    @Transactional
    @Override
    public String updateWishlist(Long userId, List<WishlistUpdateRequestDto.WishlistProductUpdate> updates) {

        // 1. wishlist 조회 (wishlistProduct fetch join) 및 검증
        Wishlist wishlist = fetchWishlistWithWishlistProductAndValidate(userId);
        Long wishlistId = wishlist.getId();

        // 2. wishlist 수정 및 검증
        updateWishlistAndValidate(updates, wishlistId, wishlist);

        return "wishlist 수정 성공";
    }

    // wishlist 삭제
    @Transactional
    @Override
    public Long deleteWishlist(Long userId) {
        // 1. wishlist 조회 및 검증
        Wishlist wishlist = fetchWishlistByIdAndValidate(userId);

        // 2. wishlist 삭제 (Cascade, orphanRemoval 삭제)
        Long wishlistId = wishlist.getId();
        wishlistRepository.deleteById(wishlist.getId());

        return wishlistId;
    }

    private Wishlist fetchWishlistByIdAndValidate(Long userId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId).orElseThrow(() -> {
                    log.debug("위시리스트가 존재하지 않습니다. userId = {}", userId);
                    return new WishlistException(ErrorCode.WISHLIST_NOT_FOUND, userId);
                });
        return wishlist;
    }

    private WishlistResponseDto convertToCreateWishlistResponse(Wishlist savedWishlist, WishlistProduct wishlistProduct) {
        return new WishlistResponseDto(
                savedWishlist.getId(),
                savedWishlist.getUserId(),
                wishlistProduct.getProduct().getId(),
                wishlistProduct.getQuantity());
    }
    private WishlistPagingResponseDto convertToWishlistPagingResponse(List<WishlistProduct> wishlistProducts, Wishlist wishlist) {
        List<WishlistProductDto> wishlistProductDtos = wishlistProducts.stream()
                .map(wp -> new WishlistProductDto(
                        wp.getProduct().getId(),
                        wp.getProduct().getTitle(),
                        wp.getProduct().getPrice(),
                        wp.getQuantity()
                )).collect(Collectors.toList());

        // nextCursor 지정
        Long nextCursor = wishlistProducts.isEmpty() ? 0 : wishlistProducts.get(wishlistProducts.size() - 1).getId();

        return new WishlistPagingResponseDto(wishlist.getId(), nextCursor, wishlistProductDtos);
    }

    private List<WishlistProduct> fetchWishllistProductsByCursor(Long cursor, int size, Wishlist wishlist) {
        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if(cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // Wishlist 와 관련된 WishlistProduct, Product 를 fetch join 으로 조회
        List<WishlistProduct> wishlistProducts = wishlistProductRepository
                .findByWishlistIdAndCursor(wishlist.getId(), cursor, pageRequest);
        return wishlistProducts;
    }

    private void updateWishlistAndValidate(List<WishlistUpdateRequestDto.WishlistProductUpdate> updates, Long wishlistId, Wishlist wishlist) {
        // 수정 해야할 productId
        List<Long> productsIds = updates.stream().map(WishlistUpdateRequestDto.WishlistProductUpdate::getProductId)
                .collect(Collectors.toList());

        // 변경 대상 조회
        List<WishlistProduct> wishlistProducts = wishlistProductRepository
                .findByWishlistIdAndProductIds(wishlistId, productsIds);

        for (WishlistUpdateRequestDto.WishlistProductUpdate update : updates) {
            // 변경 대상 entity 중에 productId가 존재 하는지 검사
            WishlistProduct wishlistProduct = wishlistProducts.stream()
                    .filter(wp -> wp.getProduct().getId().equals(update.getProductId())).findFirst()
                    .orElseThrow(() -> {
                        log.debug("요청된 상품이 존재하지 않습니다. productId = {}", update.getProductId());
                        return new ProductException(ErrorCode.PRODUCT_NOT_FOUND, update.getProductId());
                    });

            // 수량 변경
            if(update.getMethod().equals("update")) wishlistProduct.updateQuantity(update.getQuantity());
                // 삭제
            else if(update.getMethod().equals("delete")) wishlist.getWishlistProducts().remove(wishlistProduct);
        }
    }

    private Wishlist fetchWishlistWithWishlistProductAndValidate(Long userId) {
        Wishlist wishlist = wishlistRepository.findByUserIdWithProducts(userId)
                .orElseThrow(() -> {
                    log.debug("위시리스트가 존재하지 않습니다. {}", userId);
                    return new WishlistException(ErrorCode.WISHLIST_NOT_FOUND, userId);
                });
        return wishlist;
    }

    //WishlistProduct 조회 또는 생성/수량 업데이트
    private WishlistProduct findOrCreateWishlistProduct(WishlistRequestDto requestDto, Wishlist wishlist, Long productId, Product product) {
        // product 가 wishlist 에 이미 존재 하는지 확인
        WishlistProduct wishlistProduct = wishlist.getWishlistProducts().stream()
                .filter(wp -> wp.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        // 이미 존재 한다면 quantity 증가
        if (wishlistProduct != null) {
            wishlistProduct.addQuantity(requestDto.getQuantity());
        }
        // 존재 하지 않는 다면 생성
        else {
            // 중간 테이블 생성
            wishlistProduct = WishlistProduct.create(product, requestDto.getQuantity());
            // 연관 관계 메서드 로 wishlist 에 wishlistProduct 추가
            wishlist.addWishlistProducts(wishlistProduct);
        }
        return wishlistProduct;
    }
}
