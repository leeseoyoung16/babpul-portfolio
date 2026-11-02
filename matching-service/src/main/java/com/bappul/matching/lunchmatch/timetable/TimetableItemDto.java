package com.bappul.matching.lunchmatch.timetable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TimetableItemDto {
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String subjectName;
}