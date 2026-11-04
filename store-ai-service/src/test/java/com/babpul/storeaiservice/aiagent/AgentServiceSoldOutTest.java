package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.MenuClient;
import com.babpul.storeaiservice.client.StoreServiceClient;
import com.babpul.storeaiservice.client.dto.MenuDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceSoldOutTest {

    private static final String DONE = """
            {"candidates":[{"content":{"parts":[{"text":"완료"}]}}]}""";

    private final MenuClient menuClient = mock(MenuClient.class);
    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentService agentService =
            new AgentService(mock(StoreServiceClient.class), menuClient, geminiClient, objectMapper);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void passesRequestedSoldOutValue(boolean soldOut) throws Exception {
        when(menuClient.setSoldOut(3L, soldOut, 7L)).thenReturn(new MenuDto());
        when(geminiClient.send(any())).thenReturn(toolCall(soldOut), DONE);

        String reply = agentService.chat(1L, "품절 상태 변경", 7L);

        assertThat(reply).isEqualTo("완료");
        verify(menuClient).setSoldOut(3L, soldOut, 7L);
    }

    @Test
    void declaresSetSoldOutInsteadOfToggle() throws Exception {
        when(geminiClient.send(any())).thenReturn(DONE);

        agentService.chat(1L, "안녕", 7L);

        ArgumentCaptor<Object> request = ArgumentCaptor.forClass(Object.class);
        verify(geminiClient, atLeastOnce()).send(request.capture());
        assertThat(objectMapper.writeValueAsString(request.getValue()))
                .contains("setSoldOut")
                .doesNotContain("toggleSoldOut");
    }

    private String toolCall(boolean soldOut) {
        return """
                {"candidates":[{"content":{"parts":[{"functionCall":{"name":"setSoldOut","args":{"menuId":3,"soldOut":%s}}}]}}]}"""
                .formatted(soldOut);
    }
}
