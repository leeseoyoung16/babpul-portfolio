package com.babpul.matching.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FcmRequestDto {
    private String token;
    private String title;
    private String body;
}