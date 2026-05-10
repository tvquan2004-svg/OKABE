package com.okabe.service;

import com.okabe.entity.NotificationPreference;
import com.okabe.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    private final NotificationPreferenceService preferenceService;

    @Value("${app.url}")
    private String appUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendWorkspaceInvitationEmail(User inviter, String recipientEmail, String recipientName, String workspaceName, String token) {
        log.info("Preparing workspace invitation email for {} (workspace: {})", recipientEmail, workspaceName);
        
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", recipientName);
        vars.put("inviterName", inviter.getUsername());
        vars.put("workspaceName", workspaceName);
        vars.put("invitationUrl", appUrl + "/invitations/accept?token=" + token);

        sendEmail(recipientEmail, "Lời mời tham gia không gian làm việc: " + workspaceName, "workspace-invitation", vars);
        log.info("Invitation email sent to queue for {}", recipientEmail);
    }

    @Async
    public void sendWorkspaceAddedEmail(User inviter, User recipient, String workspaceName, Long workspaceId) {
        log.info("Preparing workspace added email for {} (workspace: {})", recipient.getEmail(), workspaceName);
        
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", recipient.getUsername());
        vars.put("inviterName", inviter.getUsername());
        vars.put("workspaceName", workspaceName);
        vars.put("workspaceUrl", appUrl + "/workspace/" + workspaceId);

        sendEmail(recipient.getEmail(), "Bạn đã được thêm vào không gian làm việc: " + workspaceName, "workspace-added", vars);
    }

    @Async
    public void sendBoardInvitationEmail(User inviter, User recipient, String boardName, Long boardId) {
        NotificationPreference pref = preferenceService.getPreferences(recipient.getId());
        if (!pref.isEmailInvited()) return;

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", recipient.getUsername());
        vars.put("inviterName", inviter.getUsername());
        vars.put("boardName", boardName);
        vars.put("boardUrl", appUrl + "/board/" + boardId);

        sendEmail(recipient.getEmail(), "Bạn đã được thêm vào bảng: " + boardName, "board-invitation", vars);
    }

    @Async
    public void sendCardAssignedEmail(User actor, User recipient, String cardTitle, Long boardId, Long cardId, String boardName) {
        NotificationPreference pref = preferenceService.getPreferences(recipient.getId());
        if (!pref.isEmailAssigned()) return;

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", recipient.getUsername());
        vars.put("actorName", actor.getUsername());
        vars.put("cardTitle", cardTitle);
        vars.put("boardName", boardName);
        vars.put("cardUrl", String.format("%s/board/%d?card=%d", appUrl, boardId, cardId));

        sendEmail(recipient.getEmail(), String.format("%s đã giao cho bạn thẻ: %s", actor.getUsername(), cardTitle), "card-assigned", vars);
    }

    @Async
    public void sendMentionedEmail(User actor, User recipient, String cardTitle, Long boardId, Long cardId, String commentSnippet) {
        NotificationPreference pref = preferenceService.getPreferences(recipient.getId());
        if (!pref.isEmailMentioned()) return;

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", recipient.getUsername());
        vars.put("actorName", actor.getUsername());
        vars.put("cardTitle", cardTitle);
        vars.put("commentContent", commentSnippet);
        vars.put("cardUrl", String.format("%s/board/%d?card=%d", appUrl, boardId, cardId));

        sendEmail(recipient.getEmail(), String.format("%s đã nhắc tên bạn trong %s", actor.getUsername(), cardTitle), "mentioned", vars);
    }

    @Async
    public void sendDueSoonEmail(User recipient, String cardTitle, Long boardId, Long cardId, LocalDateTime dueDate) {
        NotificationPreference pref = preferenceService.getPreferences(recipient.getId());
        if (!pref.isEmailDueSoon()) return;

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", recipient.getUsername());
        vars.put("cardTitle", cardTitle);
        vars.put("dueDate", dueDate.format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy")));
        vars.put("cardUrl", String.format("%s/board/%d?card=%d", appUrl, boardId, cardId));

        sendEmail(recipient.getEmail(), "Nhắc nhở: Thẻ " + cardTitle + " sắp đến hạn", "due-soon", vars);
    }

    @Async
    public void sendOverdueEmail(User recipient, String cardTitle, Long boardId, Long cardId, LocalDateTime dueDate) {
        NotificationPreference pref = preferenceService.getPreferences(recipient.getId());
        if (!pref.isEmailDueSoon()) return;

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", recipient.getUsername());
        vars.put("cardTitle", cardTitle);
        vars.put("dueDate", dueDate.format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy")));
        vars.put("cardUrl", String.format("%s/board/%d?card=%d", appUrl, boardId, cardId));

        sendEmail(recipient.getEmail(),
                "\u26a0\ufe0f Quá hạn: Thẻ \"" + cardTitle + "\" chưa được hoàn thành",
                "overdue",
                vars);
    }

    @Async
    public void sendEmailVerification(User user, String token) {
        log.info("Preparing email verification for {}", user.getEmail());
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", user.getUsername());
        vars.put("verificationUrl", appUrl + "/verify-email?token=" + token);

        sendEmail(user.getEmail(), "Xác thực địa chỉ email của bạn - OKABE", "email-verification", vars);
    }

    private void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            String htmlContent = templateService.render(templateName, variables);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "noreply@okabe.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent to {} with template {}", to, templateName);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send email to {}. Error type: {}, Message: {}", 
                to, e.getClass().getName(), e.getMessage());
            e.printStackTrace();
        }
    }
}
