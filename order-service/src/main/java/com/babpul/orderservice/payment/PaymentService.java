package com.babpul.orderservice.payment;

import com.babpul.orderservice.KakaoPayProperties;
import com.babpul.orderservice.order.Order;
import com.babpul.orderservice.order.OrderRepository;
import com.babpul.orderservice.order.OrderSocketPublisher;
import com.babpul.orderservice.order.OrderStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final KakaoPayProperties props;
    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderSocketPublisher orderSocketPublisher;

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + props.getSecretKey());
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    @Transactional
    public KakaoReadyResponse ready(Long orderId, int ignoredAmount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new IllegalStateException("이미 결제가 완료된 주문입니다.");
        }

        int totalAmount = order.getTotalPrice();

        Map<String, Object> params = new HashMap<>();
        params.put("cid", props.getCid());
        params.put("partner_order_id", String.valueOf(orderId));
        params.put("partner_user_id", order.getUserId().toString());
        params.put("item_name", "밥플 주문 결제");
        params.put("quantity", 1);
        params.put("total_amount", totalAmount);
        params.put("tax_free_amount", 0);
        params.put("approval_url", props.getApprovalUrl() + "?partner_order_id=" + orderId);
        params.put("cancel_url", props.getCancelUrl() + "?partner_order_id=" + orderId);
        params.put("fail_url", props.getFailUrl() + "?partner_order_id=" + orderId);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, getHeaders());
        KakaoReadyResponse response;
        try {
            response = restTemplate.postForObject(
                    "https://open-api.kakaopay.com/online/v1/payment/ready",
                    requestEntity, KakaoReadyResponse.class);
        } catch (RestClientException e) {
            log.error("[READY] 카카오페이 결제 준비 요청 실패 - orderId={}, error={}", orderId, e.getMessage());
            throw new IllegalStateException("결제 준비 요청에 실패했습니다.");
        }

        if (response == null || response.getTid() == null) {
            log.error("[READY] 카카오페이 응답이 올바르지 않습니다 - orderId={}", orderId);
            throw new IllegalStateException("결제 준비 응답이 올바르지 않습니다.");
        }

        log.info("[READY] 결제 요청 완료 - orderId={}, tid={}", orderId, response.getTid());

        Payment payment = Payment.builder()
                .order(order)
                .tid(response.getTid())
                .status(PaymentStatus.PENDING)
                .amount(totalAmount)
                .createdAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        return response;
    }

    @Transactional
    public KakaoApproveResponse approve(Long orderId, String pgToken) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        Payment payment = paymentRepository.findTopByOrderOrderByCreatedAtDesc(order)
                .orElseThrow(() -> new IllegalStateException("결제 요청 기록이 없습니다."));

        if (payment.getStatus() != PaymentStatus.PENDING ||
                order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("이미 승인된 결제입니다.");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("cid", props.getCid());
        params.put("tid", payment.getTid());
        params.put("partner_order_id", String.valueOf(orderId));
        params.put("partner_user_id", order.getUserId().toString());
        params.put("pg_token", pgToken);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, getHeaders());
        KakaoApproveResponse response;
        try {
            response = restTemplate.postForObject(
                    "https://open-api.kakaopay.com/online/v1/payment/approve",
                    requestEntity, KakaoApproveResponse.class);
        } catch (RestClientException e) {
            log.error("[APPROVE] 카카오페이 결제 승인 요청 실패 - orderId={}, error={}", orderId, e.getMessage());
            throw new IllegalStateException("결제 승인 요청에 실패했습니다.");
        }

        if (response == null || response.getTid() == null) {
            log.error("[APPROVE] 카카오페이 승인 응답이 올바르지 않습니다 - orderId={}", orderId);
            throw new IllegalStateException("결제 승인 응답이 올바르지 않습니다.");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setApprovedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        orderSocketPublisher.sendNewOrder(order);

        log.info("[APPROVE] 결제 승인 완료 - orderId={}, tid={}", orderId, payment.getTid());
        return response;
    }

    @Transactional
    public void cancelOrFail(Long orderId, boolean isFail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        Payment payment = paymentRepository.findTopByOrderOrderByCreatedAtDesc(order)
                .orElseThrow(() -> new IllegalStateException("결제 내역이 없습니다."));

        if (!isFail && payment.getStatus() == PaymentStatus.PAID) { // 환불
            Map<String, Object> params = new HashMap<>();
            params.put("cid", props.getCid());
            params.put("tid", payment.getTid());
            params.put("cancel_amount", payment.getAmount());
            params.put("cancel_tax_free_amount", 0);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, getHeaders());
            try {
                restTemplate.postForObject(
                        "https://open-api.kakaopay.com/online/v1/payment/cancel",
                        requestEntity, Map.class);
            } catch (RestClientException e) {
                log.error("[CANCEL] 카카오페이 결제 취소 요청 실패 - orderId={}, error={}", orderId, e.getMessage());
                throw new IllegalStateException("결제 취소 요청에 실패했습니다.");
            }
        }

        if (isFail) {
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.PENDING);
        } else {
            payment.setStatus(PaymentStatus.REFUNDED);
            order.setStatus(OrderStatus.CANCELLED);
        }

        paymentRepository.save(payment);
        orderRepository.save(order);
    }
}