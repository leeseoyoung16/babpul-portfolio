package com.bappul.matching.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest
@ContextConfiguration(classes = {
        SecurityConfig.class, InternalApiKeyFilter.class, JwtAuthenticationFilter.class, JwtProvider.class,
        InternalApiSecurityTest.StubController.class})
@TestPropertySource(properties = {
        "internal.api-key=" + InternalApiSecurityTest.API_KEY,
        "jwt.secret-key=" + InternalApiSecurityTest.JWT_SECRET})
class InternalApiSecurityTest {

    static final String API_KEY = "test-internal-api-key";
    static final String JWT_SECRET = "test-secret-key-test-secret-key-test-secret-key";

    @Autowired
    MockMvc mockMvc;

    @Test
    void allowsRequestWithValidKey() throws Exception {
        assertThat(status(get("/internal/ping").header("X-Internal-Api-Key", API_KEY))).isEqualTo(200);
    }

    @Test
    void rejectsRequestWithoutKey() throws Exception {
        assertThat(status(get("/internal/ping"))).isNotEqualTo(200);
    }

    @Test
    void rejectsRequestWithWrongKey() throws Exception {
        assertThat(status(get("/internal/ping").header("X-Internal-Api-Key", "wrong"))).isNotEqualTo(200);
    }

    @Test
    void rejectsUserTokenWithoutKey() throws Exception {
        assertThat(status(get("/internal/ping").header("Authorization", "Bearer " + token("ADMIN")))).isNotEqualTo(200);
    }

    @Test
    void rejectsEncodedPathWithoutKey() throws Exception {
        assertThat(status(get(new URI("/%69nternal/ping")))).isNotEqualTo(200);
    }

    @Test
    void keyDoesNotGrantAccessOutsideInternalPaths() throws Exception {
        assertThat(status(get("/private").header("X-Internal-Api-Key", API_KEY))).isNotEqualTo(200);
    }

    private int status(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    private String token(String role) {
        return Jwts.builder()
                .setSubject("uid-1")
                .claim("userId", 1L)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
                .compact();
    }

    @RestController
    static class StubController {
        @GetMapping({"/internal/ping", "/private"})
        String ping() {
            return "ok";
        }
    }
}
