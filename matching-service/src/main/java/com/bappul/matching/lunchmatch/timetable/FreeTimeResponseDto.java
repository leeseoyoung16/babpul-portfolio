package com.bappul.matching.lunchmatch.timetable;

import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
public class FreeTimeResponseDto {

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public FreeTimeResponseDto(UserFreeTime ft) {
        this.dayOfWeek = ft.getDayOfWeek();
        this.startTime = ft.getStartTime();
        this.endTime   = ft.getEndTime();
    }
}