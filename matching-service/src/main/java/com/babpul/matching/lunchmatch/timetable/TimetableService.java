package com.babpul.matching.lunchmatch.timetable;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private static final LocalTime SCHOOL_START = LocalTime.of(0, 0);
    private static final LocalTime SCHOOL_END   = LocalTime.of(23, 59);

    private final UserClassTimeRepository classTimeRepository;
    private final UserFreeTimeRepository  freeTimeRepository;

    @Transactional
    public void saveClassTimes(Long userId, List<UserClassTime> classTimes) {
        classTimeRepository.deleteByUserId(userId);
        for (UserClassTime ct : classTimes) {
            ct.setUserId(userId);
        }
        classTimeRepository.saveAll(classTimes);
        recalculateFreeTime(userId);
    }

    @Transactional
    public void recalculateFreeTime(Long userId) {
        freeTimeRepository.deleteByUserId(userId);

        List<UserFreeTime> freeTimes = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            List<UserClassTime> classes = classTimeRepository
                    .findByUserIdAndDayOfWeekOrderByStartTimeAsc(userId, day);
            freeTimes.addAll(calculateFreeSlots(userId, day, classes));
        }
        freeTimeRepository.saveAll(freeTimes);
    }

    public List<UserFreeTime> getFreeTimesByDay(Long userId, DayOfWeek day) {
        return freeTimeRepository.findByUserIdAndDayOfWeekOrderByStartTimeAsc(userId, day);
    }

    public List<UserFreeTime> getAllFreeTimes(Long userId) {
        return freeTimeRepository.findByUserIdOrderByDayOfWeekAscStartTimeAsc(userId);
    }

    public boolean hasTimetable(Long userId) {
        return !classTimeRepository.findByUserIdOrderByDayOfWeekAscStartTimeAsc(userId).isEmpty();
    }

    private List<UserFreeTime> calculateFreeSlots(Long userId, DayOfWeek day,
                                                   List<UserClassTime> classes) {
        List<UserFreeTime> result = new ArrayList<>();
        LocalTime cursor = SCHOOL_START;

        for (UserClassTime cls : classes) {
            if (cursor.isBefore(cls.getStartTime())) {
                result.add(freeTime(userId, day, cursor, cls.getStartTime()));
            }
            if (cls.getEndTime().isAfter(cursor)) {
                cursor = cls.getEndTime();
            }
        }

        if (cursor.isBefore(SCHOOL_END)) {
            result.add(freeTime(userId, day, cursor, SCHOOL_END));
        }

        return result;
    }

    private UserFreeTime freeTime(Long userId, DayOfWeek day, LocalTime start, LocalTime end) {
        UserFreeTime ft = new UserFreeTime();
        ft.setUserId(userId);
        ft.setDayOfWeek(day);
        ft.setStartTime(start);
        ft.setEndTime(end);
        return ft;
    }
}