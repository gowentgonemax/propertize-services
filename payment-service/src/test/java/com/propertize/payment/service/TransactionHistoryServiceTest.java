package com.propertize.payment.service;

import com.propertize.payment.entity.TransactionHistory;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.repository.TransactionHistoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionHistoryService Tests")
class TransactionHistoryServiceTest {

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    private TransactionHistoryService transactionHistoryService;

    private TransactionHistory sampleTxn;
    private static final String TXN_ID = "txn-001";
    private static final String ORG_ID = "org-001";
    private static final String REF_NUMBER = "TXN-20260327-001";

    @BeforeEach
    void setUp() {
        sampleTxn = new TransactionHistory();
        sampleTxn.setId(TXN_ID);
        sampleTxn.setOrganizationId(ORG_ID);
        sampleTxn.setReferenceNumber(REF_NUMBER);
        sampleTxn.setAmount(new BigDecimal("1200.00"));
    }

    @Test
    @DisplayName("Should return paginated transactions for organization")
    void shouldGetTransactionsByOrganization() {
        when(transactionHistoryRepository.findByOrganizationIdOrderByTransactionDateDesc(eq(ORG_ID),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleTxn)));

        Page<TransactionHistory> result = transactionHistoryService.getTransactionsByOrganization(ORG_ID, 1, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrganizationId()).isEqualTo(ORG_ID);
    }

    @Test
    @DisplayName("Should return transaction by ID")
    void shouldGetTransactionById() {
        when(transactionHistoryRepository.findById(TXN_ID)).thenReturn(Optional.of(sampleTxn));

        TransactionHistory result = transactionHistoryService.getTransactionById(TXN_ID);

        assertThat(result.getReferenceNumber()).isEqualTo(REF_NUMBER);
    }

    @Test
    @DisplayName("Should throw when transaction not found by ID")
    void shouldThrowWhenNotFoundById() {
        when(transactionHistoryRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionHistoryService.getTransactionById("bad-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should return transaction by reference number")
    void shouldGetByReferenceNumber() {
        when(transactionHistoryRepository.findByReferenceNumber(REF_NUMBER))
                .thenReturn(Optional.of(sampleTxn));

        TransactionHistory result = transactionHistoryService.getTransactionByReferenceNumber(REF_NUMBER);

        assertThat(result.getId()).isEqualTo(TXN_ID);
    }

    @Test
    @DisplayName("Should throw when transaction not found by reference number")
    void shouldThrowWhenNotFoundByRef() {
        when(transactionHistoryRepository.findByReferenceNumber("BAD-REF"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionHistoryService.getTransactionByReferenceNumber("BAD-REF"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should generate reference number with correct format")
    void shouldGenerateReferenceNumber() {
        String ref = transactionHistoryService.generateReferenceNumber();

        assertThat(ref).startsWith("TXN-");
        assertThat(ref).matches("TXN-\\d{8}-\\d+");
    }
}
