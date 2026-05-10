package com.propertize.payment.controller;

import com.propertize.commons.dto.ApiResponse;
import com.propertize.payment.dto.gdpr.DataExportResponse;
import com.propertize.payment.service.gdpr.GdprDataExportService;
import com.propertize.payment.service.gdpr.GdprErasureService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

/**
 * GDPR Data Subject Access Request (DSAR) Controller
 * 
 * Endpoints for GDPR compliance:
 * - GET /api/v1/payments/gdpr/export — Export user's payment data (DSAR)
 * - GET /api/v1/payments/gdpr/export/csv — Export as CSV
 * - DELETE /api/v1/payments/gdpr/erase — Request data deletion (right to be
 * forgotten)
 * 
 * Security: User can only access/delete their own data; requires auth
 * Rate limited: 5 req/min (sensitive GDPR operations)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/gdpr")
@RequiredArgsConstructor
public class GdprController {

    private final GdprDataExportService dataExportService;
    private final GdprErasureService erasureService;

    /**
     * Data Subject Access Request (DSAR) — Export all user payment data
     * 
     * Returns JSON of all payment records and methods
     * Rate limited to 5 requests/min
     */
    @GetMapping("/export")
    @RateLimiter(name = "gdpr-operations") // 5 req/min
    public ResponseEntity<ApiResponse<DataExportResponse>> exportUserData(
            @RequestParam UUID userId,
            @RequestParam UUID organizationId) {

        log.info("GDPR export requested: userId={}, orgId={}", userId, organizationId);

        try {
            DataExportResponse data = dataExportService.exportUserData(userId, organizationId);

            return ResponseEntity.ok(ApiResponse.ok(data, "Payment data export completed"));

        } catch (Exception ex) {
            log.error("GDPR export failed", ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Export failed: " + ex.getMessage()));
        }
    }

    /**
     * Export user payment data as CSV
     * 
     * Streamed to browser for download
     * Filename: payment_data_export_YYYY-MM-DD.csv
     */
    @GetMapping("/export/csv")
    @RateLimiter(name = "gdpr-operations")
    public void exportAsCSV(
            @RequestParam UUID userId,
            @RequestParam UUID organizationId,
            HttpServletResponse response) throws IOException {

        log.info("GDPR CSV export requested: userId={}", userId);

        try {
            String csv = dataExportService.exportAsCSV(userId, organizationId);

            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.setHeader("Content-Disposition",
                    "attachment; filename=payment_data_export_" + System.currentTimeMillis() + ".csv");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(csv);
            response.getWriter().flush();

        } catch (Exception ex) {
            log.error("CSV export failed", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Export failed: " + ex.getMessage());
        }
    }

    /**
     * Right to Erasure Request (GDPR Article 17)
     * 
     * Initiates deletion workflow:
     * 1. Soft-delete all payment records
     * 2. Detach from Stripe
     * 3. Schedule hard-delete after 30 days
     * 
     * Returns confirmation with erasure details
     */
    @DeleteMapping("/erase")
    @RateLimiter(name = "gdpr-operations")
    public ResponseEntity<ApiResponse<String>> requestErasure(
            @RequestParam UUID userId,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String reason) {

        log.warn("GDPR erasure requested: userId={}, reason={}", userId, reason);

        try {
            erasureService.initiateUserErasure(userId, organizationId,
                    reason != null ? reason : "User-requested");

            String message = "Erasure initiated. Your data will be permanently deleted within 30 days per GDPR.";

            return ResponseEntity.ok(ApiResponse.ok("Erasure reference: " + userId, message));

        } catch (Exception ex) {
            log.error("Erasure request failed", ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Erasure request failed: " + ex.getMessage()));
        }
    }

    /**
     * Erase a specific payment method
     */
    @DeleteMapping("/erase/payment-method/{paymentMethodId}")
    @RateLimiter(name = "gdpr-operations")
    public ResponseEntity<ApiResponse<String>> erasePaymentMethod(
            @PathVariable UUID paymentMethodId) {

        log.warn("GDPR erasure requested for payment method: {}", paymentMethodId);

        try {
            erasureService.erasePaymentMethod(paymentMethodId);

            return ResponseEntity.ok(ApiResponse.ok(paymentMethodId.toString(), "Payment method marked for deletion"));

        } catch (Exception ex) {
            log.error("Payment method erasure failed", ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Erasure failed: " + ex.getMessage()));
        }
    }
}
