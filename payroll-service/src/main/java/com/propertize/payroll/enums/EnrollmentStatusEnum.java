package com.propertize.payroll.enums;

/**
 * Enum representing the status of a benefit enrollment.
 */
public enum EnrollmentStatusEnum {
    PENDING,     // Enrollment submitted, awaiting processing
    ACTIVE,      // Currently enrolled and active
    TERMINATED,  // Enrollment ended due to termination
    WAIVED,      // Employee waived enrollment
    CANCELLED,   // Enrollment cancelled before effective date
    EXPIRED      // Enrollment expired (e.g., plan ended)
}

