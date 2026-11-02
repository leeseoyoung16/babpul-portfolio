package com.babpul.matching.config;

import com.babpul.matching.chat.ChatRoom;
import com.babpul.matching.chat.ChatRoomRepository;
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
import java.util.Optional;
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
        classes = {WebSocketConfig.class, StompJwtChannelInterceptor.class, JwtProvider.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportAutoConfiguration({
        ServletWebServerFactoryAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        WebSocketServletAutoConfiguration.class})
@TestPropertySource(properties = "jwt.secret-key=" + StompSecurityIntegrationTest.SECRET)
class StompSecurityIntegrationTest {

    static final String SECRET = "test-secret-key-test-secret-key-test-secret-key";
    private static final String ROOM = "/sub/chat/room/10";

    @LocalServerPort
    int port;

    @MockBean
    ChatRoomRepository chatRoomRepository;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        ChatRoom room = new ChatRoom();
        room.setUser1Id(1L);
        room.setUser2Id(2L);
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(chatRoomRepository.findById(404L)).willReturn(Optional.empty());
    }

    @Test
    void rejectsConnectWithoutToken() {
        assertConnectRejected(new StompHeaders());
    }

    @Test
    void rejectsConnectWithInvalidToken() {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer not-a-jwt");
        assertConnectRejected(headers);
    }

    @Test
    void rejectsConnectWithExpiredToken() {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token(1L, -60_000));
        assertConnectRejected(headers);
    }

    @Test
    void participantReceivesRoomMessages() throws Exception {
        TestClient client = connect(1L);
        client.session.subscribe(ROOM, client);

        awaitDelivery(client);

        assertThat(client.session.isConnected()).isTrue();
    }

    @Test
    void rejectsSubscribeByNonParticipant() throws Exception {
        assertSubscribeRejected(3L, ROOM);
    }

    @Test
    void rejectsSubscribeToMissingRoom() throws Exception {
        assertSubscribeRejected(1L, "/sub/chat/room/404");
    }

    @Test
    void rejectsWildcardSubscribe() throws Exception {
        assertSubscribeRejected(1L, "/sub/chat/room/*");
        assertSubscribeRejected(1L, "/sub/**");
    }

    @Test
    void forgedSendToBrokerDestinationIsNotDelivered() throws Exception {
        TestClient victim = connect(2L);
        victim.session.subscribe(ROOM, victim);
        awaitDelivery(victim);

        TestClient attacker = connect(1L);
        attacker.session.send(ROOM, "forged");

        String error = attacker.errors.poll(2, TimeUnit.SECONDS);
        assertThat(victim.messages).doesNotContain("forged");
        assertThat(error).isNotNull();
    }

    // 구독 처리는 비동기이고 Simple Broker는 RECEIPT를 보내지 않으므로 수신될 때까지 재발행한다
    private void awaitDelivery(TestClient client) {
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).until(() -> {
            messagingTemplate.convertAndSend(ROOM, "ping");
            return !client.messages.isEmpty();
        });
    }

    private void assertSubscribeRejected(Long userId, String destination) throws Exception {
        TestClient client = connect(userId);
        client.session.subscribe(destination, client);
        assertThat(client.errors.poll(5, TimeUnit.SECONDS)).isNotNull();
    }

    private void assertConnectRejected(StompHeaders connectHeaders) {
        assertThatThrownBy(() -> connectWith(connectHeaders, new TestClient()).get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    private TestClient connect(Long userId) throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token(userId, 60_000));
        TestClient client = new TestClient();
        client.session = connectWith(headers, client).get(5, TimeUnit.SECONDS);
        return client;
    }

    private CompletableFuture<StompSession> connectWith(StompHeaders headers, TestClient handler) {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());
        return stompClient.connectAsync("ws://localhost:" + port + "/ws/chat/websocket",
                new WebSocketHttpHeaders(), headers, handler);
    }

    private String token(Long userId, long ttlMillis) {
        return Jwts.builder()
                .setSubject("uid-" + userId)
                .claim("userId", userId)
                .claim("role", "USER")
                .setExpiration(new Date(System.currentTimeMillis() + ttlMillis))
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
