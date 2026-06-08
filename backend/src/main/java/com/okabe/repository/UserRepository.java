package com.okabe.repository;

import com.okabe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm user theo email
    Optional<User> findByEmail(String email);

    // Tìm user theo tên đăng nhập
    Optional<User> findByUsername(String username);

    // Kiểm tra email đã tồn tại hay chưa
    boolean existsByEmail(String email);
}
