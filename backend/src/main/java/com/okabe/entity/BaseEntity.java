package com.okabe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass // Lớp cơ sở cho các entity, không có bảng riêng
@EntityListeners(AuditingEntityListener.class) // Tự động cập nhật thời gian tạo/sửa
public abstract class BaseEntity {

    @CreatedDate // Tự động gán thời gian tạo
    @Column(name = "created_at", nullable = false, updatable = false) // Thời gian tạo bản ghi (không được null, không thể sửa)
    private LocalDateTime createdAt; // Thời gian tạo bản ghi

    @LastModifiedDate // Tự động gán thời gian cập nhật
    @Column(name = "updated_at") // Thời gian cập nhật bản ghi gần nhất
    private LocalDateTime updatedAt; // Thời gian cập nhật gần nhất
}
