package com.babpul.matching.point;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}