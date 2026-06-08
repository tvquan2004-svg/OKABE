package com.okabe.repository;

import com.okabe.entity.EmailVerificationToken;
import com.okabe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    // Tìm token xác thực email theo token string
    Optional<EmailVerificationToken> findByToken(String token);
    // Tìm token xác thực email theo user
    Optional<EmailVerificationToken> findByUser(User user);
    // Xoá token xác thực email của user
    void deleteByUser(User user);
}
