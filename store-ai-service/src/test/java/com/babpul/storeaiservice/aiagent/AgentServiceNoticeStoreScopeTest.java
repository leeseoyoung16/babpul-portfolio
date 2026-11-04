package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.MenuClient;
import com.babpul.storeaiservice.client.StoreServiceClient;
import com.babpul.storeaiservice.client.dto.StoreNoticeDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceNoticeStoreScopeTest {

    private static final long STORE_ID = 1L;
    private static final long OWNER_ID = 7L;
    private static final String DONE = """
            {"candidates":[{"content":{"parts":[{"text":"완료"}]}}]}""";

    private final StoreServiceClient storeServiceClient = mock(StoreServiceClient.class);
    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentService agentService =
            new AgentService(storeServiceClient, mock(MenuClient.class), geminiClient, objectMapper);

    @BeforeEach
    void setUp() {
        StoreNoticeDto notice = new StoreNoticeDto();
        notice.setId(9L);
        when(storeServiceClient.getNotices(STORE_ID)).thenReturn(List.of(notice));
    }

    @Test
    void deletesNoticeOfCurrentStoreWithOwnerId() throws Exception {
        String sent = deleteNotice(9L);

        verify(storeServiceClient).deleteNotice(9L, OWNER_ID);
        assertThat(sent).contains("공지가 삭제되었습니다.");
    }

    @Test
    void rejectsNoticeOfOtherStore() throws Exception {
        String sent = deleteNotice(90L);

        verify(storeServiceClient, never()).deleteNotice(anyLong(), anyLong());
        assertThat(sent).contains("이 가게에서 해당 공지를 찾을 수 없습니다.");
    }

    private String deleteNotice(long noticeId) throws Exception {
        when(geminiClient.send(any())).thenReturn("""
                {"candidates":[{"content":{"parts":[{"functionCall":{"name":"deleteNotice","args":{"noticeId":%d}}}]}}]}"""
                .formatted(noticeId), DONE);

        assertThat(agentService.chat(STORE_ID, "공지 삭제", OWNER_ID)).isEqualTo("완료");

        ArgumentCaptor<Object> request = ArgumentCaptor.forClass(Object.class);
        verify(geminiClient, atLeastOnce()).send(request.capture());
        return objectMapper.writeValueAsString(request.getValue());
    }
}
