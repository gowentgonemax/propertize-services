package com.propertize.platform.auth.entity;

/**
 * Defines the type of IP access rule.
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
public enum IpRuleType {

    /** Allow access from matching IPs */
    WHITELIST,

    /** Deny access from matching IPs */
    BLACKLIST
}
