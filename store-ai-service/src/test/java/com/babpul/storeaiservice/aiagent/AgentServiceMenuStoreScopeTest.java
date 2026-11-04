package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.MenuClient;
import com.babpul.storeaiservice.client.StoreServiceClient;
import com.babpul.storeaiservice.client.dto.MenuDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceMenuStoreScopeTest {

    private static final long STORE_ID = 1L;
    private static final long OTHER_STORE_ID = 2L;
    private static final long OWNER_ID = 7L;
    private static final long MENU_ID = 3L;
    private static final long OTHER_STORE_MENU_ID = 30L;
    private static final long STORE_ID_MISSING_MENU_ID = 31L;
    private static final long UNKNOWN_MENU_ID = 32L;
    private static final String OTHER_STORE_MENU_NAME = "다른가게메뉴";
    private static final String DONE = """
            {"candidates":[{"content":{"parts":[{"text":"완료"}]}}]}""";

    private final MenuClient menuClient = mock(MenuClient.class);
    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentService agentService =
            new AgentService(mock(StoreServiceClient.class), menuClient, geminiClient, objectMapper);

    @BeforeEach
    void setUp() {
        when(menuClient.getMenu(MENU_ID)).thenReturn(menu(MENU_ID, STORE_ID, "아메리카노"));
        when(menuClient.getMenu(OTHER_STORE_MENU_ID))
                .thenReturn(menu(OTHER_STORE_MENU_ID, OTHER_STORE_ID, OTHER_STORE_MENU_NAME));
        when(menuClient.getMenu(STORE_ID_MISSING_MENU_ID))
                .thenReturn(menu(STORE_ID_MISSING_MENU_ID, null, OTHER_STORE_MENU_NAME));
        when(menuClient.getMenu(UNKNOWN_MENU_ID)).thenReturn(null);
    }

    // ── 거부 ──

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "getMenu     | {\"menuId\":%d}",
        "updateMenu  | {\"menuId\":%d,\"price\":5500}",
        "disableMenu | {\"menuId\":%d}",
        "setSoldOut  | {\"menuId\":%d,\"soldOut\":true}"
    })
    void rejectsMenuOfOtherStore(String tool, String argsFormat) throws Exception {
        assertRejected(tool, argsFormat.formatted(OTHER_STORE_MENU_ID));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "getMenu     | {\"menuId\":%d}",
        "updateMenu  | {\"menuId\":%d,\"price\":5500}",
        "disableMenu | {\"menuId\":%d}",
        "setSoldOut  | {\"menuId\":%d,\"soldOut\":true}"
    })
    void rejectsMenuWithoutStoreId(String tool, String argsFormat) throws Exception {
        assertRejected(tool, argsFormat.formatted(STORE_ID_MISSING_MENU_ID));
    }

    @Test
    void rejectsUnknownMenu() throws Exception {
        assertRejected("setSoldOut", "{\"menuId\":%d,\"soldOut\":true}".formatted(UNKNOWN_MENU_ID));
    }

    // ── 정상 ──

    @Test
    void getMenuReturnsMenuOfCurrentStore() throws Exception {
        String sent = run("getMenu", "{\"menuId\":3}");

        assertThat(sent).contains("아메리카노").doesNotContain("찾을 수 없습니다");
    }

    @Test
    void updateMenuInCurrentStorePassesOwnerId() throws Exception {
        when(menuClient.updateMenu(eq(MENU_ID), any(), eq(OWNER_ID))).thenReturn(new MenuDto());

        run("updateMenu", "{\"menuId\":3,\"price\":5500}");

        verify(menuClient).updateMenu(MENU_ID, Map.of("name", "아메리카노", "price", 5500, "prepTime", 5), OWNER_ID);
    }

    @Test
    void disableMenuInCurrentStorePassesOwnerId() throws Exception {
        run("disableMenu", "{\"menuId\":3}");

        verify(menuClient).disableMenu(MENU_ID, OWNER_ID);
    }

    @Test
    void setSoldOutInCurrentStorePassesOwnerId() throws Exception {
        when(menuClient.setSoldOut(MENU_ID, true, OWNER_ID)).thenReturn(new MenuDto());

        run("setSoldOut", "{\"menuId\":3,\"soldOut\":true}");

        verify(menuClient).setSoldOut(MENU_ID, true, OWNER_ID);
    }

    /** store-service의 MenuResponseDto와 같은 형태의 응답에서 storeId를 읽을 수 있어야 검증이 동작한다. */
    @Test
    void readsStoreIdFromMenuResponseJson() throws Exception {
        MenuDto menu = objectMapper.readValue("""
                {"id":3,"storeId":1,"name":"아메리카노","price":4000,"prepTime":5,
                 "available":true,"isSoldOut":false,"weight":1,"imgUrl":null}""", MenuDto.class);

        assertThat(menu.getStoreId()).isEqualTo(STORE_ID);
    }

    private void assertRejected(String tool, String argsJson) throws Exception {
        String sent = run(tool, argsJson);

        assertThat(sent).contains("이 가게에서 해당 메뉴를 찾을 수 없습니다.");
        assertThat(sent).doesNotContain(OTHER_STORE_MENU_NAME);
        verify(menuClient, never()).updateMenu(anyLong(), any(), anyLong());
        verify(menuClient, never()).disableMenu(anyLong(), anyLong());
        verify(menuClient, never()).setSoldOut(anyLong(), anyBoolean(), anyLong());
    }

    /** 도구를 한 번 실행하고, 그 결과가 담긴 마지막 Gemini 요청을 문자열로 돌려준다. */
    private String run(String tool, String argsJson) throws Exception {
        when(geminiClient.send(any())).thenReturn("""
                {"candidates":[{"content":{"parts":[{"functionCall":{"name":"%s","args":%s}}]}}]}"""
                .formatted(tool, argsJson), DONE);

        assertThat(agentService.chat(STORE_ID, "메뉴 작업", OWNER_ID)).isEqualTo("완료");

        ArgumentCaptor<Object> request = ArgumentCaptor.forClass(Object.class);
        verify(geminiClient, atLeastOnce()).send(request.capture());
        return objectMapper.writeValueAsString(request.getValue());
    }

    private MenuDto menu(Long id, Long storeId, String name) {
        MenuDto menu = new MenuDto();
        menu.setId(id);
        menu.setStoreId(storeId);
        menu.setName(name);
        menu.setPrice(4000);
        menu.setPrepTime(5);
        return menu;
    }
}
