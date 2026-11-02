package com.bappul.matching.matching.dto;

import java.util.List;

public record UserDto(
        String user_id,
        String start_time,
        String end_time,
        List<String> menu_pref,
        int duration_min,
        String grade,
        String department,
        String gender,
        double wait_min,
        String venue
) {}
