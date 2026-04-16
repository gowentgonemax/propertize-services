package com.propertize.payroll.enums;

/**
 * Lifecycle status of a garnishment order.
 *
 * <p>Replaces the magic string {@code "ACTIVE"} default on {@code GarnishmentEntity#status}.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — currently being deducted from pay</li>
 *   <li>{@link #SUSPENDED} — temporarily on hold (e.g. pending court review)</li>
 *   <li>{@link #COMPLETED} — total amount fully paid</li>
 *   <li>{@link #TERMINATED} — order cancelled / rescinded</li>
 * </ul>
 */
public enum GarnishmentStatusEnum {
    ACTIVE,
    SUSPENDED,
    COMPLETED,
    TERMINATED
}

