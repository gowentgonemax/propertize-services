package com.propertize.commons.enums.common;

import lombok.Getter;

/**
 * Canonical gender enum for employee/dependent records.
 *
 * <p>
 * Replaces magic {@code String gender} fields across services.
 */
@Getter
public enum GenderEnum {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-Binary"),
    PREFER_NOT_TO_SAY("Prefer Not to Say"),
    OTHER("Other");

    private final String displayName;

    GenderEnum(String displayName) {
        this.displayName = displayName;
    }
}
