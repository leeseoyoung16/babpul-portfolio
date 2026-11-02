package com.bappul.storeservice.store.notice;

import com.bappul.storeservice.store.Store;
import com.bappul.storeservice.store.StoreRepository;
import com.bappul.storeservice.store.StoreSocketPublisher;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreNoticeService {

    private final StoreRepository storeRepository;
    private final StoreNoticeRepository noticeRepository;
    private final StoreSocketPublisher storeSocketPublisher;

    /** 공지 등록 (사장님만 가능) */
    @Transactional
    public StoreNotice createNotice(Long storeId, String title, String content,
                                    LocalDateTime scheduledAt, LocalDateTime expireAt, Long ownerId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));

        if (!store.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("해당 상점의 사장님만 공지를 등록할 수 있습니다.");
        }

        if (expireAt.isBefore(scheduledAt)) {
            throw new IllegalArgumentException("공지 종료 시각은 시작 시각 이후여야 합니다.");
        }

        StoreNotice notice = StoreNotice.builder()
                .store(store)
                .title(title)
                .content(content)
                .scheduledAt(scheduledAt)
                .expireAt(expireAt)
                .active(false)
                .build();

        StoreNotice saved = noticeRepository.save(notice);
        storeSocketPublisher.sendNoticeCreated(store);

        return saved;
    }

    /** 공지 수정 (사장님만 가능) */
    @Transactional
    public StoreNotice updateNotice(Long noticeId, String title, String content,
                                    LocalDateTime scheduledAt, LocalDateTime expireAt, Long ownerId) {
        StoreNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 공지를 찾을 수 없습니다."));

        if (!notice.getStore().getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("해당 상점의 사장님만 공지를 수정할 수 있습니다.");
        }

        if (expireAt.isBefore(scheduledAt)) {
            throw new IllegalArgumentException("공지 종료 시각은 시작 시각 이후여야 합니다.");
        }

        notice.setTitle(title);
        notice.setContent(content);
        notice.setScheduledAt(scheduledAt);
        notice.setExpireAt(expireAt);

        StoreNotice saved = noticeRepository.save(notice);
        storeSocketPublisher.sendNoticeUpdated(notice.getStore());

        return saved;
    }

    /** 공지 삭제 */
    @Transactional
    public Store deleteNoticeAndReturnStore(Long noticeId, Long ownerId) {
        StoreNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 공지를 찾을 수 없습니다."));

        if (!notice.getStore().getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("해당 상점의 사장님만 공지를 삭제할 수 있습니다.");
        }

        Store store = notice.getStore();
        noticeRepository.delete(notice);

        return store;
    }

    /** 특정 상점 공지 조회 */
    @Transactional
    public List<StoreNotice> getNoticesByStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));
        return noticeRepository.findByStoreOrderByCreatedAtDesc(store);
    }

    /** 활성화된 공지 조회 */
    @Transactional
    public List<StoreNotice> getAllActiveNotices() {
        return noticeRepository.findByActiveTrueOrderByScheduledAtDesc();
    }

    public StoreNotice getNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 공지를 찾을 수 없습니다."));
    }
}
