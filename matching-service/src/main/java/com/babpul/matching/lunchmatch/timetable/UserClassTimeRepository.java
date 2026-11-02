package com.babpul.matching.lunchmatch.timetable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface UserClassTimeRepository extends JpaRepository<UserClassTime, Long> {

    List<UserClassTime> findByUserIdAndDayOfWeekOrderByStartTimeAsc(Long userId, DayOfWeek dayOfWeek);

    List<UserClassTime> findByUserIdOrderByDayOfWeekAscStartTimeAsc(Long userId);

    void deleteByUserId(Long userId);
}