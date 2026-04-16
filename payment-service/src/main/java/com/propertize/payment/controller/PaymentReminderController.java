package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.commons.dto.ApiResponse;
import com.propertize.payment.entity.PaymentReminder;
import com.propertize.payment.service.PaymentReminderService;
import com.propertize.commons.dto.ResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/payment-reminders")
@RequiredArgsConstructor
public class PaymentReminderController {

    private final PaymentReminderService paymentReminderService;

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<ApiResponse<List<PaymentReminder>>> getRemindersByPayment(@PathVariable String paymentId) {
        return ResponseHandler.handleList(() -> paymentReminderService.getRemindersByPayment(paymentId),
                "PaymentReminders");
    }
}
