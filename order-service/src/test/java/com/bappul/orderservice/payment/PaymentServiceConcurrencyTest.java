package com.bappul.orderservice.payment;

import com.bappul.orderservice.KakaoPayProperties;
import com.bappul.orderservice.order.Order;
import com.bappul.orderservice.order.OrderRepository;
import com.bappul.orderservice.order.OrderSocketPublisher;
import com.bappul.orderservice.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentRepository.findTopByOrderOrderByCreatedAtDesc()의 PESSIMISTIC_WRITE 락과
 * PaymentService.approve()의 PENDING 가드가 함께 동작해 동시 승인 요청 중 하나만
 * 성공하는지 검증한다.
 *
 * 시나리오: PENDING -> 첫 요청 락 획득 -> PAID 저장/commit -> 두 번째 요청 락 획득 -> PAID 확인 -> 예외
 */
@DataJpaTest
@Import(PaymentService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentServiceConcurrencyTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private OrderSocketPublisher orderSocketPublisher;

    @MockitoBean
    private KakaoPayProperties kakaoPayProperties;

    @Test
    void 동시_승인요청_두_번_중_한_번만_성공하고_나머지는_이미_승인된_결제_예외() throws InterruptedException {
        // given: PENDING 주문 + PENDING 결제
        Order order = new Order();
        order.setUserId(1L);
        order.setStoreId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(10000);
        order = orderRepository.save(order);
        Long orderId = order.getId();

        Payment payment = Payment.builder()
                .order(order)
                .tid("test-tid")
                .status(PaymentStatus.PENDING)
                .amount(10000)
                .createdAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        KakaoApproveResponse fakeResponse = new KakaoApproveResponse();
        fakeResponse.setTid("test-tid");
        when(restTemplate.postForObject(anyString(), any(), eq(KakaoApproveResponse.class)))
                .thenReturn(fakeResponse);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        // when: 같은 주문에 대해 승인 요청을 동시에 두 번 보낸다
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    paymentService.approve(orderId, "pg_token");
                    successCount.incrementAndGet();
                } catch (Throwable e) {
                    failures.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then: 하나는 성공, 나머지 하나는 "이미 승인된 결제" 예외로 막혀야 한다
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(IllegalStateException.class);
        assertThat(failures.get(0)).hasMessage("이미 승인된 결제입니다.");

        // 두 번째 요청은 가드에서 막혀야 하므로, 카카오 승인 API는 실제로 한 번만 호출돼야 한다
        verify(restTemplate, times(1))
                .postForObject(anyString(), any(), eq(KakaoApproveResponse.class));

        // findTopByOrderOrderByCreatedAtDesc()는 PESSIMISTIC_WRITE 락이 걸려 있어 트랜잭션 밖에서는
        // 호출할 수 없으므로, 검증은 락 없는 findAll()로 조회한다.
        Payment savedPayment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder().getId().equals(orderId))
                .findFirst()
                .orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);

        Order savedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
