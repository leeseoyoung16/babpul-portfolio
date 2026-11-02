package com.bappul.storeservice.store;

import com.bappul.storeservice.store.schedule.StoreScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreOpenScheduler {

    private final StoreRepository storeRepository;
    private final StoreScheduleService scheduleService;

    @Scheduled(cron = "0 * * * * *")
    public void updateStoreOpenStatusBySchedule() {
        List<Store> stores = storeRepository.findAllActiveStores();

        for (Store store : stores) {
            try {
                LocalDate today = LocalDate.now();
                boolean shouldBeOpen = scheduleService.isStoreOpen(store.getId());

                if (store.isManualClosed()) {
                    if (today.equals(store.getManualClosedDate())) {
                        continue;
                    } else {
                        store.setManualClosed(false);
                        store.setManualClosedDate(null);
                        storeRepository.save(store);
                        log.info("{} 날짜 변경 → 수동 종료 플래그 초기화", store.getName());
                    }
                } else if (store.isManualOpen()) {
                    if (today.equals(store.getManualOpenDate())) {
                        continue;
                    } else {
                        store.setManualOpen(false);
                        store.setManualOpenDate(null);
                        storeRepository.save(store);
                        log.info("{} 날짜 변경 → 수동 열기 플래그 초기화", store.getName());
                    }
                }

                if (store.isOpen() == shouldBeOpen) continue;

                store.setOpen(shouldBeOpen);
                storeRepository.save(store);

                log.info("{} 자동 상태 변경 → {}", store.getName(), shouldBeOpen ? "OPEN" : "CLOSED");
            } catch (Exception e) {
                log.warn("스케줄 확인 실패: storeId={} ({})", store.getId(), e.getMessage());
            }
        }
    }
}
