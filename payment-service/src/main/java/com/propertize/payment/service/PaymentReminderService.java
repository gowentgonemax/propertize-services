package com.propertize.payment.service;

import com.propertize.payment.entity.PaymentReminder;
import com.propertize.payment.enums.ReminderStatusEnum;
import com.propertize.payment.repository.PaymentReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReminderService {

    private final PaymentReminderRepository paymentReminderRepository;

    public List<PaymentReminder> getRemindersByPayment(String paymentId) {
        return paymentReminderRepository.findByPaymentId(paymentId);
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processDueReminders() {
        log.info("Processing due payment reminders...");
        List<PaymentReminder> due = paymentReminderRepository.findDueReminders(LocalDate.now());

        for (PaymentReminder reminder : due) {
            try {
                sendReminder(reminder);
                reminder.setStatus(ReminderStatusEnum.SENT);
                reminder.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to send reminder {}: {}", reminder.getId(), e.getMessage());
                reminder.setRetryCount(reminder.getRetryCount() != null ? reminder.getRetryCount() + 1 : 1);
                reminder.setStatus(ReminderStatusEnum.FAILED);
                reminder.setErrorMessage(e.getMessage());
            }
            paymentReminderRepository.save(reminder);
        }
        log.info("Processed {} due reminders", due.size());
    }

    @Scheduled(cron = "0 0 */4 * * *")
    @Transactional
    public void retryFailedReminders() {
        log.info("Retrying failed payment reminders...");
        List<PaymentReminder> failed = paymentReminderRepository.findFailedRemindersForRetry();

        for (PaymentReminder reminder : failed) {
            try {
                sendReminder(reminder);
                reminder.setStatus(ReminderStatusEnum.SENT);
                reminder.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Retry failed for reminder {}: {}", reminder.getId(), e.getMessage());
                reminder.setRetryCount(reminder.getRetryCount() + 1);
                reminder.setErrorMessage(e.getMessage());
            }
            paymentReminderRepository.save(reminder);
        }
    }

    private void sendReminder(PaymentReminder reminder) {
        // Integration point for email/SMS/push notification service
        // For now: log the reminder (actual sending via notification service or Kafka
        // event)
        log.info("Sending {} reminder for payment {} to {}",
                reminder.getReminderTypeEnum(), reminder.getPaymentId(), reminder.getRecipientEmail());
    }
}
