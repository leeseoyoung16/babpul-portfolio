package com.bappul.matching.lunchmatch.timetable;

import com.bappul.matching.config.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/lunch/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @PostMapping
    public void saveClassTimes(
            @RequestBody @Valid List<ClassTimeRequestDto> dtos,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        List<UserClassTime> classTimes = dtos.stream()
                .map(ClassTimeRequestDto::toEntity)
                .toList();
        timetableService.saveClassTimes(authUser.getUserId(), classTimes);
    }

    @GetMapping("/free")
    public List<FreeTimeResponseDto> getFreeTimes(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return timetableService.getAllFreeTimes(authUser.getUserId())
                .stream().map(FreeTimeResponseDto::new).toList();
    }

    @GetMapping("/free/{dayOfWeek}")
    public List<FreeTimeResponseDto> getFreeTimesByDay(
            @PathVariable DayOfWeek dayOfWeek,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return timetableService.getFreeTimesByDay(authUser.getUserId(), dayOfWeek)
                .stream().map(FreeTimeResponseDto::new).toList();
    }
}