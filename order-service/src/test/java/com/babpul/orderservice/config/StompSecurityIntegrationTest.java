package com.babpul.orderservice.config;

import com.babpul.orderservice.client.StoreServiceClient;
import com.babpul.orderservice.client.dto.StoreInfoDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

/** 실제 WebSocket 연결로 STOMP 인증/인가를 검증한다. */
@SpringBootTest(
        classes = {WebSocketConfig.class, StompJwtChannelInterceptor.class, JwtStompAuthenticator.class, JwtProvider.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportAutoConfiguration({
        ServletWebServerFactoryAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        WebSocketServletAutoConfiguration.class})
@TestPropertySource(properties = "jwt.secret-key=" + StompSecurityIntegrationTest.SECRET)
class StompSecurityIntegrationTest {

    static final String SECRET = "test-secret-key-test-secret-key-test-secret-key";
    private static final Long OWNER_ID = 7L;
    private static final String STORE = "/sub/store/20";

    @LocalServerPort
    int port;

    @MockBean
    StoreServiceClient storeServiceClient;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        StoreInfoDto store = new StoreInfoDto();
        store.setId(20L);
        store.setOwnerId(OWNER_ID);
        given(storeServiceClient.getStoreInfo(20L)).willReturn(store);
        given(storeServiceClient.getStoreInfo(21L)).willThrow(new RuntimeException("store-service 연결 불가"));
    }

    @Test
    void rejectsConnectWithoutToken() {
        assertThatThrownBy(() -> connectWith(new StompHeaders(), new TestClient()).get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void userReceivesOwnChannel() throws Exception {
        TestClient client = connect(1L);
        client.session.subscribe("/sub/user/1", client);

        awaitDelivery(client, "/sub/user/1");
    }

    @Test
    void rejectsSubscribeToOtherUser() throws Exception {
        assertSubscribeRejected(1L, "/sub/user/2");
    }

    @Test
    void ownerReceivesStoreChannel() throws Exception {
        TestClient client = connect(OWNER_ID);
        client.session.subscribe(STORE, client);

        awaitDelivery(client, STORE);
    }

    @Test
    void rejectsSubscribeToStoreByNonOwner() throws Exception {
        assertSubscribeRejected(1L, STORE);
    }

    @Test
    void rejectsStoreSubscribeWhenStoreServiceIsDown() throws Exception {
        assertSubscribeRejected(OWNER_ID, "/sub/store/21");
    }

    @Test
    void rejectsWildcardSubscribe() throws Exception {
        assertSubscribeRejected(1L, "/sub/user/*");
        assertSubscribeRejected(OWNER_ID, "/sub/**");
    }

    @Test
    void forgedSendToStoreChannelIsNotDelivered() throws Exception {
        TestClient owner = connect(OWNER_ID);
        owner.session.subscribe(STORE, owner);
        awaitDelivery(owner, STORE);

        TestClient attacker = connect(1L);
        attacker.session.send(STORE, "forged");

        String error = attacker.errors.poll(2, TimeUnit.SECONDS);
        assertThat(owner.messages).doesNotContain("forged");
        assertThat(error).isNotNull();
    }

    // 구독 처리는 비동기이고 Simple Broker는 RECEIPT를 보내지 않으므로 수신될 때까지 재발행한다
    private void awaitDelivery(TestClient client, String destination) {
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).until(() -> {
            messagingTemplate.convertAndSend(destination, "ping");
            return !client.messages.isEmpty();
        });
    }

    private void assertSubscribeRejected(Long userId, String destination) throws Exception {
        TestClient client = connect(userId);
        client.session.subscribe(destination, client);
        assertThat(client.errors.poll(5, TimeUnit.SECONDS)).isNotNull();
    }

    private TestClient connect(Long userId) throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token(userId));
        TestClient client = new TestClient();
        client.session = connectWith(headers, client).get(5, TimeUnit.SECONDS);
        return client;
    }

    private CompletableFuture<StompSession> connectWith(StompHeaders headers, TestClient handler) {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());
        return stompClient.connectAsync("ws://localhost:" + port + "/ws/websocket",
                new WebSocketHttpHeaders(), headers, handler);
    }

    private String token(Long userId) {
        return Jwts.builder()
                .setSubject("uid-" + userId)
                .claim("userId", userId)
                .claim("role", "USER")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
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
