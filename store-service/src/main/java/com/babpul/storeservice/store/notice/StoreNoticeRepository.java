package com.babpul.storeservice.store.notice;

import com.babpul.storeservice.store.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface StoreNoticeRepository extends JpaRepository<StoreNotice, Long> {

    List<StoreNotice> findByStoreOrderByCreatedAtDesc(Store store);

    List<StoreNotice> findByActiveTrueOrderByScheduledAtDesc();

    List<StoreNotice> findByStore(Store store);

    @Query("SELECT n FROM StoreNotice n WHERE n.scheduledAt <= :now AND n.expireAt > :now AND n.active = false")
    List<StoreNotice> findNoticesToActivate(LocalDateTime now);

    @Query("SELECT n FROM StoreNotice n WHERE n.expireAt <= :now AND n.active = true")
    List<StoreNotice> findNoticesToDeactivate(LocalDateTime now);
}
