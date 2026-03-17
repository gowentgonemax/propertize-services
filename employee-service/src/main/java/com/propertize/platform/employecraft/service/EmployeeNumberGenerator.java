package com.propertize.platform.employecraft.service;

import com.propertize.platform.employecraft.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique employee numbers
 */
@Service
@RequiredArgsConstructor
public class EmployeeNumberGenerator {

    private final EmployeeRepository employeeRepository;
    private static final AtomicLong counter = new AtomicLong(0);

    public String generate(UUID organizationId) {
        String prefix = "EMP";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = String.format("%04d", counter.incrementAndGet() % 10000);
        return prefix + "-" + dateStr + "-" + seq;
    }
}
