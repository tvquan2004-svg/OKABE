package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "email_assigned", nullable = false)
    private boolean emailAssigned;

    @Column(name = "email_mentioned", nullable = false)
    private boolean emailMentioned;

    @Column(name = "email_due_soon", nullable = false)
    private boolean emailDueSoon;

    @Column(name = "email_invited", nullable = false)
    private boolean emailInvited;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Manual Getters and Setters to avoid Lombok issues
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isEmailAssigned() { return emailAssigned; }
    public void setEmailAssigned(boolean emailAssigned) { this.emailAssigned = emailAssigned; }

    public boolean isEmailMentioned() { return emailMentioned; }
    public void setEmailMentioned(boolean emailMentioned) { this.emailMentioned = emailMentioned; }

    public boolean isEmailDueSoon() { return emailDueSoon; }
    public void setEmailDueSoon(boolean emailDueSoon) { this.emailDueSoon = emailDueSoon; }

    public boolean isEmailInvited() { return emailInvited; }
    public void setEmailInvited(boolean emailInvited) { this.emailInvited = emailInvited; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
