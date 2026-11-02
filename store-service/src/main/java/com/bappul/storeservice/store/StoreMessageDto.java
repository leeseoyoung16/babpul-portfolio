package com.bappul.storeservice.store;

import lombok.Getter;

@Getter
public class StoreMessageDto {
    private String eventType;
    private Long storeId;
    private String storeName;

    public StoreMessageDto(Store store, StoreEventType eventType) {
        this.storeId = store.getId();
        this.storeName = store.getName();
        this.eventType = eventType.name();
    }
}
