package com.babpul.storeservice.menu;

import com.babpul.storeservice.store.Store;
import com.babpul.storeservice.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MenuSoldOutTest {

    private final MenuRepository menuRepository = mock(MenuRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final MenuService menuService = new MenuService(menuRepository, storeRepository);
    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new InternalMenuController(menuService)).build();

    private final Menu menu = new Menu();

    @BeforeEach
    void setUp() {
        menu.setId(3L);
        menu.setStoreId(1L);
        menu.setPrice(5000);

        Store store = new Store();
        store.setOwnerId(7L);

        when(menuRepository.findById(3L)).thenReturn(Optional.of(menu));
        when(menuRepository.save(any(Menu.class))).thenAnswer(call -> call.getArgument(0));
        when(menuRepository.findByStoreIdAndIsAvailableTrueAndIsSoldoutFalse(1L)).thenReturn(List.of());
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
    }

    @Test
    void repeatedSoldOutRequestStaysSoldOut() {
        menuService.setSoldOut(3L, true, 7L);
        menuService.setSoldOut(3L, true, 7L);

        assertThat(menu.isSoldout()).isTrue();
    }

    @Test
    void repeatedResumeRequestStaysOnSale() {
        menu.setSoldout(true);

        menuService.setSoldOut(3L, false, 7L);
        menuService.setSoldOut(3L, false, 7L);

        assertThat(menu.isSoldout()).isFalse();
    }

    @Test
    void otherOwnerCannotChangeSoldOut() {
        assertThatThrownBy(() -> menuService.setSoldOut(3L, true, 99L))
                .isInstanceOf(RuntimeException.class);

        assertThat(menu.isSoldout()).isFalse();
        verify(menuRepository, never()).save(any());
    }

    @Test
    void internalApiAppliesRequestedValue() throws Exception {
        menu.setSoldout(true);

        mockMvc.perform(patch("/internal/menus/3/soldout")
                        .param("soldOut", "true")
                        .param("ownerId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSoldOut").value(true));
    }

    @Test
    void internalApiRejectsRequestWithoutSoldOut() throws Exception {
        mockMvc.perform(patch("/internal/menus/3/soldout").param("ownerId", "7"))
                .andExpect(status().isBadRequest());

        assertThat(menu.isSoldout()).isFalse();
    }
}
