package com.aerospace.notification_service.event.consumer;

import com.aerospace.notification_service.event.*;
import com.aerospace.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcurementConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "rfq-created-events", containerFactory = "rfqCreatedListenerFactory")
    public void onRfqCreated(@Payload RfqCreatedEvent event) {
        if (event.getSupplierEmail() == null || event.getSupplierEmail().isBlank()) {
            log.warn("[NOTIFY] RFQ {} — supplier {} has no email on file, skipping",
                    event.getRfqNumber(), event.getSupplierName());
            return;
        }
        String due = event.getDueDate() != null
                ? event.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                : "—";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;">
                  <h2 style="color:#4338ca;">New Request for Quotation — %s</h2>
                  <p>Dear %s,</p>
                  <p>You're invited to quote on the following requirement:</p>
                  <table style="border-collapse:collapse;width:100%%;">
                    <tr><td style="padding:8px;font-weight:bold;">Part</td><td style="padding:8px;">%s</td></tr>
                    <tr><td style="padding:8px;font-weight:bold;">Quantity</td><td style="padding:8px;">%d</td></tr>
                    <tr><td style="padding:8px;font-weight:bold;">Quote by</td><td style="padding:8px;">%s</td></tr>
                  </table>
                  <p>Please log in to the Supplier Portal to submit your quotation.</p>
                  <p style="color:#94a3b8;font-size:12px;">RFQ Reference: %s</p>
                </div>
                """.formatted(event.getRfqNumber(), event.getSupplierName(), event.getPartName(),
                event.getQuantity(), due, event.getRfqNumber());

        emailService.sendCustomEmail(event.getSupplierEmail(),
                "RFQ " + event.getRfqNumber() + " — Quotation Requested", html);
    }

    @KafkaListener(topics = "quotation-awarded-events", containerFactory = "quotationAwardedListenerFactory")
    public void onQuotationAwarded(@Payload QuotationAwardedEvent event) {
        if (event.getSupplierEmail() == null || event.getSupplierEmail().isBlank()) return;
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;">
                  <h2 style="color:#059669;">Your Quotation Was Approved — %s</h2>
                  <p>Dear %s,</p>
                  <p>Congratulations — your quotation for RFQ <b>%s</b> at
                  <b>%s</b> per unit has been selected. A Purchase Order will follow shortly
                  through the usual approval process.</p>
                </div>
                """.formatted(event.getRfqNumber(), event.getSupplierName(),
                event.getRfqNumber(), event.getUnitPrice());
        emailService.sendCustomEmail(event.getSupplierEmail(),
                "Quotation Approved — " + event.getRfqNumber(), html);
    }

    @KafkaListener(topics = "quotation-rejected-events", containerFactory = "quotationRejectedListenerFactory")
    public void onQuotationRejected(@Payload QuotationRejectedEvent event) {
        if (event.getSupplierEmail() == null || event.getSupplierEmail().isBlank()) return;
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;">
                  <h2 style="color:#dc2626;">RFQ %s — Outcome</h2>
                  <p>Dear %s,</p>
                  <p>Thank you for your quotation on RFQ <b>%s</b>. On this occasion, we've
                  selected another supplier. We hope to work with you again on a future
                  requirement.</p>
                </div>
                """.formatted(event.getRfqNumber(), event.getSupplierName(), event.getRfqNumber());
        emailService.sendCustomEmail(event.getSupplierEmail(),
                "RFQ " + event.getRfqNumber() + " — Not Selected", html);
    }

    @KafkaListener(topics = "supplier-invite-created-events", containerFactory = "supplierInviteListenerFactory")
    public void onSupplierInviteCreated(@Payload SupplierInviteCreatedEvent event) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;">
                  <h2 style="color:#4338ca;">You've Been Invited to the Supplier Portal</h2>
                  <p>Dear %s,</p>
                  <p>You've been invited to access our Supplier Portal to receive RFQs and
                  submit quotations. Click below to set your password and activate your account:</p>
                  <p><a href="%s" style="background:#4338ca;color:#fff;padding:10px 20px;
                     border-radius:8px;text-decoration:none;">Activate My Account</a></p>
                  <p style="color:#94a3b8;font-size:12px;">This link expires in 7 days.</p>
                </div>
                """.formatted(event.getSupplierName(), event.getActivationUrl());
        emailService.sendCustomEmail(event.getEmail(),
                "Activate Your Supplier Portal Account", html);
    }
}