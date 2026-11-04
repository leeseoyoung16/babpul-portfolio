package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.dto.StoreScheduleDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleUpdateMergerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void closingKeepsExistingHours() throws Exception {
        Map<String, Object> body = ScheduleUpdateMerger.merge(
                args("{\"dayOfWeek\":\"SATURDAY\",\"closed\":true}"),
                List.of(schedule(DayOfWeek.SATURDAY, "09:00", "18:00", false)));

        assertBody(body, "SATURDAY", "09:00", "18:00", true);
    }

    @Test
    void reopeningRestoresHoursKeptWhileClosed() throws Exception {
        Map<String, Object> body = ScheduleUpdateMerger.merge(
                args("{\"dayOfWeek\":\"SATURDAY\",\"closed\":false}"),
                List.of(schedule(DayOfWeek.SATURDAY, "09:00", "18:00", true)));

        assertBody(body, "SATURDAY", "09:00", "18:00", false);
    }

    @Test
    void partialUpdateKeepsOtherTime() throws Exception {
        Map<String, Object> body = ScheduleUpdateMerger.merge(
                args("{\"dayOfWeek\":\"MONDAY\",\"closeTime\":\"20:00\"}"),
                List.of(schedule(DayOfWeek.MONDAY, "09:00", "18:00", false)));

        assertBody(body, "MONDAY", "09:00", "20:00", false);
    }

    @Test
    void omittedClosedKeepsClosedDayClosed() throws Exception {
        Map<String, Object> body = ScheduleUpdateMerger.merge(
                args("{\"dayOfWeek\":\"SATURDAY\",\"closeTime\":\"20:00\"}"),
                List.of(schedule(DayOfWeek.SATURDAY, "09:00", "18:00", true)));

        assertBody(body, "SATURDAY", "09:00", "20:00", true);
    }

    @Test
    void usesOnlyTheRequestedDay() throws Exception {
        Map<String, Object> body = ScheduleUpdateMerger.merge(
                args("{\"dayOfWeek\":\"saturday\",\"closeTime\":\"20:00\",\"openTime\":\"10:00\"}"),
                List.of(schedule(DayOfWeek.MONDAY, "09:00", "18:00", true)));

        assertBody(body, "SATURDAY", "10:00", "20:00", false);
    }

    @Test
    void closingWithoutExistingScheduleStoresNoHours() throws Exception {
        Map<String, Object> body = ScheduleUpdateMerger.merge(
                args("{\"dayOfWeek\":\"SATURDAY\",\"closed\":true}"), List.of());

        assertBody(body, "SATURDAY", "00:00", "00:00", true);
    }

    @Test
    void openingWithoutExistingScheduleRequiresHours() throws Exception {
        JsonNode args = args("{\"dayOfWeek\":\"SATURDAY\",\"closed\":false}");

        assertThatThrownBy(() -> ScheduleUpdateMerger.merge(args, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reopeningDayWithoutSavedHoursRequiresHours() throws Exception {
        JsonNode args = args("{\"dayOfWeek\":\"SATURDAY\",\"closed\":false}");
        List<StoreScheduleDto> schedules = List.of(schedule(DayOfWeek.SATURDAY, "00:00", "00:00", true));

        assertThatThrownBy(() -> ScheduleUpdateMerger.merge(args, schedules))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void singleTimeOnDayWithoutSavedHoursRequiresBothTimes() throws Exception {
        JsonNode args = args("{\"dayOfWeek\":\"SATURDAY\",\"closeTime\":\"20:00\"}");
        List<StoreScheduleDto> schedules = List.of(schedule(DayOfWeek.SATURDAY, "00:00", "00:00", true));

        assertThatThrownBy(() -> ScheduleUpdateMerger.merge(args, schedules))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reopeningWithNewHoursReplacesMissingHours() throws Exception {
        Map<String, Object> body = ScheduleUpdateMerger.merge(
                args("{\"dayOfWeek\":\"SATURDAY\",\"closed\":false,\"openTime\":\"10:00\",\"closeTime\":\"15:00\"}"),
                List.of(schedule(DayOfWeek.SATURDAY, "00:00", "00:00", true)));

        assertBody(body, "SATURDAY", "10:00", "15:00", false);
    }

    private void assertBody(Map<String, Object> body, String day, String open, String close, boolean closed) {
        assertThat(body)
                .containsEntry("dayOfWeek", day)
                .containsEntry("openTime", open)
                .containsEntry("closeTime", close)
                .containsEntry("closed", closed);
    }

    private JsonNode args(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private StoreScheduleDto schedule(DayOfWeek day, String open, String close, boolean closed) {
        StoreScheduleDto dto = new StoreScheduleDto();
        dto.setDayOfWeek(day);
        dto.setOpenTime(open);
        dto.setCloseTime(close);
        dto.setClosed(closed);
        return dto;
    }
}
