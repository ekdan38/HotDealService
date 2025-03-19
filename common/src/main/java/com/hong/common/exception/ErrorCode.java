package com.hong.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // hotDealProduct
    HOTDEAL_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "HOTDEAL_PRODUCT_00",
            "요청된 핫딜 상품이 존재 하지 않습니다. hotDealProductIds = %s"),

    HOTDEAL_PRODUCT_INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "HOTDEAL_PRODUCT_01",
            "요청 수량보다 재고가 부족합니다. hotDealProductId = %s, 요청 수량 = %s, 재고 수량 = %s"),

    HOTDEAL_PRODUCT_STOCK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "HOTDEAL_PRODUCT_02",
            "요청 수량보다 재고가 부족합니다. hotDealProductId = %s"),

    HOTDEAL_PRODUCT_INVALID_FOUND(HttpStatus.BAD_REQUEST, "HOTDEAL_PRODUCT_03",
            "요청된 핫딜 상품에 대한 수량이 누락 되었습니다. hotDealProductId = %s"),

    HOTDEAL_PRODUCT_ORIGINAL_STOCK_DECREASE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "HOTDEAL_PRODUCT_04",
            "원본 상품 재고 감소 호출을 실패했습니다. request = %s"),

    HOTDEAL_PRODUCT_ORIGINAL_STOCK_INCREASE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "HOTDEAL_PRODUCT_05",
            "원본 상품 재고 증가 호출을 실패했습니다. request = %s"),

    HOTDEAL_PRODUCT_LOCK_FAILED(HttpStatus.CONFLICT, "HOTDEAL_PRODUCT_06",
            "hotDealProduct = %s 에 대한 락 획득에 실패했습니다."),

    HOTDEAL_PRODUCT_LOCK_INTERRUPTED(HttpStatus.SERVICE_UNAVAILABLE, "HOTDEAL_PRODUCT_07",
            "hotDealProduct = %s 에 대한 락 획득중 입터럽트가 발생했습니다."),

    HOTDEAL_PRODUCT_PARSE_RESPONSE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "HOTDEAL_PRODUCT_08",
            "feign Client 에러 응답 파싱 실패했습니다."),

    // hotDeal
    HOTDEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "HOTDEAL_00",
            "요청된 핫딜이 존재하지 않습니다. hotDealId = %s"),

    HOTDEAL_TITLE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "HOTDEAL_01",
            "이미 존재하는 핫딜 Title 입니다. hotDealTitle = %s"),

    HOTDEAL_INVALID_TIME(HttpStatus.BAD_REQUEST, "HOTDEAL_02",
            "시작 시간이 종료 시간보다 이후일 수 없습니다. startTime = %s, endTime = %s"),

    HOTDEAL_NON_ACTIVE(HttpStatus.BAD_REQUEST, "HOTDEAL_03",
            "활성화 된 핫딜이 아닙니다. hotDealId = %s"),

    HOTDEAL_LOCK_INTERRUPTED(HttpStatus.SERVICE_UNAVAILABLE, "HOTDEAL_04",
            "hotDeal_status 에 대한 락 획득 중 입터럽트가 발생했습니다."),

    // order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_00",
            "요청된 주문이 존재하지 않습니다. userId = %s, orderId = %s"),

    ORDER_CANCEL_EXPIRED(HttpStatus.NOT_FOUND, "ORDER_01",
            "주문 취소는 주문 후 하루 이내 가능합니다. userId = %s, orderId = %s"),

    ORDER_RETURN_EXPIRED(HttpStatus.NOT_FOUND, "ORDER_02",
            "환불은 배송 완료 후 하루 이내 가능합니다. userId = %s, orderId = %s"),

    ORDER_FETCH_HOTDEAL_PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_03",
            "핫딜 상품 조회 호출을 실패했습니다. userId = %s, hotDealProducts = %s"),

    ORDER_INCREASE_HOTDEAL_PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_04",
            "핫딜 상품 재고 증가 호출을 실패했습니다. userId = %s, orderId = %s, hotDealProducts = %s"),

    ORDER_DECREASE_HOTDEAL_PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_04",
            "핫딜 상품 재고 감소 호출을 실패했습니다. userId = %s, orderId = %s, hotDealProducts = %s"),

    ORDER_FETCH_PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_05",
            "상품 조회 호출을 실패했습니다. userId = %s, products = %s"),

    ORDER_INCREASE_PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_06",
            "상품 재고 증가 호출을 실패했습니다. userId = %s, orderId = %s, products = %s"),

    ORDER_DECREASE_PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_07",
            "상품 재고 감소 호출을 실패했습니다. userId = %s, orderId = %s, products = %s"),

    ORDER_PRODUCT_PARSE_RESPONSE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_08",
            "feign Client 에러 응답 파싱 실패했습니다."),

    ORDER_HOTDEAL_PRODUCT_SERVICE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_09",
            "%s"),

    ORDER_PRODUCT_SERVICE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_10",
            "%s"),

    ORDER_NOT_PENDING_PAYMENT(HttpStatus.BAD_REQUEST, "ORDER_11",
            "주문이 결제 대기 상태가 아닙니다. userId = %s, orderId = %s"),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_00",
            "존재 하지 않는 결제입니다. userId = %s, paymentId = %s"),

    PAYMENT_EXISTS(HttpStatus.BAD_REQUEST, "PAYMENT_01",
            "이미 존재 하는 결제 입니다. paymentId = %s, orderId = %s"),

    PAYMENT_EXPIRED(HttpStatus.BAD_REQUEST, "PAYMENT_02",
            "만료된 결제입니다. userId = %s, paymentId = %s"),

    PAYMENT_COMPLETED(HttpStatus.BAD_REQUEST, "PAYMENT_03",
            "이미 완료된 결제입니다. userId = %s, paymentId = %s"),

    PAYMENT_PG_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_04",
            "결제 잔액이 부족합니다. userId = %s, paymentId = %s"),

    PAYMENT_PARSE_RESPONSE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_05",
            "feign Client 에러 응답 파싱 실패했습니다."),

    PAYMENT_ORDER_SERVICE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_06",
            "%s"),

    PAYMENT_FETCH_ORDER_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_07",
            "주문 조회 호출을 실패했습니다. userId = %s, orderId = %s"),

    PAYMENT_UPDATE_ORDER_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_08",
            "주문 상태 업데이트 호출을 실패했습니다. userId = %s, orderId = %s"),

    PAYMENT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_09",
            "결제를 실패했습니다. userId = %s, paymentId = %s"),

    PAYMENT_DECREASE_HOTDEAL_PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_09",
            "핫딜 상품 재고 감소 호출을 실패했습니다. userId = %s, orderId = %s, hotDealProducts = %s"),

    PAYMENT_INCREASE_HOTDEAL_PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_10",
            "핫딜 상품 재고 증가 호출을 실패했습니다. userId = %s, orderId = %s, hotDealProducts = %s"),

    PAYMENT_DECREASE__PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_11",
            "상품 재고 감소 호출을 실패했습니다. userId = %s, orderId = %s, products = %s"),

    PAYMENT_INCREASE_PRODUCT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_12",
            "상품 재고 증가 호출을 실패했습니다. userId = %s, orderId = %s, products = %s"),


    // Delivery
    DELIVERY_LOCK_INTERRUPTED(HttpStatus.SERVICE_UNAVAILABLE, "DELIVERY_00",
            "delivery_status 에 대한 락 획득 중 입터럽트가 발생했습니다."),


    // category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_00",
            "요청된 카테고리가 존재하지 않습니다. categoryId = %s"),

    CATEGORY_ROOT_EXISTS(HttpStatus.BAD_REQUEST, "CATEGORY_01",
            "이미 존재 하는 최상위 카테고리 title 입니다. title = %s"),

    CATEGORY_PARENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "CATEGORY_02",
            "존재 하지 않는 부모 카테고리 입니다. parentCategoryId = %s"),

    CATEGORY_PARENT_CHILD_SAME_TITLE(HttpStatus.BAD_REQUEST, "CATEGORY_03",
            "부모 카테고리와 자식 카테고리의 title이 같습니다. parent's Title = %s, child's Title = %s"),

    CATEGORY_PARENT_UNDER_CHILD_EXISTS(HttpStatus.BAD_REQUEST, "CATEGORY_04",
            "부모 카테고리에 이미 존재하는 자식 카테고리 입니다. parent's Title = %s, child's Title = %s"),

    CATEGORY_HAS_CHILDREN(HttpStatus.BAD_REQUEST, "CATEGORY_05",
            "삭제하려는 카테고리에게 자식 카테고리가 존재합니다. categoryId = %s"),

    CATEGORY_IN_USE_BY_PRODUCT(HttpStatus.BAD_REQUEST, "CATEGORY_06",
            "삭제하려는 카테고리를 사용하는 상품이 존재합니다. categoryId = %s"),

    // product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_00",
            "요청된 상품이 존재하지 않습니다. productId = %s"),

    PRODUCT_TITLE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PRODUCT_01",
            "이미 존재하는 상품 title 입니다. title = %s"),

    PRODUCT_LOCK_FAILED(HttpStatus.CONFLICT, "PRODUCT_02",
            "hotDealProduct = %s 에 대한 락 획득에 실패했습니다."),

    PRODUCT_LOCK_INTERRUPTED(HttpStatus.SERVICE_UNAVAILABLE, "PRODUCT_03",
            "hotDealProduct = %s 에 대한 락 획득중 입터럽트가 발생했습니다."),

    PRODUCT_INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_03",
            "요청 수량보다 재고가 부족합니다. productId = %s, 요청 수량 = %s, 재고 수량 = %s"),

    PRODUCT_STOCK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "PRODUCT_04",
            "요청 수량보다 재고가 부족합니다. productId = %s"),


    // wishlist
    WISHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "WISHLIST_00",
            "위시리스트가 존재하지 않습니다. userId = %s"),


    // email
    EMAIL_SENDER_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_00",
            "이메일 인증 코드 전송을 실패했습니다. errorMessage = %s"),

    // SIGNUP
    EMAIL_EMPTY_CODE(HttpStatus.BAD_REQUEST, "SIGNUP_00",
            "해당 이메일로 생성 된 인증 코드가 없습니다. email = %s"),

    EMAIL_VERIFICATION_STATUS_NOT_FOUND(HttpStatus.BAD_REQUEST, "SIGNUP_01",
            "이메일 인증 상태가 존재하지 않습니다. email = %s"),

    EMAIL_VERIFICATION_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "SIGNUP_02",
            "이메일 인증을 받지 않았습니다. email = %s"),

    EMAIL_VERIFICATION_STATUS_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "SIGNUP_03",
            "이미 이메일 인증을 완료 했습니다. email = %s"),

    EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "SIGNUP_04",
            "이메일 인증 코드가 다릅니다. email = %s"),
    // user
    USER_USERNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER_00",
            "이미 존재하는 username 입니다. username = %s"),

    USER_EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER_01",
            "이미 존재하는 email 입니다. email = %s"),

    // Token
    REFRESH_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "TOKEN_00",
            "만료된 RefreshToken 입니다."),

    REFRESH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "TOKEN_01",
            "잘못된 RefreshToken 입니다."),

    REFRESH_TOKEN_NULL(HttpStatus.BAD_REQUEST, "TOKEN_02",
            "RefreshToken 이 존재 하지 않습니다."),

    // 암호, 복호화 오류
    CRYPTO_ENCRYPT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CRYPTO_00",
            "암호화 처리중 오류가 발생했습니다."),

    CRYPTO_DECRYPT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CRYPTO_01",
            "복호화 처리중 오류가 발생했습니다.")

    ;
    private final HttpStatus status;
    private final String errorCode;
    private final String errorMessage;

    ErrorCode(HttpStatus status, String errorCode, String errorMessage) {
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
