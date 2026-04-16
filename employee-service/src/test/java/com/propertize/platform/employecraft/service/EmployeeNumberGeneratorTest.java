package com.propertize.platform.employecraft.service;

import com.propertize.platform.employecraft.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmployeeNumberGeneratorTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeNumberGenerator employeeNumberGenerator;

    private UUID organizationId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
    }

    @Test
    void generate_returnsNumberWithEmpPrefix() {
        String number = employeeNumberGenerator.generate(organizationId);
        assertThat(number).startsWith("EMP-");
    }

    @Test
    void generate_containsCurrentDate() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String number = employeeNumberGenerator.generate(organizationId);
        assertThat(number).contains(dateStr);
    }

    @Test
    void generate_containsFourDigitSequence() {
        String number = employeeNumberGenerator.generate(organizationId);
        // Format: EMP-YYYYMMDD-XXXX where XXXX is 4-digit sequence
        String[] parts = number.split("-");
        assertThat(parts).hasSize(3);
        assertThat(parts[2]).hasSize(4).matches("\\d{4}");
    }

    @Test
    void generate_producesUniqueNumbers() {
        String first = employeeNumberGenerator.generate(organizationId);
        String second = employeeNumberGenerator.generate(organizationId);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void generate_worksWithDifferentOrganizations() {
        UUID org1 = UUID.randomUUID();
        UUID org2 = UUID.randomUUID();
        String n1 = employeeNumberGenerator.generate(org1);
        String n2 = employeeNumberGenerator.generate(org2);
        assertThat(n1).isNotEqualTo(n2);
    }
}
