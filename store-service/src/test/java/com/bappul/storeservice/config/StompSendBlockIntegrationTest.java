package com.bappul.storeservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** 실제 WebSocket 연결로 공개 구독은 유지되고 클라이언트 SEND만 거부되는지 검증한다. */
@SpringBootTest(
        classes = {WebSocketConfig.class, StompSendBlockInterceptor.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportAutoConfiguration({
        ServletWebServerFactoryAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        WebSocketServletAutoConfiguration.class})
class StompSendBlockIntegrationTest {

    private static final String STORE = "/sub/store/20";

    @LocalServerPort
    int port;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Test
    void forgedSendIsNotDeliveredButServerPublishIs() throws Exception {
        TestClient subscriber = connect();
        subscriber.session.subscribe(STORE, subscriber);
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).until(() -> {
            messagingTemplate.convertAndSend(STORE, "ping");
            return !subscriber.messages.isEmpty();
        });

        TestClient attacker = connect();
        attacker.session.send(STORE, "forged");

        String error = attacker.errors.poll(2, TimeUnit.SECONDS);
        assertThat(subscriber.messages).doesNotContain("forged");
        assertThat(error).isNotNull();
    }

    private TestClient connect() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());
        TestClient client = new TestClient();
        client.session = stompClient.connectAsync("ws://localhost:" + port + "/ws/websocket",
                new WebSocketHttpHeaders(), new StompHeaders(), client).get(5, TimeUnit.SECONDS);
        return client;
    }

    static class TestClient extends StompSessionHandlerAdapter implements StompFrameHandler {
        StompSession session;
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        final BlockingQueue<String> errors = new LinkedBlockingQueue<>();

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            if (headers.getSubscription() != null) {
                messages.add(String.valueOf(payload));
            } else {
                errors.add(String.valueOf(headers.getFirst("message")));
            }
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            errors.add(String.valueOf(exception.getMessage()));
        }
    }
}
