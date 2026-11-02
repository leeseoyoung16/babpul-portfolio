package com.bappul.storeservice.store;

import com.bappul.storeservice.client.OrderServiceClient;
import com.bappul.storeservice.client.UserServiceClient;
import com.bappul.storeservice.store.schedule.StoreScheduleService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreScheduleService storeScheduleService;
    private final UserServiceClient userServiceClient;
    private final OrderServiceClient orderServiceClient;

    /** 상점 등록 (1인 1가게) */
    @Transactional
    public Store createStore(String name, String address, String phone, Long ownerId, String ownerName) {
        storeRepository.findByOwnerId(ownerId).ifPresent(existingStore -> {
            if (existingStore.isAvailable()) {
                throw new IllegalStateException("이미 운영 중인 상점이 존재합니다. 폐업 후 다시 등록해주세요.");
            }
        });

        Store newStore = new Store();
        newStore.setName(name);
        newStore.setAddress(address);
        newStore.setPhone(phone);
        newStore.setOwnerId(ownerId);
        newStore.setOwnerName(ownerName);
        newStore.setAvailable(true);
        newStore.setOpen(false);

        Store savedStore = storeRepository.save(newStore);
        storeScheduleService.initializeDefaultSchedule(savedStore.getId());

        // user-service에 역할 변경 요청
        try {
            userServiceClient.updateUserRole(ownerId, "OWNER");
        } catch (Exception e) {
            // user-service 역할 변경 실패 시 무시 (추후 보완 필요)
        }

        return savedStore;
    }

    /** 상점 정보 수정 (사장님만 가능) */
    @Transactional
    public Store updateStore(Long storeId, String name, String address, String phone, Long ownerId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));

        if (!store.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("해당 상점의 사장님만 정보를 수정할 수 있습니다.");
        }

        if (!store.isAvailable()) {
            throw new IllegalStateException("폐업된 상점은 수정할 수 없습니다.");
        }

        store.setName(name);
        store.setAddress(address);
        store.setPhone(phone);

        return storeRepository.save(store);
    }

    /** 상점 폐업 처리 */
    @Transactional
    public void closeStore(Long storeId, Long ownerId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));

        if (!store.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("해당 상점의 사장님만 폐업 처리할 수 있습니다.");
        }

        if (!store.isAvailable()) {
            throw new IllegalStateException("이미 폐업된 상점입니다.");
        }

        if (Boolean.TRUE.equals(orderServiceClient.hasActiveOrders(storeId))) {
            throw new IllegalStateException("진행 중인 주문이 있어 폐업 처리할 수 없습니다.");
        }

        store.setAvailable(false);
        store.setOpen(false);
        storeRepository.save(store);
    }

    /** 상점 단일 조회 */
    @Transactional
    public Store getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));

        if (!store.isAvailable()) {
            throw new IllegalStateException("폐업된 상점입니다.");
        }

        return store;
    }

    /** 사장님 본인 상점 조회 */
    @Transactional
    public Store getActiveStoreByOwnerId(Long ownerId) {
        return storeRepository.findActiveStoreByOwnerIdFetch(ownerId)
                .filter(Store::isAvailable)
                .orElseThrow(() -> new EntityNotFoundException("운영 중인 상점이 없습니다."));
    }

    /** 영업 상태 수동 변경 */
    @Transactional
    public void updateStoreOpenStatus(Long storeId, boolean isOpen, Long ownerId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("상점을 찾을 수 없습니다."));

        if (!store.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("해당 상점의 사장님만 영업 상태를 변경할 수 있습니다.");
        }

        if (!store.isAvailable()) {
            throw new IllegalStateException("폐업된 상점의 영업 상태는 변경할 수 없습니다.");
        }

        if (isOpen) {
            store.setOpen(true);
            store.setManualOpen(true);
            store.setManualOpenDate(LocalDate.now());
            store.setManualClosed(false);
            store.setManualClosedDate(null);
        } else {
            if (Boolean.TRUE.equals(orderServiceClient.hasActiveOrders(storeId))) {
                throw new IllegalStateException("진행 중인 주문이 있어 영업을 종료할 수 없습니다.");
            }
            store.setOpen(false);
            store.setManualClosed(true);
            store.setManualClosedDate(LocalDate.now());
            store.setManualOpen(false);
            store.setManualOpenDate(null);
        }
        storeRepository.save(store);
    }

    /** 운영 중 상점 전체 조회 */
    @Transactional
    public List<Store> getAllActiveStores() {
        return storeRepository.findAllActiveStores();
    }
}
