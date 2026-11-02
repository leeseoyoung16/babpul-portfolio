package com.babpul.matching.point;

import com.babpul.matching.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserServiceClient userServiceClient;

    @Transactional
    public void earnPoints(Long userId, int amount, String reason) {
        userServiceClient.updatePoints(userId, amount);
        pointHistoryRepository.save(new PointHistory(userId, PointType.EARN, amount, reason));
    }

    @Transactional
    public void deductPoints(Long userId, int amount, String reason) {
        userServiceClient.updatePoints(userId, -amount);
        pointHistoryRepository.save(new PointHistory(userId, PointType.DEDUCT, amount, reason));
    }

    @Transactional(readOnly = true)
    public List<PointHistoryResponseDto> getHistory(Long userId) {
        return pointHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(PointHistoryResponseDto::new).collect(Collectors.toList());
    }
}