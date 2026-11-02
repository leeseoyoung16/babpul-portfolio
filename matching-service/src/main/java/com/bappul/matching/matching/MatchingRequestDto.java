package com.bappul.matching.matching;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchingRequestDto {

    @JsonProperty("start_time")
    @NotBlank(message = "시작 시간은 필수입니다.")
    private String startTime;

    @JsonProperty("end_time")
    @NotBlank(message = "종료 시간은 필수입니다.")
    private String endTime;

    @JsonProperty("menu_pref")
    @NotNull(message = "선호 메뉴는 필수입니다.")
    private List<String> menuPref;

    @JsonProperty("duration_min")
    @Min(value = 10, message = "최소 식사 시간은 10분 이상이어야 합니다.")
    private int durationMin;

    @JsonProperty("wait_min")
    @Min(value = 0, message = "대기 시간은 0분 이상이어야 합니다.")
    private double waitMin;

    @NotBlank(message = "희망 장소는 필수입니다.")
    private String venue;
}
