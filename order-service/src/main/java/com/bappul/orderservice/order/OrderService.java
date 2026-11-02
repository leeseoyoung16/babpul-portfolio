package com.bappul.orderservice.order;

import com.bappul.orderservice.cart.Cart;
import com.bappul.orderservice.cart.CartRepository;
import com.bappul.orderservice.cart.CartStatus;
import com.bappul.orderservice.client.MatchingServiceClient;
import com.bappul.orderservice.client.StoreServiceClient;
import com.bappul.orderservice.client.UserServiceClient;
import com.bappul.orderservice.client.dto.FcmRequestDto;
import com.bappul.orderservice.client.dto.StoreInfoDto;
import com.bappul.orderservice.client.dto.UserInfoDto;
import com.bappul.orderservice.orderitem.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderSocketPublisher orderSocketPublisher;
    private final UserServiceClient userServiceClient;
    private final StoreServiceClient storeServiceClient;
    private final MatchingServiceClient matchingServiceClient;

    @Transactional
    public Order createOrder(Long userId, LocalDateTime reservedAt, OrderMode orderMode, String requirement) {
        // 장바구니 조회
        Cart cart = cartRepository.findAllByUserIdAndStatusWithFullItems(userId, CartStatus.ACTIVE)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("활성화된 장바구니가 없습니다."));

        // 장바구니에서 storeId 추출
        Long storeId = cart.getItems().stream()
                .map(cartItem -> cartItem.getStoreId())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("장바구니에 상점 정보가 없습니다."));

        // store-service로 매장 정보 조회
        StoreInfoDto storeInfo = storeServiceClient.getStoreInfo(storeId);
        if (!storeInfo.getIsOpen()) {
            throw new IllegalStateException("현재 상점은 영업 중이 아닙니다.");
        }

        // 품절 메뉴 검사 (store-service 호출)
        cart.getItems().forEach(item -> {
            var menu = storeServiceClient.getMenu(item.getMenuId());
            if (menu.getIsSoldOut()) {
                throw new IllegalStateException("['" + item.getMenuName() + "'] 메뉴가 품절되어 주문할 수 없습니다.");
            }
        });

        // user-service로 사용자 이름 조회
        UserInfoDto userInfo = userServiceClient.getUserById(userId);

        Order order = new Order();
        order.setUserId(userId);
        order.setStoreId(storeId);
        order.setStoreNameSnapshot(storeInfo.getName());
        order.setOwnerIdSnapshot(storeInfo.getOwnerId());
        order.setUserNameSnapshot(userInfo.getUsername());
        order.setOrderAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(cart.getTotalPrice());
        order.setMode(orderMode);
        order.setRequirement(requirement);

        int maxPrepTime = cart.getItems().stream()
                .mapToInt(item -> item.getPrepTime() != null ? item.getPrepTime() : 10)
                .max().orElse(10);

        if (orderMode == OrderMode.IMMEDIATE) {
            order.setReservedAt(null);
            order.setPickupAt(LocalDateTime.now().plusMinutes(maxPrepTime));
        } else {
            if (reservedAt.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new IllegalArgumentException("예약은 최소 1시간 이후부터 가능합니다.");
            }
            if (reservedAt.isAfter(LocalDateTime.now().plusDays(1))) {
                throw new IllegalArgumentException("예약은 최대 다음날 까지만 가능합니다.");
            }
            order.setReservedAt(reservedAt);
            order.setPickupAt(reservedAt);
        }

        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuId(cartItem.getMenuId());
            orderItem.setMenuName(cartItem.getMenuName());
            orderItem.setPriceSnap(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setLineAmount(cartItem.getUnitPrice() * cartItem.getQuantity());
            orderItem.setRealName(cartItem.getMenuName());
            orderItem.setRealPrice(cartItem.getUnitPrice());
            orderItem.setRandomPrice(cartItem.getUnitPrice());
            return orderItem;
        }).collect(Collectors.toList());

        order.setOrderItems(orderItems);
        cart.markCheckedOut();
        cartRepository.save(cart);

        LocalDate today = LocalDate.now();
        int seq = orderRepository.findMaxDailySeq(storeId, today) + 1;
        char storeChar = (char) ('A' + (storeId - 1) % 26);
        order.setOrderDate(today);
        order.setDailySeq(seq);
        order.setOrderNumber(storeChar + "-" + seq);

        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 주문만 취소할 수 있습니다.");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("이미 조리 중이거나 완료된 주문은 취소할 수 없습니다.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderSocketPublisher.sendCancelled(order);
        Order saved = orderRepository.save(order);
        sendOrderStatusFcm(saved.getUserId(), saved.getStoreNameSnapshot(), OrderStatus.CANCELLED);
        return saved;
    }

    @Transactional
    public Order updateOrderStatus(Long requesterId, Long orderId, OrderStatus nextStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));

        if (!order.getOwnerIdSnapshot().equals(requesterId)) {
            throw new IllegalStateException("해당 주문의 매장 운영자가 아닙니다.");
        }

        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    String.format("잘못된 상태 전환입니다: %s → %s", current, nextStatus));
        }

        if (nextStatus == OrderStatus.READY) {
            order.setPickupAt(LocalDateTime.now());
        }

        order.setStatus(nextStatus);
        orderSocketPublisher.sendStatusChanged(order);
        Order saved = orderRepository.save(order);
        sendOrderStatusFcm(saved.getUserId(), saved.getStoreNameSnapshot(), nextStatus);

        if (nextStatus == OrderStatus.COMPLETED) {
            try {
                int points = Math.max(1, (int) Math.round(saved.getTotalPrice() * 0.01));
                matchingServiceClient.earnPoints(Map.of(
                        "userId", saved.getUserId(),
                        "amount", points,
                        "reason", "픽업 완료 포인트"
                ));
            } catch (Exception e) {
                log.warn("[COMPLETED] 포인트 지급 실패: {}", e.getMessage());
            }
        }

        return saved;
    }

    @Transactional
    public Order rejectOrder(Long requesterId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));

        if (!order.getOwnerIdSnapshot().equals(requesterId)) {
            throw new IllegalStateException("해당 주문의 매장 운영자가 아닙니다.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("조리 시작 이후에는 주문을 거절할 수 없습니다.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderSocketPublisher.sendStatusChanged(order);
        Order saved = orderRepository.save(order);
        sendOrderStatusFcm(saved.getUserId(), saved.getStoreNameSnapshot(), OrderStatus.CANCELLED);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrdersByUser(Long userId) {
        return orderRepository.findAllByUserIdWithAll(userId)
                .stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getActiveOrdersByUser(Long userId) {
        return orderRepository.findActiveOrdersByUserIdWithAll(userId)
                .stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getActiveOrdersByStore(Long storeId) {
        return orderRepository.findByStoreIdAndStatusInOrderByPickupAtAsc(
                storeId, List.of(OrderStatus.PAID, OrderStatus.ACCEPTED, OrderStatus.COOKING, OrderStatus.READY))
                .stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getPendingOrdersByStore(Long storeId) {
        return orderRepository.findByStoreIdAndStatusOrderByOrderAtAsc(storeId, OrderStatus.PENDING)
                .stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrdersByStore(Long storeId) {
        return orderRepository.findByStoreIdOrderByOrderAtDesc(storeId)
                .stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatusToPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID=" + orderId));
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    private void sendOrderStatusFcm(Long userId, String storeNameSnapshot, OrderStatus status) {
        String body = switch (status) {
            case ACCEPTED  -> storeNameSnapshot + " 매장에서 주문을 수락했습니다.";
            case COOKING   -> storeNameSnapshot + " 매장에서 조리를 시작했습니다.";
            case READY     -> "음식이 준비됐어요! 픽업하러 와주세요.";
            case COMPLETED -> "픽업이 완료됐습니다. 맛있게 드세요!";
            case CANCELLED -> storeNameSnapshot + " 주문이 취소됐습니다.";
            default -> null;
        };
        if (body == null) return;

        try {
            String fcmToken = userServiceClient.getFcmToken(userId);
            if (fcmToken == null || fcmToken.isBlank()) return;
            userServiceClient.sendFcm(new FcmRequestDto(fcmToken, "주문 상태 변경", body));
        } catch (Exception e) {
            System.out.println("FCM 전송 실패: " + e.getMessage());
        }
    }
}