package com.hong.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // hotDealProduct
    HOTDEAL_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "HOTDEAL_PRODUCT_00",
            "요청된 핫딜 상품이 존재 하지 않습니다. hotDealProductIds = %s"),

    HOTDEAL_PRODUCT_TITLE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "HOTDEAL_PRODUCT_01",
            "이미 존재 하는 핫딜 상품 title 입니다. requestedTitle = %s"),

    HOTDEAL_PRODUCT_INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "HOTDEAL_PRODUCT_02",
            "요청 수량보다 재고가 부족합니다. hotDealProductId = %s, 요청 수량 = %s, 재고 수량 = %s"),


    HOTDEAL_PRODUCT_NOT_ENOUGH_STOCK(HttpStatus.BAD_REQUEST, "HOTDEAL_PRODUCT_03",
            "요청 수량보다 재고가 부족합니다. hotDealProductId = %s"),

    HOTDEAL_PRODUCT_FAIL_DECREASE_STOCK(HttpStatus.BAD_REQUEST, "HOTDEAL_PRODUCT_04",
            "재고 감소를 실패했습니다. productIds = %s"),

    HOTDEAL_PRODUCT_NOT_FOUND_STOCK_IN_REDIS(HttpStatus.NOT_FOUND, "HOTDEAL_PRODUCT_05",
            "Redis 에 재고가 없습니다. productId = {}"),

    HOTDEAL_PRODUCT_INVALID_FOUND(HttpStatus.BAD_REQUEST, "HOTDEAL_PRODUCT_06",
            "요청된 핫딜 상품에 대한 수량이 누락 되었습니다. hotDealProductId = %s"),

    HOTDEAL_PRODUCT_ORIGINAL_STOCK_DECREASE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "HOTDEAL_PRODUCT_07",
            "원본 상품 재고 감소 호출을 실패했습니다. request = %s"),

    HOTDEAL_PRODUCT_ORIGINAL_STOCK_INCREASE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "HOTDEAL_PRODUCT_08",
            "원본 상품 재고 증가 호출을 실패했습니다. request = %s"),

    HOTDEAL_PRODUCT_LOCK_FAILED(HttpStatus.CONFLICT, "HOTDEAL_PRODUCT_09",
            "hotDealProduct = %s 에 대한 락 획득에 실패했습니다."),

    HOTDEAL_PRODUCT_LOCK_INTERRUPTED(HttpStatus.SERVICE_UNAVAILABLE, "HOTDEAL_PRODUCT_10",
            "hotDealProduct = %s 에 대한 락 획득중 입터럽트가 발생했습니다."),

    HOTDEAL_PRODUCT_PARSE_RESPONSE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "HOTDEAL_PRODUCT_11",
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


   //reserve
   RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_00",
           "요청된 재고 점유 내역이 존재하지 않습니다. orderId = %s"),


    // order

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_00",
            "요청된 주문이 존재하지 않습니다. orderId = %s, userId = %s"),

    ORDER_INVALID_CANCEL(HttpStatus.BAD_REQUEST, "ORDER_01",
            "결제 완료된 주문만 주문 취소 할 수 있습니다. userId = %s, orderId = %s"),

    ORDER_CANCEL_EXPIRED(HttpStatus.BAD_REQUEST, "ORDER_02",
            "주문 취소는 주문 후 하루 이내 가능합니다. userId = %s, orderId = %s"),

    // 환불
    ORDER_INVALID_REFUND(HttpStatus.BAD_REQUEST, "ORDER_03",
            "환불은 배송 완료된 주문만 가능합니다. userId = %s, orderId = %s"),

    ORDER_REFUND_EXPIRED(HttpStatus.BAD_REQUEST, "ORDER_04",
            "환불은 배송 완료 후 1일 이내에만 가능합니다. userId = %s, orderId = %s"),

    ORDER_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_13",
            "주문 서비스에서 오류가 발생했습니다."),


    // Feign
    // hotdeal
    ORDER_HOTDEAL_SERVICE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_05", "%s"),
    ORDER_RESERVE_STOCK_BAD_REQUEST(HttpStatus.BAD_REQUEST, "ORDER_06", "%s"),
    ORDER_RESERVE_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_07", "%s"),

    ORDER_CONFIRM_STOCK_BAD_REQUEST(HttpStatus.BAD_REQUEST, "ORDER_08", "%s"),
    ORDER_CONFIRM_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_09", "%s"),

    // user
    ORDER_USER_SERVICE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_10",
            "%s"),

    // payment
    ORDER_PAYMENT_SERVICE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_11",
            "%s"),
    ORDER_PAYMENT_EXPIRE_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_12", "%s"),


    // order 끝


    // paymentSession, payment
    PAYMENT_SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "PAYMENT_SESSION_00",
            "결제 세션이 존재하지 않습니다. userId = %s, sessionId = %s"),

    PAYMENT_SESSION_INVALID_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_SESSION_01",
            "결제 수행 가능한 결제 세션이 아닙니다. userId = %s, sessionId = %s"),

    PAYMENT_SESSION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PAYMENT_SESSION_02",
            "이미 진행 중인 결제 세션이 존재합니다. userId = %s, sessionId = %s"),

    PAYMENT_NOT_READY(HttpStatus.NOT_FOUND, "PAYMENT_00",
            "결제가 준비되지 않았습니다. 잠시 후 다시 시도해주세요. userId = %s, orderId = %s"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_01",
            "존재 하지 않는 결제입니다. userId = %s, orderId = %s, paymentId = %s"),

    PAYMENT_ORDER_SERVICE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_02",
            "%s"),

    PAYMENT_FETCH_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_03", "%s"),

    PAYMENT_UPDATE_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_04", "%s"),

    PAYMENT_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_13",
            "결제 처리 중 오류가 발생했습니다."),

    // Delivery
    DELIVERY_LOCK_INTERRUPTED(HttpStatus.SERVICE_UNAVAILABLE, "DELIVERY_00",
            "delivery_status 에 대한 락 획득 중 입터럽트가 발생했습니다."),


    // wishlist
    CART_ITEM_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "CART_00",
            "장바구니에는 최대 20개의 상품만 담을 수 있습니다. userId = %s"),
    CART_ITEM_LIMIT_QUANTITY(HttpStatus.BAD_REQUEST, "CART_01",
            "장바구니에 상품의 수량은 최대 99개 입니다. userId = %s"),


    // SIGNUP_EMAIL

    EMAIL_EMPTY_CODE(HttpStatus.BAD_REQUEST, "SIGNUP_EMAIL_00",
            "해당 이메일로 생성 된 인증 코드가 없습니다. email = %s"),

    EMAIL_VERIFICATION_STATUS_NOT_FOUND(HttpStatus.BAD_REQUEST, "SIGNUP_EMAIL_01",
            "이메일 인증 상태가 존재하지 않습니다. email = %s"),

    EMAIL_VERIFICATION_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "SIGNUP_EMAIL_02",
            "이메일 인증을 받지 않았습니다. email = %s"),

    EMAIL_VERIFICATION_STATUS_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "SIGNUP_EMAIL_03",
            "이미 이메일 인증을 완료 했습니다. email = %s"),

    EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "SIGNUP_EMAIL_04",
            "이메일 인증 코드가 다릅니다. email = %s"),

    EMAIL_SENDER_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "SIGNUP_EMAIL_05",
            "이메일 인증 코드 전송을 실패했습니다. errorMessage = %s"),

    // SIGNUP_USER

    USER_USERNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "SIGNUP_USER_00",
            "이미 존재하는 username 입니다. username = %s"),

    USER_EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "SINGUP_USER_02",
            "이미 존재하는 email 입니다. email = %s"),

    // JWT_TOKEN
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
