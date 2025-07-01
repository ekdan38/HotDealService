package com.hong.userservice.service;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.CartException;
import com.hong.userservice.domain.CartItem;
import com.hong.userservice.dto.CartItemDto;
import com.hong.userservice.dto.CartItemResponseDto;
import com.hong.userservice.repository.CartItemRepository;
import com.hong.userservice.web.dto.CartItemRequestDto;
import com.hong.userservice.web.dto.CartItemUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[CartItemServiceImpl]")
@Transactional(readOnly = true)
public class CartItemServiceImpl implements CartItemService{

    private final CartItemRepository cartItemRepository;

    // cart 에 상품 추가(최대 10개)
    @Transactional
    @Override
    public CartItemResponseDto createCartItem(Long userId, CartItemRequestDto requestDto) {
        // 개선 후
        List<CartItem> userCartItems = cartItemRepository.findAllByUserId(userId);
        if (userCartItems.size() >= 20) {
            log.error("장바구니에는 최대 20개의 상품만 담을 수 있습니다. userId = {}", userId);
            throw new CartException(ErrorCode.CART_ITEM_LIMIT_EXCEEDED, userId);
        }
        Long productId = requestDto.getProductId();
        Integer quantity = requestDto.getQuantity();

        Optional<CartItem> existingItem = userCartItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
        CartItem cartItem;
        CartItemDto cartItemDto;

        // 같은 상품이 존재하지 않는 경우, cartItem 생성
        if (existingItem.isEmpty()) {
            // cartItem 생성, 저장
            cartItem = CartItem.create(userId, productId, quantity);
            cartItemRepository.save(cartItem);
            cartItemDto = new CartItemDto(productId, cartItem.getQuantity());
        }
        // 같은 상품이 존재하는 경우, 수량만 증가
        else {
            cartItem = existingItem.get();
            int newTotalQuantity = cartItem.getQuantity() + quantity;
            // 수량 제한 검증
            if (newTotalQuantity > 99999) {
                log.error("장바구니에 상품의 수량은 최대 99999개 입니다. userId = {}", userId);
                throw new CartException(ErrorCode.CART_ITEM_LIMIT_QUANTITY, userId);
            }
            // 수량 업데이트 (JPA dirty checking으로 자동 업데이트)
            cartItem.updateQuantity(newTotalQuantity);
            cartItemDto = new CartItemDto(productId, newTotalQuantity);
        }
        return new CartItemResponseDto(userId, List.of(cartItemDto));
    }
//        //개선 전
//
//        //cartItem 최대 10개 검증
//        int count = cartItemRepository.countByUserId(userId);
//        // todo
//        if (count >= 100000) {
//            log.error("장바구니에는 최대 10개의 상품만 담을 수 있습니다. userId = {}", userId);
//            throw new CartException(ErrorCode.CART_ITEM_LIMIT_EXCEEDED, userId);
//        }
//
//        Long productId = requestDto.getProductId();
//        Integer quantity = requestDto.getQuantity();
//
//        // 해당 상품이 cartItem 에 존재 하는지 DB 조회
//        Optional<CartItem> optionalItem = cartItemRepository.findByUserIdAndProductId(userId, productId);
//        CartItem cartItem;
//        CartItemDto cartItemDto;
//        // 같은 상품 존재 하지 않는 경우, cartItem 생성
//        if(optionalItem.isEmpty()){
//            // cartItem 생성, 저장
//            cartItem = CartItem.create(userId, productId, quantity);
//            cartItemRepository.save(cartItem);
//            cartItemDto = new CartItemDto(productId, cartItem.getQuantity());
//        }
//
//        // 같은 상품이 존재 하는 경우, 수량만 증가 최대 10개
//        else {
//            // 이미 존재, 수량만 증가
//            cartItem = optionalItem.get();
//            if(cartItem.getQuantity() + quantity > 99999){
//                log.error("장바구니에 상품의 수량은 최대 99999개 입니다. userId = {}", userId);
//                throw new CartException(ErrorCode.CART_ITEM_LIMIT_QUANTITY, userId);
//            }
//            cartItem.updateQuantity(cartItem.getQuantity() + quantity);
//            cartItemDto = new CartItemDto(productId, cartItem.getQuantity() + quantity);
//        }
//        return new CartItemResponseDto(userId, List.of(cartItemDto));
//    }

    // cart 조회
    @Override
    public CartItemResponseDto getCartItems(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findAllByUserId(userId);
        List<CartItemDto> cartItemDtos = cartItems.stream()
                .map(ci -> new CartItemDto(ci.getProductId(), ci.getQuantity()))
                .toList();
        return new CartItemResponseDto(userId, cartItemDtos);
    }

    // cart 수정
    @Transactional
    @Override
    public CartItemResponseDto updateCartItems(Long userId, CartItemUpdateRequestDto requestDto) {
        // 1. 기존 장바구니 항목 조회
        List<CartItem> existingItems = cartItemRepository.findAllByUserId(userId);
        Map<Long, CartItem> existingMap = existingItems.stream()
                .collect(Collectors.toMap(CartItem::getProductId, item -> item));

        // 2. 요청으로 들어온 항목 처리
        List<CartItemRequestDto> requestItems = requestDto.getRequestItems();
        Set<Long> requestedProductIds = new HashSet<>();

        List<CartItem> responseItems = new ArrayList<>();

        for (CartItemRequestDto dto : requestItems) {
            Long productId = dto.getProductId();
            Integer quantity = dto.getQuantity();
            requestedProductIds.add(productId);

            // 수량 수정
            if (existingMap.containsKey(productId)) {
                CartItem existing = existingMap.get(productId);
                existing.updateQuantity(quantity);
                responseItems.add(existing);
            }
            // 새로 추가
            else {
                CartItem newItem = CartItem.create(userId, productId, quantity);
                CartItem savedItem = cartItemRepository.save(newItem);
                responseItems.add(savedItem);
            }
        }

        // 3. 요청에 없는 상품은 삭제
        for (CartItem existing : existingItems) {
            if (!requestedProductIds.contains(existing.getProductId())) {
                cartItemRepository.delete(existing);
            }
        }

        // 4. 응답 Dto 변환
        List<CartItemDto> cartItemDtos = responseItems.stream()
                .map(item -> new CartItemDto(item.getProductId(), item.getQuantity()))
                .toList();

        return new CartItemResponseDto(userId, cartItemDtos);
    }
}
