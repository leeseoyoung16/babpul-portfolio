package com.babpul.storeaiservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;

@Getter
@Setter
@NoArgsConstructor
public class StoreScheduleDto {
    private Long id;
    private DayOfWeek dayOfWeek;
    private String openTime;
    private String closeTime;
    private boolean closed;
}