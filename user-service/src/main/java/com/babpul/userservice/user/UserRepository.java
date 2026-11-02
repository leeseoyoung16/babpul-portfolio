package com.babpul.userservice.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUidAndProvider(String uid, String provider);
    Optional<User> findByUid(String uid);

    @Query("SELECT u FROM User u WHERE u.uid = :uidString")
    Optional<User> findUserByUidString(@Param("uidString") String uidString);
}