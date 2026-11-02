package com.bappul.userservice.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void sendOrderStatusNotification(String targetToken, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase가 초기화되지 않아 FCM 알림 전송을 건너뜁니다.");
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 알림 전송 성공: {}", response);
        } catch (Exception e) {
            log.warn("FCM 알림 전송 실패: {}", e.getMessage());
        }
    }
}
