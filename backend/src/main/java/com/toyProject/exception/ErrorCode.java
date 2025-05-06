package com.toyProject.exception;

public enum ErrorCode {
    ALREADY_WAITING_PAYMENT("이미 결제 대기 중입니다. 결제를 완료해주세요."),
    ALREADY_JOINED("이미 참여 완료하였습니다."),
    ALREADY_IN_WAITING_LIST("이미 대기열에 등록되어 있습니다."),
    PARTICIPATION_NOT_FOUND("참여 이력이 존재하지 않습니다."),
    PRODUCT_NOT_FOUND("상품이 존재하지 않습니다."),
    ORDER_NOT_FOUND("주문 정보가 존재하지 않습니다."),
    PAYMENT_NOT_FOUND("결제 정보가 존재하지 않습니다."),
    PARTICIPATION_NOT_ALLOWED("현재 참여할 수 없는 상태입니다."),
    ALREADY_ADD_PRODUCT("장바구니에 해당 상품이 존재합니다."),
    LOGIN_FAILED("아이디 또는 비밀번호가 일치하지 않습니다."),
    USERNAME_NOT_FOUND("존재하지 않는 사용자입니다."),
    UNAUTHORIZED("로그인이 필요합니다."),
    CART_EMPTY("장바구니가 비어있습니다."),
    EXPIRED_PAYMENT_FAIL("결제 만료 처리 중 오류가 발생했습니다."),
    PAYMENT_NOT_COMPLETED("결제가 완료되지 않았습니다."),
    PAYMENT_TAMPERED("결제 금액 위변조가 의심됩니다."),
    PAYMENT_VERIFICATION_FAILED( "결제 검증 중 오류 발생하였습니다."),
    PAYMENT_CANCELED_FAILED( "결제 취소 중 오류 발생하였습니다"),
    NOT_WAITING_USER("대기자가 아닙니다."),
    CART_NOT_FOUND("해당 사용자의 장바구니를 찾을 수 없습니다.");
    public final String message;

    ErrorCode(String message) {
        this.message = message;
    }

}


