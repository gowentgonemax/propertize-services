package com.propertize.payment.service;

import com.propertize.payment.entity.TransactionHistory;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.repository.TransactionHistoryRepository;
import com.propertize.payment.util.PaginationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionHistoryService {

    private final TransactionHistoryRepository transactionHistoryRepository;
    private static final AtomicLong SEQUENCE = new AtomicLong(1);

    public Page<TransactionHistory> getTransactionsByOrganization(String organizationId, int page, int size) {
        Pageable pageable = PaginationValidator.createPageable(page, size, "transactionDate", "desc");
        return transactionHistoryRepository.findByOrganizationIdOrderByTransactionDateDesc(organizationId, pageable);
    }

    public TransactionHistory getTransactionById(String id) {
        return transactionHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
    }

    public TransactionHistory getTransactionByReferenceNumber(String referenceNumber) {
        return transactionHistoryRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "referenceNumber", referenceNumber));
    }

    public String generateReferenceNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = SEQUENCE.getAndIncrement();
        return String.format("TXN-%s-%05d", date, seq);
    }
}
