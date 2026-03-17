package com.propertize.platform.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA AttributeConverter for converting {@code List<String>} to/from
 * a comma-separated String stored in the database.
 *
 * <p>
 * Used by entities that store multi-valued string fields (e.g., role lists)
 * as a single delimited column rather than a join table.
 * </p>
 *
 * @version 1.0 - Phase 3: Dynamic Role Composition
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = ",";

    /**
     * Converts a {@code List<String>} to a comma-separated String for database
     * storage.
     *
     * @param attribute the list of strings to convert
     * @return comma-separated string, or null if the input is null or empty
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(DELIMITER));
    }

    /**
     * Converts a comma-separated String from the database back to a
     * {@code List<String>}.
     *
     * @param dbData the comma-separated string from the database
     * @return list of strings, or an empty list if the input is null or blank
     */
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(dbData.split(DELIMITER))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
