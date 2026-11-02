package com.babpul.matching.lunchmatch.timetable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface UserFreeTimeRepository extends JpaRepository<UserFreeTime, Long> {

    List<UserFreeTime> findByUserIdAndDayOfWeekOrderByStartTimeAsc(Long userId, DayOfWeek dayOfWeek);

    List<UserFreeTime> findByUserIdOrderByDayOfWeekAscStartTimeAsc(Long userId);

    void deleteByUserId(Long userId);
}