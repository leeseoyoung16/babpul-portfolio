package com.bappul.storeservice.store.schedule;

import com.bappul.storeservice.store.Store;
import com.bappul.storeservice.store.StoreRepository;
import com.bappul.storeservice.store.StoreSocketPublisher;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreScheduleService {

    private final StoreRepository storeRepository;
    private final StoreScheduleRepository scheduleRepository;
    private final StoreSocketPublisher storeSocketPublisher;

    /** 스케줄 등록/수정 (사장님만 가능) */
    @Transactional
    public void createOrUpdateSchedule(Long storeId, List<StoreSchedule> schedules, Long ownerId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));

        if (!store.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("해당 상점의 사장님만 스케줄을 수정할 수 있습니다.");
        }

        scheduleRepository.deleteByStore(store);

        for (StoreSchedule schedule : schedules) {
            schedule.setStore(store);
            scheduleRepository.save(schedule);
        }

        storeSocketPublisher.sendScheduleUpdated(store);
    }

    /** 특정 상점의 스케줄 전체 조회 */
    @Transactional
    public List<StoreSchedule> getSchedulesByStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));
        return scheduleRepository.findByStoreOrderByDayOfWeekAsc(store);
    }

    /** 현재 영업 중인지 확인 */
    @Transactional
    public boolean isStoreOpen(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        StoreSchedule schedule = scheduleRepository.findByStoreAndDayOfWeek(store, today);

        if (schedule == null) return false;
        if (schedule.isClosed()) return false;

        LocalTime now = LocalTime.now();
        LocalTime open = schedule.getOpenTime();
        LocalTime close = schedule.getCloseTime();

        return !now.isBefore(open) && !now.isAfter(close);
    }

    /** 기본 스케줄 초기화 (월~금: 09~18, 토~일: 휴무) */
    @Transactional
    public void initializeDefaultSchedule(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));

        if (!scheduleRepository.findByStoreOrderByDayOfWeekAsc(store).isEmpty()) {
            return;
        }

        List<StoreSchedule> defaultSchedules = Arrays.stream(DayOfWeek.values())
                .map(day -> {
                    StoreSchedule schedule = new StoreSchedule();
                    schedule.setStore(store);
                    schedule.setDayOfWeek(day);

                    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                        schedule.setClosed(true);
                        schedule.setOpenTime(LocalTime.of(0, 0));
                        schedule.setCloseTime(LocalTime.of(0, 0));
                    } else {
                        schedule.setClosed(false);
                        schedule.setOpenTime(LocalTime.of(9, 0));
                        schedule.setCloseTime(LocalTime.of(18, 0));
                    }

                    return schedule;
                })
                .toList();

        scheduleRepository.saveAll(defaultSchedules);
    }
}
