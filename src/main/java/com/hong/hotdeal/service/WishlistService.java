package com.hong.hotdeal.service;

import com.hong.hotdeal.domain.Product;
import com.hong.hotdeal.domain.User;
import com.hong.hotdeal.domain.Wishlist;
import com.hong.hotdeal.domain.WishlistProduct;
import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.custom.WishlistException;
import com.hong.hotdeal.repository.WishlistProductRepository;
import com.hong.hotdeal.repository.WishlistRepository;
import com.hong.hotdeal.web.dto.request.wishlist.WishlistUpdateRequestDto;
import com.hong.hotdeal.web.dto.response.wishlist.WishlistPagingResponseDto;
import com.hong.hotdeal.web.dto.response.wishlist.WishlistResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistProductRepository wishlistProductRepository;
    private final UserService userService;
    private final ProductService productService;

    // 상품을 wishlist에 등록
    @Transactional
    public Long saveProductToWishlist(Long userId, Long productId, int quantity) {
        // 유저 조회
        User foundUser = userService.findById(userId);

        // Wishlist 조회 또는 생성
        Wishlist foundWishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> wishlistRepository.save(Wishlist.create(foundUser)));

        // 상품 조회
        Product product = productService.findById(productId);

        // Wishlist 에 상품 추가
        Optional<WishlistProduct> optionalWishlistProduct = foundWishlist.getWishlistProducts().stream()
                .filter(wp -> wp.getProduct().getId().equals(productId))
                .findFirst();

        if (optionalWishlistProduct.isPresent()) {
            // 상품이 이미 존재 하면 수량 증가
            WishlistProduct wishlistProduct = optionalWishlistProduct.get();
            wishlistProduct.addQuantity(quantity);
        } else {
            // 상품이 없으면 새로 추가
            WishlistProduct wishlistProduct = WishlistProduct.create(product, quantity);
            foundWishlist.addProduct(wishlistProduct);
        }
        return foundWishlist.getId();
    }

    // wishlist 페이징
    public WishlistPagingResponseDto getWishlists(Long userId, Long cursor, int size){
        PageRequest pageRequest = PageRequest.of(0, size);
        Page<WishlistResponseDto> page = wishlistRepository.findWishlistProductsByCursor(userId, cursor, pageRequest);
        // dto List 꺼내기
        List<WishlistResponseDto> contents = page.getContent();
        // wishlistId 가져 오기
        Long wishlistId = contents.isEmpty() ? null : contents.get(0).getWishlistId();
        // cursor 갱신
        Long newCursor = contents.isEmpty() ? null : contents.get(contents.size() - 1).getWishlistProductId();
        return new WishlistPagingResponseDto(wishlistId, newCursor, contents);
    }

    // wishlist 내 항목 수정 (상품 수량 변경 포함)
    @Transactional
    public String updateWishlist(Long userId, List<WishlistUpdateRequestDto.WishlistProductUpdate> updates){
        // 최대한 적은 쿼리로 조회, 업데이트
        Wishlist foundWishlist = wishlistRepository.findByUserIdWithProducts(userId)
                .orElseThrow(() -> new WishlistException(ErrorCode.WISHLIST_NOT_FOUND));

        // 수정 해야할 productId
        List<Long> productsId = updates.stream().map(WishlistUpdateRequestDto.WishlistProductUpdate::getProductId)
                .collect(Collectors.toList());

        // 변경 대상 조회
        List<WishlistProduct> foundWishlistProducts = wishlistProductRepository.findAllById(productsId);

        for (WishlistUpdateRequestDto.WishlistProductUpdate update : updates) {
            // 변경 대상 entity 중에 productId가 존재 하는지 검사
            WishlistProduct wishlistProduct = foundWishlistProducts.stream()
                    .filter(wp -> wp.getProduct().getId().equals(update.getProductId())).findFirst()
                    .orElseThrow(() -> new WishlistException(ErrorCode.WISHLIST_PRODUCT_NOT_FOUND));

            // 수량 변경
            if(update.getMethod().equals("update")) wishlistProduct.updateQuantity(update.getQuantity());
            // 삭제
            else if(update.getMethod().equals("delete")) foundWishlist.getWishlistProducts().remove(wishlistProduct);
        }
        return "wishlist 수정 완료.";
    }

    @Transactional
    public Long deleteWishlist(Long userId){
        Wishlist foundWishlist = wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new WishlistException(ErrorCode.WISHLIST_NOT_FOUND));
        // Cascade 삭제
        Long wishlistId = foundWishlist.getId();
        wishlistRepository.deleteById(foundWishlist.getId());
        return wishlistId;
    }

}
