package com.babpul.storeaiservice.client;

import com.babpul.storeaiservice.client.dto.StoreNoticeDto;
import com.babpul.storeaiservice.client.dto.StoreScheduleDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StoreServiceClientFallback implements StoreServiceClient {

    @Override
    public List<StoreNoticeDto> getNotices(Long storeId) {
        throw new RuntimeException("store-service 연결 불가 - 공지사항을 조회할 수 없습니다.");
    }

    @Override
    public StoreNoticeDto createNotice(Long storeId, Map<String, String> body, Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - 공지사항을 등록할 수 없습니다.");
    }

    @Override
    public StoreNoticeDto updateNotice(Long noticeId, Map<String, String> body, Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - 공지사항을 수정할 수 없습니다.");
    }

    @Override
    public void deleteNotice(Long noticeId, Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - 공지사항을 삭제할 수 없습니다.");
    }

    @Override
    public List<StoreScheduleDto> getSchedules(Long storeId) {
        throw new RuntimeException("store-service 연결 불가 - 영업 스케줄을 조회할 수 없습니다.");
    }

    @Override
    public List<StoreScheduleDto> updateSchedule(Long storeId, String dayOfWeek, Map<String, Object> body, Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - 영업 스케줄을 수정할 수 없습니다.");
    }

    @Override
    public Map<String, Boolean> isStoreOpen(Long storeId) {
        throw new RuntimeException("store-service 연결 불가 - 영업 상태를 조회할 수 없습니다.");
    }

    @Override
    public Map<String, Object> getStoreByOwnerId(Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - 상점 정보를 조회할 수 없습니다.");
    }
}
