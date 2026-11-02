package com.bappul.orderservice.config;

import com.bappul.orderservice.client.StoreServiceClient;
import com.bappul.orderservice.client.StoreServiceFallback;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 Feign 클라이언트가 내부 API 키 헤더를 붙여 호출하는지 검증한다. */
@SpringBootTest(
        classes = {InternalFeignConfigTest.TestConfig.class, InternalFeignConfig.class, StoreServiceFallback.class},
        properties = "internal.api-key=test-internal-api-key")
class InternalFeignConfigTest {

    private static final AtomicReference<String> receivedKey = new AtomicReference<>();
    private static HttpServer server;

    @Autowired
    StoreServiceClient storeServiceClient;

    @DynamicPropertySource
    static void storeServiceUrl(DynamicPropertyRegistry registry) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            receivedKey.set(exchange.getRequestHeaders().getFirst("X-Internal-Api-Key"));
            byte[] body = "{\"id\":20,\"ownerId\":7}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        registry.add("spring.cloud.openfeign.client.config.storeServiceClient.url",
                () -> "http://localhost:" + server.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsInternalApiKeyHeader() {
        assertThat(storeServiceClient.getStoreInfo(20L).getOwnerId()).isEqualTo(7L);
        assertThat(receivedKey.get()).isEqualTo("test-internal-api-key");
    }

    @Configuration
    @EnableFeignClients(clients = StoreServiceClient.class)
    @ImportAutoConfiguration({
            FeignAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class})
    static class TestConfig {
    }
}
