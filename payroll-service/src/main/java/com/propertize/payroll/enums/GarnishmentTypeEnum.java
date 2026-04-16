package com.propertize.payroll.enums;
/**
 * Types of court-ordered garnishments.
 * Replaces the free-text String garnishmentType on GarnishmentEntity.
 */
public enum GarnishmentTypeEnum {
    CHILD_SUPPORT,
    TAX_LEVY,
    CREDITOR,
    STUDENT_LOAN,
    BANKRUPTCY
}
