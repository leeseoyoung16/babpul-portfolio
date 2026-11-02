package com.bappul.userservice.config;

import com.bappul.userservice.auth.CustomOAuth2UserService;
import com.bappul.userservice.user.CustomUserDetailsService;
import com.bappul.userservice.auth.JwtAuthenticationFilter;
import com.bappul.userservice.auth.JwtProvider;
import com.bappul.userservice.auth.OAuth2SuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@WebMvcTest
@ContextConfiguration(classes = {
        SecurityConfig.class, InternalApiKeyFilter.class, JwtAuthenticationFilter.class,
        InternalApiSecurityTest.StubController.class})
@TestPropertySource(properties = "internal.api-key=" + InternalApiSecurityTest.API_KEY)
class InternalApiSecurityTest {

    static final String API_KEY = "test-internal-api-key";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    JwtProvider jwtProvider;
    @MockBean
    CustomUserDetailsService customUserDetailsService;
    @MockBean
    CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void allowsRoleChangeWithValidKey() throws Exception {
        assertThat(status(patch("/internal/users/1/role").header("X-Internal-Api-Key", API_KEY))).isEqualTo(200);
    }

    @Test
    void rejectsRoleChangeWithoutKey() throws Exception {
        assertThat(status(patch("/internal/users/1/role"))).isNotEqualTo(200);
    }

    @Test
    void rejectsRoleChangeWithWrongKey() throws Exception {
        assertThat(status(patch("/internal/users/1/role").header("X-Internal-Api-Key", "wrong"))).isNotEqualTo(200);
    }

    @Test
    void rejectsEncodedPathWithoutKey() throws Exception {
        assertThat(status(patch(new URI("/%69nternal/users/1/role")))).isNotEqualTo(200);
    }

    @Test
    void keyDoesNotGrantAccessOutsideInternalPaths() throws Exception {
        assertThat(status(get("/private").header("X-Internal-Api-Key", API_KEY))).isNotEqualTo(200);
    }

    private int status(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    @RestController
    static class StubController {
        @PatchMapping("/internal/users/1/role")
        String role() {
            return "ok";
        }

        @GetMapping("/private")
        String secured() {
            return "ok";
        }
    }
}
