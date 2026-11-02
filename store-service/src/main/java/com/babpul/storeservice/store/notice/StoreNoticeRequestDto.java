package com.babpul.storeservice.store.notice;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StoreNoticeRequestDto {

    @NotBlank(message = "공지 제목은 필수입니다.")
    private String title;

    @NotBlank(message = "공지 내용은 필수입니다.")
    private String content;

    @NotNull(message = "게시 시작 시간은 필수입니다.")
    @Future(message = "게시 시작 시간은 미래 시각이어야 합니다.")
    private LocalDateTime scheduledAt;

    @NotNull(message = "게시 종료 시간은 필수입니다.")
    @Future(message = "게시 종료 시간은 미래 시각이어야 합니다.")
    private LocalDateTime expireAt;
}
