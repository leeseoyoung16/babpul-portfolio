package com.bappul.storeservice.store;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoreSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNoticeCreated(Store store) {
        messagingTemplate.convertAndSend("/sub/store/" + store.getId(),
                new StoreMessageDto(store, StoreEventType.NOTICE_CREATED));
    }

    public void sendNoticeUpdated(Store store) {
        messagingTemplate.convertAndSend("/sub/store/" + store.getId(),
                new StoreMessageDto(store, StoreEventType.NOTICE_UPDATED));
    }

    public void sendNoticeDeleted(Store store) {
        messagingTemplate.convertAndSend("/sub/store/" + store.getId(),
                new StoreMessageDto(store, StoreEventType.NOTICE_DELETED));
    }

    public void sendScheduleUpdated(Store store) {
        messagingTemplate.convertAndSend("/sub/store/" + store.getId(),
                new StoreMessageDto(store, StoreEventType.SCHEDULE_UPDATED));
    }

    public void sendNoticeActivated(Store store) {
        messagingTemplate.convertAndSend("/sub/store/" + store.getId(),
                new StoreMessageDto(store, StoreEventType.NOTICE_ACTIVATED));
    }

    public void sendNoticeExpired(Store store) {
        messagingTemplate.convertAndSend("/sub/store/" + store.getId(),
                new StoreMessageDto(store, StoreEventType.NOTICE_EXPIRED));
    }
}
