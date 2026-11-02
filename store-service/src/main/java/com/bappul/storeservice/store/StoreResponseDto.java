package com.bappul.storeservice.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class StoreResponseDto {
    private final Long id;
    private final String name;
    private final String address;
    private final String phone;
    @JsonProperty("isOpen")
    private final boolean isOpen;
    @JsonProperty("isAvailable")
    private final boolean isAvailable;
    private final String ownerName;
    private final Long ownerId;
    private final Integer randomPrice;

    public StoreResponseDto(Store store) {
        this.id = store.getId();
        this.name = store.getName();
        this.address = store.getAddress();
        this.phone = store.getPhone();
        this.isOpen = store.isOpen();
        this.isAvailable = store.isAvailable();
        this.ownerName = store.getOwnerName();
        this.ownerId = store.getOwnerId();
        this.randomPrice = store.getRandomPrice();
    }
}
