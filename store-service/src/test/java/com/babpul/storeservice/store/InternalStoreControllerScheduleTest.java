package com.babpul.storeservice.store;

import com.babpul.storeservice.store.notice.StoreNoticeService;
import com.babpul.storeservice.store.schedule.StoreSchedule;
import com.babpul.storeservice.store.schedule.StoreScheduleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalStoreControllerScheduleTest {

    private final StoreScheduleService scheduleService = mock(StoreScheduleService.class);
    private final InternalStoreController controller = new InternalStoreController(
            mock(StoreNoticeService.class), scheduleService, mock(StoreRepository.class));

    @Test
    @SuppressWarnings("unchecked")
    void closedDayKeepsRequestedHours() {
        when(scheduleService.getSchedulesByStore(1L)).thenReturn(List.of());

        controller.updateSchedule(1L, "SATURDAY",
                Map.of("closed", true, "openTime", "09:00", "closeTime", "18:00"), 7L);

        ArgumentCaptor<List<StoreSchedule>> saved = ArgumentCaptor.forClass(List.class);
        verify(scheduleService).createOrUpdateSchedule(eq(1L), saved.capture(), eq(7L));
        StoreSchedule schedule = saved.getValue().get(0);
        assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(schedule.isClosed()).isTrue();
        assertThat(schedule.getOpenTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(schedule.getCloseTime()).isEqualTo(LocalTime.of(18, 0));
    }
}
