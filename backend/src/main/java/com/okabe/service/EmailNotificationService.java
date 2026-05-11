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

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("EmailNotificationService initialized with App URL: {} and From Email: {}", appUrl, fromEmail);
    }

    @Async
    public void sendWorkspaceInvitationEmail(User inviter, String recipientEmail, String recipientName, String workspaceName, String token) {
        String invitationUrl = appUrl + "/invitations/accept?token=" + token;
        log.info("[DEBUG-EMAIL] Preparing invitation: recipient={}, inviter={}, workspace={}, url={}", 
            recipientEmail, inviter.getUsername(), workspaceName, invitationUrl);
        
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", recipientName);
        vars.put("inviterName", inviter.getUsername());
        vars.put("workspaceName", workspaceName);
        vars.put("invitationUrl", invitationUrl);

        sendEmail(recipientEmail, "Lời mời tham gia không gian làm việc: " + workspaceName, "workspace-invitation", vars);
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
        log.info("[DEBUG-EMAIL] Starting sendEmail to: {}, subject: {}, template: {}", to, subject, templateName);
        try {
            String htmlContent = templateService.render(templateName, variables);
            log.info("[DEBUG-EMAIL] Template rendered successfully, length: {}", htmlContent.length());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String sender = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail : "noreply@okabe.com";
            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.info("[DEBUG-EMAIL] Attempting to send MimeMessage from: {} to: {}", sender, to);
            mailSender.send(message);
            log.info("[SUCCESS-EMAIL] Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("[ERROR-EMAIL] FAILED to send email to {}.", to);
            log.error("[ERROR-EMAIL] Exception type: {}", e.getClass().getName());
            log.error("[ERROR-EMAIL] Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("[ERROR-EMAIL] Root cause: {}", e.getCause().getMessage());
            }
            e.printStackTrace();
        }
    }
}
