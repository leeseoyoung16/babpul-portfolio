package com.bappul.orderservice.order;

public enum OrderStatus {
    PENDING,        // 주문 접수됨 (결제 대기)
    PAID,           // 결제 완료됨
    ACCEPTED,       // 매장 측에서 주문 수락
    COOKING,        // 음식 조리 중
    READY,          // 픽업 대기 중
    COMPLETED,      // 픽업 완료
    CANCELLED;      // 주문 취소

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == PAID || next == CANCELLED;
            case PAID -> next == ACCEPTED || next == CANCELLED;
            case ACCEPTED -> next == COOKING || next == CANCELLED;
            case COOKING -> next == READY;
            case READY -> next == COMPLETED;
            default -> false;
        };
    }
}