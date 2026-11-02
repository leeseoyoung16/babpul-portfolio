package com.bappul.storeservice.store.notice;

import com.bappul.storeservice.store.StoreSocketPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreNoticeScheduler {

    private final StoreNoticeRepository noticeRepository;
    private final StoreSocketPublisher storeSocketPublisher;

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void autoUpdateActiveStatus() {
        LocalDateTime now = LocalDateTime.now();

        List<StoreNotice> toActivate = noticeRepository.findNoticesToActivate(now);
        for (StoreNotice notice : toActivate) {
            notice.setActive(true);
            noticeRepository.save(notice);
            storeSocketPublisher.sendNoticeActivated(notice.getStore());
            log.info("[NOTICE] 활성화됨 id={}, store={}", notice.getId(), notice.getStore().getId());
        }

        List<StoreNotice> toExpire = noticeRepository.findNoticesToDeactivate(now);
        for (StoreNotice notice : toExpire) {
            notice.setActive(false);
            noticeRepository.save(notice);
            storeSocketPublisher.sendNoticeExpired(notice.getStore());
            log.info("[NOTICE] 만료됨 id={}, store={}", notice.getId(), notice.getStore().getId());
        }
    }
}
