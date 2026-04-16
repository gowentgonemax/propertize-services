package com.propertize.commons.enums.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link RelationshipTypeEnum}.
 *
 * <p>
 * Stores the canonical enum name (e.g., {@code BROTHER}) in the database.
 * Reads values leniently via {@link RelationshipTypeEnum#fromString(String)},
 * which handles both enum names ({@code BROTHER}) and legacy display names
 * ({@code Brother}) that may already be stored in older rows.
 *
 * <p>
 * Use this converter instead of {@code @Enumerated(EnumType.STRING)} on any
 * entity field of type {@code RelationshipTypeEnum}.
 */
@Converter
public class RelationshipTypeEnumConverter implements AttributeConverter<RelationshipTypeEnum, String> {

    @Override
    public String convertToDatabaseColumn(RelationshipTypeEnum attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RelationshipTypeEnum convertToEntityAttribute(String dbData) {
        return RelationshipTypeEnum.fromString(dbData);
    }
}
