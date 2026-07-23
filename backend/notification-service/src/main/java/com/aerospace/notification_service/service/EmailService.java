package com.aerospace.notification_service.service;

import com.aerospace.notification_service.entity.NotificationLog;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.email.from}")
    private String from;

    @Value("${notification.email.recipients}")
    private String recipientsCsv;

    /**
     * Fires asynchronously — never blocks the Kafka consumer thread.
     */
    @Async
    public void sendAsync(NotificationLog notification) {
        if (!emailEnabled) {
            log.info("[EMAIL] Disabled — skipping for partId={}", notification.getPartId());
            return;
        }

        List<String> recipients = Arrays.stream(recipientsCsv.split(","))
                .map(String::trim)
                .toList();

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(buildSubject(notification));
            helper.setText(buildHtml(notification), true);

            mailSender.send(mime);

            log.info("[EMAIL] ✉️  Sent | type={} partId={} → {}",
                    notification.getType(), notification.getPartId(), recipients);

        } catch (Exception ex) {
            log.error("[EMAIL] ❌ Failed | partId={} error={}", notification.getPartId(), ex.getMessage(), ex);
        }
    }

    @Async
    public void sendCustomEmail(String to, String subject, String html ) {
      if(!emailEnabled) {
        log.info("[Email] Disabled - skipping custom email to {}", to);
        return;
      }
      try{
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(mime);
        log.info("[EMAIL] ✉️  Sent custom email to {} — subject: {}", to, subject);
      } catch(Exception ex) {
        log.error("[EMAIL] ❌ Failed sending to {}: {}", to, ex.getMessage(), ex);
      }
    }
    // ── Private ──────────────────────────────────────────────────────────────

    private String buildSubject(NotificationLog n) {
        String prefix = switch (n.getSeverity() != null ? n.getSeverity() : "INFO") {
            case "SUCCESS" -> "✅";
            case "ERROR"   -> "❌";
            case "WARNING" -> "⚠️";
            default        -> "ℹ️";
        };
        return String.format("[Aerospace] %s %s — %s (Batch: %s)",
                prefix, n.getType(), n.getPartName(), n.getBatchNumber());
    }

    private String buildHtml(NotificationLog n) {
        String accent = switch (n.getSeverity() != null ? n.getSeverity() : "INFO") {
            case "SUCCESS" -> "#22c55e";
            case "ERROR"   -> "#ef4444";
            case "WARNING" -> "#f59e0b";
            default        -> "#3b82f6";
        };

        String inspector = n.getInspectorName() != null ? n.getInspectorName() : "—";
        String location  = n.getLocation()      != null ? n.getLocation()      : "—";

        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;background:#f8fafc;margin:0;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#fff;
                          border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,.1);overflow:hidden;">
                <div style="background:%s;padding:20px 30px;">
                  <h2 style="margin:0;color:#fff;font-size:20px;">
                    Aerospace Parts QA &amp; Inventory — %s
                  </h2>
                </div>
                <div style="padding:30px;">
                  <p style="font-size:15px;color:#1e293b;margin-top:0;">%s</p>
                  <table style="width:100%%;border-collapse:collapse;margin-top:20px;">
                    <tr style="background:#f1f5f9;">
                      <td style="padding:10px 14px;font-weight:bold;color:#475569;width:40%%;">Part ID</td>
                      <td style="padding:10px 14px;color:#1e293b;">%d</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 14px;font-weight:bold;color:#475569;">Part Name</td>
                      <td style="padding:10px 14px;color:#1e293b;">%s</td>
                    </tr>
                    <tr style="background:#f1f5f9;">
                      <td style="padding:10px 14px;font-weight:bold;color:#475569;">Batch Number</td>
                      <td style="padding:10px 14px;color:#1e293b;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 14px;font-weight:bold;color:#475569;">Inspector</td>
                      <td style="padding:10px 14px;color:#1e293b;">%s</td>
                    </tr>
                    <tr style="background:#f1f5f9;">
                      <td style="padding:10px 14px;font-weight:bold;color:#475569;">Location</td>
                      <td style="padding:10px 14px;color:#1e293b;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 14px;font-weight:bold;color:#475569;">Timestamp</td>
                      <td style="padding:10px 14px;color:#1e293b;">%s</td>
                    </tr>
                  </table>
                </div>
                <div style="padding:14px 30px;background:#f8fafc;border-top:1px solid #e2e8f0;
                            font-size:11px;color:#94a3b8;text-align:center;">
                  Aerospace Parts Management System — Automated Notification
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                accent, n.getType(),
                n.getMessage(),
                n.getPartId(), n.getPartName(), n.getBatchNumber(),
                inspector, location, n.getCreatedAt()
        );
    }
}