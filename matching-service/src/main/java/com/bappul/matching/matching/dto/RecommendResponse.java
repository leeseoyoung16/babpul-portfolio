package com.bappul.matching.matching.dto;

import java.util.List;

public record RecommendResponse(
        int count,
        List<RecommendMatchDto> top_k
) {}
