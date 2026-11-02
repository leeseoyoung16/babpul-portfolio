package com.bappul.storeservice.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByOwnerId(Long ownerId);

    @Query("SELECT s FROM Store s WHERE s.isAvailable = true")
    List<Store> findAllActiveStores();

    @Query("SELECT s.isAvailable FROM Store s WHERE s.ownerId = :ownerId")
    Boolean findAvailabilityByOwnerId(Long ownerId);

    @Query("SELECT s FROM Store s WHERE s.ownerId = :ownerId")
    Optional<Store> findActiveStoreByOwnerIdFetch(Long ownerId);
}
