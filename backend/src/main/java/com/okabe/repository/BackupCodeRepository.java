package com.okabe.repository;

import com.okabe.entity.BackupCode;
import com.okabe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BackupCodeRepository extends JpaRepository<BackupCode, Long> {
    List<BackupCode> findByUserAndIsUsedFalse(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM BackupCode b WHERE b.user.id = :userId")
    void deleteByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
