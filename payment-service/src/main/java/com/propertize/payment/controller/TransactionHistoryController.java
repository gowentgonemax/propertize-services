package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.commons.dto.ApiResponse;
import com.propertize.payment.entity.TransactionHistory;
import com.propertize.payment.service.TransactionHistoryService;
import com.propertize.commons.dto.ResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/transactions")
@RequiredArgsConstructor
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionHistory>>> getTransactionsByOrganization(
            @RequestParam String organizationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHandler.handlePaginated(
                () -> transactionHistoryService.getTransactionsByOrganization(organizationId, page, size),
                "Transactions");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionHistory>> getTransactionById(@PathVariable String id) {
        return ResponseHandler.handleFind(() -> transactionHistoryService.getTransactionById(id), "Transaction");
    }

    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<ApiResponse<TransactionHistory>> getTransactionByReferenceNumber(
            @PathVariable String referenceNumber) {
        return ResponseHandler.handleFind(
                () -> transactionHistoryService.getTransactionByReferenceNumber(referenceNumber), "Transaction");
    }
}
