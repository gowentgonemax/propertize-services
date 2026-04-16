package com.propertize.commons.enums.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * Canonical relationship type enum for emergency contacts, dependents, and
 * references.
 *
 * <p>
 * Replaces magic {@code String relationship} fields across services.
 */
@Getter
public enum RelationshipTypeEnum {
    SPOUSE("Spouse"),
    PARENT("Parent"),
    CHILD("Child"),
    SIBLING("Sibling"),
    BROTHER("Brother"),
    SISTER("Sister"),
    GRANDPARENT("Grandparent"),
    GRANDCHILD("Grandchild"),
    PARTNER("Domestic Partner"),
    FRIEND("Friend"),
    GUARDIAN("Legal Guardian"),
    OTHER("Other");

    private final String displayName;

    RelationshipTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Lenient factory used by Jackson deserialization and manual conversions.
     * Matches by enum name (case-insensitive) first, then by display name, and
     * falls back to {@link #OTHER} for unknown values instead of throwing.
     */
    @JsonCreator
    public static RelationshipTypeEnum fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        String normalised = value.trim();
        for (RelationshipTypeEnum v : values()) {
            if (v.name().equalsIgnoreCase(normalised) || v.displayName.equalsIgnoreCase(normalised)) {
                return v;
            }
        }
        return OTHER;
    }
}
