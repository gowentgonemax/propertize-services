package com.propertize.payroll.enums;
/**
 * Outcome status of a recorded payroll audit action.
 * Replaces the magic string "SUCCESS" default on PayrollAuditLogEntity#actionStatus.
 */
public enum PayrollAuditActionStatusEnum {
    SUCCESS,
    FAILURE
}
