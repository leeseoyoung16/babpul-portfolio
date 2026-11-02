package com.babpul.matching.lunchmatch.timetable;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class ClassTimeRequestDto {

    @NotNull
    private DayOfWeek dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private String subjectName;

    public UserClassTime toEntity() {
        UserClassTime ct = new UserClassTime();
        ct.setDayOfWeek(dayOfWeek);
        ct.setStartTime(startTime);
        ct.setEndTime(endTime);
        ct.setSubjectName(subjectName);
        return ct;
    }
}