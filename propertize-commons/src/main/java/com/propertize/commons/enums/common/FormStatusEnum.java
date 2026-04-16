package com.propertize.commons.enums.common;

import lombok.Getter;

/**
 * Canonical form/document workflow status enum.
 *
 * <p>
 * Replaces magic {@code String formStatus = "DRAFT"} defaults in
 * W2FormEntity, QuarterlyTaxDepositEntity, and similar entities.
 */
@Getter
public enum FormStatusEnum {
    DRAFT("Draft"),
    PENDING_REVIEW("Pending Review"),
    SUBMITTED("Submitted"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    FILED("Filed"),
    AMENDED("Amended");

    private final String displayName;

    FormStatusEnum(String displayName) {
        this.displayName = displayName;
    }
}
