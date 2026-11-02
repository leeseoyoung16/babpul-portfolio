package com.bappul.storeservice.store.schedule;

import com.bappul.storeservice.store.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface StoreScheduleRepository extends JpaRepository<StoreSchedule, Long> {
    List<StoreSchedule> findByStoreOrderByDayOfWeekAsc(Store store);
    StoreSchedule findByStoreAndDayOfWeek(Store store, DayOfWeek dayOfWeek);
    void deleteByStore(Store store);
}
