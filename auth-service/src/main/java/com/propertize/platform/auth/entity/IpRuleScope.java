package com.propertize.platform.auth.entity;

/**
 * Defines the scope at which an IP access rule applies.
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
public enum IpRuleScope {

    /** Rule applies to all users globally */
    GLOBAL,

    /** Rule applies to a specific organization */
    ORGANIZATION,

    /** Rule applies to users with a specific role */
    ROLE,

    /** Rule applies to a specific user */
    USER
}
