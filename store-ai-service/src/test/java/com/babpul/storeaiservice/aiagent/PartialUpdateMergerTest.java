package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.MenuClient;
import com.babpul.storeaiservice.client.StoreServiceClient;
import com.babpul.storeaiservice.client.dto.MenuDto;
import com.babpul.storeaiservice.client.dto.StoreNoticeDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartialUpdateMergerTest {

    private static final String DONE = """
            {"candidates":[{"content":{"parts":[{"text":"완료"}]}}]}""";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── 메뉴 ──

    @Test
    void menuPriceOnlyKeepsNameAndPrepTime() throws Exception {
        Map<String, Object> body = MenuUpdateMerger.merge(args("{\"menuId\":3,\"price\":5500}"), menu());

        assertThat(body)
                .containsEntry("name", "아메리카노")
                .containsEntry("price", 5500)
                .containsEntry("prepTime", 5);
    }

    @Test
    void menuNameOnlyKeepsPriceAndPrepTime() throws Exception {
        Map<String, Object> body = MenuUpdateMerger.merge(args("{\"menuId\":3,\"name\":\"라떼\"}"), menu());

        assertThat(body)
                .containsEntry("name", "라떼")
                .containsEntry("price", 4000)
                .containsEntry("prepTime", 5);
    }

    @Test
    void menuRejectsEmptyAndInvalidValues() throws Exception {
        for (String json : List.of(
                "{\"menuId\":3}",
                "{\"menuId\":3,\"name\":\"  \"}",
                "{\"menuId\":3,\"price\":\"5,500\"}",
                "{\"menuId\":3,\"price\":50}",
                "{\"menuId\":3,\"prepTime\":0}")) {
            JsonNode args = args(json);
            assertThatThrownBy(() -> MenuUpdateMerger.merge(args, menu()))
                    .as(json)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── 공지 ──

    @Test
    void noticeTitleOnlyKeepsOtherFields() throws Exception {
        Map<String, String> body = NoticeUpdateMerger.merge(args("{\"noticeId\":9,\"title\":\"새 제목\"}"), notices());

        assertThat(body)
                .containsEntry("title", "새 제목")
                .containsEntry("content", "내용")
                .containsEntry("scheduledAt", "2026-11-05T09:00")
                .containsEntry("expireAt", "2026-11-06T18:00");
        LocalDateTime.parse(body.get("scheduledAt"));
    }

    @Test
    void noticeExpireOnlyKeepsSchedule() throws Exception {
        Map<String, String> body = NoticeUpdateMerger.merge(
                args("{\"noticeId\":9,\"expireAt\":\"2026-11-07T20:00:00\"}"), notices());

        assertThat(body)
                .containsEntry("title", "제목")
                .containsEntry("scheduledAt", "2026-11-05T09:00")
                .containsEntry("expireAt", "2026-11-07T20:00");
    }

    @Test
    void noticeRejectsEmptyInvalidAndUnknownRequests() throws Exception {
        for (String json : List.of(
                "{\"noticeId\":9}",
                "{\"noticeId\":9,\"title\":\" \"}",
                "{\"noticeId\":9,\"expireAt\":\"2026-11-01T00:00:00\"}",
                "{\"noticeId\":404,\"title\":\"새 제목\"}")) {
            JsonNode args = args(json);
            assertThatThrownBy(() -> NoticeUpdateMerger.merge(args, notices()))
                    .as(json)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void noticeRejectsMalformedDate() throws Exception {
        JsonNode args = args("{\"noticeId\":9,\"expireAt\":\"내일\"}");

        assertThatThrownBy(() -> NoticeUpdateMerger.merge(args, notices()))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    // ── AgentService 연결 ──

    @Test
    void agentSendsMergedMenuBody() throws Exception {
        MenuClient menuClient = mock(MenuClient.class);
        GeminiClient gemini = mock(GeminiClient.class);
        when(menuClient.getMenu(3L)).thenReturn(menu());
        when(menuClient.updateMenu(eq(3L), any(), eq(7L))).thenReturn(menu());
        when(gemini.send(any())).thenReturn(toolCall("updateMenu", "{\"menuId\":3,\"price\":5500}"), DONE);

        new AgentService(mock(StoreServiceClient.class), menuClient, gemini, objectMapper)
                .chat(1L, "아메리카노 가격만 5,500원으로 바꿔줘", 7L);

        verify(menuClient).updateMenu(3L, Map.of("name", "아메리카노", "price", 5500, "prepTime", 5), 7L);
    }

    @Test
    void agentDoesNotSaveInvalidMenuUpdate() throws Exception {
        MenuClient menuClient = mock(MenuClient.class);
        GeminiClient gemini = mock(GeminiClient.class);
        when(menuClient.getMenu(3L)).thenReturn(menu());
        when(gemini.send(any())).thenReturn(toolCall("updateMenu", "{\"menuId\":3}"), DONE);

        new AgentService(mock(StoreServiceClient.class), menuClient, gemini, objectMapper)
                .chat(1L, "메뉴 수정해줘", 7L);

        verify(menuClient, never()).updateMenu(anyLong(), any(), anyLong());
    }

    @Test
    void agentSendsMergedNoticeBody() throws Exception {
        StoreServiceClient storeClient = mock(StoreServiceClient.class);
        GeminiClient gemini = mock(GeminiClient.class);
        when(storeClient.getNotices(1L)).thenReturn(notices());
        when(storeClient.updateNotice(eq(9L), any(), eq(7L))).thenReturn(new StoreNoticeDto());
        when(gemini.send(any())).thenReturn(toolCall("updateNotice", "{\"noticeId\":9,\"title\":\"새 제목\"}"), DONE);

        new AgentService(storeClient, mock(MenuClient.class), gemini, objectMapper)
                .chat(1L, "공지 제목만 바꿔줘", 7L);

        verify(storeClient).updateNotice(9L, Map.of(
                "title", "새 제목",
                "content", "내용",
                "scheduledAt", "2026-11-05T09:00",
                "expireAt", "2026-11-06T18:00"), 7L);
    }

    private String toolCall(String name, String argsJson) {
        return """
                {"candidates":[{"content":{"parts":[{"functionCall":{"name":"%s","args":%s}}]}}]}"""
                .formatted(name, argsJson);
    }

    private JsonNode args(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private MenuDto menu() {
        MenuDto menu = new MenuDto();
        menu.setId(3L);
        menu.setStoreId(1L);
        menu.setName("아메리카노");
        menu.setPrice(4000);
        menu.setPrepTime(5);
        return menu;
    }

    private List<StoreNoticeDto> notices() {
        StoreNoticeDto notice = new StoreNoticeDto();
        notice.setId(9L);
        notice.setTitle("제목");
        notice.setContent("내용");
        notice.setScheduledAt(LocalDateTime.of(2026, 11, 5, 9, 0));
        notice.setExpireAt(LocalDateTime.of(2026, 11, 6, 18, 0));
        return List.of(notice);
    }
}
