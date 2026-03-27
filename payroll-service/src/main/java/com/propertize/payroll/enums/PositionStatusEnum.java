package com.propertize.payroll.enums;

/**
 * Enum representing the status of a position.
 */
public enum PositionStatusEnum {
    /**
     * Position is active and can be assigned to employees.
     */
    ACTIVE,

    /**
     * Position is inactive and cannot be assigned to new employees.
     */
    INACTIVE,

    /**
     * Position is archived and no longer in use.
     */
    ARCHIVED
}
