package com.babpul.storeservice.store.schedule;

import lombok.Getter;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;

@Getter
public class StoreScheduleResponseDto {

    private Long id;
    private DayOfWeek dayOfWeek;
    private String openTime;
    private String closeTime;
    private boolean closed;

    public StoreScheduleResponseDto(StoreSchedule schedule) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        this.id = schedule.getId();
        this.dayOfWeek = schedule.getDayOfWeek();
        this.openTime = schedule.getOpenTime() != null ? schedule.getOpenTime().format(formatter) : null;
        this.closeTime = schedule.getCloseTime() != null ? schedule.getCloseTime().format(formatter) : null;
        this.closed = schedule.isClosed();
    }
}
