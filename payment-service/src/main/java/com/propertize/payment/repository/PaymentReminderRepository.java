package com.propertize.payment.repository;

import com.propertize.payment.entity.PaymentReminder;
import com.propertize.commons.enums.payment.ReminderStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentReminderRepository extends JpaRepository<PaymentReminder, Long> {

    List<PaymentReminder> findByPaymentId(String paymentId);

    List<PaymentReminder> findByStatus(ReminderStatusEnum status);

    @Query("SELECT r FROM PaymentReminder r WHERE r.scheduledDate <= :today AND r.status = 'SCHEDULED'")
    List<PaymentReminder> findDueReminders(@Param("today") LocalDate today);

    @Query("SELECT r FROM PaymentReminder r WHERE r.scheduledDate < :today AND r.status = 'SCHEDULED'")
    List<PaymentReminder> findOverdueReminders(@Param("today") LocalDate today);

    @Query("SELECT r FROM PaymentReminder r WHERE r.status = 'FAILED' AND r.retryCount < r.maxRetries")
    List<PaymentReminder> findFailedRemindersForRetry();
}
