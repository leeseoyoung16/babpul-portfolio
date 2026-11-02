package com.babpul.orderservice.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/ready/{orderId}")
    public ResponseEntity<KakaoReadyResponse> ready(
            @PathVariable Long orderId,
            @RequestParam int totalAmount) {
        log.info("[READY REQUEST] 주문 ID: {}, 금액: {}", orderId, totalAmount);
        return ResponseEntity.ok(paymentService.ready(orderId, totalAmount));
    }

    @GetMapping("/approve")
    public ResponseEntity<String> approveBridge(
            @RequestParam("partner_order_id") Long orderId,
            @RequestParam("pg_token") String pgToken) {
        log.info("[BRIDGE] 카카오 승인 콜백 수신. orderId={}", orderId);
        String appUrl = "myapp://payment/approve?pg_token=" + pgToken + "&partner_order_id=" + orderId;
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .body(makeReturnHtml("결제 완료", "결제가 완료되었습니다!",
                        "잠시 후 앱으로 이동합니다.<br>이동하지 않으면 아래 버튼을 눌러주세요.", appUrl, "#FEE500"));
    }

    @PostMapping("/approve")
    public ResponseEntity<KakaoApproveResponse> approveFinalize(
            @RequestParam("partner_order_id") Long orderId,
            @RequestParam("pg_token") String pgToken) {
        log.info("[FINAL APPROVE] 앱에서 최종 승인 요청 도착!");
        return ResponseEntity.ok(paymentService.approve(orderId, pgToken));
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> cancel(@RequestParam("partner_order_id") Long orderId) {
        log.warn("[CANCEL] 결제 취소 요청. 주문 ID: {}", orderId);
        paymentService.cancelOrFail(orderId, false);
        String appUrl = "myapp://payment/cancel?partner_order_id=" + orderId;
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .body(makeReturnHtml("결제가 취소되었습니다.", "결제가 취소되었습니다.",
                        "아래 버튼을 눌러 앱으로 돌아가세요.", appUrl, "#ddd"));
    }

    @GetMapping("/fail")
    public ResponseEntity<String> fail(@RequestParam("partner_order_id") Long orderId) {
        log.error("[FAIL] 결제 실패 요청. 주문 ID: {}", orderId);
        paymentService.cancelOrFail(orderId, true);
        String appUrl = "myapp://payment/fail?partner_order_id=" + orderId;
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .body(makeReturnHtml("결제에 실패했습니다.", "결제에 실패했습니다.",
                        "아래 버튼을 눌러 앱으로 돌아가세요.", appUrl, "#ddd"));
    }

    private String makeReturnHtml(String title, String heading, String description, String appUrl, String buttonColor) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>" + title + "</title></head>"
                + "<body style='text-align:center; padding-top:50px; font-family: sans-serif;'>"
                + "<h1>" + heading + "</h1>"
                + "<p>" + description + "</p>"
                + "<a href='" + appUrl + "' style='display:inline-block; background-color:" + buttonColor + "; "
                + "color:#000; padding:15px 30px; text-decoration:none; border-radius:10px; "
                + "font-weight:bold; font-size:18px; margin-top:20px;'>앱으로 돌아가기</a>"
                + "<script>setTimeout(function() { window.location.href = '" + appUrl + "'; }, 100);</script>"
                + "</body></html>";
    }
}