package com.bappul.storeservice.store.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class StoreScheduleRequestDto {

    @NotNull(message = "요일은 필수입니다.")
    private DayOfWeek dayOfWeek;

    @NotBlank(message = "오픈 시간은 필수입니다.")
    private String openTime;

    @NotBlank(message = "마감 시간은 필수입니다.")
    private String closeTime;

    private boolean closed;

    public StoreSchedule toEntity() {
        StoreSchedule schedule = new StoreSchedule();
        schedule.setDayOfWeek(this.dayOfWeek);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        schedule.setOpenTime(LocalTime.parse(this.openTime, formatter));
        schedule.setCloseTime(LocalTime.parse(this.closeTime, formatter));

        schedule.setClosed(this.closed);
        return schedule;
    }
}
