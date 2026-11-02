package com.babpul.storeservice.store.schedule;

import com.babpul.storeservice.store.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface StoreScheduleRepository extends JpaRepository<StoreSchedule, Long> {
    List<StoreSchedule> findByStoreOrderByDayOfWeekAsc(Store store);
    StoreSchedule findByStoreAndDayOfWeek(Store store, DayOfWeek dayOfWeek);
    void deleteByStore(Store store);
}
