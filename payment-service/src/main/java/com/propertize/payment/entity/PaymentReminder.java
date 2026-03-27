package com.propertize.payment.entity;

import com.propertize.payment.entity.base.AuditableEntity;
import com.propertize.payment.enums.ReminderStatusEnum;
import com.propertize.payment.enums.ReminderTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_reminder", indexes = {
        @Index(name = "idx_reminder_payment", columnList = "payment_id"),
        @Index(name = "idx_reminder_status", columnList = "status"),
        @Index(name = "idx_reminder_scheduled", columnList = "scheduled_date")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class PaymentReminder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", length = 30, nullable = false)
    private ReminderTypeEnum reminderTypeEnum;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ReminderStatusEnum status = ReminderStatusEnum.SCHEDULED;

    @Column(name = "notification_method", length = 50)
    private String notificationMethod;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "email_subject", length = 200)
    private String emailSubject;

    @Column(name = "message_content", length = 2000)
    private String messageContent;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "opted_out")
    private Boolean optedOut = false;

    @Column(name = "sent_at")
    private java.time.LocalDateTime sentAt;
}
