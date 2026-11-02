package com.babpul.orderservice.randomorder;

import com.babpul.orderservice.client.MatchingServiceClient;
import com.babpul.orderservice.client.MenuClient;
import com.babpul.orderservice.client.StoreServiceClient;
import com.babpul.orderservice.client.dto.MenuDto;
import com.babpul.orderservice.client.dto.StoreInfoDto;
import com.babpul.orderservice.config.AuthenticatedUser;
import com.babpul.orderservice.order.Order;
import com.babpul.orderservice.order.OrderMode;
import com.babpul.orderservice.order.OrderRepository;
import com.babpul.orderservice.order.OrderStatus;
import com.babpul.orderservice.orderitem.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class RandomOrderService {

    private final StoreServiceClient storeServiceClient;
    private final MenuClient menuClient;
    private final OrderRepository orderRepository;
    private final MatchingServiceClient matchingServiceClient;

    @Transactional
    public Order createRandomOrder(AuthenticatedUser authUser, RandomOrderRequestDto request) {
        StoreInfoDto store = storeServiceClient.getStoreInfo(request.getStoreId());
        if (store == null) throw new RuntimeException("해당 상점을 찾을 수 없습니다.");

        List<MenuDto> menus = menuClient.getMenusByStore(request.getStoreId()).stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsSoldOut()))
                .toList();

        if (menus.isEmpty()) throw new IllegalStateException("선택 가능한 메뉴가 없습니다.");

        MenuDto selected = pickRandom(menus);

        int avgPrice = (store.getRandomPrice() != null && store.getRandomPrice() > 0)
                ? store.getRandomPrice()
                : (int) Math.round(
                        menus.stream()
                                .filter(m -> m.getPrice() != null)
                                .mapToInt(MenuDto::getPrice)
                                .average()
                                .orElse(0)
                );

        Order order = new Order();
        order.setUserId(authUser.getUserId());
        order.setStoreId(request.getStoreId());
        order.setStoreNameSnapshot(store.getName());
        order.setOwnerIdSnapshot(store.getOwnerId());
        order.setOrderAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(avgPrice);
        order.setMode(request.getMode());
        order.setRequirement(request.getRequirement());
        order.setRandom(true);

        if (request.getMode() == OrderMode.IMMEDIATE) {
            order.setPickupAt(LocalDateTime.now().plusMinutes(
                    selected.getPrepTime() != null ? selected.getPrepTime() : 15));
        } else {
            LocalDateTime reservedAt = request.getReservedAt();
            if (reservedAt == null) throw new IllegalArgumentException("예약 주문 시 예약 시간을 입력해야 합니다.");
            if (reservedAt.isBefore(LocalDateTime.now().plusHours(1)))
                throw new IllegalArgumentException("예약은 최소 1시간 이후부터 가능합니다.");
            if (reservedAt.isAfter(LocalDateTime.now().plusDays(1)))
                throw new IllegalArgumentException("예약은 최대 1일 이후까지만 가능합니다.");
            order.setReservedAt(reservedAt);
            order.setPickupAt(reservedAt);
        }

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setMenuId(selected.getId());
        item.setMenuName("아무거나");
        item.setPriceSnap(avgPrice);
        item.setQuantity(1);
        item.setLineAmount(avgPrice);
        item.setRealName(selected.getName());
        item.setRealPrice(selected.getPrice());
        item.setRandomPrice(avgPrice);
        order.setOrderItems(List.of(item));

        LocalDate today = LocalDate.now();
        int seq = orderRepository.findMaxDailySeq(request.getStoreId(), today) + 1;
        char storeChar = (char) ('A' + (request.getStoreId() - 1) % 26);
        order.setOrderDate(today);
        order.setDailySeq(seq);
        order.setOrderNumber(storeChar + "-" + seq);

        Order saved = orderRepository.save(order);

        // 선택된 메뉴 실제 가격이 평균보다 저렴하면 가격의 1% 보너스 포인트 지급
        if (selected.getPrice() != null && selected.getPrice() < avgPrice) {
            int bonusPoints = Math.max(1, (int) Math.round(avgPrice * 0.01));
            try {
                matchingServiceClient.earnPoints(Map.of(
                        "userId", authUser.getUserId(),
                        "amount", bonusPoints,
                        "reason", "아무거나 메뉴 보너스 포인트"
                ));
            } catch (Exception e) {
                log.warn("[아무거나] 보너스 포인트 지급 실패: {}", e.getMessage());
            }
        }

        return saved;
    }

    private MenuDto pickRandom(List<MenuDto> menus) {
        return menus.get(new Random().nextInt(menus.size()));
    }
}
