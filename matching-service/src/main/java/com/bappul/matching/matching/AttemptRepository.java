package com.bappul.matching.matching;

import com.bappul.matching.matching.Attempt.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findTop200ByStatusAndExpiresAtAfterOrderByCreatedAtAsc(Status status, LocalDateTime now);

    List<Attempt> findByStatusAndExpiresAtBefore(Attempt.Status status, LocalDateTime expiresAt);

    List<Attempt> findByStatusAndExpiresAtAfter(Attempt.Status status, LocalDateTime expiresAt);

    Optional<Attempt> findTop1ByUserIdAndStatusOrderByCreatedAtDesc(String userId, Attempt.Status status);
}
